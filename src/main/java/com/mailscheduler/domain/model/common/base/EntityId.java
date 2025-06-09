package com.mailscheduler.domain.model.common.base;

import java.util.Objects;

/**
 * Type-safe identifier for domain entities.
 * This record provides a generic wrapper around a Long ID value to ensure type safety across different entity types.
 *
 * @param <T> The entity type this ID belongs to.
 * @param value The underlying Long value of the ID
 */
public record EntityId<T>(Long value) {
    /**
     * Constructor with null validation
     *
     * @param value The Long value to be used as the ID
     * @throws NullPointerException if value is null
     */
    public EntityId {
        Objects.requireNonNull(value, "Entity ID value cannot be null");
    }

    /**
     * Factory method to create a new EntityId.
     *
     * @param <T> The entity type this ID belongs to
     * @param value The underlying Long value of the ID
     * @return A new EntityId instance
     * @throws NullPointerException if value is null
     */
    public static <T> EntityId<T> of(Long value) {
        return new EntityId<>(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + value + ">";
    }
}
