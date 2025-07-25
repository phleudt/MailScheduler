package com.mailscheduler.domain.model.common.vo.email;

/**
 * Value object representing an email body content.
 * <p>
 *     Provides a wrapper for email body text with validation and utility methods.
 * </p>
 */
public record Body(String value) {
    /**
     * Creates a validated email body.
     *
     * @param value The body text
     * @throws IllegalArgumentException if the body is null
     */
    public Body {
        if (value == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
    }

    /**
     * Factory method to create a new body.
     *
     * @param value The body text
     * @return A new validated Body
     * @throws IllegalArgumentException if the body is null
     */
    public static Body of(String value) {
        return new Body(value);
    }

    public boolean isEmpty() {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Body that) {
            return this.value.equals(that.value);
        }
        return false;
    }

    @Override
    public String toString() {
        if (value.length() > 50) {
            return value.substring(0, 47) + "...";
        }
        return value;
    }
}
