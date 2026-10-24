package com.syndeo.backend.dto;

public record ProjectMemberResponse(
        long userId,
        String displayName,
        String role
) {
}
