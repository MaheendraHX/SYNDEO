package com.syndeo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        Long circleId,

        @NotBlank(message = "Project name is required.")
        @Size(max = 160, message = "Project name must not exceed 160 characters.")
        String name,

        @NotBlank(message = "Project description is required.")
        @Size(max = 3000, message = "Project description must not exceed 3000 characters.")
        String description,

        @Size(max = 24, message = "Project status must not exceed 24 characters.")
        String status
) {
}
