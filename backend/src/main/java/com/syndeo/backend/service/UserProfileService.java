
package com.syndeo.backend.service;

import com.syndeo.backend.dto.UserProfileResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserProfileService {

    private final JdbcTemplate jdbcTemplate;

    public UserProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProfileResponse getProfile(long userId) {
        List<UserProfileResponse> profiles = jdbcTemplate.query(
                """
                SELECT
                    u.id,
                    u.email,
                    p.display_name,
                    uat.account_type_code,
                    p.bio,
                    p.experience_level,
                    p.goals
                FROM users u
                JOIN profiles p ON p.user_id = u.id
                JOIN user_account_types uat ON uat.user_id = u.id
                WHERE u.id = ?
                """,
                (rs, rowNum) -> new UserProfileResponse(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("account_type_code"),
                        rs.getString("bio"),
                        rs.getString("experience_level"),
                        rs.getString("goals"),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                userId
        );

        if (profiles.isEmpty()) {
            throw new IllegalStateException(
                    "Profile not found for the authenticated user."
            );
        }

        UserProfileResponse profile = profiles.get(0);

        List<String> skills = jdbcTemplate.queryForList(
                """
                SELECT s.name
                FROM user_skills us
                JOIN skills s ON s.id = us.skill_id
                WHERE us.user_id = ?
                ORDER BY s.name
                """,
                String.class,
                userId
        );

        List<String> interests = jdbcTemplate.queryForList(
                """
                SELECT i.name
                FROM user_interests ui
                JOIN interests i ON i.id = ui.interest_id
                WHERE ui.user_id = ?
                ORDER BY i.name
                """,
                String.class,
                userId
        );

        List<String> availability = jdbcTemplate.queryForList(
                """
                SELECT availability_code
                FROM profile_availability
                WHERE user_id = ?
                ORDER BY availability_code
                """,
                String.class,
                userId
        );

        return new UserProfileResponse(
                profile.userId(),
                profile.email(),
                profile.displayName(),
                profile.accountType(),
                profile.bio(),
                profile.experienceLevel(),
                profile.goals(),
                skills,
                interests,
                availability
        );
    }
}
