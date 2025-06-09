package com.mailscheduler.application.synchronization.spreadsheet.gateway;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.infrastructure.google.sheet.GoogleSheetService;

import java.io.IOException;
import java.util.List;

/**
 * Adapter for Google Sheets implementing the SpreadsheetGateway interface.
 * Provides a layer of abstraction between the application and the Google Sheets API.
 */
public class GoogleSheetAdapter implements SpreadsheetGateway {
    private final GoogleSheetService googleSheetService;

    public GoogleSheetAdapter(GoogleSheetService googleSheetService) {
        this.googleSheetService = googleSheetService;
    }

    @Override
    public Spreadsheet getSpreadsheet(String spreadsheetId) throws IOException {
        return googleSheetService.getSpreadsheet(spreadsheetId);
    }

    @Override
    public void createSheet(String spreadsheetId, String title, Integer index) throws IOException {
        googleSheetService.createSheet(spreadsheetId, title, index);
    }

    @Override
    public List<List<Object>> readData(String spreadsheetId, SpreadsheetReference range) throws IOException {
        if (spreadsheetId == null || range == null) return List.of();

        return googleSheetService.readSpreadsheet(spreadsheetId, range);
    }

    @Override
    public List<ValueRange> readDataBatch(String spreadsheetId, List<SpreadsheetReference> ranges) throws IOException {
        if (spreadsheetId == null || ranges == null || ranges.isEmpty()) return List.of();
        return googleSheetService.readSpreadsheetBatch(spreadsheetId, ranges);
    }

    @Override
    public void writeData(String spreadsheetId, List<SpreadsheetReference> rows, List<List<Object>> values) throws IOException {
        googleSheetService.writeToSpreadsheetRows(spreadsheetId, rows, values);
    }

    @Override
    public void writeDataToCell(String spreadsheetId, SpreadsheetReference cell, String value) throws IOException {
        googleSheetService.writeToSpreadsheetCell(spreadsheetId, cell, value);
    }

    @Override
    public void writeDataToCells(String spreadsheetId, List<SpreadsheetReference> cells, List<String> values) throws IOException {
        googleSheetService.writeToSpreadsheetCells(spreadsheetId, cells, values);
    }

    @Override
    public void clearValues(String spreadsheetId, SpreadsheetReference range) throws IOException {
        System.out.println("Method not implemented yet");
    }

    @Override
    public void formatCells(String spreadsheetId, SpreadsheetReference range, Boolean bold, String backgroundColor, String textColor) throws IOException {
        System.out.println("Method not implemented yet");
    }
}
