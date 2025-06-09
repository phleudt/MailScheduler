package com.mailscheduler.domain.factory;

import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Factory for creating spreadsheet and sheet configuration objects.
 * <p>
 *     This factory handles both manual configuration creation and automatic configuration detection
 *     from existing Google Spreadsheets.
 * </p>
 */
public class SpreadsheetConfigurationFactory {
    private final SpreadsheetGateway spreadsheetGateway;

    /**
     * Creates a new SpreadsheetConfigurationFactory with the required gateway.
     *
     * @param spreadsheetGateway The gateway for interacting with Google Sheets API
     */
    public SpreadsheetConfigurationFactory(SpreadsheetGateway spreadsheetGateway) {
        this.spreadsheetGateway = Objects.requireNonNull(spreadsheetGateway,
                "SpreadsheetGateway cannot be null");
    }

    /**
     * Creates a spreadsheet configuration manually with the specified parameters.
     *
     * @param spreadsheetId The ID of the Google Spreadsheet
     * @param name The display name of the spreadsheet
     * @param sheetConfigurations The list of sheet configurations within the spreadsheet
     * @return A new SpreadsheetConfiguration instance
     * @throws IllegalArgumentException If any required parameter is invalid
     */
    public SpreadsheetConfiguration createSpreadsheetConfiguration(
            String spreadsheetId,
            String name,
            List<SheetConfiguration> sheetConfigurations
    ) {
        validateSpreadsheetParams(spreadsheetId, name, sheetConfigurations);
        return new SpreadsheetConfiguration(spreadsheetId, name, sheetConfigurations);
    }

    /**
     * Creates a sheet configuration manually with the specified parameters.
     *
     * @param sheetId The ID of the sheet within the spreadsheet
     * @param sheetName The name of the sheet
     * @param index The index position of the sheet within the spreadsheet
     * @return A new SheetConfiguration instance
     * @throws IllegalArgumentException If any required parameter is invalid
     */
    public SheetConfiguration createSheetConfiguration(
            String sheetId,
            String sheetName,
            Integer index
    ) {
        validateSheetParams(sheetId, sheetName, index);
        return new SheetConfiguration(sheetId, sheetName, index);
    }

    /**
     * Creates a spreadsheet configuration by detecting sheets and headers from the specified spreadsheet template.
     * This method connects to Google Sheets API to retrieve the structure and information of the spreadsheet.
     *
     * @param spreadsheetId The ID of the Google Spreadsheet to analyze
     * @return A new SpreadsheetConfiguration populated with detected information
     * @throws IOException If there's an error communicating with Google Sheets API
     * @throws IllegalArgumentException If spreadsheetId is invalid
     */
    public SpreadsheetConfiguration createFromExistingSpreadsheet(String spreadsheetId) throws IOException {
        if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet ID cannot be null or empty");
        }

        // Get sheet metadata from the gateway
        Spreadsheet spreadsheet = spreadsheetGateway.getSpreadsheet(spreadsheetId);
        if (spreadsheet == null || spreadsheet.getSheets() == null) {
            throw new IllegalStateException("Failed to retrieve valid spreadsheet data");
        }

        List<SheetConfiguration> sheetConfigurations = new ArrayList<>();
        String title = spreadsheet.getProperties() != null ?
                spreadsheet.getProperties().getTitle() : "Untitled Spreadsheet";

        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties() == null) continue;

            String sheetId = String.valueOf(sheet.getProperties().getSheetId());
            String sheetTitle = sheet.getProperties().getTitle();
            Integer index = sheet.getProperties().getIndex();

            sheetConfigurations.add(createSheetConfiguration(
                    sheetId, sheetTitle, index));
        }

        return createSpreadsheetConfiguration(spreadsheetId, title, sheetConfigurations);
    }

    /**
     * Validates parameters for spreadsheet configuration.
     */
    private void validateSpreadsheetParams(String spreadsheetId, String name, List<SheetConfiguration> configurations) {
        if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet ID cannot be null or empty");
        }
        if (name == null) {
            throw new IllegalArgumentException("Spreadsheet name cannot be null");
        }
        if (configurations == null) {
            throw new IllegalArgumentException("Sheet configurations list cannot be null");
        }
    }

    /**
     * Validates parameters for sheet configuration.
     */
    private void validateSheetParams(String sheetId, String sheetName, Integer index) {
        if (sheetId == null || sheetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sheet ID cannot be null or empty");
        }
        if (sheetName == null) {
            throw new IllegalArgumentException("Sheet name cannot be null");
        }
        if (index == null || index < 0) {
            throw new IllegalArgumentException("Sheet index cannot be null or negative");
        }
    }
}
