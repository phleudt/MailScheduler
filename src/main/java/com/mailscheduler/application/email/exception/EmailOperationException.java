package com.mailscheduler.application.email.exception;

/**
 * Exception thrown for errors during email operations.
 */
public class EmailOperationException extends Exception {

    public EmailOperationException(String message) {
        super(message);
    }

    public EmailOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}