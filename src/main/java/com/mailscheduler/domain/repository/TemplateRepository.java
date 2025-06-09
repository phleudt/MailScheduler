package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import com.mailscheduler.domain.model.template.Template;
import com.mailscheduler.domain.model.template.TemplateMetadata;
import com.mailscheduler.domain.model.template.TemplateType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Template entities.
 * <p>
 *     Templates are reusable email content patterns that can be personalized for different
 *     recipients and used in various stages of email sequences.
 * </p>
 */
public interface TemplateRepository extends Repository<Template, TemplateMetadata> {
    /**
     * Finds a template by its Gmail draft ID.
     *
     * @param draftId The ID of the Gmail draft
     * @return An Optional containing the template with its metadata if found, empty otherwise
     */
    Optional<EntityData<Template, TemplateMetadata>> findByDraftId(String draftId);

    /**
     * Finds a template by its email subject.
     *
     * @param subject The subject of the email template
     * @return An Optional containing the template with its metadata if found, empty otherwise
     */
    Optional<EntityData<Template, TemplateMetadata>> findBySubject(Subject subject);

    /**
     * Finds all templates of a specific type.
     *
     * @param type The template type to filter by
     * @return A list of templates with their metadata matching the specified type
     */
    List<EntityData<Template, TemplateMetadata>> findByType(TemplateType type);

    /**
     * Finds all default templates of a specific type.
     *
     * @param type The template type to filter by
     * @return A list of default templates with their metadata matching the specified type
     */
    List<EntityData<Template, TemplateMetadata>> findDefaultTemplates(TemplateType type);

}
