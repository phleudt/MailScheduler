package com.mailscheduler.model;

import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Represents a type-safe and validated spreadsheet reference.
 * Immutable class that ensures the integrity of spreadsheet cell, column, and row references.
 */
public final class SpreadsheetReference {
    private static Logger LOGGER = Logger.getLogger(SpreadsheetReference.class.getName());

    /**
     * Enumeration of possible spreadsheet reference types.
     */
    public enum ReferenceType {
        COLUMN, // Single column (e.g., "A")
        COLUMN_RANGE, // Single column range (e.g., "A1:A5")
        ROW, // Single row (e.g., "1")
        ROW_RANGE, // Single row range (e.g., "A1:D1")
        CELL, // Single cell (e.g., "A1")
        RANGE // Single range (e.g., "A1:B2")
    }

    // Validation Patterns
    private static final Pattern COLUMN_PATTERN = Pattern.compile("^[A-Z]+$");
    private static final Pattern ROW_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private static final Pattern CELL_PATTERN = Pattern.compile("^[A-Z]+[1-9]\\d*$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^[A-Z]+[1-9]\\d*:[A-Z]+[1-9]\\d*$");

    private final String reference;
    private final ReferenceType type;

    private SpreadsheetReference(String reference, ReferenceType type) {
        this.reference = reference;
        this.type = type;
        validateReference();
    }

    /**
     * Validates the reference based on its type.
     *
     * @throws IllegalArgumentException If the reference is invalid for its type
     */
    private void validateReference() {
        switch (type) {
            case COLUMN -> validateColumn();
            case ROW -> validateRow();
            case CELL -> validateCell();
            case RANGE -> validateRange();
            case COLUMN_RANGE -> validateColumnRange();
            case ROW_RANGE -> validateRowRange();
        };
    }

    private void validateColumn() {
        if (!COLUMN_PATTERN.matcher(reference).matches()) {
            throw new IllegalArgumentException("Invalid column: Must be uppercase letters A-Z");
        }
    }

    private void validateRow() {
        if (!ROW_PATTERN.matcher(reference).matches()) {
            throw new IllegalArgumentException("Invalid row: Must be positive integer");
        }
    }

    private void validateCell() {
        if (!CELL_PATTERN.matcher(reference).matches()) {
            throw new IllegalArgumentException("Invalid cell format: Must be column letters followed by row number");
        }
    }

    private void validateRange() {
        String[] parts = reference.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Range must have two parts separated by ':'");
        }

        SpreadsheetReference start = SpreadsheetReference.ofCell(parts[0]);
        SpreadsheetReference end = SpreadsheetReference.ofCell(parts[1]);
    }

    private void validateColumnRange() {
        String[] parts = reference.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Column range must have two parts");
        }

        if (!parts[0].replaceAll("[0-9]", "").equals(parts[1].replaceAll("[0-9]", ""))) {
            throw new IllegalArgumentException("Column range must be within the same column");
        }
    }

    private void validateRowRange() {
        String[] parts = reference.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Row range must have two parts");
        }

        if (!parts[0].replaceAll("[A-Z]", "").equals(parts[1].replaceAll("[A-Z]", ""))) {
            throw new IllegalArgumentException("Row range must be within the same row");
        }
    }

    // Factory Methods
    public static SpreadsheetReference ofColumn(String column) {
        return new SpreadsheetReference(column, ReferenceType.COLUMN);
    }

    public static SpreadsheetReference ofRow(String row) {
        return new SpreadsheetReference(row, ReferenceType.ROW);
    }

    public static SpreadsheetReference ofRow(int row) {
        return new SpreadsheetReference(String.valueOf(row), ReferenceType.ROW);
    }

    public static SpreadsheetReference ofCell(String cell) {
        return new SpreadsheetReference(cell, ReferenceType.CELL);
    }

    public static SpreadsheetReference ofRange(String range) {
        return new SpreadsheetReference(range, ReferenceType.RANGE);
    }

    public static SpreadsheetReference ofColumnRange(String columnRange) {
        return new SpreadsheetReference(columnRange, ReferenceType.COLUMN_RANGE);
    }

    public static SpreadsheetReference ofRowRange(String rowRange) {
        return new SpreadsheetReference(rowRange, ReferenceType.ROW_RANGE);
    }

    // Extraction Methods
    public String getColumn() {
        return extractColumn();
    }

    public String getRow() {
        return extractRow();
    }

    private String extractColumn() {
        return switch (type) {
            case COLUMN -> reference;
            case CELL -> reference.replaceAll("[^A-Z]", "");
            case COLUMN_RANGE, ROW_RANGE, RANGE -> reference.split(":")[0].replaceAll("[^A-Z]", "");
            default -> throw new UnsupportedOperationException("Cannot extract row from this reference type.");
        };
    }

    private String extractRow() {
        return switch (type) {
            case ROW -> reference;
            case CELL -> reference.replaceAll("[^0-9]", "");
            case COLUMN_RANGE, ROW_RANGE, RANGE -> reference.split(":")[0].replaceAll("[^0-9]", "");
            default -> throw new UnsupportedOperationException("Cannot extract column from this reference type.");
        };
    }

    // Conversion Methods
    public char toColumnIndex() {
        String column = getColumn();
        return (char) (column.charAt(0) - 'A');
    }

    public int toRowIndex() {
        return Integer.parseInt(getRow()) - 1;
    }

    /**
     * Retrieves the start cell for a range reference.
     *
     * @return The start cell of the range
     * @throws UnsupportedOperationException If not a range reference
     */
    public String getStartCell() {
        if (type != ReferenceType.RANGE) {
            throw new UnsupportedOperationException("Start cell is only available for ranges.");
        }
        return reference.split(":")[0];
    }

    /**
     * Retrieves the end cell for a range reference.
     *
     * @return The end cell of the range
     * @throws UnsupportedOperationException If not a range reference
     */
    public String getEndCell() {
        if (type != ReferenceType.RANGE) {
            throw new UnsupportedOperationException("End cell is only available for ranges.");
        }
        return reference.split(":")[1];
    }

    // Utility Methods
    public ReferenceType getType() {
        return type;
    }

    public String getReference() {
        return reference;
    }

    public String getGoogleSheetsReference() {
        switch (type) {
            case CELL, COLUMN, ROW -> {
                return reference + ":" + reference;
            }
            case COLUMN_RANGE, ROW_RANGE, RANGE -> {
                return reference;
            }
            default -> throw new IllegalStateException("Unsupported reference type: " + type);
        }
    }

    // Static validation methods
    public static boolean isValidColumn(String column) {
        if (column == null || column.isEmpty()) return false;
        return COLUMN_PATTERN.matcher(column).matches() && column.length() == 1;
    }

    public static boolean isValidRow(String row) {
        return ROW_PATTERN.matcher(row).matches();
    }

    public static boolean isValidCell(String cell) {
        return CELL_PATTERN.matcher(cell).matches();
    }

    public static boolean isValidRange(String range) {
        return RANGE_PATTERN.matcher(range).matches();
    }

    // Standard Object methods
    public boolean isEmpty() {
        return reference.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpreadsheetReference that)) return false;
        return Objects.equals(reference, that.reference) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, type);
    }

    @Override
    public String toString() {
        return "SpreadsheetReference{" +
                "reference='" + reference + '\'' +
                ", type=" + type +
                '}';
    }
}
