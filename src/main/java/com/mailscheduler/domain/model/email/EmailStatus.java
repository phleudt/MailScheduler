package com.mailscheduler.domain.model.email;

/**
 * Represents the status of an email in the mailing system.
 * <p>
 *     This enum tracks the lifecycle of an email from creation through delivery or failure.
 *     It's used for filtering, reporting, and determining what actions are appropriate for an email.
 * </p>
 */
public enum EmailStatus {
    /**
     * Email is created but not yet sent.
     */
    PENDING,

    /**
     * Email has been successfully sent to the recipient.
     */
    SENT,

    /**
     * Email sending attempt failed.
     */
    FAILED,

    /**
     * Email was manually cancelled before sending.
     */
    CANCELLED
}
