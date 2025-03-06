package com.mailscheduler.application.email.sending;

import com.google.api.services.gmail.model.Message;
import com.mailscheduler.common.exception.validation.EmailNotSentException;
import com.mailscheduler.infrastructure.email.EmailConverter;
import com.mailscheduler.infrastructure.google.gmail.GmailService;
import com.mailscheduler.domain.email.Email;

import java.io.IOException;
import java.util.logging.Logger;

public class EmailSendingService {
    private final Logger LOGGER = Logger.getLogger(EmailSendingService.class.getName());

    private final EmailReplyChecker replyChecker;
    private final EmailDispatcher dispatcher;

    public EmailSendingService(GmailService gmailService, boolean saveMode) {
        this.replyChecker = new EmailReplyChecker(gmailService);
        this.dispatcher = new EmailDispatcher(gmailService, saveMode);
    }

    /**
     * Sends an email after checking for replies.
     *
     * @param email The email to send
     * @return The result of the sending operation
     * @throws IOException if checking for replies fails
     */
    public EmailSendResult sendEmail(Email email) throws IOException {
        if (replyChecker.hasReplies(email)) {
            LOGGER.info("Email has been replied to. No followup mail being send.");
            return EmailSendResult.alreadyReplied();
        }

        try {
            var sentMessage = dispatcher.dispatch(email);
            return sentMessage != null ?
                    EmailSendResult.success(sentMessage) :
                    EmailSendResult.failure();
        } catch (EmailNotSentException e) {
            LOGGER.severe("Failed to send email: " + e.getMessage());
            return EmailSendResult.failure();
        }
    }
}