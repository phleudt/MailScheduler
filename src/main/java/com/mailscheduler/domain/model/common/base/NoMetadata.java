package com.mailscheduler.domain.model.common.base;

/**
 * Represents a null object for entities that don't require metadata.
 * <p>
 *     This class implements the Null Object pattern to maintain consistency in the repository pattern while allowing
 *     entities to opt-out of metadata. It should be used when an entity doesn't require any additional metadata to be
 *     stored.
 * </p>
 */
public record NoMetadata() implements EntityMetadata {
    // Singleton instance to avoid unnecessary object creation
    public static final NoMetadata INSTANCE = new NoMetadata();

    /**
     * Gets the singleton instance of NoMetadata
     */
    public static NoMetadata getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "NoMetadata";
    }
}