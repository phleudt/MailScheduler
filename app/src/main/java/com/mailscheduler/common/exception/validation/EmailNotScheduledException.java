package com.mailscheduler.common.exception.validation;

public class EmailNotScheduledException extends Exception {
    public EmailNotScheduledException(String message) {
        super(message);
    }

    public EmailNotScheduledException(String message, Throwable cause) {
        super(message, cause);
    }
}
