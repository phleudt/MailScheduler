package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.EntityMetadata;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.infrastructure.persistence.repository.exception.RepositoryException;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface that defines standard CRUD operations for domain entities.
 * Each entity type can have associated metadata that is stored alongside it.
 *
 * @param <T> The domain entity type that extends IdentifiableEntity
 * @param <M> The metadata type that extends EntityMetadata
 */
public interface Repository <T extends IdentifiableEntity<T>, M extends EntityMetadata> {

    /**
     * Creates a new entity with metadata in the database.
     *
     * @param entity The entity to create
     * @param metadata The metadata associated with the entity
     * @return The created entity with its metadata wrapped in EntityData
     */
    EntityData<T, M> createWithMetadata(T entity, M metadata) throws RepositoryException;

    /**
     * Updates an existing entity with metadata in the database.
     *
     * @param entity The entity to update
     * @param metadata The metadata associated with the entity
     * @return The updated entity with its metadata wrapped in EntityData
     */
    EntityData<T, M> updateWithMetadata(T entity, M metadata) throws RepositoryException;

    /**
     * Saves (creates or updates) an entity with metadata based on whether it has an ID.
     *
     * @param entity The entity to save
     * @param metadata The metadata associated with the entity
     * @return The saved entity with its metadata wrapped in EntityData
     */
    default EntityData<T, M> saveWithMetadata(T entity, M metadata) throws RepositoryException {
        return entity.getId() == null ?
                createWithMetadata(entity, metadata) :
                updateWithMetadata(entity, metadata);
    }

    /**
     * Finds an entity and its metadata by ID.
     *
     * @param id The ID of the entity to find
     * @return An Optional containing the entity with its metadata if found, empty otherwise
     */
    Optional<EntityData<T, M>> findByIdWithMetadata(EntityId<T> id);

    /**
     * Lists all entities with their metadata.
     *
     * @return A list of all entities with their metadata
     */
    List<EntityData<T, M>> findAllWithMetadata();

    /**
     * Deletes an entity by its ID.
     *
     * @param id The ID of the entity to delete
     */
    void delete(EntityId<T> id);

    /**
     * Checks if an entity exists by its ID.
     *
     * @param id The ID of the entity to check
     * @return true if the entity exists, false otherwise
     */
    boolean exists(EntityId<T> id);
}
