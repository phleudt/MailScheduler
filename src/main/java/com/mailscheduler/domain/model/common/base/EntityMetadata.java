package com.mailscheduler.domain.model.common.base;

/**
 * Marker interface for entity metadata.
 * <p>
 *     This interface serves as a common type for various kinds of metadata that
 *     can be associated with domain entities. Implementing classes provide specific
 *     metadata structures like creation timestamps, modification information, etc.
 * </p>
 * <p>
 *     Entities that don't require metadata can use the {@link NoMetadata} implementation
 * </p>
 */
public interface EntityMetadata {
    // Marker interface - no methods required
}
