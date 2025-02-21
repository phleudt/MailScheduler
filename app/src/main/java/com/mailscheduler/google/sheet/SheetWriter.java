package com.mailscheduler.google.sheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SheetWriter {
    private static final Logger LOGGER = Logger.getLogger(SheetWriter.class.getName());
    private final Sheets sheetsService;

    public SheetWriter(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    public void writeToSpreadsheetCell(String spreadsheetId, String cell, String value) throws IOException {
        if (cell.isEmpty()) return;
        LOGGER.info("Writing to spreadsheet cell " + cell + " with value: " + value);

        ValueRange body = new ValueRange()
                .setValues(Collections.singletonList(Collections.singletonList(value)));

        sheetsService.spreadsheets().values()
                .update(spreadsheetId, cell, body)
                .setValueInputOption("RAW")
                .execute();
    }

    public void writeToSpreadsheetCells(String spreadsheetId, List<String> cells, String value) throws IOException {
        if (cells.isEmpty()) return;
        LOGGER.info("Writing to multiple spreadsheet cells: " + cells + " with value: " + value);

        List<ValueRange> data = cells.stream()
                .map(cell -> new ValueRange()
                        .setRange(cell)
                        .setValues(Collections.singletonList(Collections.singletonList(value))))
                .collect(Collectors.toList());

        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(data);

        sheetsService.spreadsheets().values()
                .batchUpdate(spreadsheetId, body)
                .execute();
    }

    public void writeToSpreadsheetCells(String spreadsheetId, List<String> cells, List<String> values) throws IOException {
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

    private Request createUpdateCellRequest(String cell, String value) {
        String column = cell.split(":")[0].replaceAll("[^A-Z]", "");
        int row = Integer.parseInt(cell.split(":")[0].replaceAll("[^0-9]", "")) - 1;

        CellData cellData = createCellData(value);
        GridRange gridRange = createGridRange(row, column);

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest()
                .setRange(gridRange)
                .setRows(Collections.singletonList(new RowData().setValues(Collections.singletonList(cellData))))
                .setFields("userEnteredValue");

        return new Request().setUpdateCells(updateCellsRequest);
    }

    private CellData createCellData(String value) {
        CellData cellData = new CellData()
                .setUserEnteredValue(new ExtendedValue().setFormulaValue(value));
        if (value.startsWith("=")) {
            cellData.setUserEnteredValue(new ExtendedValue().setFormulaValue(value));
        } else {
            cellData.setUserEnteredValue(new ExtendedValue().setStringValue(value));
        }
        return cellData;
    }

    private GridRange createGridRange(int row, String column) {
        return new GridRange()
                .setSheetId(0)
                .setStartRowIndex(row)
                .setEndRowIndex(row + 1)
                .setStartColumnIndex(column.charAt(0) - 'A')
                .setEndColumnIndex(column.charAt(0) - 'A' + 1);
    }
}
