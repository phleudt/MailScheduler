package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.common.exception.SpreadsheetOperationException;
import com.mailscheduler.domain.template.PlaceholderManager;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.application.spreadsheet.SpreadsheetService;
import com.mailscheduler.common.exception.PlaceholderException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlaceholderResolver {
    private final SpreadsheetService spreadsheetService;

    public PlaceholderResolver(SpreadsheetService spreadsheetService) {
        this.spreadsheetService = spreadsheetService;
    }

    public PlaceholderManager resolvePlaceholders(
            PlaceholderManager manager,
            SpreadsheetReference row
    ) throws PlaceholderException {
        PlaceholderManager updatedPlaceholderManager = new PlaceholderManager();

        List<SpreadsheetReference> cellsToRetrieveValuesFrom = new ArrayList<>();
        for (Map.Entry<String, PlaceholderManager.PlaceholderValue> entry : manager.getAllPlaceholders().entrySet()) {
            PlaceholderManager.PlaceholderValue value = entry.getValue();
            PlaceholderManager.ValueType type = value.type();
            switch (type) {
                case STRING -> updatedPlaceholderManager.addStringPlaceholder(entry.getKey(), value.getStringValue());
                case SPREADSHEET_REFERENCE -> {
                    SpreadsheetReference column = value.getSpreadsheetReference();

                    // Combine column with row to create a cell
                    SpreadsheetReference cell = SpreadsheetReference.ofCell(column.getReference() + row.getReference());
                    cellsToRetrieveValuesFrom.add(cell);
                }
            }
        }
        // Retrieve data from cells and add them to the updated placeholder manager
        List<String> values;
        try {
            if (!cellsToRetrieveValuesFrom.isEmpty()) {
                values = spreadsheetService.readSpreadsheetBatch(cellsToRetrieveValuesFrom);
                int index = 0;
                for (Map.Entry<String, PlaceholderManager.PlaceholderValue> entry : manager.getAllPlaceholders().entrySet()) {
                    if (entry.getValue().type() == PlaceholderManager.ValueType.SPREADSHEET_REFERENCE) {
                        if (values.get(index).isEmpty()) throw new PlaceholderException("Cell value is empty");
                        updatedPlaceholderManager.addStringPlaceholder(entry.getKey(), values.get(index));
                        index++;
                    }
                }
            }
        } catch (SpreadsheetOperationException e) {
            throw new PlaceholderException("Failed to retrieve values from spreadsheet: " + e.getMessage());
        }

        return updatedPlaceholderManager;
    }
}