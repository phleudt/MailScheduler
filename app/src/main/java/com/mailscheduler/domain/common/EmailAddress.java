package com.mailscheduler.domain.common;

import java.util.regex.Pattern;

public record EmailAddress(String value) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public EmailAddress {
        if (value != null) {
            value = value.strip();
        }
        validate(value);
    }

    private static void validate(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }

    public static EmailAddress of(String value) throws IllegalArgumentException {
        return new EmailAddress(value);
    }
}
