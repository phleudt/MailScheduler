package com.mailscheduler.application.email;

import com.mailscheduler.application.email.exception.EmailOperationException;
import com.mailscheduler.application.email.scheduling.PlanWithTemplatesRecipientsMap;
import com.mailscheduler.application.email.scheduling.RecipientScheduledEmailsMap;
import com.mailscheduler.application.email.sending.SendStatus;
import com.mailscheduler.application.recipient.RecipientService;
import com.mailscheduler.application.email.sending.EmailSendRequest;
import com.mailscheduler.application.email.sending.EmailSendingResult;
import com.mailscheduler.application.email.service.EmailSchedulingService;
import com.mailscheduler.application.email.service.EmailSendingService;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for coordinating email operations across the application.
 * Handles email scheduling, preparation, and sending operations.
 */
public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private final EmailSendingService sendingService;
    private final EmailSchedulingService schedulingService;
    private final RecipientService recipientService;


    public EmailService(
            EmailSendingService sendingService,
            EmailSchedulingService schedulingService,
            RecipientService recipientService
    ) {
        this.sendingService = Objects.requireNonNull(sendingService, "Sending service cannot be null");
        this.schedulingService = Objects.requireNonNull(schedulingService, "Scheduling service cannot be null");
        this.recipientService = Objects.requireNonNull(recipientService, "Recipient service cannot be null");
    }

    /**
     * Schedules emails for recipients based on the provided plan.
     *
     * @param recipientsMap mapping of plans to recipients
     * @return mapping of recipients to their scheduled emails
     */
    public RecipientScheduledEmailsMap scheduleEmailsForRecipients(PlanWithTemplatesRecipientsMap recipientsMap)
            throws EmailOperationException {
        validateRecipientsMap(recipientsMap);

        try {
            LOGGER.info("Scheduling emails for recipients");
            return schedulingService.scheduleForRecipients(recipientsMap);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to schedule emails for recipients", e);
            throw new EmailOperationException("Failed to schedule emails", e);
        }
    }

    /**
     * Prepares email send requests for all pending emails.
     *
     * @param senderAddress The email address to use as sender
     * @return List of prepared email send requests
     * @throws EmailOperationException if building requests fails
     */
    public List<EmailSendRequest> prepareSendRequests(EmailAddress senderAddress) throws EmailOperationException {
        if (senderAddress == null) {
            throw new IllegalArgumentException("Valid sender email address is required");
        }

        try {
            List<EntityData<Email, EmailMetadata>> pendingEmails = schedulingService.getPendingEmails();
            if (pendingEmails.isEmpty()) {
                LOGGER.info("No pending emails found to prepare for sending");
                return Collections.emptyList();
            }

            LOGGER.info("Building send requests for " + pendingEmails.size() + " pending emails");
            List<EmailSendRequest> requests = new ArrayList<>(pendingEmails.size());

            for (EntityData<Email, EmailMetadata> pendingEmail : pendingEmails) {
                EmailSendRequest request = buildSendRequest(pendingEmail, senderAddress);
                if (request != null) {
                    requests.add(request);
                }
            }

            LOGGER.info("Prepared " + requests.size() + " send requests");
            return requests;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to build email send requests", e);
            throw new EmailOperationException("Failed to prepare email send requests", e);
        }
    }

    /**
     * Sends emails using the provided requests.
     *
     * @param requests Requests containing email data
     * @param saveAsDraft Whether to save as draft instead of sending
     * @return Results of the sending operation
     * @throws EmailOperationException if sending emails fails
     */
    public List<EmailSendingResult> sendEmails(List<EmailSendRequest> requests, boolean saveAsDraft)
            throws EmailOperationException {
        if (requests == null || requests.isEmpty()) {
            LOGGER.info("No emails to send");
            return Collections.emptyList();
        }

        try {
            LOGGER.info("Sending " + requests.size() + " emails (saveAsDraft=" + saveAsDraft + ")");
            List<EmailSendingResult> results = new ArrayList<>(requests.size());

            for (EmailSendRequest request : requests) {
                EmailSendingResult result = processEmailSend(request, saveAsDraft);
                results.add(result);
            }

            LOGGER.info("Completed processing " + results.size() + " email send requests");
            return results;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send emails", e);
            throw new EmailOperationException("Failed to send emails", e);
        }
    }

    /**
     * Resends a previously failed email.
     *
     * @param request The original email request
     * @param saveAsDraft Whether to save as draft
     * @return The result of the resend operation
     * @throws EmailOperationException if resending fails
     */
    public EmailSendingResult resendEmail(EmailSendRequest request, boolean saveAsDraft)
            throws EmailOperationException {
        if (request == null) {
            throw new IllegalArgumentException("Email request cannot be null for resend");
        }

        try {
            LOGGER.info("Resending email to recipient ID: " + request.metadata().recipientId());
            return processEmailSend(request, saveAsDraft);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to resend email", e);
            throw new EmailOperationException("Failed to resend email", e);
        }
    }

    private void validateRecipientsMap(PlanWithTemplatesRecipientsMap recipientsMap) {
        if (recipientsMap == null) {
            throw new IllegalArgumentException("Recipients map cannot be null");
        }

        if (recipientsMap.isEmpty()) {
            LOGGER.warning("Empty recipients map provided for scheduling");
        }
    }

    private EmailSendRequest buildSendRequest(EntityData<Email, EmailMetadata> emailData, EmailAddress senderAddress) {
        try {
            var recipientId = emailData.metadata().recipientId();
            var recipientData = recipientService.getRecipient(recipientId);

            Email updatedEmail = new Email.Builder()
                    .from(emailData.entity())
                    .setSenderEmail(senderAddress)
                    .setRecipientEmail(recipientData.entity().getEmailAddress())
                    .build();

            return new EmailSendRequest.Builder()
                    .withEmail(updatedEmail)
                    .withMetadata(emailData.metadata())
                    .withThreadId(recipientData.metadata().threadId())
                    .withFollowUpNumber(emailData.metadata().followupNumber())
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to build send request for email ID: " +
                    emailData.entity().getId(), e);
            return null;
        }
    }

    private EmailSendingResult processEmailSend(EmailSendRequest request, boolean saveAsDraft) {
        EmailSendingResult result = sendingService.send(request, saveAsDraft);

        try {
            // If recipient has replied, update their status
            if (result.hasReplied()) {
                LOGGER.info("Recipient " + result.recipientId() + " has replied, updating status");
                recipientService.markAsReplied(result.recipientId());
                return result;
            }

            // If initial email sent successfully, update thread ID
            if (result.sendResult() != null &&
                    result.sendResult().status() == SendStatus.SUCCESS &&
                    request.followUpNumber() == 0 &&
                    result.sendResult().threadId() != null) {

                LOGGER.info("Updating thread ID for recipient " + request.metadata().recipientId());
                recipientService.updateThreadId(
                        request.metadata().recipientId(),
                        result.sendResult().threadId()
                );
            }

            return result;
        } catch (Exception e) {
            // Log but don't re-throw as the email was still sent
            LOGGER.log(Level.WARNING, "Failed to update recipient after sending email", e);
            return result;
        }
    }
}