
package com.syndeo.backend.dto;

public record LoginResponse(
        long userId,
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
