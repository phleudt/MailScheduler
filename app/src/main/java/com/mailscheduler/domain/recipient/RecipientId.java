package com.mailscheduler.domain.recipient;

public record RecipientId(int value) {
    public static RecipientId of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Recipient ID cannot be negative");
        }
        return new RecipientId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
