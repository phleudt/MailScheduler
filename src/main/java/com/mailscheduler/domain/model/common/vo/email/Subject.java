package com.mailscheduler.domain.model.common.vo.email;

/**
 * Value object representing an email subject line.
 * <p>
 *     Provides validation for subject text, enforcing length constraints and ensuring content is valid for use
 *     in email communications.
 * </p>
 */
public record Subject(String value) {
    /**
     * The maximum allowed length for an email subject.
     */
    public static final int MAX_LENGTH = 200;

    /**
     * Creates a validated email subject.
     *
     * @param value The subject text
     * @throws IllegalArgumentException if the subject is invalid
     */
    public Subject {
        if (value == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }

        // Normalize by trimming whitespace
        value = value.strip();

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Subject cannot be longer than " + MAX_LENGTH + " characters " +
                    "(current: " + value.length() + ")");
        }
    }

    /**
     * Factory method to create a new subject.
     *
     * @param value The subject text
     * @return A new validated Subject
     * @throws IllegalArgumentException if the subject is invalid
     */
    public static Subject of(String value) throws IllegalArgumentException {
        return new Subject(value);
    }

    /**
     * Returns a truncated version of the subject.
     * If the subject is shorter than the specified length, it is returned unchanged.
     * If longer, it will be truncated and appended with "...".
     *
     * @param length The maximum length before truncation
     * @return The truncated subject text
     */
    public String truncated(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Truncation length cannot be negative");
        }

        if (value.length() <= length) {
            return value;
        }

        return value.substring(0, length) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Subject that) {
            return this.value.equals(that.value());
        }
        return false;
    }

    @Override
    public String toString() {
        return value;
    }
}
