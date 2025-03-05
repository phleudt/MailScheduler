package com.mailscheduler.domain.email;

public enum EmailStatus {
    PENDING, // TODO: Change to SCHEDULED
    SENT,
    REPLIED,
    FAILED,
    CANCELLED;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isSent() {
        return this == SENT;
    }
}
