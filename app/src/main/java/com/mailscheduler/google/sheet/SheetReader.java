package com.mailscheduler.google.sheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class SheetReader {
    private static final Logger LOGGER = Logger.getLogger(SheetReader.class.getName());
    private final Sheets sheetsService;

    public SheetReader(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    public List<List<Object>> readSpreadsheet(String spreadsheetId, String range) throws IOException {
        LOGGER.info("Reading spreadsheet with range: " + range);
        return sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
                .getValues();
    }

    public List<ValueRange> readSpreadsheetBatch(String spreadsheetId, List<String> ranges) throws IOException {
        LOGGER.info("Reading spreadsheet with ranges: " + ranges);
        return sheetsService.spreadsheets().values()
                .batchGet(spreadsheetId)
                .setRanges(ranges)
                .execute()
                .getValueRanges();
    }
}
