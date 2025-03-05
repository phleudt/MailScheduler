package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.common.exception.service.EmailSchedulingException;

public class EmailSchedulingValidationException extends EmailSchedulingException {
    public EmailSchedulingValidationException(String message) {
        super(message);
    }
}
