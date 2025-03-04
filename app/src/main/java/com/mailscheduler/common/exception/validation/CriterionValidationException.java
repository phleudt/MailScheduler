package com.mailscheduler.common.exception.validation;

public class CriterionValidationException extends Exception {
    public CriterionValidationException(String message) {
        super(message);
    }

    public CriterionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
