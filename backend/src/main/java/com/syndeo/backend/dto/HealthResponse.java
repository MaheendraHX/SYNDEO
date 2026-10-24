package com.syndeo.backend.dto;

import java.time.Instant;

import com.syndeo.backend.model.ServiceStatus;

public record HealthResponse(ServiceStatus status, Instant checkedAt) {
}
