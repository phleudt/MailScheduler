package com.mailscheduler.domain.model.common.vo.spreadsheet;

import java.util.Objects;

/**
 * Represents a mapping between a named column and its location in a spreadsheet.
 * <p>
 *     This value object maps a logical column name (e.g., "Email", "Website") to its physical location
 *     in a spreadsheet. Columns are categorized by their usage type (CONTACT, RECIPIENT, EMAIL)
 *     to support different application features.
 * </p>
 */
public record ColumnMapping(
        MappingType type,
        String columnName,
        SpreadsheetReference columnReference
) {
    /**
     * Defines the different types of column mappings used in the application.
     */
    public enum MappingType {
        /** Columns containing contact information (e.g., name, phone) */
        CONTACT,

        /** Columns containing recipient information for emails */
        RECIPIENT,

        /** Columns containing email-specific information (e.g., status, sent date) */
        EMAIL
    }

    /**
     * Creates a validated ColumnMapping.
     *
     * @throws NullPointerException if any required parameter is null
     */
    public ColumnMapping {
        Objects.requireNonNull(type, "Mapping type cannot be null");
        Objects.requireNonNull(columnName, "Column name cannot be null");
        Objects.requireNonNull(columnReference, "Column reference cannot be null");

        if (columnName.isBlank()) {
            throw new IllegalArgumentException("Column name cannot be blank");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnMapping that)) return false;

        if (!Objects.equals(type, that.type)) return false;
        if (!Objects.equals(columnName, that.columnName)) return false;
        return Objects.equals(columnReference, that.columnReference);
    }

    @Override
    public String toString() {
        return type + ":" + columnName + "=" + columnReference;
    }
}
