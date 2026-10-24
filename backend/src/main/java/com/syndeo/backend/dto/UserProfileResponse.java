
package com.syndeo.backend.dto;

import java.util.List;

public record UserProfileResponse(
        long userId,
        String email,
        String displayName,
        String accountType,
        String bio,
        String experienceLevel,
        String goals,
        List<String> skills,
        List<String> interests,
        List<String> availability
) {
}
