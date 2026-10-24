
package com.syndeo.backend.security;

import com.syndeo.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            JdbcTemplate jdbcTemplate) {
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty() || !jwtService.isTokenValid(token)) {
            LOGGER.warn(
                    "Rejected bearer token for {} {}: token is empty, invalid, or expired.",
                    request.getMethod(),
                    request.getRequestURI());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        long userId;

        try {
            userId = jwtService.extractUserId(token);

            if (!isActiveUser(userId)) {
                LOGGER.warn(
                        "Rejected bearer token for {} {}: user account is inactive or missing.",
                        request.getMethod(),
                        request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
        } catch (IllegalArgumentException exception) {
            LOGGER.warn(
                    "Rejected bearer token for {} {}: user ID claim is invalid.",
                    request.getMethod(),
                    request.getRequestURI());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean isActiveUser(long userId) {
        try {
            Boolean active = jdbcTemplate.queryForObject(
                    "SELECT is_active FROM users WHERE id = ?",
                    (resultSet, rowNum) -> resultSet.getBoolean("is_active"),
                    userId
            );

            return Boolean.TRUE.equals(active);
        } catch (EmptyResultDataAccessException exception) {
            return false;
        }
    }
}
