package com.mailscheduler.infrastructure.persistence.database.exception;

/**
 * Exception thrown when database schema errors occur.
 */
public class SchemaException extends DatabaseException {
    public SchemaException(String message) {
        super(message);
    }

    public SchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}