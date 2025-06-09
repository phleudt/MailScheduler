package com.mailscheduler.application.synchronization.spreadsheet.strategies;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.application.synchronization.exception.SyncException;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for spreadsheet synchronization strategies that provides
 * common functionality for all concrete strategy implementations.
 */
public abstract class AbstractSpreadsheetSynchronizationStrategy implements SpreadsheetSynchronizationStrategy {
    private static final String CONFIGURATION_SHEET_NAME = "Configuration";
    protected final Logger logger;
    protected final SpreadsheetGateway spreadsheetGateway;

    /**
     * Creates a new abstract synchronization strategy with the required gateway.
     *
     * @param spreadsheetGateway Gateway for accessing spreadsheet data
     */
    protected AbstractSpreadsheetSynchronizationStrategy(SpreadsheetGateway spreadsheetGateway) {
        this.spreadsheetGateway = Objects.requireNonNull(spreadsheetGateway, "SpreadsheetGateway cannot be null");
        this.logger = Logger.getLogger(this.getClass().getName());
    }

    @Override
    public void synchronize(SpreadsheetConfiguration configuration) {
        // Validate the configuration
        try {
            validateConfiguration(configuration);
        } catch (IllegalArgumentException e) {
            logger.severe("Invalid configuration: " + e.getMessage());
            handleSynchronizationError(new SyncException("Configuration validation failed", e));
            return;
        }

        logger.info(() -> String.format("%s starting synchronization", getStrategyName()));

        try {
            // Process each sheet in the configuration
            processSheetsInSpreadsheet(configuration);

            // Perform any post-processing needed across sheets
            doPostProcessing(configuration);

            logger.info(() -> String.format("%s completed synchronization successfully", getStrategyName()));
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    String.format("%s encountered error during synchronization", getStrategyName()), e);
            handleSynchronizationError(e);
        }
    }

    /**
     * Default implementation checks if the sheet is not the configuration sheet.
     * Subclasses can override for custom filtering logic.
     */
    @Override
    public boolean shouldProcessSheet(SheetConfiguration sheetConfiguration) {
        return sheetConfiguration != null && !CONFIGURATION_SHEET_NAME.equals(sheetConfiguration.title());
    }

    /**
     * Validates the given spreadsheet configuration before processing.
     *
     * @param configuration The configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    protected void validateConfiguration(SpreadsheetConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Spreadsheet configuration cannot be null");
        }
        if (configuration.spreadsheetId() == null || configuration.spreadsheetId().isBlank()) {
            throw new IllegalArgumentException("Spreadsheet ID cannot be null or blank");
        }
        if (configuration.sheetConfigurations() == null) {
            throw new IllegalArgumentException("Sheet configurations cannot be null");
        }
    }

    /**
     * Processes all sheets in the spreadsheet that should be handled by this strategy.
     *
     * @param configuration The spreadsheet configuration
     */
    protected void processSheetsInSpreadsheet(SpreadsheetConfiguration configuration) {
        int processedCount = 0;
        int errorCount = 0;

        for (SheetConfiguration sheetConfiguration : configuration.sheetConfigurations()) {
            if (shouldProcessSheet(sheetConfiguration)) {
                try {
                    logger.fine(() -> String.format("Processing sheet: %s", sheetConfiguration.title()));
                    processSheet(configuration.spreadsheetId(), sheetConfiguration);
                    processedCount++;
                } catch (Exception e) {
                    errorCount++;
                    logger.log(Level.WARNING,
                            String.format("Error processing sheet %s: %s",
                                    sheetConfiguration.title(), e.getMessage()), e);
                    // Continue with other sheets despite this error
                }
            }
        }

        int finalProcessedCount = processedCount;
        int finalErrorCount = errorCount;
        logger.info(() -> String.format("%s processed %d sheets successfully, %d with errors",
                getStrategyName(), finalProcessedCount, finalErrorCount));
    }

    /**
     * Fetches data from the spreadsheet using the provided references.
     *
     * @param spreadsheetId ID of the spreadsheet
     * @param references List of references defining which data to fetch
     * @return List of ValueRange objects containing the spreadsheet data
     */
    protected List<ValueRange> fetchSpreadsheetData(String spreadsheetId, List<SpreadsheetReference> references) {
        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            logger.warning("Invalid spreadsheet ID provided");
            return Collections.emptyList();
        }

        if (references == null || references.isEmpty()) {
            logger.fine("No references provided for data fetch");
            return Collections.emptyList();
        }

        try {
            List<ValueRange> results = spreadsheetGateway.readDataBatch(spreadsheetId, references);
            logger.fine(() -> String.format("Fetched %d value ranges from spreadsheet",
                    results != null ? results.size() : 0));
            return results;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read spreadsheet data", e);
            return Collections.emptyList();
        }
    }

    /**
     * Process a single sheet in the configuration.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param sheetConfiguration The configuration for the sheet to process
     */
    protected abstract void processSheet(String spreadsheetId, SheetConfiguration sheetConfiguration);

    /**
     * Performs any necessary post-processing operations after all sheets have been processed.
     *
     * @param configuration The spreadsheet configuration
     */
    protected abstract void doPostProcessing(SpreadsheetConfiguration configuration);

    /**
     * Handles errors that occur during synchronization.
     *
     * @param exception The exception that was thrown
     */
    protected void handleSynchronizationError(Exception exception) {
        // Default implementation logs the error but does not re-throw
        // Subclasses can override for custom error handling
    }
}