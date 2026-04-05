package com.finance.dashboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AppException extends RuntimeException {

    private final HttpStatus status;

    protected AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // ── Typed exception classes ────────────────────────────────────────────────

    /**
     * Thrown when a requested resource (Expense, User, Category) is not found.
     * Maps to HTTP 404 Not Found.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends AppException {
        public ResourceNotFoundException(String resource, Long id) {
            super(resource + " not found with id: " + id, HttpStatus.NOT_FOUND);
        }
        public ResourceNotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Thrown when the authenticated user lacks permission to access a resource.
     * Maps to HTTP 403 Forbidden.
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDeniedException extends AppException {
        public AccessDeniedException(String message) {
            super(message, HttpStatus.FORBIDDEN);
        }
        public AccessDeniedException() {
            super("You do not have permission to perform this action", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Thrown for invalid input that passes annotation validation but violates business rules.
     * Maps to HTTP 400 Bad Request.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class BadRequestException extends AppException {
        public BadRequestException(String message) {
            super(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Thrown when a unique constraint is violated (duplicate email, duplicate category name).
     * Maps to HTTP 409 Conflict.
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ConflictException extends AppException {
        public ConflictException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    /**
     * Thrown when authentication credentials are missing or invalid.
     * Maps to HTTP 401 Unauthorized.
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends AppException {
        public UnauthorizedException(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }
}
