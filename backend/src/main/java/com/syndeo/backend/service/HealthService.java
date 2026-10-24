package com.syndeo.backend.service;

import java.time.Clock;
import java.time.Instant;

import com.syndeo.backend.dto.HealthResponse;
import com.syndeo.backend.model.ServiceStatus;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final Clock clock;

    public HealthService(Clock clock) {
        this.clock = clock;
    }

    public HealthResponse getHealth() {
        return new HealthResponse(ServiceStatus.UP, Instant.now(clock));
    }
}
