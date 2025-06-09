package com.mailscheduler.domain.model.common.vo.email;

import java.util.regex.Pattern;

/**
 * Value object representing a valid email address.
 * <p>
 *     Ensures email addresses are properly formatted through validation
 *     against a relaxed regex for basic format checking.
 * </p>
 */
public record EmailAddress(String value) {
    /**
     * Regex pattern for validating email addresses.
     * Validates local part and domain according to common email format rules.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Creates a validated email address.
     *
     * @param value The email address string
     * @throws IllegalArgumentException if the email address format is invalid
     */
    public EmailAddress {
        if (value == null) {
            throw new IllegalArgumentException("Email address cannot be null");
        }

        value = value.strip();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty");
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }
    }

    /**
     * Factory method to create a new email address.
     *
     * @param value The email address string
     * @return A new validated EmailAddress
     * @throws IllegalArgumentException if the email address format is invalid
     */
    public static EmailAddress of(String value) throws IllegalArgumentException {
        return new EmailAddress(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EmailAddress that) {
            return this.value.equals(that.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return value;
    }
}
