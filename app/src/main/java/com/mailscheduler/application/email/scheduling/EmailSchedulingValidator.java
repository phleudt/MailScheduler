package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.common.exception.service.EmailSchedulingException;

import java.util.List;

public class EmailSchedulingValidator {

    public boolean validateRecipients(List<Recipient> recipients) {
        return recipients != null && !recipients.isEmpty();
    }

    public void validateRecipient(Recipient recipient) throws EmailSchedulingValidationException {
        if (recipient == null) {
            throw new EmailSchedulingValidationException("Recipient cannot be null");
        }
        if (recipient.getInitialEmailDate() == null) {
            throw new EmailSchedulingValidationException(
                    String.format("Initial email date must be set for recipient %s", recipient.getName()));
        }
    }
}