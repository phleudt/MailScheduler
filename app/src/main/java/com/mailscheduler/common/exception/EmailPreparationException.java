package com.mailscheduler.common.exception;


public class EmailPreparationException extends Exception {
    public EmailPreparationException(String message) {
        super(message);
    }

    public EmailPreparationException(String message, Throwable cause) {
        super(message, cause);
    }
}
