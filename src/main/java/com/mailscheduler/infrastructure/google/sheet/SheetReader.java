package com.mailscheduler.infrastructure.google.sheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for reading data from Google Sheets.
 */
public class SheetReader {
    private static final Logger LOGGER = Logger.getLogger(SheetReader.class.getName());
    private final Sheets sheetsService;

    public SheetReader(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    /**
     * Reads data from a specific range in a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param range The range to read
     * @return List of rows, each containing a list of cell values
     * @throws IOException If an I/O error occurs
     */
    public List<List<Object>> readSpreadsheet(String spreadsheetId, String range) throws IOException {
        LOGGER.log(Level.INFO, "Reading spreadsheet {0} with range: {1}", new Object[]{spreadsheetId, range});
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            LOGGER.info("No data found in specified range");
        }

        return values;
    }

    /**
     * Reads multiple ranges from a spreadsheet in a single batch request.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param ranges List of A1 notation ranges to read
     * @return List of ValueRange objects containing the data for each range
     * @throws IOException If an I/O error occurs
     */
    public List<ValueRange> readSpreadsheetBatch(String spreadsheetId, List<String> ranges) throws IOException {
        LOGGER.log(Level.INFO, "Reading spreadsheet {0} with {1} ranges", new Object[]{spreadsheetId, ranges.size()});
        return sheetsService.spreadsheets().values()
                .batchGet(spreadsheetId)
                .setRanges(ranges)
                .execute()
                .getValueRanges();
    }
}
