package com.mailscheduler.domain.model.template;

import com.mailscheduler.domain.model.common.base.EntityMetadata;

/**
 * Metadata associated with an email template.
 * <p>
 * This record contains persistence-specific data related to a template,
 * such as draft IDs for external system references and creation/modification dates.
 * It implements the EntityMetadata interface to support the repository pattern.
 * </p>
 */
public record TemplateMetadata(String draftId) implements EntityMetadata {

    @Override
    public String toString() {
        return "TemplateMetadata{ draftId=" + draftId + "}";
    }
}
