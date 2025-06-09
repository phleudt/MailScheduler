package com.mailscheduler.infrastructure.google.sheet;

import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for searching data in Google Sheets.
 */
public class SheetSearcher {
    private static final Logger LOGGER = Logger.getLogger(SheetSearcher.class.getName());
    private final SheetReader sheetReader;

    public SheetSearcher(SheetReader sheetReader) {
        this.sheetReader = sheetReader;
    }

    /**
     * Finds a cell index in a column that contains the specified expression.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param column The column to search in
     * @param expression The expression to find
     * @return The cell index if found, null otherwise
     * @throws IOException If an I/O error occurs
     */
    public String findCellIndex(String spreadsheetId, SpreadsheetReference column, String expression) throws IOException {
        LOGGER.log(Level.INFO, "Searching for \"{0}\" in spreadsheet {1}, column {2}",
                new Object[]{expression, spreadsheetId, column});

        List<List<Object>> values = sheetReader.readSpreadsheet(spreadsheetId, column.getGoogleSheetsReference());

        if (values == null || values.isEmpty()) {
            LOGGER.info("No data found in column for search");
            return null;
        }

        int rowIndex = 1;
        for (List<Object> row : values) {
            if (row.isEmpty()) {
                rowIndex++;
                continue;
            }

            for (Object value : row) {
                if (value != null && expression.equals(value.toString())) {
                    String result = column.getReference() + rowIndex;
                    LOGGER.log(Level.INFO, "Found match at cell {0}", result);
                    return result;
                }
            }
            rowIndex++;
        }

        LOGGER.log(Level.INFO, "No match found for \"{0}\"", expression);
        return null;
    }

    /**
     * Finds multiple cell indices in a column that contain the specified expressions.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param column The column to search in
     * @param expressions The expressions to find
     * @return List of cell indices that match the expressions
     * @throws IOException If an I/O error occurs
     */
    public List<String> findCellIndices(String spreadsheetId, SpreadsheetReference column, List<String> expressions) throws IOException {
        LOGGER.log(Level.INFO, "Searching for {0} expressions in spreadsheet {1}, column {2}",
                new Object[]{expressions.size(), spreadsheetId, column});

        List<List<Object>> values = sheetReader.readSpreadsheet(spreadsheetId, column.getGoogleSheetsReference());
        List<String> cellIndices = new ArrayList<>();


        if (values == null || values.isEmpty()) {
            LOGGER.info("No data found in column for search");
            return cellIndices;
        }

        // Create a copy of expressions to track which ones we've found
        List<String> remainingExpressions = new ArrayList<>(expressions);

        int rowIndex = 1;
        for (List<Object> row : values) {
            if (row.isEmpty()) {
                rowIndex++;
                continue;
            }

            for (Object value : row) {
                if (value != null) {
                    String stringValue = value.toString();
                    for (int i = remainingExpressions.size() - 1; i >= 0; i--) {
                        String expression = remainingExpressions.get(i);
                        if (expression.equals(stringValue)) {
                            String cellIndex = column.getReference() + rowIndex;
                            cellIndices.add(cellIndex);
                            LOGGER.log(Level.FINE, "Found match for \"{0}\" at cell {1}",
                                    new Object[]{expression, cellIndex});
                            remainingExpressions.remove(i);
                            break;
                        }
                    }
                }
                // If we found all expressions, we can stop searching
                if (remainingExpressions.isEmpty()) {
                    LOGGER.info("Found all requested expressions");
                    return cellIndices;
                }
            }
            rowIndex++;
        }

        if (cellIndices.isEmpty()) {
            LOGGER.info("No matches found for any expressions");
        } else {
            LOGGER.log(Level.INFO, "Found {0} of {1} requested expressions",
                    new Object[]{cellIndices.size(), expressions.size()});
        }

        return cellIndices;
    }
}
