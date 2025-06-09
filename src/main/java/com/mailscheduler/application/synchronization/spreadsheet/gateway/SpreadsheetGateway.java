package com.mailscheduler.application.synchronization.spreadsheet.gateway;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;

import java.io.IOException;
import java.util.List;

public interface SpreadsheetGateway {
    /**
     * Retrieves a spreadsheet by its ID.
     */
    Spreadsheet getSpreadsheet(String spreadsheetId) throws IOException;

    /**
     * Creates a new sheet in the specified spreadsheet.
     */
    void createSheet(String spreadsheetId, String title, Integer index) throws IOException;

    /**
     * Reads data from a specified range in the spreadsheet.
     */
    List<List<Object>> readData(String spreadsheetId, SpreadsheetReference range) throws IOException;

    /**
     * Reads data from multiple ranges in the spreadsheet.
     */
    List<ValueRange> readDataBatch(String spreadsheetId, List<SpreadsheetReference> ranges) throws IOException;

    /**
     * Writes data to specified rows in the spreadsheet.
     */
    void writeData(String spreadsheetId, List<SpreadsheetReference> rows, List<List<Object>> values) throws IOException;

    /**
     * Writes data to a specific cell in the spreadsheet.
     */
    void writeDataToCell(String spreadsheetId, SpreadsheetReference cell, String value) throws IOException;

    /**
     * Writes data to multiple cells in the spreadsheet.
     */
    void writeDataToCells(String spreadsheetId, List<SpreadsheetReference> cells, List<String> value) throws IOException;

    // void updateValues(String spreadsheetId, List<SpreadsheetReference> rows, List<List<Object>> values) throws IOException;

    /**
     * Clears values in a specified range of the spreadsheet.
     */
    void clearValues(String spreadsheetId, SpreadsheetReference range) throws IOException;

    /**
     * Formats cells in the spreadsheet with specified styles.
     */
    void formatCells(String spreadsheetId, SpreadsheetReference range,
                     Boolean bold, String backgroundColor, String textColor) throws IOException;

}