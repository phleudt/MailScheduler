package com.mailscheduler.exception.service;

public class EmailServiceException extends Exception {
    public EmailServiceException(Throwable cause) {
        super(cause);
    }
    public EmailServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
