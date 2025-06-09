package com.mailscheduler.application.email.sending.gateway;


import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.mailscheduler.application.email.sending.EmailSendException;
import com.mailscheduler.application.email.sending.SendResult;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.common.vo.ThreadId;
import com.mailscheduler.infrastructure.google.gmail.GmailService;
import com.mailscheduler.util.EmailConverter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of EmailGateway using Gmail API.
 */
public class GmailAdapter implements EmailGateway {
    private static final Logger LOGGER = Logger.getLogger(GmailAdapter.class.getName());

    private final GmailService gmailService;

    public GmailAdapter(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @Override
    public SendResult send(Email email, ThreadId threadId) throws EmailSendException {
        Message message = convertEmailToMessage(email, threadId);

        try {
            Message sentMessage = gmailService.sendEmail(message);
            LOGGER.info("Email sent successfully");

            // Use existing thread ID if provided, otherwise get it from sent message
            ThreadId resultThreadId = threadId != null
                    ? threadId
                    : new ThreadId(sentMessage.getThreadId());

            return SendResult.success(resultThreadId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email", e);
            throw new EmailSendException("Failed to send email" , e);
        }
    }

    @Override
    public SendResult saveDraft(Email email, ThreadId threadId) throws EmailSendException {
        Message message = convertEmailToMessage(email, threadId);

        try {
            Draft savedDraft = gmailService.createDraft(message);

            // Use existing thread ID if provided, otherwise get it from the draft message
            ThreadId resultThreadId = threadId.value() != null
                    ? threadId
                    : new ThreadId(savedDraft.getMessage().getThreadId());

            return SendResult.success(resultThreadId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save draft", e);
            throw new EmailSendException("Failed to save email as draft", e);
        }
    }

    @Override
    public boolean hasReplies(ThreadId threadId, int minimumReplyCount) throws IOException {
        if (threadId == null || threadId.value() == null) {
            return false;
        }

        try {
            return gmailService.hasReplies(threadId.value(), minimumReplyCount);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error checking for replies", e);
            throw e;
        }
    }

    private Message convertEmailToMessage(Email email, ThreadId threadId) throws EmailSendException {
        try {
            return EmailConverter.convertEmailToMessage(email, threadId);
        } catch (EmailConverter.ConversionException e) {
            throw new EmailSendException("Failed to convert email to message", e);
        }
    }
}
