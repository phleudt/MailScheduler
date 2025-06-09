package com.mailscheduler.domain.model.common.vo.spreadsheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for a Google Spreadsheet.
 * <p>
 *     This value object holds identification and structural information about a Google Spreadsheet.
 *     It includes the spreadsheet ID, title, and configurations for individual sheets within the spreadsheet.
 * </p>
 */
public record SpreadsheetConfiguration(
        String spreadsheetId,
        String title,
        List<SheetConfiguration> sheetConfigurations
) {
    /**
     * Creates a validated SpreadsheetConfiguration.
     *
     * @throws NullPointerException if required parameters are null
     * @throws IllegalArgumentException if invalid parameters are provided
     */
    public SpreadsheetConfiguration {
        Objects.requireNonNull(spreadsheetId, "Spreadsheet ID cannot be null");
        Objects.requireNonNull(title, "Spreadsheet title cannot be null");

        if (spreadsheetId.isBlank()) {
            throw new IllegalArgumentException("Spreadsheet ID cannot be blank");
        }

        if (title.isBlank()) {
            throw new IllegalArgumentException("Spreadsheet title cannot be blank");
        }

        // Defensive copy of the sheet configurations list
        sheetConfigurations = sheetConfigurations == null ?
                Collections.emptyList() :
                List.copyOf(sheetConfigurations);
    }

    /**
     * Creates a new spreadsheet configuration with an additional sheet.
     *
     * @param sheetConfiguration The sheet configuration to add
     * @return A new SpreadsheetConfiguration with the added sheet
     */
    public SpreadsheetConfiguration addSheet(SheetConfiguration sheetConfiguration) {
        Objects.requireNonNull(sheetConfiguration, "Sheet configuration cannot be null");

        List<SheetConfiguration> newSheets = new ArrayList<>(sheetConfigurations);
        newSheets.add(sheetConfiguration);
        return new SpreadsheetConfiguration(spreadsheetId, title, newSheets);
    }

    @Override
    public String toString() {
        return "Spreadsheet[" + title + ", id=" + spreadsheetId +
                ", sheets=" + (sheetConfigurations == null ? 0 : sheetConfigurations.size()) + "]";
    }
}
