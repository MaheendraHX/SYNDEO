
package com.syndeo.backend.service;

import com.syndeo.backend.dto.UserProfileUpdateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserProfileUpdateService {

    private final JdbcTemplate jdbcTemplate;

    public UserProfileUpdateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void updateProfile(long userId, UserProfileUpdateRequest request) {
        String displayName = request.displayName().trim();
        String bio = normalizeOptional(request.bio());
        String experience = normalizeOptional(request.experienceLevel());
        String goals = normalizeOptional(request.goals());

        int updated = jdbcTemplate.update(
                """
                UPDATE profiles
                SET display_name = ?, bio = ?, experience_level = ?, goals = ?
                WHERE user_id = ?
                """,
                displayName, bio, experience, goals, userId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Profile not found for the authenticated user."
            );
        }

        List<String> skills = normalizeNames(request.skills(), "skill");
        List<String> interests = normalizeNames(request.interests(), "interest");
        List<String> availability = normalizeAvailability(request.availability());

        // Validate all availability codes before changing associations.
        for (String code : availability) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM availability_options WHERE code = ?",
                    Integer.class,
                    code
            );

            if (count == null || count != 1) {
                throw new IllegalArgumentException(
                        "Invalid availability option: " + code
                );
            }
        }

        // Replace only this user's associations; shared lookup rows are retained.
        jdbcTemplate.update("DELETE FROM user_skills WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_interests WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM profile_availability WHERE user_id = ?", userId);

        for (String skill : skills) {
            jdbcTemplate.update(
                    """
                    INSERT INTO skills (name)
                    VALUES (?)
                    ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
                    """,
                    skill
            );

            Long skillId = jdbcTemplate.queryForObject(
                    "SELECT id FROM skills WHERE name = ?",
                    Long.class,
                    skill
            );

            jdbcTemplate.update(
                    """
                    INSERT INTO user_skills (user_id, skill_id)
                    VALUES (?, ?)
                    """,
                    userId, skillId
            );
        }

        for (String interest : interests) {
            jdbcTemplate.update(
                    """
                    INSERT INTO interests (name)
                    VALUES (?)
                    ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
                    """,
                    interest
            );

            Long interestId = jdbcTemplate.queryForObject(
                    "SELECT id FROM interests WHERE name = ?",
                    Long.class,
                    interest
            );

            jdbcTemplate.update(
                    """
                    INSERT INTO user_interests (user_id, interest_id)
                    VALUES (?, ?)
                    """,
                    userId, interestId
            );
        }

        for (String code : availability) {
            jdbcTemplate.update(
                    """
                    INSERT INTO profile_availability (user_id, availability_code)
                    VALUES (?, ?)
                    """,
                    userId, code
            );
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeNames(List<String> values, String label) {
        Set<String> unique = new LinkedHashSet<>();

        for (String value : values) {
            String normalized = value.trim().replaceAll("\\s+", " ");

            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(
                        "A " + label + " cannot be blank."
                );
            }

            unique.add(normalized);
        }

        if ("skill".equals(label) && unique.isEmpty()) {
            throw new IllegalArgumentException(
                    "Add at least one skill to your profile."
            );
        }

        return new ArrayList<>(unique);
    }

    private List<String> normalizeAvailability(List<String> values) {
        Set<String> unique = new LinkedHashSet<>();

        for (String value : values) {
            unique.add(value.trim().toUpperCase(Locale.ROOT));
        }

        return new ArrayList<>(unique);
    }
}
