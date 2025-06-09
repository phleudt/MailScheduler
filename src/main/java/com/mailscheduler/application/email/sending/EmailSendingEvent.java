package com.mailscheduler.application.email.sending;

import java.time.LocalDate;

/**
 * Event representing an email sending operation.
 */
public record EmailSendingEvent(
        EmailSendRequest request,
        EventType type,
        String message,
        LocalDate timestamp
) {
    /**
     * Types of email sending events.
     */
    public enum EventType {
        COMPLETED,
        FAILED,
        SKIPPED
    }

    /**
     * Creates a new event with the current timestamp.
     */
    public EmailSendingEvent(EmailSendRequest request, EventType type, String message) {
        this(request, type, message, LocalDate.now());
    }

    /**
     * Creates a completed event.
     */
    public static EmailSendingEvent completed(EmailSendRequest request, SendResult result) {
        return new EmailSendingEvent(
                request,
                EventType.COMPLETED,
                "Email sent successfully with status: " + result.status()
        );
    }

    /**
     * Creates a failed event.
     */
    public static EmailSendingEvent failed(EmailSendRequest request, String error) {
        return new EmailSendingEvent(
                request,
                EventType.FAILED,
                "Email sending failed: " + error
        );
    }

    /**
     * Creates a skipped event.
     */
    public static EmailSendingEvent skipped(EmailSendRequest request) {
        return new EmailSendingEvent(
                request,
                EventType.SKIPPED,
                "Email sending skipped due to detected replies"
        );
    }

    /**
     * Returns the recipient ID from the request.
     */
    public String recipientId() {
        return request.metadata() != null && request.metadata().recipientId() != null
                ? request.metadata().recipientId().toString()
                : "unknown";
    }
}