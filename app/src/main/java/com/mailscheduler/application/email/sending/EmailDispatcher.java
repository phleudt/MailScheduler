package com.mailscheduler.application.email.sending;

import com.google.api.services.gmail.model.Message;
import com.mailscheduler.common.exception.validation.EmailNotSentException;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.infrastructure.email.EmailConverter;
import com.mailscheduler.infrastructure.google.gmail.GmailService;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Responsible for the actual sending of emails through the Gmail service.
 */
public class EmailDispatcher {
    private static final Logger LOGGER = Logger.getLogger(EmailDispatcher.class.getName());
    private final GmailService gmailService;
    private final boolean saveMode;

    public EmailDispatcher(GmailService gmailService, boolean saveMode) {
        this.gmailService = gmailService;
        this.saveMode = saveMode;
    }

    /**
     * Sends an email or creates a draft depending on the save mode.
     *
     * @param email The email to send
     * @return The sent message
     * @throws EmailNotSentException if sending fails
     */
    public Message dispatch(Email email) throws EmailNotSentException {
        try {
            Message message = EmailConverter.convertEmailToMessage(email);

            if (saveMode) {
                LOGGER.info("Save mode enabled, creating draft for email: " + email);
                return gmailService.createDraft(message).getMessage();
            } else {
                return gmailService.sendEmail(message);
            }
        } catch (EmailConverter.ConversionException | IOException e) {
            LOGGER.severe("Failed to send email: " + e.getMessage());
            throw new EmailNotSentException("Failed to send email: " + email.getCategory(), e);
        }
    }
}