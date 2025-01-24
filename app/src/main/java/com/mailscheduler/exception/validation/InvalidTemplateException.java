package com.mailscheduler.exception.validation;

public class InvalidTemplateException extends Exception {
    public InvalidTemplateException() {
        super("Invalid Template");
    }

    public InvalidTemplateException(String message) {
        super(message);
    }
}
