package com.mailscheduler.application.email.sending;

import com.google.api.services.gmail.model.Message;
import com.mailscheduler.domain.common.SendStatus;

/**
 * Represents the result of an email sending operation.
 */
public record EmailSendResult(
        SendStatus status,
        Message message
) {
    /**
     * Creates a successful send result.
     *
     * @param message The sent message
     * @return A new EmailSendResult indicating success
     */
    public static EmailSendResult success(Message message) {
        return new EmailSendResult(SendStatus.SENT, message);
    }

    /**
     * Creates a failed send result.
     *
     * @return A new EmailSendResult indicating failure
     */
    public static EmailSendResult failure() {
        return new EmailSendResult(SendStatus.SENDING_ERROR, null);
    }

    /**
     * Creates an already replied result.
     *
     * @return A new EmailSendResult indicating the email was already replied to
     */
    public static EmailSendResult alreadyReplied() {
        return new EmailSendResult(SendStatus.ALREADY_REPLIED, null);
    }

    /**
     * Checks if the sending was successful.
     *
     * @return true if the status is SENT and message is not null
     */
    public boolean isSuccess() {
        return status == SendStatus.SENT && message != null;
    }
}