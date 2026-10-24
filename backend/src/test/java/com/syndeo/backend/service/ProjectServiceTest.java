package com.syndeo.backend.service;

import com.syndeo.backend.dto.ProjectCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ProjectServiceTest {

    private JdbcTemplate jdbcTemplate;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        projectService = new ProjectService(jdbcTemplate);
    }

    @Test
    void unknownProjectStatusIsRejectedBeforeWriting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.createProject(
                        4,
                        new ProjectCreateRequest(
                                null, "Project", "Description", "ON_HOLD")));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void invalidCircleIdentifierIsRejectedBeforeDatabaseAccess() {
        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.createProject(
                        4,
                        new ProjectCreateRequest(
                                0L, "Project", "Description", "PLANNING")));

        verifyNoInteractions(jdbcTemplate);
    }
}
