package com.mailscheduler.domain.model.common.base;

import java.util.Objects;

/**
 * Abstract base class for domain entities that have a unique identifier.
 * <p>
 *     This class provides a standard implementation of the {@link Identifiable} interface that can be extended by
 *     concrete entity classes. It manages the entity's ID and implements common functionality like equality
 *     based on entity identity.
 * </p>
 *
 * @param <T> The entity type this ID belongs to
 */
public abstract class IdentifiableEntity<T> implements Identifiable<T> {
    private EntityId<T> id;

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityId<T> getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setId(EntityId<T> id) {
        this.id = id;
    }

    /**
     * Checks if this entity has been assigned an ID.
     *
     * @return true if the entity has an ID, false otherwise assigned an ID.*
     */
    public boolean hasId() {
        return id != null;
    }

    /**
     * Determines equality based on entity IDs
     * <p>
     *     Two entities are considered equal if thy are of the same class and have the same ID.
     *     Entities without an ID are only equal to themselves.
     * </p>
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentifiableEntity<?> that = (IdentifiableEntity<?>) o;

        // if both entities have IDs, compare the IDs
        if (this.id != null && that.id != null) {
            return Objects.equals(this.id.value(), that.id.value());
        }

        // If either entity lacks an ID, they're qual only if they're the same instance
        return Objects.equals(this.id, that.id);
    }

    /**
     * Generate a hash code based on the entity ID.
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (id != null ? " [id=" + id.value() + "]" : "[unsaved]");
    }
}
