package com.mailscheduler.application.synchronization;

import com.mailscheduler.application.synchronization.spreadsheet.SpreadsheetSynchronizationService;
import com.mailscheduler.application.synchronization.template.TemplateSyncStrategy;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates synchronization between various data sources including Gmail templates
 * and Google Spreadsheets. Manages the overall synchronization process and provides
 * consolidated reporting on results.
 */
public class SynchronizationCoordinator {
    private static final Logger LOGGER = Logger.getLogger(SynchronizationCoordinator.class.getName());

    private final SpreadsheetSynchronizationService spreadsheetSyncService;
    private final TemplateSyncStrategy templateSyncStrategy;
    private final SpreadsheetConfiguration spreadsheetConfig;

    public SynchronizationCoordinator(
            SpreadsheetSynchronizationService spreadsheetSyncService,
            TemplateSyncStrategy templateSyncStrategy,
            SpreadsheetConfiguration spreadsheetConfig
    ) {
        this.spreadsheetSyncService = spreadsheetSyncService;
        this.templateSyncStrategy = templateSyncStrategy;
        this.spreadsheetConfig = spreadsheetConfig;
    }

    /**
     * Synchronizes all components of the system.
     * This includes Gmail templates and Google Sheets data.
     *
     * @return true if synchronization was successful, false otherwise
     */
    public boolean synchronizeAll() {
        LOGGER.info("Starting full system synchronization");
        boolean templateSyncSuccess = false;
        boolean spreadsheetSyncSuccess = false;

        try {
            // Synchronize Gmail templates
            templateSyncSuccess = templateSyncStrategy.synchronize();
            LOGGER.info("Template synchronization " + (templateSyncSuccess ? "succeeded" : "failed"));

            // Synchronize spreadsheet data
            spreadsheetSyncService.performFullSync(spreadsheetConfig);
            spreadsheetSyncSuccess = true;
            LOGGER.info("Spreadsheet synchronization succeeded");
            return templateSyncSuccess && spreadsheetSyncSuccess;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Synchronization failed with exception", e);
            return false;
        } finally {
            LOGGER.info("Synchronization completed - Templates: " +
                    (templateSyncSuccess ? "Success" : "Failed") +
                    ", Spreadsheet: " + (spreadsheetSyncSuccess ? "Success" : "Failed"));
        }
    }

    /**
     * Synchronizes only Gmail templates.
     *
     * @return true if synchronization was successful, false otherwise
     */
    public boolean synchronizeTemplatesOnly() {
        LOGGER.info("Starting template-only synchronization");
        try {
            boolean success = templateSyncStrategy.synchronize();
            LOGGER.info("Template synchronization " + (success ? "succeeded" : "failed"));
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Template synchronization failed with exception", e);
            return false;
        }
    }

    /**
     * Synchronizes only spreadsheet data.
     *
     * @return true if synchronization was successful, false otherwise
     */
    public boolean synchronizeSpreadsheetOnly() {
        LOGGER.info("Starting spreadsheet-only synchronization");
        try {
            spreadsheetSyncService.performFullSync(spreadsheetConfig);
            LOGGER.info("Spreadsheet synchronization succeeded");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Spreadsheet synchronization failed with exception", e);
            return false;
        }
    }
}
