package com.mailscheduler.infrastructure.google.sheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Helper class for writing data to Google Sheets.
 */
public class SheetWriter {
    private static final Logger LOGGER = Logger.getLogger(SheetWriter.class.getName());
    private final Sheets sheetsService;

    public SheetWriter(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    /**
     * Writes a value to a single cell in a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param cell The cell reference in A1 notation
     * @param value The value to write
     * @throws IOException If an I/O error occurs
     */
    public void writeToSpreadsheetCell(String spreadsheetId, String cell, String value) throws IOException {
        if (cell == null || cell.isEmpty()) {
            LOGGER.warning("Cannot write to empty cell reference");
            return;
        }

        LOGGER.log(Level.INFO, "Writing to spreadsheet {0} cell {1} with value: {2}",
                new Object[]{spreadsheetId, cell, value});

        ValueRange body = new ValueRange()
                .setValues(Collections.singletonList(Collections.singletonList(value)));

        sheetsService.spreadsheets().values()
                .update(spreadsheetId, cell, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    /**
     * Writes the same value to multiple cells in a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param cells The cell references in A1 notation
     * @param value The value to write to all cells
     * @throws IOException If an I/O error occurs
     */
    public void writeToSpreadsheetCells(String spreadsheetId, List<String> cells, String value) throws IOException {
        if (cells == null || cells.isEmpty()) {
            LOGGER.warning("Cannot write to empty cell list");
            return;
        }

        LOGGER.log(Level.INFO, "Writing to spreadsheet {0} with {1} cells with value: {2}",
                new Object[]{spreadsheetId, cells.size(), value});

        List<ValueRange> data = cells.stream()
                .map(cell -> new ValueRange()
                        .setRange(cell)
                        .setValues(Collections.singletonList(Collections.singletonList(value))))
                .collect(Collectors.toList());

        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption("USER_ENTERED")
                .setData(data);

        sheetsService.spreadsheets().values()
                .batchUpdate(spreadsheetId, body)
                .execute();
    }

    /**
     * Writes different values to multiple cells in a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param cells The cell references in A1 notation
     * @param values The values to write to each cell
     * @throws IOException If an I/O error occurs
     */
    public void writeToSpreadsheetCells(String spreadsheetId, List<String> cells, List<String> values) throws IOException {
        if (cells == null || cells.isEmpty()) {
            LOGGER.warning("Cannot write to empty cell list");
            return;
        }

        if (cells.size() != values.size()) {
            LOGGER.warning("Cells and values lists must be the same size");
            return;
        }

        LOGGER.log(Level.INFO, "Writing to spreadsheet {0} with {1} cell-value pairs",
                new Object[]{spreadsheetId, cells.size()});

        List<ValueRange> data = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            data.add(new ValueRange()
                    .setRange(cells.get(i))
                    .setValues(Collections.singletonList(Collections.singletonList(values.get(i))))
            );
        }

        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption("USER_ENTERED")
                .setData(data);

        sheetsService.spreadsheets().values()
                .batchUpdate(spreadsheetId, body)
                .execute();
    }
}
