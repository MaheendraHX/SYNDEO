package com.syndeo.backend.dto;

public record CircleMemberResponse(
        long userId,
        String displayName,
        String role
) {
}
