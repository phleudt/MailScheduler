package com.mailscheduler.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.mailscheduler.model.SpreadsheetReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        this.sheetReader = new SheetReader();
        this.sheetWriter = new SheetWriter();
        this.sheetSearcher = new SheetSearcher();
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
            System.err.println("Spreadsheet validation failed: " + e.getMessage());
            return false;
        }
    }

    public List<List<Object>> readSpreadsheet(String spreadsheetId, SpreadsheetReference range) throws IOException {
        return sheetReader.readSpreadsheet(spreadsheetId, range.getGoogleSheetsReference());
    }

    public List<ValueRange> readSpreadsheetBatch(String spreadsheetId, List<SpreadsheetReference> ranges) throws IOException {
        List<String> rangeStrings = ranges.stream()
                .map(SpreadsheetReference::getGoogleSheetsReference) // changed form getReference
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
        return SpreadsheetReference.ofCell(sheetSearcher.findCellIndex(spreadsheetId, column, expression));
    }

    public List<SpreadsheetReference> getSpreadsheetCellIndices(String spreadsheetId, SpreadsheetReference column, List<String> expressions) throws IOException {
        List<String> stringCellIndices = sheetSearcher.findCellIndices(spreadsheetId, column, expressions);
        List<SpreadsheetReference> cellIndices = new ArrayList<>(stringCellIndices.size());
        for (String stringCellIndex : stringCellIndices) {
            SpreadsheetReference cellIndex = SpreadsheetReference.ofCell(stringCellIndex);
            cellIndices.add(cellIndex);
        }
        return cellIndices;
    }

    public class SheetReader {
        List<List<Object>> readSpreadsheet(String spreadsheetId, String range) throws IOException {
            LOGGER.info("Reading spreadsheet with range: " + range);
            return sheetsService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute().getValues();
        }

        List<ValueRange> readSpreadsheetBatch(String spreadsheetId, List<String> ranges) throws IOException {
            LOGGER.info("Reading spreadsheet with ranges: " + ranges);
            return sheetsService.spreadsheets().values()
                    .batchGet(spreadsheetId).setRanges(ranges)
                    .execute().getValueRanges();
        }
    }

    public class SheetWriter {
        void writeToSpreadsheetCell(String spreadsheetId, String cell, String value) throws IOException {
            if (cell.isEmpty()) return;
            LOGGER.info("Writing to spreadsheet cell " + cell + " with value: " + value);
            ValueRange body = new ValueRange()
                    .setValues(Collections.singletonList(Collections.singletonList(value)));
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, cell, body)
                    .setValueInputOption("RAW")
                    .execute();
        }

        void writeToSpreadsheetCells(String spreadsheetId, List<String> cells, String value) throws IOException {
            if (cells.isEmpty()) return;
            LOGGER.info("Writing to multiple spreadsheet cells: " + cells + " with value: " + value);
            List<ValueRange> data = cells.stream()
                    .map(cell -> new ValueRange()
                            .setRange(cell)
                            .setValues(Collections.singletonList(Collections.singletonList(value))))
                    .collect(Collectors.toList());

            BatchUpdateValuesRequest body  = new BatchUpdateValuesRequest()
                    .setValueInputOption("RAW")
                    .setData(data);

            sheetsService.spreadsheets().values()
                    .batchUpdate(spreadsheetId, body)
                    .execute();
        }

        void writeToSpreadsheetCells(String spreadsheetId, List<String> cells, List<String> values) throws IOException {
            if (cells.isEmpty()) return;
            LOGGER.info("Writing to multiple spreadsheet cells: " + cells + " with values: " + values);

            List<Request> requests = new ArrayList<>();
            for (int i = 0; i < cells.size(); i++) {
                requests.add(createUpdateCellRequest(cells.get(i), values.get(i)));
            }

            BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
            sheetsService.spreadsheets().
                    batchUpdate(spreadsheetId, body)
                    .execute();
        }

        private String generateScheduleExpression(
                SpreadsheetReference referenceColumnIndex,
                SpreadsheetReference referenceRowIndex,
                int offsetDays) {
            return String.format("=%s%s + %d", referenceColumnIndex.getReference(), referenceRowIndex.getRow(), offsetDays);
        }

        private Request createUpdateCellRequest(String cell, String value) {
            String column = cell.split(":")[0].replaceAll("[^A-Z]", "");
            int row = Integer.parseInt(cell.split(":")[0].replaceAll("[^0-9]", "")) - 1;

            CellData cellData = new CellData()
                    .setUserEnteredValue(new ExtendedValue().setFormulaValue(value));

            // Check if the value is a formula (starts with '=')
            if (value.startsWith("=")) {
                cellData.setUserEnteredValue(new ExtendedValue().setFormulaValue(value));
            } else {
                cellData.setUserEnteredValue(new ExtendedValue().setStringValue(value));
            }


            GridRange gridRange = new GridRange()
                    .setSheetId(0)
                    .setStartRowIndex(row)
                    .setEndRowIndex(row + 1)
                    .setStartColumnIndex(column.charAt(0) - 'A')
                    .setEndColumnIndex(column.charAt(0) - 'A' + 1);

            UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest()
                    .setRange(gridRange)
                    .setRows(Collections.singletonList(new RowData().setValues(Collections.singletonList(cellData))))
                    .setFields("userEnteredValue");

            return new Request().setUpdateCells(updateCellsRequest);
        }
    }

    public class SheetSearcher {
        String findCellIndex(String spreadsheetId, SpreadsheetReference column, String expression) throws IOException {
            LOGGER.info("Get spreadsheet cell index in range: " + column + " searching for: " + expression);
            List<List<Object>> values = readSpreadsheet(spreadsheetId, column);
            int rowIndex = 1;
            for (List<Object> row : values) {
                for (Object value : row) {
                    if (expression.equals(value.toString())) {
                        return column + "" + rowIndex;
                    }
                }
                rowIndex++;
            }

            LOGGER.info("Cell with value: " + expression + " not found.");
            return null;
        }

        List<String> findCellIndices(String spreadsheetId, SpreadsheetReference column, List<String> expressions) throws IOException {
            LOGGER.info("Getting spreadsheet cell index in range: " + column + " for: " + expressions);
            List<List<Object>> values = readSpreadsheet(spreadsheetId, column);
            List<String> cellIndices = new ArrayList<>();
            int rowIndex = 1;
            for (List<Object> row : values) {
                for (Object value : row) {
                    for (String expression : expressions) {
                        if (expression.equals(value.toString())) {
                            cellIndices.add(column.getReference() + rowIndex);
                        }
                        if (cellIndices.size() == expressions.size()) return cellIndices;
                    }
                }
                rowIndex++;
            }
            if (cellIndices.isEmpty()) {
                LOGGER.info("No cell indices found");
            }
            return cellIndices;
        }
    }
}
