package com.mailscheduler.domain.recipient;

public record FullName(String value) {
    public FullName {
        // if (value == null) throw new IllegalArgumentException("Full Name cannot be null");
        value = value != null ? value.trim() : "";
    }

    public static FullName of(String value) throws IllegalArgumentException {
        return new FullName(value);
    }
}
