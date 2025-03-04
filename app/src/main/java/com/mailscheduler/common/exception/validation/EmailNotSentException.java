package com.mailscheduler.common.exception.validation;

public class EmailNotSentException extends Exception {
    public EmailNotSentException(String messsage) {
        super(messsage);
    }
    public EmailNotSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
