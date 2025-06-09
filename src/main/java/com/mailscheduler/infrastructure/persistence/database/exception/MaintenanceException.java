package com.mailscheduler.infrastructure.persistence.database.exception;

/**
 * Exception thrown when database maintenance operations fail.
 */
public class MaintenanceException extends DatabaseException {
    public MaintenanceException(String message) {
        super(message);
    }

    public MaintenanceException(String message, Throwable cause) {
        super(message, cause);
    }
}