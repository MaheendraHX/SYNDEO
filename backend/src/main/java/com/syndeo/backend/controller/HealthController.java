package com.syndeo.backend.controller;

import com.syndeo.backend.dto.HealthResponse;
import com.syndeo.backend.service.DatabaseHealthService;
import com.syndeo.backend.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;
    private final DatabaseHealthService databaseHealthService;

    public HealthController(HealthService healthService, DatabaseHealthService databaseHealthService) {
        this.healthService = healthService;
        this.databaseHealthService = databaseHealthService;
    }

    @GetMapping
    public HealthResponse getHealth() {
        return healthService.getHealth();
    }

    @GetMapping("/database")
    public HealthResponse getDatabaseHealth() {
        return databaseHealthService.getHealth();
    }
}
