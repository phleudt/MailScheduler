package com.mailscheduler.application.email.sending;

/**
 * Enum representing the status of an email sending operation.
 */
public enum SendStatus {
    SUCCESS("Email sent successfully"),
    FAILURE("Failed to send email");

    private final String description;

    SendStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns a user-friendly status message suitable for display in spreadsheets.
     */
    public String getUserFriendlyStatus() {
        return switch (this) {
            case SUCCESS -> "Gesendet";
            case FAILURE -> "Failed";
        };
    }
}
