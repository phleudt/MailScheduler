package com.mailscheduler.infrastructure.google.sheet.mapper;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.domain.model.recipient.Contact;

import java.util.Collections;
import java.util.List;

/**
 * Maps spreadsheet data to Contact domain objects.
 * This mapper extracts contact information from spreadsheet rows.
 */
public class SpreadsheetContactMapper extends AbstractSpreadsheetMapper<Contact> {
    // Column indices for Contact attributes
    private static final int WEBSITE_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int PHONE_NUMBER_INDEX = 2;

    /**
     * Maps spreadsheet data to a list of Contact domain objects.
     *
     * @param valueRanges The value ranges from the spreadsheet
     * @param sheetTitle The title of the sheet
     * @return A list of Contact objects
     */
    public List<Contact> buildContactsFromColumns(List<ValueRange> valueRanges, String sheetTitle) {
        logger.info("Building contacts from spreadsheet data with sheet title: " + sheetTitle);
        return mapFromValueRanges(valueRanges, sheetTitle);
    }

    @Override
    protected List<Contact> mapRowToEntities(List<String> rowValues, int rowNumber, String sheetTitle) {
        Contact contact = createContactFromRow(rowValues, rowNumber, sheetTitle);
        return contact != null ? List.of(contact) : Collections.emptyList();
    }

    private Contact createContactFromRow(List<String> rowValues, int rowNumber, String sheetTitle) {
        // Your existing logic to create a contact
        String website = getValueOrNull(rowValues, WEBSITE_INDEX);
        String name = getValueOrNull(rowValues, NAME_INDEX);
        String phoneNumber = getValueOrNull(rowValues, PHONE_NUMBER_INDEX);

        // Skip if all main fields are empty
        if (isEmpty(website) && isEmpty(name) && isEmpty(phoneNumber)) {
            return null;
        }

        try {
            return new Contact.Builder()
                    .setSpreadsheetRow(rowNumber)
                    .setWebsite(website)
                    .setName(name)
                    .setPhoneNumber(phoneNumber)
                    .setSheetTitle(sheetTitle)
                    .build();
        } catch (Exception e) {
            logger.warning("Failed to build Contact from row " + rowNumber + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a string is null or empty.
     *
     * @param value The string to check
     * @return true if the string is null or empty, false otherwise
     */
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
