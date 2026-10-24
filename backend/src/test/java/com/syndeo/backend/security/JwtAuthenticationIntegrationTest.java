package com.syndeo.backend.security;

import com.syndeo.backend.dto.LoginRequest;
import com.syndeo.backend.dto.LoginResponse;
import com.syndeo.backend.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "SYNDEO_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "SYNDEO_JWT_EXPIRATION_MS=900000",
                "spring.datasource.url=jdbc:h2:mem:jwt-auth;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.flyway.enabled=false",
                "logging.level.org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration=OFF"
        })
@Import(JwtAuthenticationIntegrationTest.ProtectedTestController.class)
class JwtAuthenticationIntegrationTest {

    private static final String TEST_SECRET =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT PRIMARY KEY,
                    email VARCHAR(254) NOT NULL,
                    password_hash VARCHAR(100) NOT NULL,
                    is_active BOOLEAN NOT NULL
                )
                """);
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, is_active) VALUES (?, ?, ?, ?)",
                42L,
                "jwt-test@example.com",
                new BCryptPasswordEncoder().encode("TestPassword123!"),
                true);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, is_active) VALUES (?, ?, ?, ?)",
                43L,
                "inactive-test@example.com",
                new BCryptPasswordEncoder().encode("TestPassword123!"),
                false);
    }

    @Test
    void jwtFilterIsNotRegisteredAsASecondServletFilter() {
        long servletRegistrations = servletContext.getFilterRegistrations()
                .values()
                .stream()
                .filter(registration -> JwtAuthenticationFilter.class.getName()
                        .equals(registration.getClassName()))
                .count();

        assertEquals(0, servletRegistrations);
    }

    @Test
    void authenticatesRealJwtAndSetsAuthenticatedPrincipal() {
        String validToken = jwtService.generateToken(42L);
        ResponseEntity<Map> validResponse = request(validToken);
        assertEquals(200, validResponse.getStatusCode().value());
        assertEquals(42, validResponse.getBody().get("userId"));
        assertEquals(true, validResponse.getBody().get("authenticated"));
    }

    @Test
    void acceptsTokenIssuedByLoginThroughSameConfigurationAndDatabase()
            throws Exception {
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                new LoginRequest("jwt-test@example.com", "TestPassword123!"),
                LoginResponse.class);

        assertEquals(200, loginResponse.getStatusCode().value());
        assertEquals(42L, loginResponse.getBody().userId());
        assertEquals("Bearer", loginResponse.getBody().tokenType());

        ResponseEntity<Map> protectedResponse =
                request(loginResponse.getBody().accessToken());
        assertEquals(200, protectedResponse.getStatusCode().value());
        assertEquals(42, protectedResponse.getBody().get("userId"));
        assertEquals(true, protectedResponse.getBody().get("authenticated"));
    }

    @Test
    void rejectsMissingMalformedInvalidSignatureAndExpiredTokens() {
        assertEquals(401, request(null).getStatusCode().value());
        assertEquals(401, request("not-a-jwt").getStatusCode().value());
        assertEquals(401, request(tokenSignedWithDifferentKey()).getStatusCode().value());
        assertEquals(401, request(expiredToken()).getStatusCode().value());
    }

    @Test
    void rejectsInactiveAndNonexistentUsers() {
        assertEquals(401, request(jwtService.generateToken(43L))
                .getStatusCode().value());
        assertEquals(401, request(jwtService.generateToken(44L))
                .getStatusCode().value());
    }

    private ResponseEntity<Map> request(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                "http://localhost:" + port + "/api/protected-test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
    }

    private String expiredToken() {
        SecretKey signingKey = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(TEST_SECRET));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(signingKey)
                .compact();
    }

    private String tokenSignedWithDifferentKey() {
        byte[] otherSecret = new byte[32];
        otherSecret[0] = 1;
        SecretKey otherSigningKey = Keys.hmacShaKeyFor(otherSecret);

        return Jwts.builder()
                .subject("42")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(otherSigningKey)
                .compact();
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/api/protected-test")
        Map<String, Object> protectedResource(Authentication authentication) {
            return Map.of(
                    "userId", authentication.getPrincipal(),
                    "authenticated", authentication.isAuthenticated());
        }
    }
}
