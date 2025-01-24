package com.mailscheduler.email;

import com.google.api.services.gmail.model.Message;
import com.mailscheduler.exception.validation.EmailNotSentException;
import com.mailscheduler.google.GmailService;
import com.mailscheduler.model.Email;
import com.mailscheduler.model.SendStatus;

import java.io.IOException;
import java.util.logging.Logger;

public class EmailSender {
    private final Logger LOGGER = Logger.getLogger(EmailSender.class.getName());
    private final GmailService gmailService;
    private final boolean saveMode;

    public EmailSender(GmailService gmailService, boolean saveMode) {
        this.gmailService = gmailService;
        this.saveMode = saveMode;
    }

    public EmailSendResult sendEmail(Email email) throws EmailNotSentException, IOException {
        if (hasReplies(email)) {
            LOGGER.info("Email has been replied to. No followup mail being send.");
            return new EmailSendResult(SendStatus.ALREADY_REPLIED, null);
        }

        Message sentMessage = switch (email.getEmailCategory()) {
            case INITIAL -> sendInitialEmail(email);
            case FOLLOW_UP -> sendFollowUpEmail(email);
            default -> throw new IllegalArgumentException("Unsupported email category: " + email.getEmailCategory());
        };

        if (sentMessage != null) {
            return new EmailSendResult(SendStatus.SENT, sentMessage);
        } else {
            return new EmailSendResult(SendStatus.SENDING_ERROR, null);
        }
    }

    public record EmailSendResult(SendStatus status, Message message) {}

    private boolean hasReplies(Email email) throws IOException {
        try {
            LOGGER.info("Checking if email: " + email.getSubject() + " has replies");
            return email.getThreadId() != null && gmailService.hasReplies(email.getThreadId(), email.getFollowupNumber());
        } catch (IOException e) {
            LOGGER.severe("Failed to check for replies for email: " + email.getSubject() + ", :" + e.getMessage());
            // return false
            throw new IOException("Failed to check, if the email had been replied to", e);
        }
    }

    private Message sendInitialEmail(Email email) throws EmailNotSentException {
        LOGGER.info("Sending initial email: " + email);
        try {
            Message message = EmailConverter.convertEmailToMessage(email);
            if (saveMode) {
                LOGGER.info("Save mode enabled, creating draft for initial email: " + email);
                return gmailService.createDraft(message).getMessage();
            } else {
                return gmailService.sendEmail(message);
            }
        } catch (EmailConverter.ConversionException | IOException e) {
            System.out.println("Failed to send email: " + e.getMessage());
            throw new EmailNotSentException("Failed to send initial email", e);
        }
    }

    private Message sendFollowUpEmail(Email email) throws EmailNotSentException {
        try {
            Message message = EmailConverter.convertEmailToMessage(email);
            if (saveMode) {
                LOGGER.info("Save mode enabled, creating draft for initial email: " + email);
                return gmailService.createDraft(message).getMessage();
            } else {
                return gmailService.sendEmail(message);
            }
        } catch (EmailConverter.ConversionException | IOException e) {
            System.out.println("Failed to send followup email: " + e.getMessage());
            throw new EmailNotSentException("Failed to send followup email", e);
        }
    }

}
