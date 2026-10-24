package com.syndeo.backend.exception;

public class DatabaseUnavailableException extends RuntimeException {

    public DatabaseUnavailableException() {
        super("Database connectivity check failed.");
    }
}
