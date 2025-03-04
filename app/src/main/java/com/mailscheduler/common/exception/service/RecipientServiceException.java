package com.mailscheduler.common.exception.service;


public class RecipientServiceException extends Exception {
    public RecipientServiceException(String message) {
        super(message);
    }
    public RecipientServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
