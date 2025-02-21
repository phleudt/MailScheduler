package com.mailscheduler.google.sheet;

import com.mailscheduler.model.SpreadsheetReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SheetSearcher {
    private static final Logger LOGGER = Logger.getLogger(SheetSearcher.class.getName());
    private final SheetReader sheetReader;

    public SheetSearcher(SheetReader sheetReader) {
        this.sheetReader = sheetReader;
    }

    public String findCellIndex(String spreadsheetId, SpreadsheetReference column, String expression) throws IOException {
        LOGGER.info("Get spreadsheet cell index in range: " + column + " searching for: " + expression);
        List<List<Object>> values = sheetReader.readSpreadsheet(spreadsheetId, column.getGoogleSheetsReference());

        int rowIndex = 1;
        for (List<Object> row : values) {
            for (Object value : row) {
                if (expression.equals(value.toString())) {
                    return column.getReference() + rowIndex;
                }
            }
            rowIndex++;
        }

        LOGGER.info("Cell with value: " + expression + " not found.");
        return null;
    }

    public List<String> findCellIndices(String spreadsheetId, SpreadsheetReference column, List<String> expressions) throws IOException {
        LOGGER.info("Getting spreadsheet cell index in range: " + column + " for: " + expressions);
        List<List<Object>> values = sheetReader.readSpreadsheet(spreadsheetId, column.getGoogleSheetsReference());
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
