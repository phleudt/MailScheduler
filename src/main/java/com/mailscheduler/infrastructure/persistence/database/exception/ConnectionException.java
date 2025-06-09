package com.mailscheduler.infrastructure.persistence.database.exception;

/**
 * Exception thrown when database connection errors occur.
 */
public class ConnectionException extends DatabaseException {
    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
