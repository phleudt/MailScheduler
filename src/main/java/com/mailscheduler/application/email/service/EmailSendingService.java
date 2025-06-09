package com.mailscheduler.application.email.service;

import com.mailscheduler.application.email.sending.*;
import com.mailscheduler.application.email.sending.gateway.EmailGateway;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailStatus;
import com.mailscheduler.domain.repository.EmailRepository;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for sending emails and managing their status.
 * Handles communication with email gateways and updates email status in the repository.
 */
public class EmailSendingService {
    private static final Logger LOGGER = Logger.getLogger(EmailSendingService.class.getName());

    private final EmailGateway emailGateway;
    private final EmailRepository emailRepository;

    public EmailSendingService(EmailGateway emailGateway, EmailRepository emailRepository) {
        this.emailGateway = emailGateway;
        this.emailRepository = emailRepository;
    }

    /**
     * Sends an email or saves it as a draft.
     *
     * @param request The email send request containing all necessary data
     * @param saveAsDraft Whether to save as draft instead of sending
     * @return The result of the sending operation
     * @throws IllegalArgumentException If the request is invalid
     */
    public EmailSendingResult send(EmailSendRequest request, boolean saveAsDraft) {
        validateRequest(request);

        try {
            LOGGER.info(String.format("Processing email request for recipient %s (follow-up #%d)",
                    request.metadata().recipientId(), request.followUpNumber()));

            // Check if recipient has already replied (for follow-ups only)
            if (shouldSkipDueToReplies(request)) {
                LOGGER.info("Skipping follow-up email due to detected replies from recipient "
                        + request.metadata().recipientId());

                return new EmailSendingResult(
                        request.metadata().recipientId(),
                        true,
                        null,
                        request
                );
            }
            // Send the email or save as draft
            LOGGER.info(String.format("%s email for recipient %s",
                    saveAsDraft ? "Saving draft" : "Sending",
                    request.metadata().recipientId()));

            SendResult result = sendOrSaveDraft(request, saveAsDraft);

            // Update email status based on result
            updateEmailStatus(request, result);

            return new EmailSendingResult(
                    request.metadata().recipientId(),
                    false,
                    result,
                    request
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending email: " + e.getMessage(), e);

            handleSendingFailure(request, e);

            // Return failure result
            SendResult failResult = new SendResult(SendStatus.FAILURE, null, e.getMessage());
            return new EmailSendingResult(
                    request.metadata().recipientId(),
                    false,
                    failResult,
                    request
            );
        }
    }

    /**
     * Sends a batch of emails.
     *
     * @param requests The requests to process
     * @param saveAsDraft Whether to save as drafts
     * @return Array of results matching the input requests
     */
    public EmailSendingResult[] sendBatch(EmailSendRequest[] requests, boolean saveAsDraft) {
        if (requests == null || requests.length == 0) {
            return new EmailSendingResult[0];
        }

        EmailSendingResult[] results = new EmailSendingResult[requests.length];
        for (int i = 0; i < requests.length; i++) {
            try {
                results[i] = send(requests[i], saveAsDraft);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing batch email #" + i, e);
                results[i] = createFailureResult(requests[i], e.getMessage());
            }
        }

        return results;
    }

    /**
     * Retries sending a previously failed email.
     *
     * @param request The original send request
     * @param saveAsDraft Whether to save as draft
     * @return The result of the retry operation
     */
    public EmailSendingResult retry(EmailSendRequest request, boolean saveAsDraft) {
        LOGGER.info("Retrying email send for recipient " + request.metadata().recipientId());
        return send(request, saveAsDraft);
    }

    private void validateRequest(EmailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Email send request cannot be null");
        }

        if (request.email() == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        if (request.metadata() == null) {
            throw new IllegalArgumentException("Email metadata cannot be null");
        }

        if (request.metadata().recipientId() == null) {
            throw new IllegalArgumentException("Recipient ID cannot be null");
        }

        // Thread ID can be null for initial emails
        if (request.followUpNumber() > 0 && request.threadId() == null) {
            throw new IllegalArgumentException("Thread ID required for follow-up emails");
        }
    }

    private boolean shouldSkipDueToReplies(EmailSendRequest request) {
        // Only check for replies on follow-up emails
        if (request.followUpNumber() <= 0 || request.threadId() == null) {
            return false;
        }

        try {
            return emailGateway.hasReplies(request.threadId(), request.followUpNumber() + 1);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking for replies: " + e.getMessage(), e);
            // Safety first - assume replied to avoid potentially annoying recipients
            return true;
        }
    }

    private SendResult sendOrSaveDraft(EmailSendRequest request, boolean saveAsDraft) throws EmailSendException {
        try {
            if (saveAsDraft) {
                return emailGateway.saveDraft(request.email(), request.threadId());
            } else {
                return emailGateway.send(request.email(), request.threadId());
            }
        } catch (Exception e) {
            throw new EmailSendException("Failed to " + (saveAsDraft ? "save draft" : "send email"), e);
        }
    }

    private void updateEmailStatus(EmailSendRequest request, SendResult result) {
        if (result == null || request == null) return;

        switch (result.status()) {
            case SUCCESS -> {
                EmailMetadata updatedMetadata = new EmailMetadata.Builder()
                        .from(request.metadata())
                        .status(EmailStatus.SENT)
                        .sentDate(LocalDate.now())
                        .build();

                emailRepository.updateWithMetadata(request.email(), updatedMetadata);
            }

            case FAILURE -> {
                EmailMetadata failedMetadata = new EmailMetadata.Builder()
                        .from(request.metadata())
                        .status(EmailStatus.FAILED)
                        .failureReason(result.errorMessage())
                        .build();

                emailRepository.updateWithMetadata(request.email(), failedMetadata);
            }
        }
    }

    private void handleSendingFailure(EmailSendRequest request, Exception exception) {
        EmailMetadata failedMetadata = new EmailMetadata.Builder()
                .from(request.metadata())
                .status(EmailStatus.FAILED)
                .failureReason("Exception: " + exception.getMessage())
                .build();

        try {
            emailRepository.updateWithMetadata(request.email(), failedMetadata);
        } catch (Exception e) {
            // If updating the repository fails too, just log it
            LOGGER.log(Level.SEVERE, "Failed to update email status after sending error", e);
        }
    }

    private EmailSendingResult createFailureResult(EmailSendRequest request, String errorMessage) {
        SendResult failResult = SendResult.failed(errorMessage);
        return new EmailSendingResult(
                request.metadata() != null ? request.metadata().recipientId() : null,
                false,
                failResult,
                request
        );
    }
}
