package com.mailscheduler.infrastructure.persistence.repository.exception;

/**
 * Base exception for all repository-related errors.
 * Provides a consistent way to handle repository exceptions throughout the application.
 */
public class RepositoryException extends RuntimeException {
    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
