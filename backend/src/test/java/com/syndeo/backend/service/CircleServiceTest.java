package com.syndeo.backend.service;

import com.syndeo.backend.dto.CircleCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CircleServiceTest {

    private JdbcTemplate jdbcTemplate;
    private CircleService circleService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        circleService = new CircleService(jdbcTemplate);
    }

    @Test
    void blankCircleFieldsAreRejectedWithoutDatabaseAccess() {
        assertThrows(
                IllegalArgumentException.class,
                () -> circleService.createCircle(
                        3, new CircleCreateRequest(" ", "Study", "Description")));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void circleOwnerCannotLeave() {
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM circles WHERE id = ?"),
                eq(Integer.class),
                eq(9L)))
                .thenReturn(1);
        when(jdbcTemplate.queryForList(
                any(String.class), eq(String.class), eq(9L), eq(3L)))
                .thenReturn(java.util.List.of("OWNER"));

        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> circleService.leaveCircle(3, 9));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        verify(jdbcTemplate).queryForList(
                any(String.class), eq(String.class), eq(9L), eq(3L));
    }

    @Test
    void invalidCircleIdentifiersAreRejectedWithoutDatabaseAccess() {
        assertThrows(
                IllegalArgumentException.class,
                () -> circleService.joinCircle(3, 0));
        verifyNoInteractions(jdbcTemplate);
    }
}
