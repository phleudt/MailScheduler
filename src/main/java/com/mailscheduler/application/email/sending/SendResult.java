package com.mailscheduler.application.email.sending;

import com.mailscheduler.domain.model.common.vo.ThreadId;

/**
 * Represents the result of an email sending operation.
 */
public record SendResult(
        SendStatus status,
        ThreadId threadId,
        String errorMessage
) {

    public static SendResult success(ThreadId threadId) {
        return new SendResult(SendStatus.SUCCESS, threadId, "");

    }

    public static SendResult failed(String errorMessage) {
        return new SendResult(SendStatus.FAILURE, null, errorMessage);
    }

    /**
     * Returns the status in a format suitable for display in spreadsheets.
     */
    public String getDisplayStatus() {
        return status.getUserFriendlyStatus();
    }

    /**
     * Checks if the sending operation was successful.
     */
    public boolean isSuccess() {
        return status == SendStatus.SUCCESS;
    }

    /**
     * Checks if the sending operation failed.
     */
    public boolean isFailure() {
        return status == SendStatus.FAILURE;
    }
}
