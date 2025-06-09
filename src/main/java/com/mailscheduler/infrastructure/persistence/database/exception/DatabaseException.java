package com.mailscheduler.infrastructure.persistence.database.exception;

/**
 * Base exception for all database-related errors.
 * Provides a consistent way to handle database exceptions throughout the application.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
