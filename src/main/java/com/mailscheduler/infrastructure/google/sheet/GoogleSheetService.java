package com.mailscheduler.infrastructure.google.sheet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.infrastructure.google.auth.GoogleAuthService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for interacting with Google Sheets API.
 * Provides methods for reading, writing, and searching data in Google Sheets.
 */
public class GoogleSheetService extends GoogleAuthService<Sheets> {
    private final Logger LOGGER = Logger.getLogger(GoogleSheetService.class.getName());

    private Sheets sheetsService;
    private static volatile GoogleSheetService instance;

    private final SheetReader sheetReader;
    private final SheetWriter sheetWriter;
    private final SheetSearcher sheetSearcher;

    /**
     * Private constructor for singleton pattern.
     * Initializes the service and its dependencies.
     *
     * @throws Exception If initialization fails
     */
    private GoogleSheetService() throws Exception {
        super();
        if (instance != null) {
            throw new IllegalStateException("GoogleSheetService instance already exists");
        }
        this.sheetReader = new SheetReader(sheetsService);
        this.sheetWriter = new SheetWriter(sheetsService);
        this.sheetSearcher = new SheetSearcher(sheetReader);
    }

    /**
     * Gets the singleton instance of GoogleSheetService.
     *
     * @return The GoogleSheetService instance
     * @throws Exception If initialization fails
     */
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

    /**
     * {@inheritDoc}
     * Returns the required OAuth2 scopes for Google Sheets API.
     */
    @Override
    protected List<String> getScopes() {
        return Collections.singletonList(SheetsScopes.SPREADSHEETS);
    }

    /**
     * {@inheritDoc}
     * Initializes the Sheets service with the provided credential.
     */
    @Override
    protected void initializeService(Credential credential) {
        this.sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Validates whether a spreadsheet ID is valid and accessible.
     *
     * @param spreadsheetId The ID of the spreadsheet to validate
     * @return true if valid, false otherwise
     */
    public boolean validateSpreadsheetID(String spreadsheetId) {
        try {
            sheetsService.spreadsheets().get(spreadsheetId).execute();
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Spreadsheet validation failed: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets a spreadsheet by its ID.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @return The Spreadsheet object
     * @throws IOException If an I/O error occurs
     */
    public Spreadsheet getSpreadsheet(String spreadsheetId) throws IOException {
        return sheetsService.spreadsheets().get(spreadsheetId).execute();
    }

    /**
     * Reads data from a spreadsheet range.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param range The range to read
     * @return List of rows, each containing a list of cell values
     * @throws IOException If an I/O error occurs
     */
    public List<List<Object>> readSpreadsheet(String spreadsheetId, SpreadsheetReference range) throws IOException {
        return sheetReader.readSpreadsheet(spreadsheetId, range.getGoogleSheetsReference());
    }

    /**
     * Reads multiple ranges from a spreadsheet in a single batch request.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param ranges The ranges to read
     * @return List of ValueRange objects containing the data
     * @throws IOException If an I/O error occurs
     */
    public List<ValueRange> readSpreadsheetBatch(String spreadsheetId, List<SpreadsheetReference> ranges) throws IOException {
        List<String> rangeStrings = ranges.stream()
                .map(SpreadsheetReference::getGoogleSheetsReference)
                .toList();
        return sheetReader.readSpreadsheetBatch(spreadsheetId, rangeStrings);
    }

    /**
     * Writes a value to a single cell in a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param cell The cell reference
     * @param value The value to write
     * @throws IOException If an I/O error occurs
     */
    public void writeToSpreadsheetCell(String spreadsheetId, SpreadsheetReference cell, String value) throws IOException {
        sheetWriter.writeToSpreadsheetCell(spreadsheetId, cell.getGoogleSheetsReference(), value);
    }

    /**
     * Writes the same value to multiple cells in a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param cells The cell references
     * @param value The value to write to all cells
     * @throws IOException If an I/O error occurs
     */
    public void writeToSpreadsheetCells(String spreadsheetId, List<SpreadsheetReference> cells, String value) throws IOException {
        List<String> cellStrings = cells.stream()
                .map(SpreadsheetReference::getGoogleSheetsReference)
                .toList();
        sheetWriter.writeToSpreadsheetCells(spreadsheetId, cellStrings, value);
    }

    /**
     * Writes different values to multiple cells in a spreadsheet (cells list size must equal values list size).
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param cells The cell references
     * @param values The values to write to each cell
     * @throws IOException If an I/O error occurs
     */
    public void writeToSpreadsheetCells(String spreadsheetId, List<SpreadsheetReference> cells, List<String> values) throws IOException {
        if (cells.isEmpty()) {
            LOGGER.info("No cells specified for writing, operation skipped");
            return;
        }

        if (cells.size() != values.size()) {
            LOGGER.warning("Cells and values count mismatch. Expected equal counts, operation aborted.");
            return;
        }

        LOGGER.info("Writing to multiple spreadsheet cells: " + cells + " with values: " + values);

        List<String> cellStrings = cells.stream()
                .map(SpreadsheetReference::getGoogleSheetsReference)
                .toList();
        sheetWriter.writeToSpreadsheetCells(spreadsheetId, cellStrings, values);
    }

    /**
     * Writes multiple rows of data to a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param rows The row references
     * @param values The values to write to each row
     * @throws IOException If an I/O error occurs
     */
    public void writeToSpreadsheetRows(String spreadsheetId, List<SpreadsheetReference> rows, List<List<Object>> values) throws IOException {
        if (rows.isEmpty() || values.isEmpty() || rows.size() != values.size()) {
            LOGGER.warning("Invalid rows or values provided for batch write");
            return;
        }

        LOGGER.info("Writing to " + rows.size() + " rows in spreadsheet");

        List<ValueRange> data = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            data.add(new ValueRange()
                    .setRange(rows.get(i).getGoogleSheetsReference())
                    .setValues(Collections.singletonList(values.get(i))));
        }

        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption("USER_ENTERED")
                .setData(data);

        sheetsService.spreadsheets().values()
                .batchUpdate(spreadsheetId, body)
                .execute();
    }

    /**
     * Finds a cell index in a column that contains the specified expression.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param column The column to search in
     * @param expression The expression to find
     * @return The cell reference if found, null otherwise
     * @throws IOException If an I/O error occurs
     */
    public SpreadsheetReference getSpreadsheetCellIndex(String spreadsheetId, SpreadsheetReference column, String expression) throws IOException {
        String cellIndex = sheetSearcher.findCellIndex(spreadsheetId, column, expression);
        return cellIndex != null ? SpreadsheetReference.ofCell(cellIndex) : null;
    }

    /**
     * Finds multiple cell indices in a column that contain the specified expressions.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param column The column to search in
     * @param expressions The expressions to find
     * @return List of cell references that match the expressions
     * @throws IOException If an I/O error occurs
     */
    public List<SpreadsheetReference> getSpreadsheetCellIndices(String spreadsheetId, SpreadsheetReference column, List<String> expressions) throws IOException {
        List<String> stringCellIndices = sheetSearcher.findCellIndices(spreadsheetId, column, expressions);
        List<SpreadsheetReference> cellIndices = new ArrayList<>(stringCellIndices.size());
        for (String stringCellIndex : stringCellIndices) {
            cellIndices.add(SpreadsheetReference.ofCell(stringCellIndex));
        }
        return cellIndices;
    }

    /**
     * Creates a new sheet in a spreadsheet.
     *
     * @param spreadsheetId The ID of the spreadsheet
     * @param title The title of the new sheet
     * @param index The optional position index for the new sheet (null for default positioning)
     * @throws IOException If an I/O error occurs
     */
    public void createSheet(String spreadsheetId, String title, Integer index) throws IOException {
        SheetProperties properties = new SheetProperties()
                .setTitle(title);

        if (index != null) {
            properties.setIndex(index);
        }

        AddSheetRequest addSheetRequest = new AddSheetRequest()
                .setProperties(properties);

        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest()
                .setRequests(Collections.singletonList(
                        new Request().setAddSheet(addSheetRequest)
                ));

        sheetsService.spreadsheets().batchUpdate(spreadsheetId, request).execute();
        LOGGER.info("Created new sheet '" + title + "' in spreadsheet: " + spreadsheetId);
    }
}
