
package com.syndeo.backend.service;

import com.syndeo.backend.dto.RegisterRequest;
import com.syndeo.backend.dto.RegisterResponse;
import com.syndeo.backend.exception.DuplicateEmailException;
import com.syndeo.backend.exception.InvalidAccountTypeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(jdbcTemplate);
    }

    @Test
    void registerCreatesUserProfileAndAccountType() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "  STUDENT@example.com ",
                "SecurePass123!",
                "  Alex Student  ",
                "student"
        );

        when(jdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM account_types"),
                eq(Integer.class),
                eq("STUDENT")
        )).thenReturn(1);

        when(jdbcTemplate.update(
                any(org.springframework.jdbc.core.PreparedStatementCreator.class),
                any(KeyHolder.class)
        )).thenAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(
                    java.util.Map.of("GENERATED_KEY", 42L));
            return 1;
        });

        when(jdbcTemplate.update(
        contains("INSERT INTO profiles"),
        eq(42L),
        eq("Alex Student")
)).thenReturn(1);

when(jdbcTemplate.update(
        contains("INSERT INTO user_account_types"),
        eq(42L),
        eq("STUDENT")
)).thenReturn(1);

        RegisterResponse response = authService.register(request);

        assertEquals(42L, response.userId());
        assertEquals("student@example.com", response.email());
        assertEquals("Alex Student", response.displayName());
        assertEquals("STUDENT", response.accountType());
        assertEquals("Registration successful.", response.message());

        ArgumentCaptor<org.springframework.jdbc.core.PreparedStatementCreator>
                creatorCaptor =
                ArgumentCaptor.forClass(
                        org.springframework.jdbc.core.PreparedStatementCreator.class);

        verify(jdbcTemplate).update(
                creatorCaptor.capture(), any(KeyHolder.class));

        try (Connection connection = mock(Connection.class);
             PreparedStatement statement = mock(PreparedStatement.class)) {
            when(connection.prepareStatement(
                    anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                    .thenReturn(statement);

            creatorCaptor.getValue().createPreparedStatement(connection);

            verify(statement).setString(1, "student@example.com");
            ArgumentCaptor<String> hashCaptor =
                    ArgumentCaptor.forClass(String.class);
            verify(statement).setString(eq(2), hashCaptor.capture());

            String storedHash = hashCaptor.getValue();
            assertNotEquals("SecurePass123!", storedHash);
            assertTrue(new BCryptPasswordEncoder().matches(
                    "SecurePass123!", storedHash));
        }

        verify(jdbcTemplate).update(
                contains("INSERT INTO profiles"),
                eq(42L),
                eq("Alex Student"));

        verify(jdbcTemplate).update(
                contains("INSERT INTO user_account_types"),
                eq(42L),
                eq("STUDENT"));
    }

    @Test
    void registerRejectsUnknownAccountType() {
        RegisterRequest request = new RegisterRequest(
                "student@example.com",
                "SecurePass123!",
                "Alex",
                "UNKNOWN"
        );

        when(jdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM account_types"),
                eq(Integer.class),
                eq("UNKNOWN")
        )).thenReturn(0);

        assertThrows(
                InvalidAccountTypeException.class,
                () -> authService.register(request));

        verify(jdbcTemplate, never()).update(
                contains("INSERT INTO users"),
                any(),
                any());
    }

    @Test
    void registerRejectsPasswordLongerThan72Utf8Bytes() {
        String password = "é".repeat(37);

        assertTrue(password.getBytes(StandardCharsets.UTF_8).length > 72);

        RegisterRequest request = new RegisterRequest(
                "student@example.com",
                password,
                "Alex",
                "STUDENT"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "student@example.com",
                "SecurePass123!",
                "Alex",
                "STUDENT"
        );

        when(jdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM account_types"),
                eq(Integer.class),
                eq("STUDENT")
        )).thenReturn(1);

        when(jdbcTemplate.update(
                any(org.springframework.jdbc.core.PreparedStatementCreator.class),
                any(KeyHolder.class)
        )).thenThrow(new DuplicateKeyException("Duplicate email"));

        when(jdbcTemplate.queryForObject(
                contains("SELECT COUNT(*) FROM users"),
                eq(Integer.class),
                eq("student@example.com")
        )).thenReturn(1);

        assertThrows(
                DuplicateEmailException.class,
                () -> authService.register(request));
    }
}
