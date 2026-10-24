package com.syndeo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TaskCreateRequest(
        @NotBlank(message = "Task title is required.")
        @Size(max = 200, message = "Task title must not exceed 200 characters.")
        String title,

        @Size(max = 3000, message = "Task description must not exceed 3000 characters.")
        String description,

        @Size(max = 24, message = "Task status must not exceed 24 characters.")
        String status,

        @Positive(message = "Assignee ID must be a positive number.")
        Long assigneeUserId
) {
}
