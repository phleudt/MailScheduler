package com.mailscheduler.infrastructure.persistence.entity;

import java.util.Objects;

/**
 * Abstract base class for all database table entities.
 * Provides common functionality and properties for all entities.
 */
public abstract class TableEntity {
    private Long id;

    /**
     * Gets the entity's unique identifier.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the entity's unique identifier.
     */
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableEntity that = (TableEntity) o;

        // If both entities have IDs, compare them
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }

        // Otherwise, they're only equal if they're the same object instance
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                '}';
    }
}