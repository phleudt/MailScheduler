package com.mailscheduler.domain.model.common.base;

import java.util.Objects;

/**
 * Container for entity and its metadata
 */
public record EntityData<T, M>(T entity, M metadata) {
    public EntityData {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
    }

    /**
     * Creates a new EntityData instance with the given entity and metadata
     *
     * @param entity   The entity
     * @param metadata The metadata
     * @return A new EntityData instance
     * @throws NullPointerException if entity or metadata is null
     */
    public static <T, M> EntityData<T, M> of(T entity, M metadata) {
        return new EntityData<>(entity, metadata);
    }
}
