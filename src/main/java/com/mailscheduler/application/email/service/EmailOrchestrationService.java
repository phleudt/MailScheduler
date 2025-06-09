package com.mailscheduler.application.email.service;

import com.mailscheduler.application.email.EmailService;
import com.mailscheduler.application.email.exception.EmailOperationException;
import com.mailscheduler.application.email.scheduling.PlanWithTemplatesRecipientsMap;
import com.mailscheduler.application.email.scheduling.RecipientScheduledEmailsMap;
import com.mailscheduler.application.email.sending.EmailSendRequest;
import com.mailscheduler.application.email.sending.EmailSendingResult;
import com.mailscheduler.application.recipient.RecipientService;
import com.mailscheduler.application.recipient.exception.RecipientOperationException;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.config.ApplicationConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.model.schedule.*;
import com.mailscheduler.domain.repository.*;
import com.mailscheduler.domain.service.ContactService;
import com.mailscheduler.application.service.PlanService;
import com.mailscheduler.infrastructure.spreadsheet.SpreadsheetService;
import com.mailscheduler.infrastructure.spreadsheet.exception.SpreadsheetOperationException;


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates the email workflow from identifying recipients to sending emails
 * and updating spreadsheets with the results.
 */
public class EmailOrchestrationService {
    private static final Logger LOGGER = Logger.getLogger(EmailOrchestrationService.class.getName());

    private final EmailService emailService;
    private final ConfigurationRepository configRepository;
    private final SpreadsheetService spreadsheetService;
    private final RecipientService recipientService;
    private final ContactService contactService;
    private final PlanService planService;

    public EmailOrchestrationService(
            EmailService emailService,
            ConfigurationRepository configRepository,
            SpreadsheetService spreadsheetService,
            RecipientService recipientService,
            ContactService contactService,
            PlanService planService
    ) {
        this.emailService = Objects.requireNonNull(emailService, "Email service cannot be null");
        this.configRepository = Objects.requireNonNull(configRepository, "Configuration repository cannot be null");
        this.spreadsheetService = Objects.requireNonNull(spreadsheetService, "Spreadsheet service cannot be null");
        this.recipientService = Objects.requireNonNull(recipientService, "Recipient service cannot be null");
        this.contactService = Objects.requireNonNull(contactService, "Contact service cannot be null");
        this.planService = Objects.requireNonNull(planService, "Plan service cannot be null");    }

    /**
     * Main method to orchestrate the email workflow - from identifying recipients
     * to sending emails and updating spreadsheets.
     *
     * @param saveAsDraft Whether to save emails as drafts instead of sending them
     * @throws OrchestrationException If any part of the workflow fails
     */
    public void processPendingEmailsAndSend(boolean saveAsDraft) throws OrchestrationException {
        LOGGER.info("Starting email orchestration workflow" + (saveAsDraft ? " (draft mode)" : ""));

        try {
            // Get application configuration
            ApplicationConfiguration appConfig = getConfiguration();

            scheduleEmails(appConfig);

            // Send initial emails for eligible recipients
            List<EmailSendingResult> sendingResults = sendEmails(appConfig, saveAsDraft);

            // Update spreadsheet with sending results
            if (!sendingResults.isEmpty()) {
                updateSpreadsheetWithSendingResults(
                        appConfig,
                        sendingResults
                );
            }
            LOGGER.info("Email orchestration workflow completed successfully");
        } catch (RecipientOperationException e) {
            LOGGER.log(Level.SEVERE, "Recipient processing error during email orchestration", e);
            throw new OrchestrationException("Error processing recipients: " + e.getMessage(), e);
        } catch (EmailOperationException e) {
            LOGGER.log(Level.SEVERE, "Email operation error during orchestration", e);
            throw new OrchestrationException("Error with email operations: " + e.getMessage(), e);
        } catch (SpreadsheetOperationException e) {
            LOGGER.log(Level.SEVERE, "Spreadsheet error during email orchestration", e);
            throw new OrchestrationException("Error updating spreadsheet: " + e.getMessage(), e);
        } catch (PlanCreationException e) {
            LOGGER.log(Level.SEVERE, "Plan creation error during email orchestration", e);
            throw new OrchestrationException("Error creating plan: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during email orchestration", e);
            throw new OrchestrationException("Unexpected error: " + e.getMessage(), e);
        }
    }

    private List<EntityData<Recipient, RecipientMetadata>> scheduleEmails(ApplicationConfiguration appConfig) throws RecipientOperationException, SpreadsheetOperationException, PlanCreationException, EmailOperationException {
        // Process spreadsheet to identify eligible recipients
        List<EntityData<Recipient, RecipientMetadata>> eligibleRecipients = identifyEligibleRecipients(appConfig);

        if (eligibleRecipients.isEmpty()) {
            LOGGER.info("No eligible recipients found. Workflow complete.");
            return null;
        }

        LOGGER.info("Found " + eligibleRecipients.size() + " eligible recipients");

        // Create email plan for recipients
        PlanWithTemplatesRecipientsMap recipientMap = createEmailPlanForRecipients(eligibleRecipients);
        Optional<PlanWithTemplate> planOpt = planService.getActivePlan(recipientMap);

        if (planOpt.isEmpty()) {
            LOGGER.warning("No valid plan found for recipients.");
            return null;
        }

        PlanWithTemplate plan = planOpt.get();
        int followUpCount = planService.getFollowupCount(plan);

        // Schedule emails for recipients
        RecipientScheduledEmailsMap scheduledEmailsMap = emailService.scheduleEmailsForRecipients(recipientMap);

        // Update spreadsheet with scheduled email information
        updateSpreadsheetWithScheduledEmails(
                appConfig,
                eligibleRecipients,
                scheduledEmailsMap,
                followUpCount
        );
        return eligibleRecipients;
    }

    private ApplicationConfiguration loadApplicationConfiguration() throws ConfigurationException {
        LOGGER.info("Loading application configuration");
        ApplicationConfiguration appConfig = configRepository.getActiveConfiguration();
        validateConfiguration(appConfig);
        return appConfig;
    }

    private void validateConfiguration(ApplicationConfiguration appConfig) throws ConfigurationException {
        if (appConfig == null) {
            throw new ConfigurationException("No active configuration found");
        }

        if (appConfig.getSpreadsheetId() == null || appConfig.getSpreadsheetId().trim().isEmpty()) {
            throw new ConfigurationException("No spreadsheet configured in application settings");
        }

        if (appConfig.getSendingCriteriaColumn() == null) {
            throw new ConfigurationException("No sending criteria column configured in application settings");
        }

        if (appConfig.getSenderEmailAddress() == null) {
            throw new ConfigurationException("Invalid sender email address in application settings");
        }
    }

    private List<EntityData<Recipient, RecipientMetadata>> identifyEligibleRecipients(
            ApplicationConfiguration appConfig) throws RecipientOperationException, SpreadsheetOperationException {

        LOGGER.info("Identifying eligible recipients");
        try {
            SpreadsheetConfiguration spreadsheetConfig =
                    spreadsheetService.loadConfiguration(appConfig.getSpreadsheetId());

            return recipientService.findEligibleRecipients(
                    spreadsheetConfig,
                    appConfig.getSendingCriteriaColumn()
            );
        } catch (RecipientOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new SpreadsheetOperationException("Failed to load spreadsheet configuration", e);
        }
    }

    private PlanWithTemplatesRecipientsMap createEmailPlanForRecipients(
            List<EntityData<Recipient, RecipientMetadata>> eligibleRecipients) throws PlanCreationException {

        LOGGER.info("Creating email plan for eligible recipients");
        return planService.createPlanForRecipients(eligibleRecipients);
    }

    private RecipientScheduledEmailsMap scheduleEmails(
            PlanWithTemplatesRecipientsMap recipientMap) throws EmailOperationException {

        LOGGER.info("Scheduling emails for recipients");
        return emailService.scheduleEmailsForRecipients(recipientMap);
    }

    private int countScheduledEmails(RecipientScheduledEmailsMap scheduledEmailsMap) {
        int count = 0;
        if (scheduledEmailsMap != null && scheduledEmailsMap.map() != null) {
            for (List<?> emails : scheduledEmailsMap.map().values()) {
                count += emails.size();
            }
        }
        return count;
    }

    private void updateSpreadsheetWithScheduledEmails(
            ApplicationConfiguration appConfig,
            List<EntityData<Recipient, RecipientMetadata>> eligibleRecipients,
            RecipientScheduledEmailsMap scheduledEmailsMap,
            int followUpCount) throws SpreadsheetOperationException {

        LOGGER.info("Updating spreadsheet with scheduled email information");
        try {
            List<ColumnMapping> emailColumnMappings = appConfig.getColumnMappings(ColumnMapping.MappingType.EMAIL);

            spreadsheetService.updateScheduledEmails(
                    appConfig.getSpreadsheetId(),
                    emailColumnMappings,
                    recipientService.mapRecipientsToRows(eligibleRecipients, scheduledEmailsMap),
                    followUpCount
            );
        } catch (Exception e) {
            throw new SpreadsheetOperationException("Failed to update spreadsheet with scheduled emails", e);
        }
    }

    private List<EmailSendingResult> sendEmails(
            ApplicationConfiguration appConfig,
            boolean saveAsDraft) throws EmailOperationException {

        LOGGER.info("Preparing to send emails" + (saveAsDraft ? " as drafts" : ""));

        try {
            List<EmailSendRequest> sendRequests =
                    emailService.prepareSendRequests(appConfig.getSenderEmailAddress());

            if (sendRequests.isEmpty()) {
                LOGGER.info("No emails to send");
                return Collections.emptyList();
            }

            LOGGER.info("Sending " + sendRequests.size() + " emails");
            return emailService.sendEmails(sendRequests, saveAsDraft);
        } catch (Exception e) {
            throw new EmailOperationException("Failed to send emails", e);
        }
    }

    private void updateSpreadsheetWithSendingResults(
            ApplicationConfiguration appConfig,
            List<EmailSendingResult> sendingResults) throws SpreadsheetOperationException {

        LOGGER.info("Updating spreadsheet with email sending results");
        try {
            List<ColumnMapping> emailColumnMappings = appConfig.getColumnMappings(ColumnMapping.MappingType.EMAIL);

            List<EntityId<Recipient>> recipientIds = sendingResults.stream()
                    .map(EmailSendingResult::recipientId)
                    .toList();

            spreadsheetService.updateSentEmails(
                    appConfig.getSpreadsheetId(),
                    emailColumnMappings,
                    sendingResults,
                    recipientService.mapRecipientIdsToRows(recipientIds)
            );
        } catch (Exception e) {
            throw new SpreadsheetOperationException("Failed to update spreadsheet with sending results", e);
        }
    }

    /**
     * Exception thrown when there's an issue with the email orchestration process.
     */
    public static class OrchestrationException extends Exception {
        public OrchestrationException(String message) {
            super(message);
        }

        public OrchestrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when there's a configuration issue.
     */
    public static class ConfigurationException extends Exception {
        public ConfigurationException(String message) {
            super(message);
        }
    }

    private ApplicationConfiguration getConfiguration() {
        ApplicationConfiguration appConfig = configRepository.getActiveConfiguration();
        if (appConfig == null) {
            throw new IllegalStateException("No active configuration found");
        }

        if (appConfig.getSpreadsheetId() == null) {
            throw new IllegalStateException("No spreadsheet configured");
        }

        if (appConfig.getSendingCriteriaColumn() == null) {
            throw new IllegalStateException("No sending criteria column configured");
        }

        return appConfig;
    }

}
