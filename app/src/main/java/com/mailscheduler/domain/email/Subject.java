package com.mailscheduler.domain.email;

public record Subject(String value) {
    public Subject {
        if (value == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        if (value.length() > 200) { // TODO: find reasonable length limit
            throw new IllegalArgumentException("Subject cannot be longer than ___ characters");
        }
    }

    public static Subject of(String value) {
        return new Subject(value);
    }

    public String truncated(int length) {
        if (value.length() <= length) return value;
        return value.substring(0, length) + "...";
    }
}
