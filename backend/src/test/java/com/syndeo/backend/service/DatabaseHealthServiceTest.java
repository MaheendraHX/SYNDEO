package com.syndeo.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.syndeo.backend.exception.DatabaseUnavailableException;
import com.syndeo.backend.model.ServiceStatus;
import com.syndeo.backend.repository.DatabaseHealthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthServiceTest {

    private static final Instant CHECKED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private DatabaseHealthRepository repository;

    @Test
    void reportsUpWhenDiagnosticQuerySucceeds() {
        given(repository.canExecuteDiagnosticQuery()).willReturn(true);

        var response = service().getHealth();

        assertEquals(ServiceStatus.UP, response.status());
        assertEquals(CHECKED_AT, response.checkedAt());
    }

    @Test
    void reportsUnavailableWhenDiagnosticQueryReturnsUnexpectedResult() {
        given(repository.canExecuteDiagnosticQuery()).willReturn(false);

        assertThrows(DatabaseUnavailableException.class, service()::getHealth);
    }

    @Test
    void reportsUnavailableWhenDatabaseAccessFails() {
        given(repository.canExecuteDiagnosticQuery())
                .willThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(DatabaseUnavailableException.class, service()::getHealth);
    }

    private DatabaseHealthService service() {
        return new DatabaseHealthService(repository, fixedClock());
    }

    private Clock fixedClock() {
        return Clock.fixed(CHECKED_AT, ZoneOffset.UTC);
    }
}
