package com.mailscheduler.domain.schedule;

public record EntryId(int value) {
    public static EntryId of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Entry ID cannot be negative");
        }
        return new EntryId(value);
    }
}
