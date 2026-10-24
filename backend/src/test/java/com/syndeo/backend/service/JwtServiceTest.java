
package com.syndeo.backend.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final byte[] SECRET_BYTES = new byte[32];
    private static final String SECRET =
            Base64.getEncoder().encodeToString(SECRET_BYTES);

    private JwtService jwtService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000);
        signingKey = Keys.hmacShaKeyFor(SECRET_BYTES);
    }

    @Test
    void generatesTokenContainingUserId() {
        String token = jwtService.generateToken(42);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(42, jwtService.extractUserId(token));
        assertEquals(900, jwtService.getExpirationSeconds());
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = Instant.now();

        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(signingKey)
                .compact();

        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        byte[] otherSecretBytes = new byte[32];
        otherSecretBytes[0] = 1;
        SecretKey otherKey = Keys.hmacShaKeyFor(otherSecretBytes);

        String token = Jwts.builder()
                .subject("42")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void rejectsMalformedToken() {
        assertFalse(jwtService.isTokenValid("this-is-not-a-jwt"));
    }

    @Test
    void rejectsNonPositiveExpiration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(SECRET, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(SECRET, -1000)
        );
    }
}
