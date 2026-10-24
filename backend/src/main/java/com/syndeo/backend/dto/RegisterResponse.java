
package com.syndeo.backend.dto;

public record RegisterResponse(
        Long userId,
        String email,
        String displayName,
        String accountType,
        String message
) {}
