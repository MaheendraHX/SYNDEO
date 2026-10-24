
package com.syndeo.backend.exception;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

import com.syndeo.backend.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.syndeo.backend.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(DatabaseUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseUnavailable() {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_UNAVAILABLE",
                "The database connectivity check failed.");
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception) {
        return error(
                HttpStatus.CONFLICT,
                "DUPLICATE_EMAIL",
                exception.getMessage());
    }

    @ExceptionHandler(InvalidAccountTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAccountType(
            InvalidAccountTypeException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_ACCOUNT_TYPE",
                exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT",
                exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null
                ? status.getReasonPhrase() : exception.getReason();
        return error(status, status.name(), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));

        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {

        Throwable cause = exception.getMostSpecificCause();

        LOGGER.warn(
                "Unreadable request body: {} — {}",
                cause.getClass().getSimpleName(),
                cause.getMessage());

        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "The request body is missing or malformed.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid email or password.");
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status, String code, String message) {

        ApiErrorResponse response = new ApiErrorResponse(
                code,
                message,
                Instant.now(clock));

        return ResponseEntity.status(status).body(response);
    }
}
