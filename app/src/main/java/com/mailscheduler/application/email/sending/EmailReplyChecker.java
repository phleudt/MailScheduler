package com.mailscheduler.application.email.sending;

import com.mailscheduler.domain.email.Email;
import com.mailscheduler.infrastructure.google.gmail.GmailService;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Service responsible for checking if an email has received any replies.
 */
public class EmailReplyChecker {
    private static final Logger LOGGER = Logger.getLogger(EmailReplyChecker.class.getName());
    private final GmailService gmailService;

    public EmailReplyChecker(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    /**
     * Checks if an email has received any replies.
     *
     * @param email The email to check for replies
     * @return true if the email has replies, false otherwise
     * @throws IOException if there's an error checking for replies
     */
    public boolean hasReplies(Email email) throws IOException {
        try {
            LOGGER.info("Checking if email: " + email.getSubject() + " has replies");
            return email.getThreadId().isPresent() &&
                    gmailService.hasReplies(email.getThreadId().get(), email.getFollowupNumber());
        } catch (IOException e) {
            LOGGER.severe("Failed to check for replies for email: " + email.getSubject() + ", :" + e.getMessage());
            throw new IOException("Failed to check if the email had been replied to", e);
        }
    }
}