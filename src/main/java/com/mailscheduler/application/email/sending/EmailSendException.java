package com.mailscheduler.application.email.sending;

/**
 * Exception thrown when an error occurs during email sending operations.
 */
public class EmailSendException extends Exception {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}