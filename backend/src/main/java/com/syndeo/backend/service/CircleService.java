package com.syndeo.backend.service;

import com.syndeo.backend.dto.CircleCreateRequest;
import com.syndeo.backend.dto.CircleMemberResponse;
import com.syndeo.backend.dto.CircleResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

@Service
public class CircleService {

    private final JdbcTemplate jdbcTemplate;

    public CircleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CircleResponse> listCircles(long userId) {
        return jdbcTemplate.query("""
                SELECT c.id, c.name, c.purpose, c.description, c.created_by,
                       COUNT(CASE WHEN cm.membership_status = 'ACTIVE'
                                  THEN 1 END) AS member_count,
                       own.membership_status
                FROM circles c
                LEFT JOIN circle_memberships cm ON cm.circle_id = c.id
                LEFT JOIN circle_memberships own
                       ON own.circle_id = c.id AND own.user_id = ?
                GROUP BY c.id, c.name, c.purpose, c.description, c.created_by,
                         own.membership_status
                ORDER BY c.name, c.id
                """,
                (rs, rowNum) -> new CircleResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("purpose"),
                        rs.getString("description"),
                        nullableLong(rs, "created_by"),
                        rs.getInt("member_count"),
                        rs.getString("membership_status"),
                        List.of()),
                userId);
    }

    public CircleResponse getCircle(long userId, long circleId) {
        requirePositiveId(circleId, "Circle ID");
        List<CircleResponse> circles = jdbcTemplate.query("""
                SELECT c.id, c.name, c.purpose, c.description, c.created_by,
                       (SELECT COUNT(*)
                        FROM circle_memberships cm
                        WHERE cm.circle_id = c.id
                          AND cm.membership_status = 'ACTIVE') AS member_count,
                       own.membership_status
                FROM circles c
                LEFT JOIN circle_memberships own
                       ON own.circle_id = c.id AND own.user_id = ?
                WHERE c.id = ?
                """,
                (rs, rowNum) -> new CircleResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("purpose"),
                        rs.getString("description"),
                        nullableLong(rs, "created_by"),
                        rs.getInt("member_count"),
                        rs.getString("membership_status"),
                        List.of()),
                userId, circleId);

        if (circles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found.");
        }

        List<CircleMemberResponse> members = jdbcTemplate.query("""
                SELECT cm.user_id, p.display_name, cm.membership_role
                FROM circle_memberships cm
                LEFT JOIN profiles p ON p.user_id = cm.user_id
                WHERE cm.circle_id = ? AND cm.membership_status = 'ACTIVE'
                ORDER BY p.display_name, cm.user_id
                """,
                (rs, rowNum) -> new CircleMemberResponse(
                        rs.getLong("user_id"),
                        rs.getString("display_name"),
                        rs.getString("membership_role")),
                circleId);

        CircleResponse circle = circles.get(0);
        return new CircleResponse(
                circle.id(), circle.name(), circle.purpose(), circle.description(),
                circle.createdBy(), circle.memberCount(), circle.membershipStatus(),
                members);
    }

    @Transactional
    public CircleResponse createCircle(long userId, CircleCreateRequest request) {
        String name = request.name().trim();
        String purpose = request.purpose().trim();
        String description = request.description().trim();
        if (name.isEmpty() || purpose.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException("Circle fields cannot be blank.");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO circles (created_by, name, purpose, description)
                    VALUES (?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, purpose);
            statement.setString(4, description);
            return statement;
        }, keyHolder);

        if (inserted != 1 || keyHolder.getKey() == null) {
            throw new IllegalStateException("Circle creation did not return a generated ID.");
        }

        long circleId = keyHolder.getKey().longValue();
        jdbcTemplate.update("""
                INSERT INTO circle_memberships
                    (circle_id, user_id, membership_role, membership_status)
                VALUES (?, ?, 'OWNER', 'ACTIVE')
                """, circleId, userId);
        return getCircle(userId, circleId);
    }

    @Transactional
    public CircleResponse joinCircle(long userId, long circleId) {
        requirePositiveId(circleId, "Circle ID");
        requireCircle(circleId);
        String status = membershipStatus(userId, circleId);
        if ("ACTIVE".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You are already an active circle member.");
        }

        if (status == null) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO circle_memberships
                            (circle_id, user_id, membership_role, membership_status)
                        VALUES (?, ?, 'MEMBER', 'ACTIVE')
                        """, circleId, userId);
            } catch (DuplicateKeyException exception) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Circle membership already exists.", exception);
            }
        } else {
            jdbcTemplate.update("""
                    UPDATE circle_memberships
                    SET membership_status = 'ACTIVE', membership_role = 'MEMBER'
                    WHERE circle_id = ? AND user_id = ?
                    """, circleId, userId);
        }
        return getCircle(userId, circleId);
    }

    @Transactional
    public CircleResponse leaveCircle(long userId, long circleId) {
        requirePositiveId(circleId, "Circle ID");
        requireCircle(circleId);
        List<String> roles = jdbcTemplate.queryForList("""
                SELECT membership_role
                FROM circle_memberships
                WHERE circle_id = ? AND user_id = ? AND membership_status = 'ACTIVE'
                """, String.class, circleId, userId);
        if (roles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You are not an active circle member.");
        }
        if ("OWNER".equals(roles.get(0))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The circle owner cannot leave the circle.");
        }

        jdbcTemplate.update("""
                UPDATE circle_memberships
                SET membership_status = 'LEFT'
                WHERE circle_id = ? AND user_id = ? AND membership_status = 'ACTIVE'
                """, circleId, userId);
        return getCircle(userId, circleId);
    }

    private void requireCircle(long circleId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM circles WHERE id = ?",
                Integer.class, circleId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found.");
        }
    }

    private String membershipStatus(long userId, long circleId) {
        List<String> statuses = jdbcTemplate.queryForList("""
                SELECT membership_status
                FROM circle_memberships
                WHERE circle_id = ? AND user_id = ?
                """, String.class, circleId, userId);
        return statuses.isEmpty() ? null : statuses.get(0).toUpperCase(Locale.ROOT);
    }

    private void requirePositiveId(long id, String label) {
        if (id <= 0) {
            throw new IllegalArgumentException(label + " must be a positive number.");
        }
    }

    private Long nullableLong(java.sql.ResultSet rs, String column)
            throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
