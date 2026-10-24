package com.syndeo.backend.dto;

public record TaskResponse(
        long id,
        long projectId,
        Long createdBy,
        Long assigneeUserId,
        String title,
        String description,
        String status
) {
}
