package com.mailscheduler.domain.common;

public enum SendStatus {
    SENT,           // Email successfully sent
    ALREADY_REPLIED, // Email thread already has replies
    SENDING_ERROR    // Failed to send due to an error
}
