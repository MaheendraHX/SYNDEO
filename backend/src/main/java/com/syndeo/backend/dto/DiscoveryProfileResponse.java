package com.syndeo.backend.dto;

import java.util.List;

public record DiscoveryProfileResponse(
        long id,
        String name,
        String bio,
        String experience,
        String goal,
        List<String> skills,
        List<String> interests,
        List<String> availability
) {
}
