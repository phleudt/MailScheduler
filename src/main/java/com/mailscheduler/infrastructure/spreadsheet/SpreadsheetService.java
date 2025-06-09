package com.mailscheduler.infrastructure.spreadsheet;

import com.mailscheduler.application.email.sending.EmailSendingResult;
import com.mailscheduler.application.email.service.RowToScheduledEmailsMap;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.factory.SpreadsheetConfigurationFactory;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.infrastructure.spreadsheet.exception.SpreadsheetOperationException;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for interacting with spreadsheets to synchronize email scheduling and status information.
 */
public class SpreadsheetService {
    private static final Logger LOGGER = Logger.getLogger(SpreadsheetService.class.getName());
    private static final String DEFAULT_SHEET_NAME = "Tabellenblatt1";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String OPEN_STATUS = "Offen";

    private final SpreadsheetGateway spreadsheetGateway;
    private final SpreadsheetConfigurationFactory configurationFactory;

    public SpreadsheetService(
            SpreadsheetGateway spreadsheetGateway,
            SpreadsheetConfigurationFactory configurationFactory
    ) {
        this.spreadsheetGateway = Objects.requireNonNull(spreadsheetGateway, "Spreadsheet gateway cannot be null");
        this.configurationFactory = Objects.requireNonNull(configurationFactory, "Configuration factory cannot be null");
    }

    /**
     * Loads the configuration for a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet to load
     * @return The spreadsheet configuration
     * @throws SpreadsheetOperationException If loading the configuration fails
     */
    public SpreadsheetConfiguration loadConfiguration(String spreadsheetId) throws SpreadsheetOperationException {
        if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet ID cannot be null or empty");
        }

        try {
            LOGGER.info("Loading configuration for spreadsheet: " + spreadsheetId);
            return configurationFactory.createFromExistingSpreadsheet(spreadsheetId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load spreadsheet configuration", e);
            throw new SpreadsheetOperationException("Failed to load spreadsheet configuration", e);
        }
    }

    /**
     * Updates the spreadsheet with information about scheduled emails.
     *
     * @param spreadsheetId The ID of the spreadsheet to update
     * @param columnMappings Column mappings to use for updates
     * @param scheduledEmailsMap Map of row numbers to scheduled emails
     * @param numberOfFollowUps Number of follow-up emails
     * @throws SpreadsheetOperationException If updating the spreadsheet fails
     */
    public void updateScheduledEmails(
            String spreadsheetId,
            List<ColumnMapping> columnMappings,
            RowToScheduledEmailsMap scheduledEmailsMap,
            int numberOfFollowUps
    ) throws SpreadsheetOperationException {
        validateUpdateScheduledEmailsParams(spreadsheetId, columnMappings, scheduledEmailsMap, numberOfFollowUps);

        if (scheduledEmailsMap.scheduledEmailsMap().isEmpty()) {
            LOGGER.info("No scheduled emails to update in spreadsheet");
            return;
        }

        try {
            LOGGER.info("Updating scheduled emails in spreadsheet: " + spreadsheetId);

            // Find required column mappings
            Map<String, ColumnMapping> mappings = findRequiredColumnMappings(columnMappings, numberOfFollowUps);

            // Update initial contact dates
            updateInitialContactDates(spreadsheetId, mappings.get("initialContactDate"), scheduledEmailsMap);

            // Update follow-up information
            updateFollowUpSchedules(spreadsheetId, mappings, scheduledEmailsMap, numberOfFollowUps);

            LOGGER.info("Successfully updated scheduled emails in spreadsheet");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to update scheduled emails in spreadsheet", e);
            throw new SpreadsheetOperationException("Failed to update scheduled emails in spreadsheet", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating scheduled emails", e);
            throw new SpreadsheetOperationException("Unexpected error updating scheduled emails", e);
        }
    }

    /**
     * Updates the spreadsheet with information about sent emails.
     *
     * @param spreadsheetId The ID of the spreadsheet to update
     * @param columnMappings Column mappings to use for updates
     * @param sendingResults Results of sending emails
     * @param recipientIdToRowMap Map of recipient IDs to row numbers
     * @throws SpreadsheetOperationException If updating the spreadsheet fails
     */
    public void updateSentEmails(
            String spreadsheetId,
            List<ColumnMapping> columnMappings,
            List<EmailSendingResult> sendingResults,
            Map<EntityId<Recipient>, Integer> recipientIdToRowMap
    ) throws SpreadsheetOperationException {
        validateUpdateSentEmailsParams(spreadsheetId, columnMappings, sendingResults, recipientIdToRowMap);

        if (sendingResults.isEmpty()) {
            LOGGER.info("No sending results to update in spreadsheet");
            return;
        }

        try {
            LOGGER.info("Updating sent emails in spreadsheet: " + spreadsheetId);

            // Group results by follow-up number
            Map<Integer, List<EmailSendingResult>> resultsByFollowUpNumber =
                    groupResultsByFollowUpNumber(sendingResults);

            // Skip initial emails (follow-up number 0)
            resultsByFollowUpNumber.remove(0);

            // Update status for each follow-up
            for (Map.Entry<Integer, List<EmailSendingResult>> entry : resultsByFollowUpNumber.entrySet()) {
                int followUpNumber = entry.getKey();
                List<EmailSendingResult> followUpResults = entry.getValue();

                updateFollowUpStatus(
                        spreadsheetId,
                        columnMappings,
                        followUpNumber,
                        followUpResults,
                        recipientIdToRowMap
                );
            }

            LOGGER.info("Successfully updated sent emails in spreadsheet");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to update sent emails in spreadsheet", e);
            throw new SpreadsheetOperationException("Failed to update sent emails in spreadsheet", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating sent emails", e);
            throw new SpreadsheetOperationException("Unexpected error updating sent emails", e);
        }
    }

    private void validateUpdateScheduledEmailsParams(
            String spreadsheetId,
            List<ColumnMapping> columnMappings,
            RowToScheduledEmailsMap scheduledEmailsMap,
            int numberOfFollowUps) {

        if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet ID cannot be null or empty");
        }

        if (columnMappings == null || columnMappings.isEmpty()) {
            throw new IllegalArgumentException("Column mappings cannot be null or empty");
        }

        if (scheduledEmailsMap == null) {
            throw new IllegalArgumentException("Scheduled emails map cannot be null");
        }

        if (numberOfFollowUps < 0) {
            throw new IllegalArgumentException("Number of follow-ups cannot be negative");
        }
    }

    private void validateUpdateSentEmailsParams(
            String spreadsheetId,
            List<ColumnMapping> columnMappings,
            List<EmailSendingResult> sendingResults,
            Map<EntityId<Recipient>, Integer> recipientIdToRowMap) {

        if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet ID cannot be null or empty");
        }

        if (columnMappings == null || columnMappings.isEmpty()) {
            throw new IllegalArgumentException("Column mappings cannot be null or empty");
        }

        if (sendingResults == null) {
            throw new IllegalArgumentException("Sending results cannot be null");
        }

        if (recipientIdToRowMap == null) {
            throw new IllegalArgumentException("Recipient ID to row map cannot be null");
        }
    }

    private Map<String, ColumnMapping> findRequiredColumnMappings(
            List<ColumnMapping> mappings,
            int numberOfFollowUps) throws SpreadsheetOperationException {

        Map<String, ColumnMapping> result = new HashMap<>();

        // Find initial contact date column
        Optional<ColumnMapping> initialContactDateOpt = findColumnMapping(mappings, "Initial Contact Date");
        if (initialContactDateOpt.isEmpty()) {
            throw new SpreadsheetOperationException("Initial Contact Date column not found");
        }
        result.put("initialContactDate", initialContactDateOpt.get());

        // Find first follow-up date column
        Optional<ColumnMapping> startingFollowupColumnOpt = findColumnMapping(mappings, "1 follow-up date");
        if (startingFollowupColumnOpt.isEmpty()) {
            throw new SpreadsheetOperationException("Starting follow-up column not found");
        }
        result.put("startFollowUp", startingFollowupColumnOpt.get());

        // Find last follow-up state column
        Optional<ColumnMapping> endFollowupColumnOpt =
                findColumnMapping(mappings, numberOfFollowUps + " follow-up state");
        if (endFollowupColumnOpt.isEmpty()) {
            throw new SpreadsheetOperationException("Ending follow-up column not found");
        }
        result.put("endFollowUp", endFollowupColumnOpt.get());

        return result;
    }

    private Optional<ColumnMapping> findColumnMapping(List<ColumnMapping> mappings, String columnName) {
        return mappings.stream()
                .filter(mapping -> columnName.equals(mapping.columnName()))
                .findFirst();
    }

    private void updateInitialContactDates(
            String spreadsheetId,
            ColumnMapping initialContactDateColumn,
            RowToScheduledEmailsMap scheduledEmailsMap) throws IOException {

        LOGGER.info("Updating initial contact dates");
        List<SpreadsheetReference> cellReferences = new ArrayList<>();
        List<String> cellValues = new ArrayList<>();

        for (Map.Entry<Integer, List<EntityData<Email, EmailMetadata>>> entry :
                scheduledEmailsMap.scheduledEmailsMap().entrySet()) {

            Integer row = entry.getKey();
            List<EntityData<Email, EmailMetadata>> emails = entry.getValue();

            for (EntityData<Email, EmailMetadata> email : emails) {
                if (EmailType.INITIAL.equals(email.entity().getType())) {
                    SpreadsheetReference cellRef = SpreadsheetReference.ofCell(
                            DEFAULT_SHEET_NAME,
                            initialContactDateColumn.columnReference().getReference() + row
                    );

                    String formattedDate = formatDate(email.metadata().scheduledDate());
                    cellReferences.add(cellRef);
                    cellValues.add(formattedDate);
                    break;
                }
            }
        }

        if (!cellReferences.isEmpty()) {
            LOGGER.info("Writing " + cellReferences.size() + " initial contact dates");
            spreadsheetGateway.writeDataToCells(spreadsheetId, cellReferences, cellValues);
        }
    }

    private void updateFollowUpSchedules(
            String spreadsheetId,
            Map<String, ColumnMapping> mappings,
            RowToScheduledEmailsMap scheduledEmailsMap,
            int numberOfFollowUps) throws IOException {

        LOGGER.info("Updating follow-up schedules");
        List<List<Object>> followupDataForRows = new ArrayList<>();
        List<SpreadsheetReference> followupReferenceRows = new ArrayList<>();

        for (Map.Entry<Integer, List<EntityData<Email, EmailMetadata>>> entry :
                scheduledEmailsMap.scheduledEmailsMap().entrySet()) {

            Integer row = entry.getKey();
            List<EntityData<Email, EmailMetadata>> emails = entry.getValue();

            // Build follow-up data row
            List<Object> followupDataForRow = buildFollowupDataRow(emails);
            if (!followupDataForRow.isEmpty()) {
                SpreadsheetReference rangeRef = buildFollowupRangeReference(
                        mappings.get("startFollowUp"),
                        mappings.get("endFollowUp"),
                        row
                );

                followupReferenceRows.add(rangeRef);
                followupDataForRows.add(followupDataForRow);
            }
        }

        if (!followupReferenceRows.isEmpty()) {
            LOGGER.info("Writing follow-up schedules for " + followupReferenceRows.size() + " rows");
            spreadsheetGateway.writeData(spreadsheetId, followupReferenceRows, followupDataForRows);
        }
    }

    private Map<Integer, List<EmailSendingResult>> groupResultsByFollowUpNumber(List<EmailSendingResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(result -> result.sendRequest().followUpNumber()));
    }

    private void updateFollowUpStatus(
            String spreadsheetId,
            List<ColumnMapping> columnMappings,
            int followUpNumber,
            List<EmailSendingResult> followUpResults,
            Map<EntityId<Recipient>, Integer> recipientIdToRowMap) throws IOException {

        // Find the column mapping for this follow-up's status
        Optional<ColumnMapping> followupColumnStateOpt =
                findColumnMapping(columnMappings, followUpNumber + " follow-up state");

        if (followupColumnStateOpt.isEmpty()) {
            LOGGER.warning("Column mapping not found for follow-up number " + followUpNumber);
            return;
        }

        List<SpreadsheetReference> cellReferences = new ArrayList<>();
        List<String> cellValues = new ArrayList<>();

        for (EmailSendingResult result : followUpResults) {
            Integer row = recipientIdToRowMap.get(result.recipientId());
            if (row == null) {
                LOGGER.warning("Row not found for recipient " + result.recipientId());
                continue;
            }

            SpreadsheetReference cellRef = SpreadsheetReference.ofCell(
                    DEFAULT_SHEET_NAME,
                    followupColumnStateOpt.get().columnReference().getReference() + row
            );

            String statusValue = result.sendResult() != null ?
                    result.sendResult().getDisplayStatus() :
                    "Skipped";

            cellReferences.add(cellRef);
            cellValues.add(statusValue);
        }

        if (!cellReferences.isEmpty()) {
            LOGGER.info("Writing status for " + cellReferences.size() +
                    " follow-up #" + followUpNumber + " emails");
            spreadsheetGateway.writeDataToCells(spreadsheetId, cellReferences, cellValues);
        }
    }

    private List<Object> buildFollowupDataRow(List<EntityData<Email, EmailMetadata>> scheduledEmails) {
        List<Object> followupDataForRow = new ArrayList<>();

        for (EntityData<Email, EmailMetadata> email : scheduledEmails) {
            if (EmailType.FOLLOW_UP.equals(email.entity().getType())) {
                followupDataForRow.add(formatDate(email.metadata().scheduledDate()));
                followupDataForRow.add(OPEN_STATUS);
            }
        }

        return followupDataForRow;
    }

    private SpreadsheetReference buildFollowupRangeReference(
            ColumnMapping startColumn,
            ColumnMapping endColumn,
            int row) {

        return SpreadsheetReference.ofRange(
                DEFAULT_SHEET_NAME,
                startColumn.columnReference().getReference() + row + ":" +
                        endColumn.columnReference().getReference() + row
        );
    }

    private String formatDate(java.time.temporal.TemporalAccessor date) {
        return DATE_FORMATTER.format(date);
    }
}