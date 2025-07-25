package com.mailscheduler.application.email.scheduling;


import com.mailscheduler.domain.factory.EmailFactory;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.schedule.PlanWithTemplate;
import com.mailscheduler.domain.model.template.Template;
import com.mailscheduler.domain.repository.EmailRepository;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EmailScheduler {
    private static final Logger LOGGER = Logger.getLogger(EmailScheduler.class.getName());

    private final EmailRepository emailRepository;
    private final EmailFactory emailFactory;
    private final PlaceholderResolver placeholderResolver;

    public EmailScheduler(
            EmailAddress defaultSenderEmail,
            EmailRepository emailRepository,
            PlaceholderResolver placeholderResolver
    ) {
        this.emailRepository = emailRepository;
        this.emailFactory = new EmailFactory(defaultSenderEmail);
        this.placeholderResolver = placeholderResolver;
    }

    /**
     * Schedules emails for multiple recipients based on their assigned plans.
     */
    public RecipientScheduledEmailsMap scheduleEmailsAfterPlans(PlanWithTemplatesRecipientsMap plansWithRecipients) {
        if (plansWithRecipients == null || plansWithRecipients.isEmpty()) {
            return RecipientScheduledEmailsMap.empty();
        }

        RecipientScheduledEmailsMap scheduledEmailsMap = new RecipientScheduledEmailsMap(new HashMap<>());

        for (Map.Entry<PlanWithTemplate, List<Recipient>> entry : plansWithRecipients.getMapping().entrySet()) {
            try {
                RecipientScheduledEmailsMap scheduledEmailsMap1 = scheduleEmailsForRecipients(entry.getKey(), entry.getValue());
                scheduledEmailsMap.putAll(scheduledEmailsMap1);
            } catch (EmailSchedulingException e) {
                LOGGER.log(Level.WARNING,
                        "Failed to schedule emails for plan: " + entry.getKey().getStepsWithTemplates(), e);
            }

        }

        return scheduledEmailsMap;
    }

    /**
     * Schedules emails for a list of recipients according to a specific plan.
     */
    private RecipientScheduledEmailsMap scheduleEmailsForRecipients(PlanWithTemplate plan, List<Recipient> recipients)
            throws EmailSchedulingException {
        validatePlanAndRecipients(plan, recipients);

        RecipientScheduledEmailsMap scheduledEmailsMap = new RecipientScheduledEmailsMap(new HashMap<>());

        for (Recipient recipient : recipients) {
            try {
                List<EntityData<Email, EmailMetadata>> scheduledEmails = scheduleEmailsForRecipient(plan, recipient);
                scheduledEmailsMap.addAll(recipient.getId(), scheduledEmails);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "Failed to schedule emails for recipient: " + recipient.getId(), e);
                // Continue with next recipient
            }
        }

        return scheduledEmailsMap;
    }

    private void validatePlanAndRecipients(PlanWithTemplate plan, List<Recipient> recipients) throws EmailSchedulingException {
        if (plan == null) {
            throw new EmailSchedulingException("Plan cannot be null");
        }
        if (recipients == null || recipients.isEmpty()) {
            throw new EmailSchedulingException("Recipients cannot be null or empty");
        }
        if (plan.getStepsWithTemplates() == null || plan.getStepsWithTemplates().isEmpty()) {
            throw new EmailSchedulingException("Plan must contain at least one step with template");
        }
    }

    private List<EntityData<Email, EmailMetadata>> scheduleEmailsForRecipient(PlanWithTemplate plan, Recipient recipient)
            throws EmailSchedulingException {
        if (recipient == null) {
            throw new EmailSchedulingException("Recipient cannot be null");
        }

        try {
            int maxStepNumber = plan.getStepsWithTemplates().size() - 1; // 0-based index
            EmailSchedulingContext context = createEmailSchedulingContext(recipient, maxStepNumber);

            // Different strategies based on current email scheduling status
            return switch (context.getSchedulingStatus()) {
                case NO_EMAILS_SCHEDULED -> scheduleCompleteEmailSequence(plan, recipient);
                case PARTIAL_SEQUENCE_SCHEDULED -> scheduleRemainingSequence(plan, recipient, context);
                case SEQUENCE_COMPLETE, NO_SCHEDULING_REQUIRED -> List.of();
            };
        } catch (Exception e) {
            throw new EmailSchedulingException(
                    "Failed to schedule emails for recipient: " + recipient.getSalutation(), e);
        }
    }

    /**
     * Creates and schedules a complete sequence of emails (initial + follow-ups).
     */
    private List<EntityData<Email, EmailMetadata>> scheduleCompleteEmailSequence(PlanWithTemplate plan, Recipient recipient)
            throws EmailSchedulingException {
        List<EntityData<Email, EmailMetadata>> scheduledEmails = new ArrayList<>();

        // Schedule initial email
        EntityData<Email, EmailMetadata> initialEmail = createAndScheduleInitialEmail(plan, recipient);
        scheduledEmails.add(initialEmail);

        // Track last scheduled date for follow-ups
        LocalDate lastScheduledDate = initialEmail.metadata().scheduledDate();

        // Schedule follow-up emails
        scheduledEmails.addAll(
                scheduleFollowUpEmails(plan, recipient, initialEmail, lastScheduledDate)
        );

        return scheduledEmails;
    }

    /**
     * Creates and schedules the initial email for a recipient.
     */
    private EntityData<Email, EmailMetadata> createAndScheduleInitialEmail(
            PlanWithTemplate plan, Recipient recipient) throws EmailSchedulingException {

        if (plan.getStepsWithTemplates().isEmpty() || plan.getStepsWithTemplates().get(0) == null) {
            throw new EmailSchedulingException("Plan must have an initial template (step 0)");
        }

        Template initialTemplate = plan.getStepsWithTemplates().get(0).template();
        Template resolvedTemplate = resolveTemplateForRecipient(initialTemplate, recipient);

        EntityData<Email, EmailMetadata> initialEmail =
                emailFactory.createInitialEmail(recipient, resolvedTemplate);

        try {
            var initialEmailData = emailRepository.saveWithMetadata(initialEmail.entity(), initialEmail.metadata());

            // Set initial email id to itself
            EmailMetadata updatedMetadata = new EmailMetadata.Builder()
                    .from(initialEmail.metadata())
                    .initialEmailId(initialEmailData.entity().getId())
                    .build();

            return emailRepository.saveWithMetadata(initialEmailData.entity(), updatedMetadata);
        } catch (Exception e) {
            throw new EmailSchedulingException("Failed to save initial email", e);
        }
    }

    /**
     * Schedules all follow-up emails for a recipient after the initial email.
     */
    private List<EntityData<Email, EmailMetadata>> scheduleFollowUpEmails(
            PlanWithTemplate plan,
            Recipient recipient,
            EntityData<Email, EmailMetadata> initialEmail,
            LocalDate startDate) throws EmailSchedulingException {

        List<EntityData<Email, EmailMetadata>> followUpEmails = new ArrayList<>();
        LocalDate nextScheduledDate = startDate;

        // Skip the first step (initial email) and schedule follow-ups
        for (int i = 1; i < plan.getStepsWithTemplates().size(); i++) {
            var step = plan.getStepsWithTemplates().get(i);
            int waitDays = step.step().getWaitPeriod();

            // Calculate next email date
            nextScheduledDate = nextScheduledDate.plusDays(waitDays);

            // Create follow-up email
            EntityData<Email, EmailMetadata> followUpEmail = createAndScheduleFollowUpEmail(
                    recipient,
                    step.template(),
                    nextScheduledDate,
                    step.step().getStepNumber(),
                    initialEmail.entity()
            );

            followUpEmails.add(followUpEmail);
        }

        return followUpEmails;
    }

    /**
     * Schedules the remaining emails in a sequence when some emails have already been sent.
     */
    private List<EntityData<Email, EmailMetadata>> scheduleRemainingSequence(
            PlanWithTemplate plan,
            Recipient recipient,
            EmailSchedulingContext context
    ) throws EmailSchedulingException {
        Optional<EntityId<Email>> initialEmailIdOpt = context.getInitialEmailId();
        if (initialEmailIdOpt.isEmpty()) {
            throw new EmailSchedulingException("Cannot complete sequence: Initial email ID not found");
        }

        Optional<EntityData<Email, EmailMetadata>> initialEmailOpt = context.getInitialEmail();
        if (initialEmailOpt.isEmpty()) {
            throw new EmailSchedulingException("Cannot complete sequence: Initial email not found");
        }

        // Get the last scheduled email's date to base our new schedules on
        LocalDate lastScheduledDate = context.getLastScheduledDate();
        if (lastScheduledDate == null) {
            throw new EmailSchedulingException("Cannot determine last scheduled email date");
        }

        List<EntityData<Email, EmailMetadata>> newFollowUps = new ArrayList<>();
        int currentFollowUpNumber = context.getCurrentFollowupNumber();
        int maxFollowUps = plan.getStepsWithTemplates().size() - 1; // -1 because we count from 0

        // Skip scheduled steps and continue with remaining ones
        for (int i = currentFollowUpNumber + 1; i <= maxFollowUps; i++) {
            var step = plan.getStepsWithTemplates().get(i);
            int waitDays = step.step().getWaitPeriod();

            // Calculate when to send the next email
            LocalDate nextEmailDate = lastScheduledDate.plusDays(waitDays);

            EntityData<Email, EmailMetadata> followUpEmail = createAndScheduleFollowUpEmail(
                    recipient,
                    step.template(),
                    nextEmailDate,
                    step.step().getStepNumber(),
                    initialEmailOpt.get().entity()
            );

            newFollowUps.add(followUpEmail);
            lastScheduledDate = nextEmailDate; // Update for next iteration
        }

        return newFollowUps;
    }

    /**
     * Creates and schedules a follow-up email.
     */
    private EntityData<Email, EmailMetadata> createAndScheduleFollowUpEmail(
            Recipient recipient,
            Template template,
            LocalDate scheduledDate,
            int followUpNumber,
            Email initialEmail
    ) throws EmailSchedulingException {
        try {
            // Resolve template with recipient data
            Template resolvedTemplate = resolveTemplateForRecipient(template, recipient);

            // Add "Re:" prefix to follow-up emails
            Subject followUpSubject = createFollowUpSubject(initialEmail.getSubject());
            resolvedTemplate = updateTemplateSubject(resolvedTemplate, followUpSubject);

            // Create follow-up email
            EntityData<Email, EmailMetadata> followUpEmail =
                    emailFactory.createFollowUpEmail(
                            recipient,
                            resolvedTemplate,
                            scheduledDate,
                            followUpNumber,
                            initialEmail.getId()
                    );

            // Save to repository
            emailRepository.saveWithMetadata(followUpEmail.entity(), followUpEmail.metadata());
            return followUpEmail;
        } catch (Exception e) {
            throw new EmailSchedulingException("Failed to create or save follow-up email", e);
        }
    }

    /**
     * Creates a subject line for follow-up emails by adding "Re:" prefix if needed.
     */
    private Subject createFollowUpSubject(Subject originalSubject) {
        String subjectText = originalSubject.value();
        if (!subjectText.startsWith("Re:")) {
            return Subject.of("Re: " + subjectText);
        }
        return originalSubject;
    }

    /**
     * Updates a template with a new subject.
     */
    private Template updateTemplateSubject(Template template, Subject subject) {
        return new Template.Builder()
                .setId(template.getId())
                .setType(template.getType())
                .setSubject(subject)
                .setBody(template.getBody())
                .setPlaceholderManager(template.getPlaceholderManager())
                .build();
    }

    /**
     * Resolves placeholders in a template for a specific recipient.
     */
    private Template resolveTemplateForRecipient(Template template, Recipient recipient)
            throws EmailSchedulingException {
        try {
            Body resolvedBody = placeholderResolver.resolveTemplatePlaceholders(
                    template.getBody(),
                    template.getPlaceholderManager(),
                    recipient
            );

            // Create a new template instance with resolved content
            return new Template.Builder()
                    .setId(template.getId())
                    .setType(template.getType())
                    .setSubject(template.getSubject())
                    .setBody(resolvedBody)
                    .setPlaceholderManager(null) // No longer needed after resolution
                    .build();
        } catch (Exception e) {
            throw new EmailSchedulingException("Failed to resolve template for recipient: " +
                    recipient.getSalutation(), e);
        }
    }

    /**
     * Creates a context object with information about current email scheduling state.
     */
    private EmailSchedulingContext createEmailSchedulingContext(Recipient recipient, int maxFollowupNumber) {
        if (recipient == null || recipient.getId() == null) {
            return new EmailSchedulingContext(List.of(), maxFollowupNumber);
        }

        List<EntityData<Email, EmailMetadata>> emailsWithMetadata =
                emailRepository.findByRecipientId(recipient.getId());

        return new EmailSchedulingContext(emailsWithMetadata, maxFollowupNumber);
    }
}