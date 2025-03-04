package com.mailscheduler.domain.email;

public enum EmailStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isSent() {
        return this == SENT;
    }
}
