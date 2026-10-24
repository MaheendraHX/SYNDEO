package com.syndeo.backend.dto;

import java.util.List;

public record CircleResponse(
        long id,
        String name,
        String purpose,
        String description,
        Long createdBy,
        int memberCount,
        String membershipStatus,
        List<CircleMemberResponse> members
) {
}
