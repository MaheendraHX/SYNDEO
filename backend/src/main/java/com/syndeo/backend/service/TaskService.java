package com.syndeo.backend.service;

import com.syndeo.backend.dto.TaskCreateRequest;
import com.syndeo.backend.dto.TaskResponse;
import com.syndeo.backend.dto.TaskUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TaskService {

    private static final Set<String> TASK_STATUSES =
            Set.of("TODO", "IN_PROGRESS", "DONE");

    private final JdbcTemplate jdbcTemplate;
    private final ProjectService projectService;

    public TaskService(JdbcTemplate jdbcTemplate, ProjectService projectService) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectService = projectService;
    }

    public List<TaskResponse> listTasks(long userId, long projectId) {
        requirePositiveProjectId(projectId);
        projectService.requireProjectAccess(userId, projectId);
        return jdbcTemplate.query("""
                SELECT id, project_id, created_by, assignee_user_id,
                       title, description, task_status
                FROM tasks
                WHERE project_id = ?
                ORDER BY created_at, id
                """, this::mapTask, projectId);
    }

    @Transactional
    public TaskResponse createTask(
            long userId,
            long projectId,
            TaskCreateRequest request) {
        requirePositiveIds(projectId, 1);
        projectService.requireProjectAccess(userId, projectId);

        String title = request.title().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be blank.");
        }
        String description = normalizeOptional(request.description());
        String status = normalizeStatus(request.status(), "TODO");
        Long assigneeId = request.assigneeUserId();
        if (assigneeId != null) {
            requireProjectAssignee(projectId, assigneeId);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tasks
                        (project_id, created_by, assignee_user_id, title,
                         description, task_status)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, projectId);
            statement.setLong(2, userId);
            if (assigneeId == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, assigneeId);
            }
            statement.setString(4, title);
            statement.setString(5, description);
            statement.setString(6, status);
            return statement;
        }, keyHolder);

        if (inserted != 1 || keyHolder.getKey() == null) {
            throw new IllegalStateException("Task creation did not return a generated ID.");
        }
        return getTask(projectId, keyHolder.getKey().longValue());
    }

    @Transactional
    public TaskResponse updateTask(
            long userId,
            long projectId,
            long taskId,
            TaskUpdateRequest request) {
        requirePositiveIds(projectId, taskId);
        projectService.requireProjectAccess(userId, projectId);
        requireTask(projectId, taskId);

        String status = request.getStatus() == null
                ? null : normalizeStatus(request.getStatus(), null);
        Long assigneeId = request.getAssigneeUserId();
        boolean assigneeSpecified = request.isAssigneeSpecified();
        if (status == null && !assigneeSpecified) {
            throw new IllegalArgumentException(
                    "Provide a task status or assignee to update.");
        }
        if (assigneeSpecified && assigneeId != null) {
            requireProjectAssignee(projectId, assigneeId);
        }

        StringBuilder sql = new StringBuilder("UPDATE tasks SET ");
        if (status != null) {
            sql.append("task_status = ?");
        }
        if (assigneeSpecified) {
            if (status != null) {
                sql.append(", ");
            }
            sql.append("assignee_user_id = ?");
        }
        sql.append(" WHERE id = ? AND project_id = ?");

        List<Object> parameters = new ArrayList<>();
        if (status != null) {
            parameters.add(status);
        }
        if (assigneeSpecified) {
            parameters.add(assigneeId);
        }
        parameters.add(taskId);
        parameters.add(projectId);
        jdbcTemplate.update(sql.toString(), parameters.toArray());
        return getTask(projectId, taskId);
    }

    private TaskResponse getTask(long projectId, long taskId) {
        List<TaskResponse> tasks = jdbcTemplate.query("""
                SELECT id, project_id, created_by, assignee_user_id,
                       title, description, task_status
                FROM tasks
                WHERE id = ? AND project_id = ?
                """, this::mapTask, taskId, projectId);
        if (tasks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found.");
        }
        return tasks.get(0);
    }

    private void requireTask(long projectId, long taskId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tasks WHERE id = ? AND project_id = ?
                """, Integer.class, taskId, projectId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found.");
        }
    }

    private void requireProjectAssignee(long projectId, long assigneeId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM users u
                JOIN project_memberships pm ON pm.user_id = u.id
                WHERE u.id = ? AND u.is_active = TRUE AND pm.project_id = ?
                """, Integer.class, assigneeId, projectId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException(
                    "Assignee must be an active member of the project.");
        }
    }

    private TaskResponse mapTask(java.sql.ResultSet rs, int rowNum)
            throws java.sql.SQLException {
        return new TaskResponse(
                rs.getLong("id"),
                rs.getLong("project_id"),
                nullableLong(rs, "created_by"),
                nullableLong(rs, "assignee_user_id"),
                rs.getString("title"),
                rs.getString("description"),
                toFrontendStatus(rs.getString("task_status")));
    }

    private String normalizeStatus(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Task status cannot be blank.");
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("TO_DO".equals(normalized)) {
            normalized = "TODO";
        }
        if (!TASK_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid task status.");
        }
        return normalized;
    }

    private String toFrontendStatus(String status) {
        return switch (status) {
            case "TODO" -> "to-do";
            case "IN_PROGRESS" -> "in-progress";
            case "DONE" -> "done";
            default -> throw new IllegalStateException(
                    "Unexpected task status stored in the database.");
        };
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void requirePositiveIds(long projectId, long taskId) {
        requirePositiveProjectId(projectId);
        if (taskId <= 0) {
            throw new IllegalArgumentException("Task ID must be a positive number.");
        }
    }

    private void requirePositiveProjectId(long projectId) {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project and task IDs must be positive numbers.");
        }
    }

    private Long nullableLong(java.sql.ResultSet rs, String column)
            throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
