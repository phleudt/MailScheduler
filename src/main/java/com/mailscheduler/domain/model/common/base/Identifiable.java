package com.mailscheduler.domain.model.common.base;

/**
 * Interface for entities that can be identified in persistence.
 * <p>
 *     This interface defines the contract for domain entities that have a unique identifier.
 *     Implementing this interface enables entities to be uniquely identified within the system and provides a
 *     consistent way to handle entity identity.
 * </p>
 *
 * @param <T> The entity type this ID belongs to
 */
public interface Identifiable<T> {

    /**
     * Gets the entity's unique identifier
     */
    EntityId<T> getId();

    /**
     * Sets the entity's unique identifier.
     */
    void setId(EntityId<T> id);
}