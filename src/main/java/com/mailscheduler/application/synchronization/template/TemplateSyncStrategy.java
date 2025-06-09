package com.mailscheduler.application.synchronization.template;

import com.mailscheduler.application.synchronization.template.gateway.GmailGateway;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import com.mailscheduler.domain.model.template.Template;
import com.mailscheduler.domain.model.template.TemplateMetadata;
import com.mailscheduler.domain.model.template.TemplateType;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.TemplateRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Strategy for synchronizing Gmail drafts with local template repository.
 * This class manages bidirectional synchronization, handling added, updated,
 * and deleted templates between Gmail and the local database.
 */
public class TemplateSyncStrategy {
    private static final Logger LOGGER = Logger.getLogger(TemplateSyncStrategy.class.getName());

    private final GmailGateway gmailGateway;
    private final TemplateRepository templateRepository;
    private final ConflictResolutionPolicy conflictPolicy;

    public TemplateSyncStrategy(GmailGateway gmailGateway, TemplateRepository templateRepository) {
        this.gmailGateway = gmailGateway;
        this.templateRepository = templateRepository;
        this.conflictPolicy = ConflictResolutionPolicy.PREFER_DRAFT;
    }

    /**
     * Synchronizes Gmail drafts with the local template repository.
     */
    public boolean synchronize() {
        SyncStats stats = new SyncStats();

        try {
            // Get all drafts from Gmail
            List<GmailGateway.GmailDraft> gmailDrafts = fetchGmailDrafts();
            stats.totalGmailDrafts = gmailDrafts.size();

            // Get all templates from the repository
            List<EntityData<Template, TemplateMetadata>> savedTemplates = fetchSavedTemplates();
            stats.totalLocalTemplates = savedTemplates.size();

            // Create maps for easier lookup
            Map<String, GmailGateway.GmailDraft> draftMap = createDraftMap(gmailDrafts);
            Map<String, EntityData<Template, TemplateMetadata>> draftIdToTemplateMap = createTemplateMap(savedTemplates);

            // Process each template that has a Gmail draft ID
            processExistingTemplates(savedTemplates, draftMap, stats);

            // Process remaining drafts in Gmail that don't correspond to templates
            processNewDrafts(draftMap, stats);

            LOGGER.info(String.format("Template synchronization completed: %d updated, %d added, %d removed, %d conflicts",
                    stats.templatesUpdated, stats.templatesAdded, stats.templatesDisconnected, stats.conflicts));

            return true;

        } catch (IOException e) {
            String errorMsg = "Failed to synchronize templates: " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMsg, e);
            return false;
        } catch (Exception e) {
            String errorMsg = "Unexpected error during template synchronization: " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMsg, e);
            return true;
        }
    }
    private List<GmailGateway.GmailDraft> fetchGmailDrafts() throws IOException {
        try {
            List<GmailGateway.GmailDraft> drafts = gmailGateway.listDrafts();
            LOGGER.info(String.format("Retrieved %d drafts from Gmail", drafts.size()));
            return drafts;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching drafts from Gmail", e);
            throw e;
        }
    }

    private List<EntityData<Template, TemplateMetadata>> fetchSavedTemplates() {
        List<EntityData<Template, TemplateMetadata>> templates = templateRepository.findAllWithMetadata();
        LOGGER.info(String.format("Retrieved %d templates from repository", templates.size()));
        return templates;
    }

    private Map<String, GmailGateway.GmailDraft> createDraftMap(List<GmailGateway.GmailDraft> gmailDrafts) {
        return gmailDrafts.stream()
                .collect(Collectors.toMap(GmailGateway.GmailDraft::id, Function.identity()));
    }

    private Map<String, EntityData<Template, TemplateMetadata>> createTemplateMap(
            List<EntityData<Template, TemplateMetadata>> savedTemplates) {
        return savedTemplates.stream()
                .filter(t -> t.metadata().draftId() != null && !t.metadata().draftId().isEmpty())
                .collect(Collectors.toMap(t -> t.metadata().draftId(), Function.identity()));
    }

    private void processExistingTemplates(List<EntityData<Template, TemplateMetadata>> savedTemplates,
                                          Map<String, GmailGateway.GmailDraft> draftMap, SyncStats stats) {

        for (EntityData<Template, TemplateMetadata> templateData : savedTemplates) {
            Template template = templateData.entity();
            String draftId = templateData.metadata().draftId();

            // Skip templates without draft ID
            if (draftId == null || draftId.isEmpty()) {
                continue;
            }

            GmailGateway.GmailDraft draft = draftMap.get(draftId);
            if (draft == null) {
                // Draft no longer exists in Gmail
                handleDeletedDraft(template, templateData.metadata(), stats);
            } else {
                // Both template and draft exist, check if they are in sync
                syncTemplateWithDraft(templateData, draft, stats);
            }

            // Remove processed draft from the map
            draftMap.remove(draftId);
        }
    }

    private void processNewDrafts(Map<String, GmailGateway.GmailDraft> draftMap, SyncStats stats) {
        for (GmailGateway.GmailDraft draft : draftMap.values()) {
            createTemplateFromDraft(draft, stats);
        }
    }

    private void handleDeletedDraft(Template template, TemplateMetadata metadata, SyncStats stats) {
        LOGGER.info(String.format("Draft %s was deleted from Gmail, updating template: %s",
                metadata.draftId(), template.getSubject().value()));

        // Draft was deleted from Gmail, update the template metadata to remove draft ID
        TemplateMetadata updatedMetadata = new TemplateMetadata(null);
        templateRepository.saveWithMetadata(template, updatedMetadata);
        stats.templatesDisconnected++;
    }

    private void syncTemplateWithDraft(EntityData<Template, TemplateMetadata> templateData,
                                       GmailGateway.GmailDraft draft, SyncStats stats) {

        Template template = templateData.entity();
        if (hasTemplateContentChanged(template, draft)) {
            LOGGER.info(String.format("Updating template '%s' with changes from Gmail draft",
                    template.getSubject().value()));

            Template updatedTemplate = new Template.Builder()
                    .from(template)
                    .setSubject(draft.subject())
                    .setBody(draft.body())
                    .build();

            templateRepository.saveWithMetadata(updatedTemplate, templateData.metadata());
            stats.templatesUpdated++;
        }
    }

    private boolean hasTemplateContentChanged(Template template, GmailGateway.GmailDraft draft) {
        return !template.getSubject().equals(draft.subject()) || !template.getBody().equals(draft.body());
    }

    private void createTemplateFromDraft(GmailGateway.GmailDraft draft, SyncStats stats) {
        // Check if a template with the draft's subject already exists
        Optional<EntityData<Template, TemplateMetadata>> existingTemplate =
                templateRepository.findBySubject(draft.subject());

        if (existingTemplate.isPresent()) {
            // A template with this name exists but doesn't have this draft ID
            handleConflict(existingTemplate.get(), draft, stats);
            return;
        }

        // Create a new template from the draft
        LOGGER.info(String.format("Creating new template from Gmail draft: %s", draft.subject().value()));

        Template newTemplate = new Template.Builder()
                .setSubject(draft.subject())
                .setBody(draft.body())
                .setType(determineTemplateType(draft.subject()))
                .build();

        TemplateMetadata metadata = new TemplateMetadata(draft.id());
        templateRepository.saveWithMetadata(newTemplate, metadata);
        stats.templatesAdded++;
    }

    private void handleConflict(EntityData<Template, TemplateMetadata> existingTemplate,
                                GmailGateway.GmailDraft draft, SyncStats stats) {

        stats.conflicts++;
        LOGGER.warning(String.format("Conflict detected: Template with subject '%s' exists but is not linked to draft %s",
                existingTemplate.entity().getSubject().value(), draft.id()));

        switch (conflictPolicy) {
            case PREFER_EXISTING:
                // Do nothing, keep existing template as is
                break;

            case PREFER_DRAFT:
                // Update existing template with draft content and link it
                Template updatedTemplate = new Template.Builder()
                        .from(existingTemplate.entity())
                        .setBody(draft.body())
                        .build();

                TemplateMetadata updatedMetadata = new TemplateMetadata(draft.id());
                templateRepository.saveWithMetadata(updatedTemplate, updatedMetadata);
                stats.templatesUpdated++;
                break;

            case CREATE_NEW:
                // Create a new template with a modified name
                String newSubjectValue = existingTemplate.entity().getSubject().value() + " (Gmail)";
                Template newTemplate = new Template.Builder()
                        .setSubject(new Subject(newSubjectValue))
                        .setBody(draft.body())
                        .setType(determineTemplateType(draft.subject()))
                        .build();

                TemplateMetadata newMetadata = new TemplateMetadata(draft.id());
                templateRepository.saveWithMetadata(newTemplate, newMetadata);
                stats.templatesAdded++;
                break;
        }
    }

    private TemplateType determineTemplateType(Subject subject) {
        if (subject == null || subject.value() == null) {
            return TemplateType.INITIAL;
        }

        String lowerCaseSubject = subject.value().toLowerCase();

        if (lowerCaseSubject.contains("follow up") ||
                lowerCaseSubject.contains("followup") ||
                lowerCaseSubject.contains("follow-up") ||
                (lowerCaseSubject.contains("follow") && lowerCaseSubject.contains("up"))
        ) {
            return TemplateType.FOLLOW_UP;
        }
        return TemplateType.INITIAL;
    }

    /**
     * Policy for resolving conflicts when a draft matches a template by subject
     * but they are not linked by draft ID
     */
    public enum ConflictResolutionPolicy {
        /** Keep existing template unchanged */
        PREFER_EXISTING,

        /** Update existing template with draft content and link it */
        PREFER_DRAFT,

        /** Create a new template with modified name to avoid conflict */
        CREATE_NEW
    }

    /**
     * Internal class for tracking synchronization statistics
     */
    private static class SyncStats {
        int totalGmailDrafts = 0;
        int totalLocalTemplates = 0;
        int templatesUpdated = 0;
        int templatesAdded = 0;
        int templatesDisconnected = 0;
        int conflicts = 0;
    }
}
