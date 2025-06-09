package com.mailscheduler.domain.model.common.vo.spreadsheet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents and validates different types of spreadsheet references.
 * <p>
 *     This value object handles various forms of references used in spreadsheets:
 *     - Columns (e.g. "A", "BC")
 *     - Rows (e.g. "1", "42")
 *     - Cells (e.g. "A1", "BC42)
 *     - Ranges (e.g., "A1:B2", "J6:J")
 *     -Sheet-qualified references (e.g. "Sheet1!A1:B2")
 * </p>
 * <p>
 *     This class provides validation for each reference type and conversion to Google Sheets API format
 * </p>
 */
public class SpreadsheetReference {

    /**
     * Defines the different types of spreadsheet references
     */
    public enum ReferenceType {
        /** Single column (e.g. "A") */
        COLUMN,

        /** Single row (e.g., "1") */
        ROW,

        /** Single row (e.g., "1") */
        CELL,

        /** Single row (e.g., "1") */
        RANGE
    }

    // Validation Patterns
    private static final Pattern COLUMN_PATTERN = Pattern.compile("^[A-Z]+$");
    private static final Pattern ROW_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private static final Pattern CELL_PATTERN = Pattern.compile("^[A-Z]+[1-9]\\d*$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^[A-Z]+[1-9]\\d*:[A-Z]+([1-9]\\d*)?$");
    private static final Pattern SHEET_REFERENCE_PATTERN = Pattern.compile("^([^!]+)!(.+)$");

    private String reference;
    private ReferenceType type;
    private String sheetTitle;

    /**
     * Private constructor for creating a validated spreadsheet reference.
     *
     * @param reference The reference string (e.g., "A1", "B:C")
     * @param type The type of reference
     * @param sheetTitle Optional sheet title (can be null)
     * @throws IllegalArgumentException if the reference is invalid for the specified type
     */
    private SpreadsheetReference(String reference, ReferenceType type, String sheetTitle) {
        this.reference = Objects.requireNonNull(reference, "Reference cannot be null");
        this.type = Objects.requireNonNull(type, "Reference type cannot be null");
        this.sheetTitle = sheetTitle; // Can be null
        validateReference();
    }

    /**
     * Private constructor for references without a sheet title.
     *
     * @param reference The reference string
     * @param type The type of reference
     * @throws IllegalArgumentException if the reference is invalid for the specified type
     */
    private SpreadsheetReference(String reference, ReferenceType type) {
        this(reference, type, null);
    }

    /**
     * Creator method for JSON deserialization.
     *
     * @param reference The reference string
     * @param type The type of reference
     * @param sheetTitle Optional sheet title
     * @return A new SpreadsheetReference instance
     */
    @JsonCreator
    public static SpreadsheetReference create(
            @JsonProperty("reference") String reference,
            @JsonProperty("type") ReferenceType type,
            @JsonProperty("sheetTitle") String sheetTitle) {
        return new SpreadsheetReference(reference, type, sheetTitle);
    }

    /**
     * Validates the reference format based on its type.
     *
     * @throws IllegalArgumentException if the reference is invalid
     */
    private void validateReference() {
        switch (type) {
            case COLUMN -> validateColumn();
            case ROW -> validateRow();
            case CELL -> validateCell();
            case RANGE -> validateRange();
        }
    }

    /**
     * Validates a column reference (e.g., "A", "BC").
     *
     * @throws IllegalArgumentException if the column reference is invalid
     */
    private void validateColumn() {
        if (!COLUMN_PATTERN.matcher(reference).matches()) {
            throw new IllegalArgumentException("Invalid column: Must be uppercase letters A-Z");
        }
    }

    /**
     * Validates a row reference (e.g., "1", "42").
     *
     * @throws IllegalArgumentException if the row reference is invalid
     */
    private void validateRow() {
        if (!ROW_PATTERN.matcher(reference).matches()) {
            throw new IllegalArgumentException("Invalid row: Must be positive integer");
        }
        if (Integer.parseInt(reference) == 0) {
            throw new IllegalArgumentException("Invalid row: Row cannot be zero");
        }
    }

    /**
     * Validates a cell reference (e.g., "A1", "Z42").
     *
     * @throws IllegalArgumentException if the cell reference is invalid
     */
    private void validateCell() {
        if (!CELL_PATTERN.matcher(reference).matches()) {
            throw new IllegalArgumentException("Invalid cell format: Must be column letters followed by row number");
        }
    }

    /**
     * Validates a range reference (e.g., "A1:B2").
     *
     * @throws IllegalArgumentException if the range reference is invalid
     */
    private void validateRange() {
        String[] parts = reference.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Range must have two parts separated by ':'");
        }

        SpreadsheetReference start = SpreadsheetReference.ofCell(parts[0]);
    }

    /**
     * Creates a new reference with the specified sheet title.
     *
     * @param sheetTitle The sheet title to set
     * @return A new SpreadsheetReference with the specified sheet title
     */
    public SpreadsheetReference withSheetTitle(String sheetTitle) {
        this.sheetTitle = sheetTitle;
        return this;
    }

    /**
     * Creates a column reference (e.g., "A", "BC").
     *
     * @param column The column reference string
     * @return A new SpreadsheetReference of COLUMN type
     * @throws IllegalArgumentException if the column reference is invalid
     */
    public static SpreadsheetReference ofColumn(String column) {
        return new SpreadsheetReference(column, ReferenceType.COLUMN);
    }

    /**
     * Creates a column reference with a sheet title (e.g., "Sheet1!A").
     *
     * @param sheetName The sheet name
     * @param column The column reference string
     * @return A new SpreadsheetReference of COLUMN type with sheet title
     * @throws IllegalArgumentException if the column reference is invalid
     */
    public static SpreadsheetReference ofColumn(String sheetName, String column) {
        return new SpreadsheetReference(column, ReferenceType.COLUMN, sheetName);
    }

    /**
     * Creates a row reference (e.g., "1", "42").
     *
     * @param row The row reference string
     * @return A new SpreadsheetReference of ROW type
     * @throws IllegalArgumentException if the row reference is invalid
     */
    public static SpreadsheetReference ofRow(String row) {
        return new SpreadsheetReference(row, ReferenceType.ROW);
    }

    /**
     * Creates a row reference (e.g., 1, 42).
     *
     * @param row The row number
     * @return A new SpreadsheetReference of ROW type
     * @throws IllegalArgumentException if the row reference is invalid
     */
    public static SpreadsheetReference ofRow(int row) {
        if (row <= 0) {
            throw new IllegalArgumentException("Row must be positive integer");
        }
        return new SpreadsheetReference(String.valueOf(row), ReferenceType.ROW);
    }

    /**
     * Creates a row reference with a sheet title (e.g., "Sheet1!1").
     *
     * @param sheetName The sheet name
     * @param row The row number
     * @return A new SpreadsheetReference of ROW type with sheet title
     * @throws IllegalArgumentException if the row reference is invalid
     */
    public static SpreadsheetReference ofRow(String sheetName, int row) {
        if (row <= 0) {
            throw new IllegalArgumentException("Row must be positive integer");
        }
        return new SpreadsheetReference(String.valueOf(row), ReferenceType.ROW, sheetName);
    }

    /**
     * Creates a cell reference (e.g., "A1", "Z42").
     *
     * @param cell The cell reference string
     * @return A new SpreadsheetReference of CELL type
     * @throws IllegalArgumentException if the cell reference is invalid
     */
    public static SpreadsheetReference ofCell(String cell) {
        return new SpreadsheetReference(cell, ReferenceType.CELL);
    }

    /**
     * Creates a cell reference with a sheet title (e.g., "Sheet1!A1").
     *
     * @param sheetName The sheet name
     * @param cell The cell reference string
     * @return A new SpreadsheetReference of CELL type with sheet title
     * @throws IllegalArgumentException if the cell reference is invalid
     */
    public static SpreadsheetReference ofCell(String sheetName, String cell) {
        return new SpreadsheetReference(cell, ReferenceType.CELL, sheetName);
    }

    /**
     * Creates a range reference (e.g., "A1:B2").
     *
     * @param range The range reference string
     * @return A new SpreadsheetReference of RANGE type
     * @throws IllegalArgumentException if the range reference is invalid
     */
    public static SpreadsheetReference ofRange(String range) {
        return new SpreadsheetReference(range, ReferenceType.RANGE);
    }

    /**
     * Creates a range reference with a sheet title (e.g., "Sheet1!A1:B2").
     *
     * @param sheetName The sheet name
     * @param range The range reference string
     * @return A new SpreadsheetReference of RANGE type with sheet title
     * @throws IllegalArgumentException if the range reference is invalid
     */
    public static SpreadsheetReference ofRange(String sheetName, String range) {
        return new SpreadsheetReference(range, ReferenceType.RANGE, sheetName);
    }

    /**
     * Parses a Google Sheets reference format into a SpreadsheetReference object.
     * Handles both sheet-qualified (e.g., "Sheet1!A1:B2") and non-qualified (e.g., "A1") references.
     *
     * @param reference The Google Sheets reference string
     * @return A new SpreadsheetReference corresponding to the input
     * @throws IllegalArgumentException if the reference format is invalid
     */
    public static SpreadsheetReference fromGoogleReference(String reference) {
        Objects.requireNonNull(reference, "Reference cannot be null");

        Matcher matcher = SHEET_REFERENCE_PATTERN.matcher(reference);
        if (matcher.matches()) {
            String sheetName = matcher.group(1);
            String actualReference = matcher.group(2);
            return parseReferenceWithSheet(actualReference, sheetName);
        } else {
            return parseReference(reference);
        }
    }

    /**
     * Parses a non-sheet-qualified reference string into a SpreadsheetReference object.
     *
     * @param reference The reference string to parse
     * @return A new SpreadsheetReference corresponding to the input
     * @throws IllegalArgumentException if the reference format is invalid
     */
    private static SpreadsheetReference parseReference(String reference) {
        // Handle column ranges like "A:A"
        Pattern columnRangePattern = Pattern.compile("^([A-Z]+):([A-Z]+)$");
        Matcher columnRangeMatcher = columnRangePattern.matcher(reference);
        if (columnRangeMatcher.matches()) {
            String startCol = columnRangeMatcher.group(1);
            String endCol = columnRangeMatcher.group(2);
            if (startCol.equals(endCol)) {
                return ofColumn(startCol);
            }
        }

        // Handle row ranges like "1:1"
        Pattern rowRangePattern = Pattern.compile("^([1-9]\\d*):([1-9]\\d*)$");
        Matcher rowRangeMatcher = rowRangePattern.matcher(reference);
        if (rowRangeMatcher.matches()) {
            String startRow = rowRangeMatcher.group(1);
            String endRow = rowRangeMatcher.group(2);
            if (startRow.equals(endRow)) {
                return ofRow(startRow);
            }
        }

        // Handle standard patterns
        if (COLUMN_PATTERN.matcher(reference).matches()) {
            return ofColumn(reference);
        } else if (ROW_PATTERN.matcher(reference).matches()) {
            return ofRow(reference);
        } else if (CELL_PATTERN.matcher(reference).matches()) {
            return ofCell(reference);
        } else if (RANGE_PATTERN.matcher(reference).matches()) {
            return ofRange(reference);
        } else {
            throw new IllegalArgumentException("Invalid reference format: " + reference);
        }
    }

    /**
     * Parses a reference string with a sheet title.
     *
     * @param reference The reference string to parse
     * @param sheetName The sheet name
     * @return A new SpreadsheetReference with the specified sheet title
     */
    private static SpreadsheetReference parseReferenceWithSheet(String reference, String sheetName) {
        SpreadsheetReference ref = parseReference(reference);
        return new SpreadsheetReference(ref.reference, ref.type, sheetName);
    }

    /**
     * Gets the reference type.
     *
     * @return The reference type
     */
    public ReferenceType getType() {
        return type;
    }

    /**
     * Gets the reference string.
     *
     * @return The reference string
     */
    public String getReference() {
        return reference;
    }

    /**
     * Gets the sheet title.
     *
     * @return The sheet title, or null if none is set
     */
    public String getSheetTitle() {
        return sheetTitle;
    }

    /**
     * Converts this reference to Google Sheets API format.
     * <p>
     *     For non-range types (CELL, COLUMN, ROW), this produces a self-to-self range
     *     (e.g. "A1:A1" for cell "A1" or "J:J" for column "J").
     * </p>

     * @return The reference in Google Sheets API format
     */
    @JsonIgnore
    public String getGoogleSheetsReference() {
        String formattedReference;

        switch (type) {
            case CELL, COLUMN, ROW -> formattedReference = reference + ":" + reference;
            case RANGE -> formattedReference = reference;
            default -> throw new IllegalStateException("Unsupported reference type: " + type);
        }

        if (sheetTitle != null && !sheetTitle.isBlank()) {
            return sheetTitle + "!" + formattedReference;
        }

        return formattedReference;
    }

    /**
     * Extracts the row number from a ROW type reference.
     *
     * @return The parsed row number
     * @throws IllegalArgumentException if the reference is not of ROW type
     */
    public int extractRowNumber() {
        if (type == ReferenceType.ROW) {
            return Integer.parseInt(reference);
        }
        throw new IllegalArgumentException("Cannot extract row number from a non-row reference type: " + type);
    }

    /**
     * Compares this SpreadsheetReference to the specified object for equality.
     * Two SpreadsheetReference objects are considered equal if they have the same
     * reference string, reference type and sheet title.
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpreadsheetReference that)) return false;

        if (!Objects.equals(reference, that.reference)) return false;
        if (!Objects.equals(sheetTitle, that.sheetTitle)) return false;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (reference != null ? reference.hashCode() : 0);
        result = 31 * result + (sheetTitle != null ? sheetTitle.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (sheetTitle != null && sheetTitle.isBlank()) {
            return sheetTitle + "!" + reference;
        }
        return reference;
    }

}
