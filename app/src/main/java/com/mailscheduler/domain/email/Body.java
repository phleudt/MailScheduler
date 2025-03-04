package com.mailscheduler.domain.email;

public record Body(String value) {
    public Body {
        if (value == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
    }

    public static Body of(String value) {
        return new Body(value);
    }
}
