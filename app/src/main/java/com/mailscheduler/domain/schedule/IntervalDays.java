package com.mailscheduler.domain.schedule;

public record IntervalDays(int value) {
    public IntervalDays {
        if (value < 0) {
            throw new IllegalArgumentException("Interval days cannot be negative");
        }
    }

    public static IntervalDays of(int value) {
        return new IntervalDays(value);
    }
}