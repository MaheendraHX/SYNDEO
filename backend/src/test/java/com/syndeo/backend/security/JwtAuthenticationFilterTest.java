package com.syndeo.backend.security;

import com.syndeo.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String TOKEN = "valid-token";

    private JwtService jwtService;
    private JdbcTemplate jdbcTemplate;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = mock(JwtService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        filter = new JwtAuthenticationFilter(jwtService, jdbcTemplate);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn(42L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAnActiveUserUsingBooleanStatus() throws Exception {
        when(jdbcTemplate.queryForObject(
                eq("SELECT is_active FROM users WHERE id = ?"),
                ArgumentMatchers.<RowMapper<Boolean>>any(),
                eq(42L)))
                .thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(
                42L,
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateAnInactiveUser() throws Exception {
        when(jdbcTemplate.queryForObject(
                eq("SELECT is_active FROM users WHERE id = ?"),
                ArgumentMatchers.<RowMapper<Boolean>>any(),
                eq(42L)))
                .thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
