package com.mailscheduler.application.recipient.exception;

/**
 * Exception thrown when an operation on recipients fails.
 */
public class RecipientOperationException extends Exception {

    public RecipientOperationException(String message) {
        super(message);
    }

    public RecipientOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}