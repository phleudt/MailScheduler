package com.mailscheduler.application.email.factory;

import com.mailscheduler.common.exception.EmailTemplateManagerException;
import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.domain.email.EmailCategory;
import com.mailscheduler.domain.email.EmailId;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.domain.template.Template;

import java.time.ZonedDateTime;

/**
 * Factory for creating different types of emails.
 */
public class EmailFactory {
    private final EmailAddress defaultSender;

    public EmailFactory(EmailAddress defaultSender) {
        this.defaultSender = defaultSender;
    }

    public Email createInitialEmail(Recipient recipient, Template template) {
        return new Email.Builder()
                .setSender(defaultSender)
                .setRecipient(recipient.getEmailAddress())
                .setRecipientId(recipient.getId())
                .setStatus("PENDING")
                .setScheduledDate(recipient.getInitialEmailDate())
                .setCategory(EmailCategory.INITIAL)
                .setTemplate(template)
                .build();
    }

    public Email createFollowUpEmail(
            Recipient recipient,
            Template template,
            ZonedDateTime followUpEmailDate,
            int followUpNumber,
            EmailId initialEmailId
    ) {
        return new Email.Builder()
                .setSender(defaultSender)
                .setRecipient(recipient.getEmailAddress())
                .setRecipientId(recipient.getId())
                .setStatus("PENDING")
                .setScheduledDate(followUpEmailDate)
                .setFollowupNumber(followUpNumber)
                .setInitialEmailId(initialEmailId)
                .setCategory(EmailCategory.FOLLOW_UP)
                .setTemplate(template)
                .build();
    }
}