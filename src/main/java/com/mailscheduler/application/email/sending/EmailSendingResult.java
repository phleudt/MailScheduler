package com.mailscheduler.application.email.sending;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.common.vo.ThreadId;

/**
 * Represents the result of an email sending operation.
 */
public record EmailSendingResult(
        EntityId<Recipient> recipientId,
        boolean hasReplied,
        SendResult sendResult,
        EmailSendRequest sendRequest
) {
    /**
     * Creates a skipped result.
     */
    public static EmailSendingResult skipped(EmailSendRequest request) {
        return new EmailSendingResult(
                request.metadata().recipientId(),
                true,
                null,
                request
        );
    }

    /**
     * Creates a failed result.
     */
    public static EmailSendingResult failed(EmailSendRequest request, String errorMessage) {
        return new EmailSendingResult(
                request.metadata().recipientId(),
                false,
                SendResult.failed(errorMessage),
                request
        );
    }

    /**
     * Creates a successful result.
     */
    public static EmailSendingResult success(EmailSendRequest request, ThreadId threadId) {
        return new EmailSendingResult(
                request.metadata().recipientId(),
                false,
                SendResult.success(threadId),
                request
        );
    }

    /**
     * Checks if the sending operation was successful.
     */
    public boolean isSuccess() {
        return !hasReplied && sendResult != null && sendResult.isSuccess();
    }

    /**
     * Checks if the sending operation was skipped due to replies.
     */
    public boolean isSkipped() {
        return hasReplied;
    }

    /**
     * Checks if the sending operation failed.
     */
    public boolean isFailure() {
        return !hasReplied && (sendResult == null || sendResult.isFailure());
    }

    /**
     * Gets the thread ID if available.
     */
    public ThreadId getThreadId() {
        return sendResult != null ? sendResult.threadId() : null;
    }

    /**
     * Gets the error message if there was an error.
     */
    public String getErrorMessage() {
        return sendResult != null ? sendResult.errorMessage() : "No send result available";
    }
}
