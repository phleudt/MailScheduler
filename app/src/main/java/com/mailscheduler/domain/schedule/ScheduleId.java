package com.mailscheduler.domain.schedule;

public record ScheduleId(int value) {
    public static ScheduleId of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Schedule ID cannot be negative");
        }
        return new ScheduleId(value);
    }
}
