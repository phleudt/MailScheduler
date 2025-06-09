package com.mailscheduler.domain.model.common.vo;

/**
 * Value object representing an email thread identifier.
 * <p>
 *     Thread IDs are used to group related emails together in a conversation.
 *     This allows the system to track replies and maintain conversation context.
 * </p>
 */
public record ThreadId(String value) {
    /**
     * Factory method to create a new ThreadId.
     *
     * @param value The thread ID value
     * @return A new ThreadId instance
     * @throws IllegalArgumentException if the value is null or empty
     */
    public static ThreadId of(String value) {
        return new ThreadId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
