package com.mailscheduler.service;

import com.mailscheduler.model.Email;
import com.mailscheduler.exception.service.EmailValidationException;

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
        validateSender(email.getSender());
        validateRecipient(email.getRecipientEmail());
        validateSendDate(email.getScheduledDate());
        validateEmailContent(email);
    }

    public void validateScheduling(Email email) throws EmailValidationException {
        validateSender(email.getSender());
        validateRecipient(email.getRecipientEmail());
        validateEmailContent(email);
    }

    private void validateSender(String senderEmail) throws EmailValidationException {
        if (senderEmail == null || (!EMAIL_PATTERN.matcher(senderEmail).matches() && !"me".equals(senderEmail))) {
            throw new EmailValidationException("Invalid sender email address");
        }
    }

    private void validateRecipient(String recipientEmail) throws EmailValidationException {
        if (recipientEmail == null || !EMAIL_PATTERN.matcher(recipientEmail).matches()) {
            throw new EmailValidationException("Invalid recipient email address");
        }
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
