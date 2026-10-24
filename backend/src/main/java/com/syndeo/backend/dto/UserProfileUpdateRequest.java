
package com.syndeo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserProfileUpdateRequest(

        @NotBlank(message = "Display name is required.")
        @Size(max = 120, message = "Display name must not exceed 120 characters.")
        String displayName,

        @Size(max = 1000, message = "Bio must not exceed 1000 characters.")
        String bio,

        @Size(max = 40, message = "Experience level must not exceed 40 characters.")
        String experienceLevel,

        @Size(max = 1000, message = "Goal must not exceed 1000 characters.")
        String goals,

        @NotNull(message = "Skills must be provided.")
        @Size(min = 1, max = 50, message = "Provide between 1 and 50 skills.")
        List<
                @NotBlank(message = "Skill names cannot be blank.")
                @Size(max = 100, message = "Skill names must not exceed 100 characters.")
                String
        > skills,

        @NotNull(message = "Interests must be provided.")
        @Size(max = 50, message = "Provide at most 50 interests.")
        List<
                @NotBlank(message = "Interest names cannot be blank.")
                @Size(max = 100, message = "Interest names must not exceed 100 characters.")
                String
        > interests,

        @NotNull(message = "Availability must be provided.")
        @Size(max = 4, message = "Select at most 4 availability options.")
        List<
                @NotBlank(message = "Availability values cannot be blank.")
                @Size(max = 32, message = "Availability values must not exceed 32 characters.")
                String
        > availability

) {}
