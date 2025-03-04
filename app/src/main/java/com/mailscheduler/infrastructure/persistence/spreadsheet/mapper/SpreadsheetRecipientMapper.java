package com.mailscheduler.infrastructure.persistence.spreadsheet.mapper;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.common.util.TimeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Mapper class responsible for converting Google Sheets data to Recipient domain objects.
 * Handles the transformation of raw spreadsheet data into domain model objects.
 */
public class SpreadsheetRecipientMapper {

    /**
     * Builds a list of Recipients from spreadsheet column data.
     */
    public static List<Recipient> buildRecipientsFromColumns(List<ValueRange> valueRanges, int rowCount) {
        List<Recipient> recipients = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            List<String> rowValues = getRowFromColumns(valueRanges, index);
            if (!isEmptyRow(rowValues)) {
                int rowIndex = getRowIndexFromValueRange(valueRanges, index);
                try {
                    recipients.add(buildRecipientFromList(rowValues, rowIndex));
                } catch (IllegalArgumentException e) {
                    // TODO: Log invalid recipient
                }
            }
        }
        return recipients;
    }

    private static int getRowIndexFromValueRange(List<ValueRange> valueRanges, int index) {
        ValueRange valueRange = valueRanges.get(0);
        if (valueRange == null) {
            throw new IllegalArgumentException("ValueRange cannot be null");
        }
        return getStartingRow(valueRange.getRange()) + index;
    }

    private static int getStartingRow(String spreadsheetRange) {
        if (spreadsheetRange == null) {
            throw new IllegalArgumentException("googleRange cannot be null");
        }
        String[] parts = spreadsheetRange.split("!");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid range format");
        }

        String[] cells = parts[1].split(":");
        if (cells.length == 0) {
            throw new IllegalArgumentException("Invalid cell range format");
        }

        String startCell = cells[0];
        String rowNumber = startCell.replaceAll("[^0-9]", "");
        return Integer.parseInt(rowNumber);
    }

    private static List<String> getRowFromColumns(List<ValueRange> valueRanges, int rowIndex) {
        List<String> rowValues = new ArrayList<>(valueRanges.size());
        for (ValueRange valueRange : valueRanges) {
            List<List<Object>> column = valueRange.getValues();
            if (rowIndex < column.size() && !column.get(rowIndex).isEmpty()) {
                rowValues.add(column.get(rowIndex).get(0).toString());
            } else {
                rowValues.add(null);
            }
        }
        return rowValues;
    }

    private static Recipient buildRecipientFromList(List<String> attributes, int spreadsheetRow) throws IllegalArgumentException {
        // TODO: Rethink sending criterion build method
        boolean sendingCriteria = getSendingCriteria(attributes);
        if (sendingCriteria && TimeUtils.parseDateToZonedDateTime(getOrNull(attributes, 5)) != null) {
            return new Recipient.Builder()
                    .setDomain(getOrNull(attributes, 0))
                    .setEmailAddress(getOrNull(attributes, 1))
                    .setName(getOrNull(attributes, 2))
                    .setSalutation(getOrNull(attributes, 3))
                    .setPhoneNumber(getOrNull(attributes, 4))
                    .setInitialEmailDate(TimeUtils.parseDateToZonedDateTime(getOrNull(attributes, 5)))
                    .setPreserveInitialEmailDate(true)
                    .setSpreadsheetRow(spreadsheetRow)
                    .setIsSendingCriteriaFulfilled(getSendingCriteria(attributes))
                    .build();
        }
        if (sendingCriteria && TimeUtils.parseDateToZonedDateTime(getOrNull(attributes, 5)) == null)  {
            return new Recipient.Builder()
                    .setDomain(getOrNull(attributes, 0))
                    .setEmailAddress(getOrNull(attributes, 1))
                    .setName(getOrNull(attributes, 2))
                    .setSalutation(getOrNull(attributes, 3))
                    .setPhoneNumber(getOrNull(attributes, 4))
                    .setInitialEmailDate(TimeUtils.parseDateToZonedDateTime(getOrNull(attributes, 5)))
                    .setSpreadsheetRow(spreadsheetRow)
                    .setIsSendingCriteriaFulfilled(getSendingCriteria(attributes))
                    .build();
        }
        return new Recipient.Builder()
                .setDomain(getOrNull(attributes, 0))
                .setEmailAddress(getOrNull(attributes, 1))
                .setName(getOrNull(attributes, 2))
                .setSalutation(getOrNull(attributes, 3))
                .setPhoneNumber(getOrNull(attributes, 4))
                .setPreserveInitialEmailDate(true)
                .setInitialEmailDate(TimeUtils.parseDateToZonedDateTime(getOrNull(attributes, 5)))
                .setSpreadsheetRow(spreadsheetRow)
                .setIsSendingCriteriaFulfilled(getSendingCriteria(attributes))
                .build();
    }

    private static boolean getSendingCriteria(List<String> attributes) {
        for (int i = 6; i < attributes.size(); i++) {
            if (getOrNull(attributes, i) == null) {
                return false;
            }
        }
        return true;
    }

    private static String getOrNull(List<String> list, int index) {
        return (index < list.size() && list.get(index) != null) ? list.get(index) : null;
    }

    private static boolean isEmptyRow(List<String> rowValues) {
        return rowValues.stream().allMatch(val -> val == null || val.isEmpty());
    }
}
