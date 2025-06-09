package com.mailscheduler.domain.factory;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailStatus;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.template.Template;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Factory for creating Email entities and their associated metadata.
 * <p>
 *     This factory handles the creation of both initial emails and follow-up emails, ensuring proper
 *     validation and initialization of all required fields.
 * </p>
 */
public class EmailFactory {
    private final EmailAddress defaultSenderEmail;

    /**
     * Creates a new EmailFactory with the specified default sender email address.
     *
     * @param defaultSenderEmail The default email address to use as the sender
     * @throws NullPointerException If defaultSenderEmail is null
     */
    public EmailFactory(EmailAddress defaultSenderEmail) {
        this.defaultSenderEmail = Objects.requireNonNull(defaultSenderEmail,
                "Default sender email cannot be null");
    }

    /**
     * Creates an initial email with resolved template content.
     * The template should already have all placeholders resolved.
     *
     * @param recipient The recipient to whom the email will be sent
     * @param resolvedTemplate The template with resolved placeholders for content
     * @return An EntityData containing the created Email and its metadata
     * @throws NullPointerException If any required parameter is null
     * @throws IllegalArgumentException If any validation fails
     */
    public EntityData<Email, EmailMetadata> createInitialEmail(Recipient recipient, Template resolvedTemplate) {
        validateInputs(recipient, resolvedTemplate);

        Email email = new Email.Builder()
                .setRecipientEmail(recipient.getEmailAddress())
                .setSenderEmail(defaultSenderEmail)
                .setSubject(resolvedTemplate.getSubject())
                .setBody(resolvedTemplate.getBody())
                .setType(EmailType.INITIAL)
                .build();

        EmailMetadata metadata = new EmailMetadata.Builder()
                .recipientId(recipient.getId())
                .followupNumber(0)
                .status(EmailStatus.PENDING)
                .scheduledDate(recipient.getInitialContactDate())
                .build();

        return new EntityData<>(email, metadata);
    }

    /**
     * Creates a follow-up email with resolved template content.
     *
     * @param recipient The recipient to whom the email will be sent
     * @param resolvedTemplate The template with resolved placeholders for content
     * @param scheduledDate The date when this follow-up email should be sent
     * @param followupNumber The sequence number of this follow-up (1 for first follow-up, etc.)
     * @param initialEmailId The ID of the original initial email this follows up on
     * @return An EntityData containing the created Email and its metadata
     * @throws NullPointerException If any required parameter is null
     * @throws IllegalArgumentException If any validation fails
     */
    public EntityData<Email, EmailMetadata> createFollowUpEmail(
            Recipient recipient,
            Template resolvedTemplate,
            LocalDate scheduledDate,
            int followupNumber,
            EntityId<Email> initialEmailId) {
        validateInputs(recipient, resolvedTemplate);
        validateFollowUpInputs(scheduledDate, followupNumber, initialEmailId);

        Email email = new Email.Builder()
                .setRecipientEmail(recipient.getEmailAddress())
                .setSenderEmail(defaultSenderEmail)
                .setSubject(resolvedTemplate.getSubject())
                .setBody(resolvedTemplate.getBody())
                .setType(EmailType.FOLLOW_UP)
                .build();

        EmailMetadata metadata = new EmailMetadata.Builder()
                .initialEmailId(initialEmailId)
                .recipientId(recipient.getId())
                .followupNumber(followupNumber)
                .status(EmailStatus.PENDING)
                .scheduledDate(scheduledDate)
                .build();

        return new EntityData<>(email, metadata);
    }

    private void validateInputs(Recipient recipient, Template template) {
        Objects.requireNonNull(recipient, "Recipient cannot be null");
        Objects.requireNonNull(template, "Template cannot be null");
        Objects.requireNonNull(recipient.getEmailAddress(), "Recipient email address cannot be null");
        Objects.requireNonNull(recipient.getId(), "Recipient ID cannot be null");
        Objects.requireNonNull(template.getSubject(), "Template subject cannot be null");
        Objects.requireNonNull(template.getBody(), "Template body cannot be null");
    }

    private void validateFollowUpInputs(LocalDate scheduledDate, int followupNumber, EntityId<Email> initialEmailId) {
        Objects.requireNonNull(scheduledDate, "Scheduled date cannot be null");
        Objects.requireNonNull(initialEmailId, "Initial email ID cannot be null");
        if (followupNumber <= 0) {
            throw new IllegalArgumentException("Follow-up number must be greater than zero");
        }
    }
}