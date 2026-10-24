package com.syndeo.backend.service;

import com.syndeo.backend.dto.ProjectCreateRequest;
import com.syndeo.backend.dto.ProjectMemberResponse;
import com.syndeo.backend.dto.ProjectResponse;
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
import java.util.Set;

@Service
public class ProjectService {

    private static final Set<String> PROJECT_STATUSES =
            Set.of("PLANNING", "ACTIVE", "COMPLETED", "ARCHIVED");

    private final JdbcTemplate jdbcTemplate;

    public ProjectService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProjectResponse> listProjects(long userId) {
        return jdbcTemplate.query("""
                SELECT p.id, p.circle_id, p.created_by, p.name, p.description,
                       p.project_status, COUNT(pm.user_id) AS member_count,
                       MAX(CASE WHEN own.user_id IS NULL THEN 0 ELSE 1 END) AS is_member
                FROM projects p
                LEFT JOIN project_memberships pm ON pm.project_id = p.id
                LEFT JOIN project_memberships own
                       ON own.project_id = p.id AND own.user_id = ?
                GROUP BY p.id, p.circle_id, p.created_by, p.name, p.description,
                         p.project_status
                ORDER BY p.updated_at DESC, p.id DESC
                """,
                (rs, rowNum) -> new ProjectResponse(
                        rs.getLong("id"),
                        nullableLong(rs, "circle_id"),
                        nullableLong(rs, "created_by"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("project_status"),
                        rs.getInt("member_count"),
                        rs.getInt("is_member") == 1,
                        List.of()),
                userId);
    }

    public ProjectResponse getProject(long userId, long projectId) {
        requirePositiveId(projectId);
        List<ProjectResponse> projects = queryProject(userId, projectId);
        if (projects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
        }
        List<ProjectMemberResponse> members = jdbcTemplate.query("""
                SELECT pm.user_id, p.display_name, pm.membership_role
                FROM project_memberships pm
                LEFT JOIN profiles p ON p.user_id = pm.user_id
                WHERE pm.project_id = ?
                ORDER BY p.display_name, pm.user_id
                """,
                (rs, rowNum) -> new ProjectMemberResponse(
                        rs.getLong("user_id"),
                        rs.getString("display_name"),
                        rs.getString("membership_role")),
                projectId);

        ProjectResponse project = projects.get(0);
        return new ProjectResponse(
                project.id(), project.circleId(), project.createdBy(), project.name(),
                project.description(), project.status(), project.memberCount(),
                project.member(), members);
    }

    @Transactional
    public ProjectResponse createProject(long userId, ProjectCreateRequest request) {
        String name = request.name().trim();
        String description = request.description().trim();
        if (name.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException("Project name and description cannot be blank.");
        }

        Long circleId = request.circleId();
        if (circleId != null) {
            requirePositiveId(circleId);
            requireCircleMembership(userId, circleId);
        }
        String status = normalizeStatus(request.status());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO projects
                        (circle_id, created_by, name, description, project_status)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            if (circleId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, circleId);
            }
            statement.setLong(2, userId);
            statement.setString(3, name);
            statement.setString(4, description);
            statement.setString(5, status);
            return statement;
        }, keyHolder);
        if (inserted != 1 || keyHolder.getKey() == null) {
            throw new IllegalStateException("Project creation did not return a generated ID.");
        }

        long projectId = keyHolder.getKey().longValue();
        jdbcTemplate.update("""
                INSERT INTO project_memberships
                    (project_id, user_id, membership_role)
                VALUES (?, ?, 'OWNER')
                """, projectId, userId);
        return getProject(userId, projectId);
    }

    @Transactional
    public ProjectResponse joinProject(long userId, long projectId) {
        requirePositiveId(projectId);
        ProjectResponse project = getProjectMetadata(userId, projectId);
        if (project.circleId() != null) {
            requireCircleMembership(userId, project.circleId());
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO project_memberships
                        (project_id, user_id, membership_role)
                    VALUES (?, ?, 'MEMBER')
                    """, projectId, userId);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You are already a project member.", exception);
        }
        return getProject(userId, projectId);
    }

    @Transactional
    public ProjectResponse leaveProject(long userId, long projectId) {
        requirePositiveId(projectId);
        ProjectResponse project = getProjectMetadata(userId, projectId);
        if (project.createdBy() != null && project.createdBy() == userId) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The project owner cannot leave the project.");
        }
        List<String> roles = jdbcTemplate.queryForList("""
                SELECT membership_role
                FROM project_memberships
                WHERE project_id = ? AND user_id = ?
                """, String.class, projectId, userId);
        if (roles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You are not a project member.");
        }
        if ("OWNER".equals(roles.get(0))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The project owner cannot leave the project.");
        }
        jdbcTemplate.update("""
                DELETE FROM project_memberships
                WHERE project_id = ? AND user_id = ?
                """, projectId, userId);
        return getProject(userId, projectId);
    }

    public void requireProjectAccess(long userId, long projectId) {
        ProjectResponse project = getProjectMetadata(userId, projectId);
        if (!project.member() && (project.createdBy() == null
                || project.createdBy() != userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Project membership is required.");
        }
    }

    private ProjectResponse getProjectMetadata(long userId, long projectId) {
        List<ProjectResponse> projects = queryProject(userId, projectId);
        if (projects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
        }
        return projects.get(0);
    }

    private List<ProjectResponse> queryProject(long userId, long projectId) {
        return jdbcTemplate.query("""
                SELECT p.id, p.circle_id, p.created_by, p.name, p.description,
                       p.project_status,
                       (SELECT COUNT(*) FROM project_memberships pm
                        WHERE pm.project_id = p.id) AS member_count,
                       CASE WHEN own.user_id IS NULL THEN 0 ELSE 1 END AS is_member
                FROM projects p
                LEFT JOIN project_memberships own
                       ON own.project_id = p.id AND own.user_id = ?
                WHERE p.id = ?
                """,
                (rs, rowNum) -> new ProjectResponse(
                        rs.getLong("id"),
                        nullableLong(rs, "circle_id"),
                        nullableLong(rs, "created_by"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("project_status"),
                        rs.getInt("member_count"),
                        rs.getInt("is_member") == 1,
                        List.of()),
                userId, projectId);
    }

    private void requireCircleMembership(long userId, long circleId) {
        Integer activeMembership = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM circle_memberships
                WHERE circle_id = ? AND user_id = ?
                  AND membership_status = 'ACTIVE'
                """, Integer.class, circleId, userId);
        if (activeMembership == null || activeMembership == 0) {
            Integer circleCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM circles WHERE id = ?",
                    Integer.class, circleId);
            if (circleCount == null || circleCount == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found.");
            }
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active circle membership is required for this project.");
        }
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            return "PLANNING";
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Project status cannot be blank.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!PROJECT_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid project status.");
        }
        return normalized;
    }

    private void requirePositiveId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Project and circle IDs must be positive numbers.");
        }
    }

    private Long nullableLong(java.sql.ResultSet rs, String column)
            throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
