package com.mailscheduler.application.synchronization.exception;

/**
 * Exception thrown during synchronization operations.
 * Represents failures in synchronizing data between external services and the application.
 */
public class SyncException extends Exception {
    public SyncException(String message) {
        super(message);
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
