package com.syndeo.backend.dto;

import java.util.List;

public record ProjectResponse(
        long id,
        Long circleId,
        Long createdBy,
        String name,
        String description,
        String status,
        int memberCount,
        boolean member,
        List<ProjectMemberResponse> members
) {
}
