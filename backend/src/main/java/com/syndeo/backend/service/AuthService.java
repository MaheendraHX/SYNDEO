
package com.syndeo.backend.service;

import com.syndeo.backend.dto.RegisterRequest;
import com.syndeo.backend.dto.RegisterResponse;
import com.syndeo.backend.exception.DuplicateEmailException;
import com.syndeo.backend.exception.InvalidAccountTypeException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;

@Service
public class AuthService {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String displayName = request.displayName().trim();
        String accountType = request.accountType().trim().toUpperCase(Locale.ROOT);

        // BCrypt only supports passwords up to 72 UTF-8 bytes.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 bytes when encoded as UTF-8.");
        }

        if (displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Display name cannot be blank.");
        }

        if (!accountTypeExists(accountType)) {
            throw new InvalidAccountTypeException();
        }

        String passwordHash = passwordEncoder.encode(request.password());

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            int rowsInserted = jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users (email, password_hash) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);

                statement.setString(1, email);
                statement.setString(2, passwordHash);

                return statement;
            }, keyHolder);

            if (rowsInserted != 1 || keyHolder.getKey() == null) {
                throw new IllegalStateException(
                        "User creation did not return a generated ID.");
            }

            Long userId = keyHolder.getKey().longValue();

            jdbcTemplate.update(
                    "INSERT INTO profiles (user_id, display_name) VALUES (?, ?)",
                    userId,
                    displayName);

            jdbcTemplate.update(
                    "INSERT INTO user_account_types (user_id, account_type_code) VALUES (?, ?)",
                    userId,
                    accountType);

            return new RegisterResponse(
                    userId,
                    email,
                    displayName,
                    accountType,
                    "Registration successful.");

        } catch (DuplicateKeyException exception) {
            if (emailExists(email)) {
                throw new DuplicateEmailException();
            }

            throw exception;
        }
    }

    private boolean accountTypeExists(String accountType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_types WHERE code = ?",
                Integer.class,
                accountType);

        return count != null && count > 0;
    }

    private boolean emailExists(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email);

        return count != null && count > 0;
    }
}
