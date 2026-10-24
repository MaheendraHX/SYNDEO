
package com.syndeo.backend.service;

import com.syndeo.backend.dto.LoginRequest;
import com.syndeo.backend.dto.LoginResponse;
import com.syndeo.backend.exception.InvalidCredentialsException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class AuthenticationService {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            JdbcTemplate jdbcTemplate,
            JwtService jwtService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        List<LoginUser> users = jdbcTemplate.query(
                """
                SELECT id, password_hash, is_active
                FROM users
                WHERE email = ?
                """,
                (resultSet, rowNum) -> new LoginUser(
                        resultSet.getLong("id"),
                        resultSet.getString("password_hash"),
                        resultSet.getBoolean("is_active")),
                email);

        if (users.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        LoginUser user = users.get(0);

        boolean passwordMatches = request.password()
                .getBytes(StandardCharsets.UTF_8).length <= 72
                && passwordEncoder.matches(
                        request.password(), user.passwordHash());

        if (!passwordMatches || !user.active()) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.id());

        return new LoginResponse(
                user.id(),
                token,
                "Bearer",
                jwtService.getExpirationSeconds());
    }

    private record LoginUser(
            long id,
            String passwordHash,
            boolean active) {
    }
}
