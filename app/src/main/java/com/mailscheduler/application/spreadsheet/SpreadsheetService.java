package com.mailscheduler.application.spreadsheet;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.common.config.Configuration;
import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.domain.email.EmailCategory;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.common.exception.SpreadsheetOperationException;
import com.mailscheduler.infrastructure.google.sheet.GoogleSheetService;
import com.mailscheduler.common.util.TimeUtils;
import com.mailscheduler.infrastructure.persistence.repository.spreadsheet.SpreadsheetRecipientRepository;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing email scheduling and recipient management operations in Google Sheets.
 * Provides high-level abstractions for spreadsheet operations with comprehensive error handling and logging.
 *
 * This service supports:
 * - Email status tracking (scheduled/sent)
 * - Recipient information management
 * - Schedule management and date calculations
 * - Batch operations for improved performance
 *
 * @see GoogleSheetService
 * @see Configuration
 */

public class SpreadsheetService {
    private static final Logger LOGGER = Logger.getLogger(SpreadsheetService.class.getName());
    private static final String SCHEDULED_MARKER = "#";
    private static final String SENT_MARKER = "x";
    private static String defaultSender;

    private final GoogleSheetService googleSheetService;
    private final String spreadsheetId;
    private final EmailStatusManager emailStatusManager;
    private final SpreadsheetRecipientRepository recipientRepository;
    private final ScheduleManager scheduleManager;

    public SpreadsheetService(GoogleSheetService googleSheetService, Configuration configuration) {
        this.googleSheetService = googleSheetService;
        this.spreadsheetId = Optional.ofNullable(configuration.getSpreadsheetId())
                .orElseThrow(() -> new IllegalArgumentException("Spreadsheet ID cannot be null"));
        this.emailStatusManager = new EmailStatusManager();
        this.recipientRepository = new SpreadsheetRecipientRepository(googleSheetService, spreadsheetId);
        this.scheduleManager = new ScheduleManager();
        defaultSender = configuration.getDefaultSender();
    }

    /**
     * Manages email status marking operations in the spreadsheet.
     * Handles both scheduling and sending status updates with optimized batch operations.
     */
    public class EmailStatusManager {
        /**
         * Marks a list of emails as scheduled in the spreadsheet.
         *
         * @param columnToSearch Column containing email addresses to search
         * @param emails List of emails to mark as scheduled
         * @param columnIndex Column where schedule markers should be placed
         * @return List of spreadsheet references for the marked cells
         * @throws SpreadsheetOperationException if the marking operation fails
         */
        public List<SpreadsheetReference> markEmailsAsScheduled(
                SpreadsheetReference columnToSearch,
                List<Email> emails,
                SpreadsheetReference columnIndex
        ) throws SpreadsheetOperationException {
            return markEmails(columnToSearch, emails, columnIndex, SCHEDULED_MARKER);
        }

        /**
         * Marks a list of emails as sent in the spreadsheet.
         *
         * @param column Column containing email addresses to search
         * @param emails List of emails to mark as sent
         * @param columnIndex Column where sent markers should be placed
         * @throws SpreadsheetOperationException if the marking operation fails
         */
        public void markEmailsAsSent(
                SpreadsheetReference column,
                List<Email> emails,
                SpreadsheetReference columnIndex
        ) throws SpreadsheetOperationException {
            markEmails(column, emails, columnIndex, SENT_MARKER);
        }

        private List<SpreadsheetReference> markEmails(
                SpreadsheetReference columnToSearch,
                List<Email> emails,
                SpreadsheetReference columnIndex,
                String marker
        ) throws SpreadsheetOperationException {
            if (emails.isEmpty()) {
                LOGGER.fine("No emails to mark - skipping operation");
                return new ArrayList<>();
            }

            try {
                List<EmailAddress> emailAddresses = emails.stream()
                        .map(Email::getRecipient)
                        .toList();

                List<SpreadsheetReference> emailCells = googleSheetService.getSpreadsheetCellIndices(
                        spreadsheetId,
                        columnToSearch,
                        emailAddresses.stream().map(String::valueOf).toList()
                );

                List<SpreadsheetReference> cellIndices = emailCells.stream()
                        .map(cell ->
                                SpreadsheetReference.ofCell(
                                        columnIndex.getReference() + cell.getRow()
                        ))
                        .collect(Collectors.toList());

                googleSheetService.writeToSpreadsheetCells(spreadsheetId, cellIndices, marker);
                LOGGER.info(String.format("Successfully marked %d emails with status: %s", emails.size(), marker));
                return emailCells;
            } catch (IOException e) {
                String errorMessage = String.format("Failed to mark emails with status: %s", marker);
                LOGGER.log(Level.SEVERE, errorMessage, e);
                throw new SpreadsheetOperationException(errorMessage, e);            }
        }
    }

    /**
     * Manages recipient data retrieval and processing operations.
     * Provides methods for batch reading and processing recipient information from multiple spreadsheet columns.
     */
    public static class RecipientManager {
        SpreadsheetRecipientRepository spreadsheetRecipientRepository;

        /**
         * Retrieves and processes recipient information from specified spreadsheet columns.
         * Handles batch reading operations for optimal performance.
         *
         * @param columns List of spreadsheet columns containing recipient information
         * @return List of Recipient objects containing processed recipient data
         * @throws SpreadsheetOperationException if recipient retrieval or processing fails
         */
        public List<Recipient> retrieveRecipient(List<SpreadsheetReference> columns)
                throws SpreadsheetOperationException {
            return spreadsheetRecipientRepository.getRecipients(columns);
        }
    }

    /**
     * Manages schedule-related operations in the spreadsheet.
     * Handles schedule marking, date calculations, and batch updates for email scheduling.
     */
    public class ScheduleManager {
        /**
         * Marks schedule information for a list of emails in the spreadsheet.
         *
         * @param columnToSearch Column containing email addresses to search
         * @param emails List of emails to schedule
         * @param columnIndex Column where schedule information should be placed
         * @param referenceColumnIndex Reference column for date calculations
         * @param offsetDays Number of days to offset from reference date
         * @throws SpreadsheetOperationException if schedule marking fails
         */
        public void markSchedule(
                SpreadsheetReference columnToSearch,
                List<Email> emails,
                SpreadsheetReference columnIndex,
                SpreadsheetReference referenceColumnIndex,
                int offsetDays
        ) throws SpreadsheetOperationException {
            if (emails.isEmpty()) {
                LOGGER.fine("No emails to schedule - skipping operation");
                return;
            }
            try {
                List<EmailAddress> emailAddresses = emails.stream()
                        .map(Email::getRecipient)
                        .toList();

                List<SpreadsheetReference> emailCells = Optional.ofNullable(
                        googleSheetService.getSpreadsheetCellIndices(
                                spreadsheetId,
                                columnToSearch,
                                emailAddresses.stream().map(String::valueOf).toList()
                        )
                ).orElseThrow(() -> new IOException("Could not find row indices"));

                List<String> scheduleExpressions = emailCells.stream()
                        .map(cell -> generateScheduleExpression(
                                referenceColumnIndex,
                                cell,
                                offsetDays
                        ))
                        .collect(Collectors.toList());

                List<SpreadsheetReference> targetCells = emailCells.stream()
                        .map(rowIndex ->
                                SpreadsheetReference.ofCell(
                                        columnIndex.getReference() + rowIndex.getRow()
                                )
                        )
                        .collect(Collectors.toList());

                googleSheetService.writeToSpreadsheetCells(spreadsheetId, targetCells, scheduleExpressions);
                LOGGER.info(String.format("Successfully marked schedules for %d emails", emails.size()));
            } catch (IOException e) {
                String errorMessage = "Failed to mark schedule information";
                LOGGER.log(Level.SEVERE, errorMessage, e);
                throw new SpreadsheetOperationException(errorMessage, e);            }
        }

        public void markScheduleWithExpression(
                List<SpreadsheetReference> cellReferences,
                SpreadsheetReference columnsToMark,
                List<String> schedules
        ) throws SpreadsheetOperationException {
            validateScheduleParameters(cellReferences, schedules);
            try {
                List<SpreadsheetReference> cellIndicesForMarking = cellReferences.stream()
                        .map(rowIndex ->
                                SpreadsheetReference.ofCell(
                                        columnsToMark.getReference() + rowIndex.getRow()
                                )
                        )
                        .collect(Collectors.toList());

                googleSheetService.writeToSpreadsheetCells(spreadsheetId, cellIndicesForMarking, schedules);
                LOGGER.info(String.format("Successfully marked %d schedule expressions", schedules.size()));
            } catch (IOException e) {
                String errorMessage = "Failed to mark schedule expressions";
                LOGGER.log(Level.SEVERE, errorMessage, e);
                throw new SpreadsheetOperationException(errorMessage, e);
            }
        }

        public void markSchedule(
                List<SpreadsheetReference> columnsToSearch,
                SpreadsheetReference column,
                SpreadsheetReference referenceColumnIndex,
                int offsetDays
        ) throws SpreadsheetOperationException {
            if (columnsToSearch == null || columnsToSearch.isEmpty()) return;
            try {
                List<String> scheduleExpressions = columnsToSearch.stream()
                        .map(cellIndexContainingEmailAddress -> generateScheduleExpression(
                                referenceColumnIndex,
                                cellIndexContainingEmailAddress,
                                offsetDays
                        ))
                        .collect(Collectors.toList());

                List<SpreadsheetReference> cellIndicesForMarking = columnsToSearch.stream()
                        .map(rowIndex ->
                                SpreadsheetReference.ofCell(
                                        column.getReference() + rowIndex.getRow()
                                )
                        )
                        .collect(Collectors.toList());

                googleSheetService.writeToSpreadsheetCells(spreadsheetId, cellIndicesForMarking, scheduleExpressions);
            } catch (IOException e) {
                throw new SpreadsheetOperationException("Failed to mark schedule", e);
            }
        }

        private void validateScheduleParameters(
                List<SpreadsheetReference> cellReferences,
                List<String> schedules
        ) throws SpreadsheetOperationException {
            if (cellReferences == null || cellReferences.isEmpty()) {
                throw new SpreadsheetOperationException("Cell references cannot be null or empty");
            }
            if (schedules == null || schedules.isEmpty()) {
                throw new SpreadsheetOperationException("Schedule expressions cannot be null or empty");
            }
            if (cellReferences.size() != schedules.size()) {
                throw new SpreadsheetOperationException(
                        "Number of cell references must match number of schedule expressions");
            }
        }

        private String generateScheduleExpression(
                SpreadsheetReference referenceColumnIndex,
                SpreadsheetReference referenceRowIndex,
                int offsetDays) {
            return String.format("=%s%s + %d", referenceColumnIndex.getReference(), referenceRowIndex.getRow(), offsetDays);
        }
    }

    /**
     * Mark emails as scheduled.
     *
     * @param column Spreadsheet range
     * @param emails List of emails
     * @param columnIndex Column to mark
     */
    public List<SpreadsheetReference> markEmailsAsScheduled(SpreadsheetReference column, List<Email> emails, SpreadsheetReference columnIndex) throws SpreadsheetOperationException {
        return emailStatusManager.markEmailsAsScheduled(column, emails, columnIndex);
    }

    public void markEmailsAsSent(SpreadsheetReference column, List<Email> emails, SpreadsheetReference columnIndex) throws SpreadsheetOperationException {
        emailStatusManager.markEmailsAsSent(column, emails, columnIndex);
    }

    /**
     * Retrieve recipients from specified columns.
     *
     * @param columns Columns to retrieve recipients from
     * @return List of recipients
     */
    public List<Recipient> retrieveRecipient(List<SpreadsheetReference> columns) throws SpreadsheetOperationException {
        return recipientRepository.getRecipients(columns);
    }

    /**
     * Mark schedule in spreadsheet.
     *
     * @param columnToSearch Spreadsheet range
     * @param emails List of emails
     * @param columnIndex Column to mark
     * @param referenceColumn Reference column
     * @param offsetDays Days to offset
     */
    public void markSchedule(
            SpreadsheetReference columnToSearch,
            List<Email> emails,
            SpreadsheetReference columnIndex,
            SpreadsheetReference referenceColumn,
            int offsetDays
    ) throws SpreadsheetOperationException {
        scheduleManager.markSchedule(columnToSearch, emails, columnIndex, referenceColumn, offsetDays);
    }

    public void markSchedule(
            List<SpreadsheetReference> cellIndicesContainingEmailAddresses,
            SpreadsheetReference columnToMark,
            List<String> schedules
    ) throws SpreadsheetOperationException {
        scheduleManager.markScheduleWithExpression(cellIndicesContainingEmailAddresses, columnToMark, schedules);
    }

    public void markSchedule(
            List<SpreadsheetReference> cellIndicesContainingEmailAddresses,
            SpreadsheetReference columnToMark,
            SpreadsheetReference referenceColumn,
            int offsetDays
    ) throws SpreadsheetOperationException {
        scheduleManager.markSchedule(cellIndicesContainingEmailAddresses, columnToMark, referenceColumn, offsetDays);
    }

    public List<Email> retrieveScheduledAndSentEmails(
            List<SpreadsheetReference> markedColumns,
            List<SpreadsheetReference> scheduleColumns,
            SpreadsheetReference recipientEmailAddressColumn
    ) throws SpreadsheetOperationException {
        if (markedColumns == null || scheduleColumns == null || recipientEmailAddressColumn == null) {
            throw new IllegalArgumentException("Spreadsheet reference cannot be null.");
        }

        try {
            // Fetch data from the spreadsheet
            List<ValueRange> markedValueRanges = googleSheetService.readSpreadsheetBatch(
                    spreadsheetId,
                    markedColumns
            );
            List<ValueRange> scheduledValueRanges = googleSheetService.readSpreadsheetBatch(
                    spreadsheetId,
                    scheduleColumns
            );
            List<List<Object>> recipientEmailRange = googleSheetService.readSpreadsheet(
                    spreadsheetId,
                    recipientEmailAddressColumn
            );

            if (markedValueRanges.size() != scheduledValueRanges.size()) {
                LOGGER.warning("Marked columns and schedule columns count mismatch");
                return new ArrayList<>();
            }
            return processEmailRanges(markedValueRanges, scheduledValueRanges, recipientEmailRange);
        } catch (IOException e) {
            String errorMessage = "Failed to retrieve scheduled and sent emails";
            LOGGER.log(Level.SEVERE, errorMessage, e);
            throw new SpreadsheetOperationException(errorMessage, e);
        }
    }

    private List<Email> processEmailRanges(
            List<ValueRange> markedValueRanges,
            List<ValueRange> scheduledValueRanges,
            List<List<Object>> recipientEmailRange
    ) {
        List<Email> emails = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < markedValueRanges.size(); columnIndex++) {
            ValueRange markedColumn = markedValueRanges.get(columnIndex);
            ValueRange scheduledColumn = scheduledValueRanges.get(columnIndex);

            int maxRows = determineMaxRowCount(recipientEmailRange, markedColumn, scheduledColumn);

            for (int rowIndex = 0; rowIndex < maxRows; rowIndex++) {
                // Process emails with existing email addresses
                processEmailRow(emails, recipientEmailRange, markedColumn, scheduledColumn, columnIndex, rowIndex);
            }
        }

        return emails;
    }

    private void processEmailRow(
            List<Email> emails,
            List<List<Object>> recipientEmails,
            ValueRange markedColumn,
            ValueRange scheduledColumn,
            int columnIndex,
            int rowIndex
            ) {
        if (!isValidEmailRow(recipientEmails, rowIndex)) {
            return;
        }

        String recipientEmail = recipientEmails.get(rowIndex).get(0).toString();
        String statusMarker = getCellValue(markedColumn, rowIndex);
        String scheduledDateStr = getCellValue(scheduledColumn, rowIndex);

        if (isValidStatusMarker(statusMarker)) {
            Email email = createEmailFromRow(
                    recipientEmail,
                    columnIndex,
                    statusMarker,
                    scheduledDateStr
            );

            if (email != null) {
                emails.add(email);
            }
        }
    }

    private boolean isValidEmailRow(List<List<Object>> recipientEmailRange, int rowIndex) {
        return rowIndex < recipientEmailRange.size() &&
                !recipientEmailRange.get(rowIndex).isEmpty() &&
                !recipientEmailRange.get(rowIndex).get(0).toString().isEmpty();
    }

    private boolean isValidStatusMarker(String marker) {
        if (marker == null) return false;
        return SCHEDULED_MARKER.equals(marker) || SENT_MARKER.equals(marker);
    }

    private String getCellValue(ValueRange valueRange, int rowIndex) {
        if (valueRange.getValues() != null &&
                rowIndex < valueRange.getValues().size() &&
                !valueRange.getValues().get(rowIndex).isEmpty()) {
            return valueRange.getValues().get(rowIndex).get(0).toString();
        }
        return null;
    }

    private Email createEmailFromRow(
            String recipientEmail,
            int columnIndex,
            String markedRowContent,
            String scheduledDateStr
    ) {
        Email.Builder builder = new Email.Builder();

        // Determine email status
        if (markedRowContent.isEmpty()) {
            return null;
        } else if (SCHEDULED_MARKER.equals(markedRowContent)) {
            builder.setStatus("PENDING");
        } else if (SENT_MARKER.equals(markedRowContent)) {
            builder.setStatus("SENT");
        } else {
            return null;
        }

        // Determine email category
        builder.setCategory(columnIndex == 0 ?
                EmailCategory.EXTERNALLY_INITIAL :
                EmailCategory.EXTERNALLY_FOLLOW_UP
        );

        // Set email details
        try {
            builder.setRecipient(recipientEmail);
        } catch (IllegalArgumentException e) {
            // TODO
            return null;
        }
        builder.setFollowupNumber(columnIndex);

        // Parse scheduled date
        try {
            if (scheduledDateStr != null) {
                builder.setScheduledDate(TimeUtils.parseDateToZonedDateTime(scheduledDateStr));
            }
        } catch (DateTimeParseException e) {
            LOGGER.severe("Failed to parse date: " + scheduledDateStr);
        }

        // Set default values for other fields
        builder.setSender(defaultSender);
        builder.setSubject("");
        builder.setBody("");

        return builder.build();
    }

    private int determineMaxRowCount(List<List<Object>> emailRange,
                                     ValueRange markedRanges,
                                     ValueRange scheduledRanges) {
        int maxRows = emailRange.size();

        if (markedRanges.getValues() != null) {
            maxRows = Math.min(maxRows, markedRanges.getValues().size());
        }

        if (scheduledRanges.getValues() != null) {
            maxRows = Math.min(maxRows, scheduledRanges.getValues().size());
        }

        return maxRows;
    }


    public static boolean validateSpreadsheetId(String spreadsheetId) throws Exception {
        if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
            return false;
        }

        return GoogleSheetService.getInstance().validateSpreadsheetID(spreadsheetId);
    }

    public static String extractSpreadsheetIdFromUrl(String url) {
        // Regex patterns to match different Google Sheets URL formats
        String[] patterns = {
                // Standard edit URL
                "https://docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9-_]+)",
                // Sharing URL
                "https://docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9-_]+)/",
                // URL with additional parameters
                "https://docs\\.google\\.com/spreadsheets/d/([a-zA-Z0-9-_]+)/[^\\s]+"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(url);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    public List<String> readSpreadsheetBatch(List<SpreadsheetReference> cells) throws SpreadsheetOperationException {
        try {
            if (cells.isEmpty()) return List.of();
            List<ValueRange> valueRanges = googleSheetService.readSpreadsheetBatch(spreadsheetId, cells);
            List<String> values = new ArrayList<>();
            for (ValueRange valueRange : valueRanges) {
                if (valueRange.getValues() != null && !valueRange.getValues().isEmpty()) {
                    values.add(valueRange.getValues().get(0).get(0).toString());
                } else {
                    values.add("");
                }
            }
            return values;
        } catch (IOException e) {
            throw new SpreadsheetOperationException("Failed to read from spreadsheet cells: " + cells, e);
        }
    }

}