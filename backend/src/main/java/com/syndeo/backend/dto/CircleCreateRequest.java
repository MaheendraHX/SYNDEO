package com.syndeo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CircleCreateRequest(
        @NotBlank(message = "Circle name is required.")
        @Size(max = 120, message = "Circle name must not exceed 120 characters.")
        String name,

        @NotBlank(message = "Circle purpose is required.")
        @Size(max = 40, message = "Circle purpose must not exceed 40 characters.")
        String purpose,

        @NotBlank(message = "Circle description is required.")
        @Size(max = 2000, message = "Circle description must not exceed 2000 characters.")
        String description
) {
}
