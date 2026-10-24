package com.syndeo.backend.service;

import com.syndeo.backend.dto.DiscoveryProfileResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DiscoveryService {

    private final JdbcTemplate jdbcTemplate;

    public DiscoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DiscoveryProfileResponse> searchProfiles(
            long requesterId,
            String search,
            String skill,
            String interest) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.user_id, p.display_name, p.bio, p.experience_level, p.goals
                FROM profiles p
                JOIN users u ON u.id = p.user_id
                WHERE u.is_active = TRUE AND p.user_id <> ?
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(requesterId);

        String normalizedSearch = normalize(search);
        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     AND (
                         LOWER(p.display_name) LIKE ?
                         OR LOWER(COALESCE(p.bio, '')) LIKE ?
                         OR LOWER(COALESCE(p.experience_level, '')) LIKE ?
                         OR LOWER(COALESCE(p.goals, '')) LIKE ?
                         OR EXISTS (
                             SELECT 1
                             FROM user_skills us
                             JOIN skills s ON s.id = us.skill_id
                             WHERE us.user_id = p.user_id AND LOWER(s.name) LIKE ?
                         )
                         OR EXISTS (
                             SELECT 1
                             FROM user_interests ui
                             JOIN interests i ON i.id = ui.interest_id
                             WHERE ui.user_id = p.user_id AND LOWER(i.name) LIKE ?
                         )
                     )
                    """);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }

        String normalizedSkill = normalize(skill);
        if (normalizedSkill != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM user_skills us
                         JOIN skills s ON s.id = us.skill_id
                         WHERE us.user_id = p.user_id AND LOWER(s.name) = ?
                     )
                    """);
            parameters.add(normalizedSkill.toLowerCase(Locale.ROOT));
        }

        String normalizedInterest = normalize(interest);
        if (normalizedInterest != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM user_interests ui
                         JOIN interests i ON i.id = ui.interest_id
                         WHERE ui.user_id = p.user_id AND LOWER(i.name) = ?
                     )
                    """);
            parameters.add(normalizedInterest.toLowerCase(Locale.ROOT));
        }

        sql.append(" ORDER BY p.display_name, p.user_id");
        List<DiscoveryProfileResponse> profiles = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new DiscoveryProfileResponse(
                        rs.getLong("user_id"),
                        rs.getString("display_name"),
                        rs.getString("bio"),
                        rs.getString("experience_level"),
                        rs.getString("goals"),
                        List.of(),
                        List.of(),
                        List.of()),
                parameters.toArray());

        return profiles.stream()
                .map(profile -> new DiscoveryProfileResponse(
                        profile.id(),
                        profile.name(),
                        profile.bio(),
                        profile.experience(),
                        profile.goal(),
                        jdbcTemplate.queryForList("""
                                SELECT s.name
                                FROM user_skills us
                                JOIN skills s ON s.id = us.skill_id
                                WHERE us.user_id = ?
                                ORDER BY s.name
                                """, String.class, profile.id()),
                        jdbcTemplate.queryForList("""
                                SELECT i.name
                                FROM user_interests ui
                                JOIN interests i ON i.id = ui.interest_id
                                WHERE ui.user_id = ?
                                ORDER BY i.name
                                """, String.class, profile.id()),
                        jdbcTemplate.queryForList("""
                                SELECT ao.display_name
                                FROM profile_availability pa
                                JOIN availability_options ao
                                  ON ao.code = pa.availability_code
                                WHERE pa.user_id = ?
                                ORDER BY pa.availability_code
                                """, String.class, profile.id())))
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
