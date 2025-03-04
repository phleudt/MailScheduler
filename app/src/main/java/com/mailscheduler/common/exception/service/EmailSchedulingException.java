package com.mailscheduler.common.exception.service;

public class EmailSchedulingException extends Exception {
    public EmailSchedulingException(String message) {
        super(message);
    }

    public EmailSchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}