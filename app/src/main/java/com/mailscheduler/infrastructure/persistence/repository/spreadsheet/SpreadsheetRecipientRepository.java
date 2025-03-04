package com.mailscheduler.infrastructure.persistence.repository.spreadsheet;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.common.exception.SpreadsheetOperationException;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.infrastructure.google.sheet.GoogleSheetService;
import com.mailscheduler.infrastructure.persistence.spreadsheet.mapper.SpreadsheetRecipientMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpreadsheetRecipientRepository {
    private final GoogleSheetService googleSheetService;
    private final String spreadsheetId;

    public SpreadsheetRecipientRepository(GoogleSheetService googleSheetService, String spreadsheetId) {
        this.googleSheetService = googleSheetService;
        this.spreadsheetId = spreadsheetId;
    }

    public List<Recipient> getRecipients(List<SpreadsheetReference> columns) throws SpreadsheetOperationException {
        if (columns == null || columns.isEmpty()) {
            return new ArrayList<>();
        }


        try {
            List<ValueRange> valueRanges = googleSheetService.readSpreadsheetBatch(spreadsheetId, columns);
            int maxRows = calculateMaxRowCount(valueRanges);
            return SpreadsheetRecipientMapper.buildRecipientsFromColumns(valueRanges, maxRows);
        } catch (IOException e) {
            throw new SpreadsheetOperationException("Failed to retrieve recipients", e);
        }
    }

    private int calculateMaxRowCount(List<ValueRange> valueRanges) {
        return valueRanges.stream()
                .mapToInt(valueRange -> valueRange.getValues().size())
                .max()
                .orElse(0);
    }
}
