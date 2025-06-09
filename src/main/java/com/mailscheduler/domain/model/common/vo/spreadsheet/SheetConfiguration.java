package com.mailscheduler.domain.model.common.vo.spreadsheet;

import java.util.Objects;

/**
 * Configuration for an individual sheet within a spreadsheet.
 * <p>
 *     This value object holds identification and structural information about a specific sheet within
 *     a Google Spreadsheet. It can be used to locate and interact with the sheet through the Google Sheets API.
 * </p>
 */
public record SheetConfiguration(
        String sheetId,
        String title,
        Integer index
        // SpreadsheetReference dataRange, // Specific data range for this sheet
        // SpreadsheetReference headerRange, // Specific header range for this sheet
) {
    /**
     * Creates a validated SheetConfiguration.
     *
     * @throws NullPointerException if required parameters are null
     * @throws IllegalArgumentException if invalid parameters are provided
     */
    public SheetConfiguration {
        Objects.requireNonNull(title, "Sheet title cannot be null");

        if (title.isBlank()) {
            throw new IllegalArgumentException("Sheet title cannot be blank");
        }

        if (index != null && index < 0) {
            throw new IllegalArgumentException("Sheet index cannot be negative");
        }
    }

    @Override
    public String toString() {
        return "Sheet[" + title + ", index=" + index + "]";
    }
}
