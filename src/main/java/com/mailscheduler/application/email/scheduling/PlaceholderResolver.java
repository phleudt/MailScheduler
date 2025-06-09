package com.mailscheduler.application.email.scheduling;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.template.placeholder.PlaceholderManager;
import com.mailscheduler.domain.repository.ContactRepository;
import com.mailscheduler.domain.repository.RecipientRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlaceholderResolver {
    private static final Logger LOGGER = Logger.getLogger(PlaceholderResolver.class.getName());
    private static final String DEFAULT_SHEET_TITLE = "Tabellenblatt1";

    private final String spreadsheetId;
    private final SpreadsheetGateway spreadsheetGateway;
    private final ContactRepository contactRepository;
    private final RecipientRepository recipientRepository;

    public PlaceholderResolver(
            SpreadsheetGateway spreadsheetGateway,
            ContactRepository contactRepository,
            RecipientRepository recipientRepository,
            String spreadsheetId
    ) {
        this.spreadsheetGateway = spreadsheetGateway;
        this.contactRepository = contactRepository;
        this.recipientRepository = recipientRepository;
        this.spreadsheetId = spreadsheetId;
    }

    public Body resolveTemplatePlaceholders(
            Body body,
            PlaceholderManager manager,
            Recipient recipient
    ) throws TemplateResolutionException {
        try {
            // Validate inputs
            validateInputs(body, manager, recipient);

            // Find row for recipient
            var recipientDataOpt = recipientRepository.findByIdWithMetadata(recipient.getId());
            if (recipientDataOpt.isEmpty()) {
                throw new TemplateResolutionException("Recipient not found: " + recipient.getId());
            }

            var contactOpt = contactRepository.findByIdWithMetadata(recipientDataOpt.get().metadata().contactId());
            if (contactOpt.isEmpty()) {
                throw new TemplateResolutionException("Contact not found for recipient: " + recipient.getId());
            }

            int row = contactOpt.get().entity().getSpreadsheetRow().extractRowNumber();

            // Get cells to retrieve from spreadsheet
            List<SpreadsheetReference> cellReferences = buildCellReferences(manager, row);
            if (cellReferences.isEmpty()) {
                // No placeholders to resolve
                return body;
            }

            // Get values from spreadsheet
            Map<String, String> placeholderValues = fetchPlaceholderValues(manager, cellReferences);

            // Replace placeholders in body
            return replacePlaceholders(body, placeholderValues);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error resolving placeholders", e);
            throw new TemplateResolutionException("Failed to resolve placeholders: " + e.getMessage(), e);
        }
    }

    private void validateInputs(Body body, PlaceholderManager manager, Recipient recipient)
            throws TemplateResolutionException {
        if (body == null) {
            throw new TemplateResolutionException("Template body cannot be null");
        }
        if (recipient == null || recipient.getId() == null) {
            throw new TemplateResolutionException("Recipient or recipient ID cannot be null");
        }
        // Manager can be null if no placeholders need resolving
    }

    private List<SpreadsheetReference> buildCellReferences(PlaceholderManager manager, int row) {
        List<SpreadsheetReference> cellReferences = new ArrayList<>();

        if (manager == null || manager.getPlaceholders() == null) {
            return cellReferences;
        }

        for (Map.Entry<String, SpreadsheetReference> entry : manager.getPlaceholders().entrySet()) {
            SpreadsheetReference column = entry.getValue();

            // Combine column letter with row number to create a cell reference
            SpreadsheetReference cell = SpreadsheetReference.ofCell(DEFAULT_SHEET_TITLE, column.getReference() + row);
            cellReferences.add(cell);
        }

        return cellReferences;
    }

    private Map<String, String> fetchPlaceholderValues(
            PlaceholderManager manager,
            List<SpreadsheetReference> cellReferences) throws IOException {

        Map<String, String> placeholderValues = new HashMap<>();

        if (cellReferences.isEmpty()) {
            return placeholderValues;
        }

        // Fetch values from spreadsheet
        List<ValueRange> values = spreadsheetGateway.readDataBatch(spreadsheetId, cellReferences);

        // Map placeholder keys to values
        for (int i = 0; i < values.size(); i++) {
            ValueRange valueRange = values.get(i);
            SpreadsheetReference cellRef = cellReferences.get(i);

            // Get column letter from cell reference
            String columnLetter = cellRef.getReference().replaceAll("\\d+", "");

            // Find the placeholder key for this column
            String placeholderKey = findPlaceholderKeyForColumn(manager, columnLetter);
            if (placeholderKey == null) {
                continue;
            }

            // Extract value from ValueRange
            String cellValue = extractCellValue(valueRange);
            placeholderValues.put(placeholderKey, cellValue);
        }

        return placeholderValues;
    }

    private String findPlaceholderKeyForColumn(PlaceholderManager manager, String columnLetter) {
        for (Map.Entry<String, SpreadsheetReference> entry : manager.getPlaceholders().entrySet()) {
            if (entry.getValue().getReference().equals(columnLetter)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String extractCellValue(ValueRange valueRange) {
        List<List<Object>> valuesList = valueRange.getValues();
        if (valuesList == null || valuesList.isEmpty() || valuesList.get(0).isEmpty()) {
            return "";
        }

        Object cellValue = valuesList.get(0).get(0);
        return cellValue != null ? cellValue.toString() : "";
    }

    private Body replacePlaceholders(Body body, Map<String, String> placeholderValues) {
        String bodyText = body.value();

        for (Map.Entry<String, String> entry : placeholderValues.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String replacement = entry.getValue();
            bodyText = bodyText.replace(placeholder, replacement);
        }

        return Body.of(bodyText);
    }
}
