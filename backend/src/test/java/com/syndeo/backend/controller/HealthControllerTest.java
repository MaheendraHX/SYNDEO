
package com.syndeo.backend.controller;

import com.syndeo.backend.config.ClockConfiguration;
import com.syndeo.backend.config.SecurityConfig;
import com.syndeo.backend.dto.HealthResponse;
import com.syndeo.backend.exception.DatabaseUnavailableException;
import com.syndeo.backend.model.ServiceStatus;
import com.syndeo.backend.service.DatabaseHealthService;
import com.syndeo.backend.service.HealthService;
import com.syndeo.backend.service.JwtService;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({
        ClockConfiguration.class,
        SecurityConfig.class,
        HealthControllerTest.ProtectedTestController.class
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @MockitoBean
    private DatabaseHealthService databaseHealthService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void reportsApplicationHealthWithoutDatabaseDetails() throws Exception {
        given(healthService.getHealth())
                .willReturn(new HealthResponse(
                        ServiceStatus.UP,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.checkedAt")
                        .value("2026-01-01T00:00:00Z"));
    }

    @Test
    void reportsSuccessfulDatabaseDiagnostic() throws Exception {
        given(databaseHealthService.getHealth())
                .willReturn(new HealthResponse(
                        ServiceStatus.UP,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/health/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void returnsSanitizedServiceUnavailableResponseWhenDatabaseIsDown()
            throws Exception {
        given(databaseHealthService.getHealth())
                .willThrow(new DatabaseUnavailableException());

        mockMvc.perform(get("/api/health/database"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code")
                        .value("DATABASE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message")
                        .value("The database connectivity check failed."));
    }

    @Test
    void rejectsProtectedRouteWithoutToken() throws Exception {
        mockMvc.perform(get("/api/protected-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsProtectedRouteWithInvalidToken() throws Exception {
        when(jwtService.isTokenValid("invalid-token"))
                .thenReturn(false);

        mockMvc.perform(get("/api/protected-test")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsValidTokenForActiveUser() throws Exception {
        when(jwtService.isTokenValid("valid-test-token"))
                .thenReturn(true);
        when(jwtService.extractUserId("valid-test-token"))
                .thenReturn(42L);
        when(jdbcTemplate.queryForObject(
                anyString(),
                org.mockito.ArgumentMatchers
                        .<org.springframework.jdbc.core.RowMapper<Boolean>>any(),
                eq(42L)))
                .thenReturn(true);

        mockMvc.perform(get("/api/protected-test")
                        .header("Authorization", "Bearer valid-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));

        verify(jdbcTemplate).queryForObject(
                anyString(),
                org.mockito.ArgumentMatchers
                        .<org.springframework.jdbc.core.RowMapper<Boolean>>any(),
                eq(42L));
    }

    @Test
    void rejectsValidTokenWhenUserIsInactive() throws Exception {
        when(jwtService.isTokenValid("inactive-test-token"))
                .thenReturn(true);
        when(jwtService.extractUserId("inactive-test-token"))
                .thenReturn(42L);
        when(jdbcTemplate.queryForObject(
                anyString(),
                org.mockito.ArgumentMatchers
                        .<org.springframework.jdbc.core.RowMapper<Boolean>>any(),
                eq(42L)))
                .thenReturn(false);

        mockMvc.perform(get("/api/protected-test")
                        .header("Authorization", "Bearer inactive-test-token"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    @RequestMapping("/api/protected-test")
    static class ProtectedTestController {

        @GetMapping
        Map<String, Object> protectedResource(Authentication authentication) {
            return Map.of("userId", authentication.getPrincipal());
        }
    }
}
