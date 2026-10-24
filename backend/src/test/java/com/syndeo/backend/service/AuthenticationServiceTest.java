
package com.syndeo.backend.service;

import com.syndeo.backend.dto.LoginRequest;
import com.syndeo.backend.dto.LoginResponse;
import com.syndeo.backend.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private JwtService jwtService;
    private AuthenticationService authenticationService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        jwtService = mock(JwtService.class);
        authenticationService =
                new AuthenticationService(jdbcTemplate, jwtService);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    private void mockUser(
            String email, long id, String hash, boolean active) {

        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq(email)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);

                    when(rs.getLong("id")).thenReturn(id);
                    when(rs.getString("password_hash")).thenReturn(hash);
                    when(rs.getBoolean("is_active")).thenReturn(active);

                    try {
                        return List.of(mapper.mapRow(rs, 0));
                    } catch (java.sql.SQLException exception) {
                        throw new RuntimeException(exception);
                    }
                });
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        String password = "TestPassword123!";

        mockUser(
                "student@example.com",
                42L,
                passwordEncoder.encode(password),
                true);

        when(jwtService.generateToken(42L))
                .thenReturn("test.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        LoginResponse response = authenticationService.login(
                new LoginRequest(" Student@Example.com ", password));

        assertEquals(42L, response.userId());
        assertEquals("test.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        verify(jwtService).generateToken(42L);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq("unknown@example.com")))
                .thenReturn(List.of());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(
                                "unknown@example.com",
                                "AnyPassword123!")));

        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    void loginRejectsIncorrectPassword() {
        mockUser(
                "student@example.com",
                42L,
                passwordEncoder.encode("CorrectPassword123!"),
                true);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(
                                "student@example.com",
                                "WrongPassword123!")));

        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    void loginRejectsInactiveAccount() {
        mockUser(
                "student@example.com",
                42L,
                passwordEncoder.encode("TestPassword123!"),
                false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(
                                "student@example.com",
                                "TestPassword123!")));

        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    void loginRejectsPasswordLongerThanBcryptLimit() {
        mockUser(
                "student@example.com",
                42L,
                passwordEncoder.encode("another-password"),
                true);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(
                                "student@example.com",
                                "a".repeat(73))));

        verify(jwtService, never()).generateToken(anyLong());
    }
}
