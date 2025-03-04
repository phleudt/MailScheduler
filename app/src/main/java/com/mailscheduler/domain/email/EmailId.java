package com.mailscheduler.domain.email;

public record EmailId(int value) {
    public static EmailId of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Email ID cannot be negative");
        }
        return new EmailId(value);
    }
}
