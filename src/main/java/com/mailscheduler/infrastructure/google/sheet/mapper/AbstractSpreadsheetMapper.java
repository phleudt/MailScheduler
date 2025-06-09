package com.mailscheduler.infrastructure.google.sheet.mapper;

import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for spreadsheet mappers providing common functionality
 * for parsing and mapping data from Google Sheets to domain objects.
 *
 * @param <T> Type of the entity this mapper produces
 */
public abstract class AbstractSpreadsheetMapper<T> {
    protected final Logger logger = Logger.getLogger(this.getClass().getName());
    protected static final int DEFAULT_HEADER_ROWS = 5; // Number of rows to skip by default

    /**
     * Maps a list of value ranges (columns) from a spreadsheet to a list of domain entities.
     *
     * @param valueRanges List of value ranges from the spreadsheet
     * @param sheetTitle Title of the sheet (optional, may be null)
     * @return List of domain entities
     */
    public List<T> mapFromValueRanges(List<ValueRange> valueRanges, String sheetTitle) {
        if (valueRanges == null || valueRanges.isEmpty() || !validateValueRanges(valueRanges)) {
            logger.warning("Invalid or empty value ranges provided");
            return Collections.emptyList();
        }

        int rowCount = calculateRowCount(valueRanges);
        List<T> entities = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            try {
                List<String> rowValues = extractRowFromColumns(valueRanges, rowIndex);

                if (isEmptyRow(rowValues)) {
                    continue;
                }

                int spreadsheetRowNumber = calculateActualRowNumber(valueRanges, rowIndex);

                if (!shouldProcessRow(spreadsheetRowNumber, rowValues)) {
                    continue;
                }

                List<T> rowEntities = mapRowToEntities(rowValues, spreadsheetRowNumber, sheetTitle);
                if (rowEntities != null && !rowEntities.isEmpty()) {
                    entities.addAll(rowEntities);
                }
            } catch (Exception e) {
                handleRowProcessingError(rowIndex, e);
            }
        }

        logger.info("Mapped " + entities.size() + " entities from spreadsheet data");
        return entities;
    }

    /**
     * Validates that the provided value ranges are properly formatted.
     *
     * @param valueRanges The value ranges to validate
     * @return true if valid, false otherwise
     */
    protected boolean validateValueRanges(List<ValueRange> valueRanges) {
        for (ValueRange range : valueRanges) {
            if (range == null || range.getValues() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the total number of rows in the value ranges.
     *
     * @param valueRanges List of value ranges
     * @return Maximum number of rows across all columns
     */
    protected int calculateRowCount(List<ValueRange> valueRanges) {
        return valueRanges.stream()
                .filter(valueRange -> valueRange.getValues() != null)
                .mapToInt(valueRange -> valueRange.getValues().size())
                .max()
                .orElse(0);
    }

    /**
     * Extracts a row of data across all columns at the specified index.
     *
     * @param valueRanges List of value ranges representing columns
     * @param rowIndex Index of the row to extract
     * @return List of string values representing the row
     */
    protected List<String> extractRowFromColumns(List<ValueRange> valueRanges, int rowIndex) {
        List<String> rowValues = new ArrayList<>(valueRanges.size());

        for (ValueRange valueRange : valueRanges) {
            List<List<Object>> column = valueRange.getValues();

            if (column != null && rowIndex < column.size()) {
                List<Object> cell = column.get(rowIndex);
                rowValues.add(cell != null && !cell.isEmpty() ? cell.get(0).toString() : null);
            } else {
                rowValues.add(null);
            }
        }

        return rowValues;
    }

    /**
     * Calculates the actual row number in the spreadsheet (1-based) from the row index (0-based).
     *
     * @param valueRanges List of value ranges
     * @param rowIndex Index of the row
     * @return Actual row number in the spreadsheet
     */
    protected int calculateActualRowNumber(List<ValueRange> valueRanges, int rowIndex) {
        ValueRange firstValueRange = valueRanges.get(0);
        return extractStartingRowNumber(firstValueRange.getRange()) + rowIndex;
    }

    /**
     * Extracts the starting row number from a range reference.
     *
     * @param rangeReference Range reference string (e.g. "Sheet1!A5:A10")
     * @return Row number from the range
     */
    protected int extractStartingRowNumber(String rangeReference) {
        if (rangeReference == null || rangeReference.isEmpty()) {
            logger.warning("Empty range reference");
            return 1;
        }

        try {
            String[] parts = rangeReference.split("!");
            if (parts.length != 2) {
                logger.warning("Invalid range format: " + rangeReference);
                return 1;
            }

            String range = parts[1];
            String[] cells = range.split(":");
            if (cells.length == 0) {
                logger.warning("Invalid cell range format: " + range);
                return 1;
            }

            String startCell = cells[0];
            String rowNumber = startCell.replaceAll("[^0-9]", "");
            return rowNumber.isEmpty() ? 1 : Integer.parseInt(rowNumber);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error extracting row number from: " + rangeReference, e);
            return 1;
        }
    }

    /**
     * Checks if a row is empty (all cells are null or empty).
     *
     * @param rowValues List of cell values
     * @return true if the row is empty, false otherwise
     */
    protected boolean isEmptyRow(List<String> rowValues) {
        return rowValues.stream().allMatch(val -> val == null || val.isEmpty());
    }

    /**
     * Gets a value from the list or returns null if index is out of bounds.
     *
     * @param list List to get value from
     * @param index Index of the value
     * @return Value at index or null
     */
    protected String getValueOrNull(List<String> list, int index) {
        return (index < list.size() && list.get(index) != null) ? list.get(index).trim() : null;
    }

    /**
     * Determines whether a row should be processed.
     * Default implementation skips header rows and empty rows.
     *
     * @param rowNumber The actual row number in the spreadsheet
     * @param rowValues The values in the row
     * @return true if the row should be processed, false otherwise
     */
    protected boolean shouldProcessRow(int rowNumber, List<String> rowValues) {
        // Skip header rows by default
        return rowNumber > DEFAULT_HEADER_ROWS;
    }

    /**
     * Handles errors that occur during row processing.
     *
     * @param rowIndex Index of the row where the error occurred
     * @param exception The exception that was thrown
     */
    protected void handleRowProcessingError(int rowIndex, Exception exception) {
        logger.log(Level.WARNING, "Error processing row " + rowIndex, exception);
    }

    /**
     * Maps a row of data to a list of domain entities.
     * This method must be implemented by concrete subclasses.
     *
     * @param rowValues Values from the row
     * @param rowNumber The actual row number in the spreadsheet
     * @param sheetTitle Title of the sheet (may be null)
     * @return List of domain entities or an empty list if the row should be skipped
     */
    protected abstract List<T> mapRowToEntities(List<String> rowValues, int rowNumber, String sheetTitle);}
