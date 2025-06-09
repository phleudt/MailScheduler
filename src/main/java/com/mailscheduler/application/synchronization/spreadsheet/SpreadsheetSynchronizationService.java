package com.mailscheduler.application.synchronization.spreadsheet;

import com.mailscheduler.application.service.ColumnMappingService;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.application.synchronization.spreadsheet.strategies.ConfigSheetSyncStrategy;
import com.mailscheduler.application.synchronization.spreadsheet.strategies.ContactRecipientSyncStrategy;
import com.mailscheduler.application.synchronization.spreadsheet.strategies.EmailSyncStrategy;
import com.mailscheduler.application.synchronization.spreadsheet.strategies.SpreadsheetSynchronizationStrategy;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.repository.*;
import com.mailscheduler.infrastructure.service.FollowUpManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that orchestrates synchronization between spreadsheet data and the application domain.
 * Uses a set of specialized strategies to handle different aspects of synchronization.
 */
public class SpreadsheetSynchronizationService {
    private static final Logger LOGGER = Logger.getLogger(SpreadsheetSynchronizationService.class.getName());

    private final List<SpreadsheetSynchronizationStrategy> synchronizationStrategies;

    public SpreadsheetSynchronizationService(
            SpreadsheetGateway spreadsheetGateway,
            ContactRepository contactRepository,
            RecipientRepository recipientRepository,
            EmailRepository emailRepository,
            ConfigurationRepository configurationRepository,
            TemplateRepository templateRepository,
            FollowUpManagementService followUpManagementService
    ) {
        Objects.requireNonNull(spreadsheetGateway, "SpreadsheetGateway cannot be null");
        Objects.requireNonNull(contactRepository, "ContactRepository cannot be null");
        Objects.requireNonNull(recipientRepository, "RecipientRepository cannot be null");
        Objects.requireNonNull(emailRepository, "EmailRepository cannot be null");
        Objects.requireNonNull(configurationRepository, "ConfigurationRepository cannot be null");
        Objects.requireNonNull(templateRepository, "TemplateRepository cannot be null");
        Objects.requireNonNull(followUpManagementService, "FollowUpManagementService cannot be null");

        ColumnMappingService mappingService = new ColumnMappingService(configurationRepository);

        this.synchronizationStrategies = initializeSynchronizationStrategies(
                spreadsheetGateway,
                contactRepository,
                recipientRepository,
                emailRepository,
                configurationRepository,
                templateRepository,
                followUpManagementService,
                mappingService
        );
    }

    /**
     * Initializes all synchronization strategies needed by this service.
     */
    private List<SpreadsheetSynchronizationStrategy> initializeSynchronizationStrategies(
            SpreadsheetGateway spreadsheetGateway,
            ContactRepository contactRepository,
            RecipientRepository recipientRepository,
            EmailRepository emailRepository,
            ConfigurationRepository configurationRepository,
            TemplateRepository templateRepository,
            FollowUpManagementService followUpManagementService,
            ColumnMappingService mappingService
    ) {
        List<SpreadsheetSynchronizationStrategy> strategies = new ArrayList<>();

        // Config sheet strategy must be first since it sets up configuration for others
        strategies.add(new ConfigSheetSyncStrategy(
                spreadsheetGateway, configurationRepository, templateRepository, followUpManagementService));

        strategies.add(new ContactRecipientSyncStrategy(
                spreadsheetGateway, contactRepository, recipientRepository, mappingService));

        strategies.add(new EmailSyncStrategy(
                spreadsheetGateway, emailRepository, recipientRepository, mappingService));

        return strategies;
    }

    /**
     * Performs a full synchronization using all registered strategies.
     *
     * @param configuration The spreadsheet configuration
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void performFullSync(SpreadsheetConfiguration configuration) {
        LOGGER.info("Starting full synchronization");

        if (configuration == null) {
            throw new IllegalArgumentException("SpreadsheetConfiguration cannot be null");
        }

        try {
            // Always run config sheet sync first and sequentially
            SpreadsheetSynchronizationStrategy configStrategy = synchronizationStrategies.get(0);
            configStrategy.synchronize(configuration);

            runStrategies(configuration);

            LOGGER.info("Full synchronization completed successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during full synchronization", e);
            throw new SynchronizationException("Full synchronization failed", e);
        }
    }

    private void runStrategies(SpreadsheetConfiguration configuration) {
        for (SpreadsheetSynchronizationStrategy strategy : synchronizationStrategies.subList(1, synchronizationStrategies.size())) {
            // if (strategy.shouldProcessSheet(configuration.getSheetConfiguration())) {
            LOGGER.info("Running strategy: " + strategy.getStrategyName());
            strategy.synchronize(configuration);
        }
    }

    public static class SynchronizationException extends RuntimeException {
        public SynchronizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
