package com.mailscheduler.application.email.validation;

import com.mailscheduler.domain.email.Email;
import com.mailscheduler.common.exception.service.EmailValidationException;

import java.time.ZonedDateTime;
import java.util.regex.Pattern;

public class EmailValidationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * Validate an email before scheduling or sending
     *
     * @param email Email to validate
     * @throws EmailValidationException if validation fails
     */
    public void validateSending(Email email) throws EmailValidationException {
        validateSendDate(email.getScheduledDate());
        validateEmailContent(email);
    }

    public void validateScheduling(Email email) throws EmailValidationException {
        validateEmailContent(email);
    }

    private void validateSendDate(ZonedDateTime sendDate) throws EmailValidationException {
        if (sendDate == null || sendDate.isAfter(ZonedDateTime.now())) {
            throw new EmailValidationException("Invalid send date");
        }
    }

    private void validateEmailContent(Email email) throws EmailValidationException {
        if (email.getSubject() == null) {
            throw new EmailValidationException("Email subject cannot be empty");
        } else if (email.getBody() == null) {
            throw new EmailValidationException("Email body cannot be empty");
        }
    }

    public static void validateEmail(String email) throws EmailValidationException {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new EmailValidationException("Invalid email address");
        }
    }
}