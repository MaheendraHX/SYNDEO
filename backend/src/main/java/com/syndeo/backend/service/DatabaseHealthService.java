package com.syndeo.backend.service;

import java.time.Clock;
import java.time.Instant;

import com.syndeo.backend.dto.HealthResponse;
import com.syndeo.backend.exception.DatabaseUnavailableException;
import com.syndeo.backend.model.ServiceStatus;
import com.syndeo.backend.repository.DatabaseHealthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHealthService.class);

    private final DatabaseHealthRepository repository;
    private final Clock clock;

    public DatabaseHealthService(DatabaseHealthRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public HealthResponse getHealth() {
        try {
            if (!repository.canExecuteDiagnosticQuery()) {
                throw new DatabaseUnavailableException();
            }
        } catch (DataAccessException exception) {
            LOGGER.warn("Database connectivity check failed.");
            throw new DatabaseUnavailableException();
        }
        return new HealthResponse(ServiceStatus.UP, Instant.now(clock));
    }
}
