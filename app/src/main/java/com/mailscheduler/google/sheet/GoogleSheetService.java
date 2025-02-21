package com.mailscheduler.google.sheet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.mailscheduler.google.GoogleAuthService;
import com.mailscheduler.model.SpreadsheetReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class GoogleSheetService extends GoogleAuthService<Sheets> {
    private final Logger LOGGER = Logger.getLogger(GoogleSheetService.class.getName());

    private Sheets sheetsService;
    private static volatile GoogleSheetService instance;

    private final SheetReader sheetReader;
    private final SheetWriter sheetWriter;
    private final SheetSearcher sheetSearcher;

    private GoogleSheetService() throws Exception {
        super();
        if (instance != null) {
            throw new IllegalStateException("GoogleSheetService instance already exists");
        }
        this.sheetReader = new SheetReader(sheetsService);
        this.sheetWriter = new SheetWriter(sheetsService);
        this.sheetSearcher = new SheetSearcher(sheetReader);
    }

    public static GoogleSheetService getInstance() throws Exception {
        if (instance == null) {
            synchronized (GoogleSheetService.class) {
                if (instance == null) {
                    instance = new GoogleSheetService();
                }
            }
        }
        return instance;
    }

    @Override
    protected List<String> getScopes() {
        return Collections.singletonList(SheetsScopes.SPREADSHEETS);
    }

    @Override
    protected void initializeService(Credential credential) {
        this.sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public boolean validateSpreadsheetID(String spreadsheetId) {
        try {
            sheetsService.spreadsheets().get(spreadsheetId).execute();
            return true;
        } catch (IOException e) {
            LOGGER.warning("Spreadsheet validation failed: " + e.getMessage());
            return false;
        }
    }

    public List<List<Object>> readSpreadsheet(String spreadsheetId, SpreadsheetReference range) throws IOException {
        return sheetReader.readSpreadsheet(spreadsheetId, range.getGoogleSheetsReference());
    }

    public List<ValueRange> readSpreadsheetBatch(String spreadsheetId, List<SpreadsheetReference> ranges) throws IOException {
        List<String> rangeStrings = ranges.stream()
                .map(SpreadsheetReference::getGoogleSheetsReference)
                .toList();
        return sheetReader.readSpreadsheetBatch(spreadsheetId, rangeStrings);
    }

    public void writeToSpreadsheetCell(String spreadsheetId, SpreadsheetReference cell, String value) throws IOException {
        sheetWriter.writeToSpreadsheetCell(spreadsheetId, cell.getGoogleSheetsReference(), value);
    }

    public void writeToSpreadsheetCells(String spreadsheetId, List<SpreadsheetReference> cells, String value) throws IOException {
        List<String> cellStrings = cells.stream()
                .map(SpreadsheetReference::getGoogleSheetsReference)
                .toList();
        sheetWriter.writeToSpreadsheetCells(spreadsheetId, cellStrings, value);
    }

    public void writeToSpreadsheetCells(String spreadsheetId, List<SpreadsheetReference> cells, List<String> values) throws IOException {
        if (cells.isEmpty()) return;
        LOGGER.info("Writing to multiple spreadsheet cells: " + cells + " with values: " + values);

        List<String> cellStrings = cells.stream()
                .map(SpreadsheetReference::getGoogleSheetsReference)
                .toList();
        sheetWriter.writeToSpreadsheetCells(spreadsheetId, cellStrings, values);
    }

    public SpreadsheetReference getSpreadsheetCellIndex(String spreadsheetId, SpreadsheetReference column, String expression) throws IOException {
        String cellIndex = sheetSearcher.findCellIndex(spreadsheetId, column, expression);
        return cellIndex != null ? SpreadsheetReference.ofCell(cellIndex) : null;
    }

    public List<SpreadsheetReference> getSpreadsheetCellIndices(String spreadsheetId, SpreadsheetReference column, List<String> expressions) throws IOException {
        List<String> stringCellIndices = sheetSearcher.findCellIndices(spreadsheetId, column, expressions);
        List<SpreadsheetReference> cellIndices = new ArrayList<>(stringCellIndices.size());
        for (String stringCellIndex : stringCellIndices) {
            cellIndices.add(SpreadsheetReference.ofCell(stringCellIndex));
        }
        return cellIndices;
    }

}
