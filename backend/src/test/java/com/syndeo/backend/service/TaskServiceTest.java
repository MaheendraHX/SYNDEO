package com.syndeo.backend.service;

import com.syndeo.backend.dto.TaskCreateRequest;
import com.syndeo.backend.dto.TaskUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TaskServiceTest {

    private JdbcTemplate jdbcTemplate;
    private ProjectService projectService;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        projectService = mock(ProjectService.class);
        taskService = new TaskService(jdbcTemplate, projectService);
    }

    @Test
    void taskListingRequiresProjectAuthorizationBeforeQueryingTasks() {
        ResponseStatusException forbidden =
                new ResponseStatusException(HttpStatus.FORBIDDEN);
        doThrow(forbidden).when(projectService).requireProjectAccess(7, 12);

        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> taskService.listTasks(7, 12));

        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void taskUpdatesRequireProjectAuthorizationBeforeQueryingTasks() {
        ResponseStatusException forbidden =
                new ResponseStatusException(HttpStatus.FORBIDDEN);
        doThrow(forbidden).when(projectService).requireProjectAccess(7, 12);

        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> taskService.updateTask(
                        7, 12, 31, new TaskUpdateRequest("done", null)));

        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void taskCreationRejectsUnknownStatusBeforeWriting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(
                        7, 12,
                        new TaskCreateRequest("Task", null, "blocked", null)));

        verify(projectService).requireProjectAccess(7, 12);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void taskUpdateRequiresAtLeastOneChange() {
        org.mockito.Mockito.when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.contains("SELECT COUNT(*) FROM tasks"),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq(31L),
                org.mockito.ArgumentMatchers.eq(12L)))
                .thenReturn(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(
                        7, 12, 31, new TaskUpdateRequest(null, null)));

        verify(projectService).requireProjectAccess(7, 12);
        verify(jdbcTemplate).queryForObject(
                org.mockito.ArgumentMatchers.contains("SELECT COUNT(*) FROM tasks"),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq(31L),
                org.mockito.ArgumentMatchers.eq(12L));
    }

    @Test
    void invalidNumericIdentifiersAreRejectedBeforeAuthorization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> taskService.updateTask(
                        7, 0, 31, new TaskUpdateRequest("done", null)));
        verifyNoInteractions(projectService, jdbcTemplate);
    }
}
