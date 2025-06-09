package com.mailscheduler.application.service;

import com.mailscheduler.application.email.scheduling.PlanWithTemplatesRecipientsMap;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.model.schedule.*;
import com.mailscheduler.domain.model.template.Template;
import com.mailscheduler.domain.model.template.TemplateMetadata;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.TemplateRepository;
import com.mailscheduler.infrastructure.service.FollowUpManagementService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for creating and managing email communication plans.
 * <p>
 *     This service provides methods for creating follow-up plans, associating them
 *     with templates and recipients, and managing the scheduling of email communications.
 * </p>
 */
public class PlanService {
    private static final Logger LOGGER = Logger.getLogger(PlanService.class.getName());

    private final FollowUpManagementService followUpManagementService;
    private final TemplateRepository templateRepository;
    private final EntityId<FollowUpPlan> defaultPlanId;

    /**
     * Creates a new PlanService.
     *
     * @param followUpManagementService The service for managing follow-ups
     * @param templateRepository The repository for accessing templates
     * @throws NullPointerException if any required parameter is null
     */
    public PlanService(
            FollowUpManagementService followUpManagementService,
            TemplateRepository templateRepository
    ) {
        this.followUpManagementService = Objects.requireNonNull(followUpManagementService,
                "Follow-up management service cannot be null");
        this.templateRepository = Objects.requireNonNull(templateRepository,
                "Template repository cannot be null");
        this.defaultPlanId = EntityId.of(1L);
    }

    /**
     * Creates a communication plan for a list of recipients.
     *
     * @param recipients The recipients to create a plan for
     * @return A map of plans to recipients
     * @throws PlanCreationException If creating the plan fails
     */
    public PlanWithTemplatesRecipientsMap createPlanForRecipients(
            List<EntityData<Recipient, RecipientMetadata>> recipients) throws PlanCreationException {

        validateRecipients(recipients);

        try {
            LOGGER.info("Creating communication plan for " + recipients.size() + " recipients");

            // Create the plan
            PlanWithTemplate plan = createPlanWithTemplate();
            if (plan.getStepsWithTemplates().isEmpty()) {
                LOGGER.warning("Created plan has no steps");
                throw new PlanCreationException("Plan has no steps");
            }

            // Associate recipients with the plan
            PlanWithTemplatesRecipientsMap recipientsMap = new PlanWithTemplatesRecipientsMap(List.of(plan));
            List<Recipient> recipientEntities = extractRecipients(recipients);
            recipientsMap.addRecipients(plan, recipientEntities);

            LOGGER.info("Successfully created plan with " + plan.getStepsWithTemplates().size() +
                    " steps for " + recipients.size() + " recipients");
            return recipientsMap;
        } catch (PlanCreationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create plan for recipients", e);
            throw new PlanCreationException("Failed to create plan for recipients", e);
        }
    }

    /**
     * Gets the active plan from a map of plans to recipients.
     *
     * @param recipientsMap The map of plans to recipients
     * @return The first plan in the map, if any
     */
    public Optional<PlanWithTemplate> getActivePlan(PlanWithTemplatesRecipientsMap recipientsMap) {
        if (recipientsMap == null || recipientsMap.isEmpty()) {
            LOGGER.info("No active plan found - recipients map is empty");
            return Optional.empty();
        }

        Optional<PlanWithTemplate> firstPlan = recipientsMap.getMapping().keySet().stream().findFirst();
        if (firstPlan.isEmpty()) {
            LOGGER.info("No active plan found - no plans in map");
        } else {
            LOGGER.info("Found active plan with " + firstPlan.get().getStepsWithTemplates().size() + " steps");
        }

        return firstPlan;
    }

    /**
     * Gets the number of follow-up emails in a plan.
     *
     * @param plan The plan to check
     * @return The number of follow-up steps (excluding initial email)
     */
    public int getFollowupCount(PlanWithTemplate plan) {
        if (plan == null) {
            return 0;
        }

        return plan.getFollowUpCount();
    }

    /**
     * Creates a plan with a specific ID.
     *
     * @param planId The ID of the plan to create
     * @return The created plan
     * @throws PlanCreationException If creating the plan fails
     */
    public PlanWithTemplate createPlanWithTemplate(EntityId<FollowUpPlan> planId) throws PlanCreationException {
        Objects.requireNonNull(planId, "Plan ID cannot be null");

        try {
            LOGGER.info("Creating plan with ID: " + planId);
            List<EntityData<FollowUpStep, FollowUpStepMetadata>> followUpStepsData =
                    followUpManagementService.getStepsWithMetadata(planId);

            if (followUpStepsData.isEmpty()) {
                LOGGER.warning("No follow-up steps found for plan ID: " + planId);
                throw new PlanCreationException("No steps found for plan ID: " + planId);
            }

            PlanWithTemplate plan = new PlanWithTemplate();

            // Add each step to the plan with its template
            for (var stepData : followUpStepsData) {
                addStepToTemplate(plan, stepData);
            }

            LOGGER.info("Created plan with " + plan.getStepsWithTemplates().size() +
                    " steps for plan ID " + planId);
            return plan;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create plan with ID: " + planId, e);
            throw new PlanCreationException("Failed to create plan with ID: " + planId, e);
        }
    }

    /**
     * Creates a default plan.
     *
     * @return The default plan
     * @throws PlanCreationException If creating the plan fails
     */
    public PlanWithTemplate createDefaultPlan() throws PlanCreationException {
        try {
            LOGGER.info("Creating default plan");
            return createPlanWithTemplate(defaultPlanId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create default plan", e);
            throw new PlanCreationException("Failed to create default plan", e);
        }
    }

    // Private helper methods

    private void validateRecipients(List<EntityData<Recipient, RecipientMetadata>> recipients) {
        if (recipients == null) {
            throw new IllegalArgumentException("Recipients list cannot be null");
        }

        if (recipients.isEmpty()) {
            LOGGER.warning("Empty recipients list provided");
        }
    }

    private List<Recipient> extractRecipients(List<EntityData<Recipient, RecipientMetadata>> recipientData) {
        return recipientData.stream()
                .map(EntityData::entity)
                .collect(Collectors.toList());
    }

    private PlanWithTemplate createPlanWithTemplate() throws PlanCreationException {
        return createPlanWithTemplate(defaultPlanId);
    }

    private void addStepToTemplate(
            PlanWithTemplate plan,
            EntityData<FollowUpStep, FollowUpStepMetadata> stepData) {

        Optional<EntityData<Template, TemplateMetadata>> templateOpt =
                templateRepository.findByIdWithMetadata(stepData.metadata().templateId());

        if (templateOpt.isPresent()) {
            plan.addStep(new PlanStepWithTemplate(stepData.entity(), templateOpt.get().entity()));
            LOGGER.fine("Added step " + stepData.entity().getStepNumber() + " to plan");
        } else {
            LOGGER.warning("Template not found for step " + stepData.entity().getStepNumber() +
                    " with template ID " + stepData.metadata().templateId());
        }
    }
}