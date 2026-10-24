package com.syndeo.backend;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaMigrationTest {

    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE\\s+TABLE\\s+([a-z_]+)", Pattern.CASE_INSENSITIVE);

    @Test
    void migrationDefinesRequiredNormalizedTablesWithoutDestructiveStatements() throws IOException {
        String initialMigration = readMigration("V1__initial_schema.sql");
        String recommendationMigration = readMigration("V2__recommendation_logs.sql");

        assertEquals(Set.of(
                "users", "account_types", "user_account_types",
                "profiles", "availability_options", "profile_availability",
                "skills", "user_skills", "interests", "user_interests",
                "circles", "circle_memberships", "projects", "project_memberships",
                "tasks"), tableNames(initialMigration));
        assertEquals(Set.of("recommendation_logs"), tableNames(recommendationMigration));
        assertNoDestructiveStatements(initialMigration);
        assertNoDestructiveStatements(recommendationMigration);
        assertTrue(initialMigration.contains("FOREIGN KEY"));
        assertTrue(recommendationMigration.contains("explanation JSON NOT NULL"));
        assertTrue(recommendationMigration.contains("algorithm_version"));
        assertTrue(initialMigration.contains("INSERT INTO account_types"));
        assertTrue(initialMigration.contains("INSERT INTO availability_options"));
    }

    @Test
    void recommendationLogsEnforceDistinctUsersAndKeepSafeForeignKeyActions() throws IOException {
        String migration = readMigration("V2__recommendation_logs.sql")
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        assertTrue(migration.contains(
                "constraint chk_recommendation_score check (score >= 0 and score <= 100)"));
        assertFalse(migration.contains("chk_recommendation_distinct_users"));
        assertTrue(migration.contains(
                "foreign key (requester_user_id) references users (id) on delete cascade on update restrict"));
        assertTrue(migration.contains(
                "foreign key (candidate_user_id) references users (id) on delete cascade on update restrict"));
        assertEquals(2, Pattern.compile("create trigger ").matcher(migration).results().count());
        assertTrue(migration.contains(
                "create trigger bi_recommendation_logs_distinct_users before insert on recommendation_logs "
                        + "for each row begin if new.requester_user_id = new.candidate_user_id then "
                        + "signal sqlstate '45000' set message_text = 'a requester cannot be recommended to "
                        + "themselves'; end if; end"));
        assertTrue(migration.contains(
                "create trigger bu_recommendation_logs_distinct_users before update on recommendation_logs "
                        + "for each row begin if new.requester_user_id = new.candidate_user_id then "
                        + "signal sqlstate '45000' set message_text = 'a requester cannot be recommended to "
                        + "themselves'; end if; end"));
    }

    @Test
    void migrationDefinesExpectedRelationshipAndStatusConstraints() throws IOException {
        String migration = readMigration("V1__initial_schema.sql")
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        assertTrue(migration.contains(
                "foreign key (user_id) references profiles (user_id) on delete cascade on update cascade"));
        assertTrue(migration.contains(
                "foreign key (circle_id) references circles (id) on delete cascade on update cascade"));
        assertTrue(migration.contains(
                "foreign key (project_id) references projects (id) on delete cascade on update cascade"));
        assertTrue(migration.contains(
                "foreign key (created_by) references users (id) on delete set null on update cascade"));
        assertTrue(migration.contains(
                "check (membership_status in ('pending', 'active', 'left'))"));
        assertTrue(migration.contains(
                "check (project_status in ('planning', 'active', 'completed', 'archived'))"));
        assertTrue(migration.contains(
                "check (task_status in ('todo', 'in_progress', 'done'))"));
    }

    private Set<String> tableNames(String migration) {
        Matcher matcher = CREATE_TABLE.matcher(migration);
        Set<String> tables = new java.util.HashSet<>();
        while (matcher.find()) {
            tables.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tables;
    }

    private void assertNoDestructiveStatements(String migration) {
        assertFalse(migration.matches("(?is).*\\b(DROP\\s+DATABASE|DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b.*"));
    }

    private String readMigration(String fileName) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/db/migration/" + fileName)) {
            assertTrue(stream != null, fileName + " migration must be on the classpath");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
