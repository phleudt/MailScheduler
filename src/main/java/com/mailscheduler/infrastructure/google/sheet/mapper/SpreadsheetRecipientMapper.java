package com.mailscheduler.infrastructure.google.sheet.mapper;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.application.synchronization.spreadsheet.RecipientSpreadsheetEntry;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.util.TimeUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for converting spreadsheet data to Recipient domain objects.
 * This mapper handles extraction of recipient information including email addresses,
 * salutations, reply status, and contact dates.
 */
public class SpreadsheetRecipientMapper extends AbstractSpreadsheetMapper<RecipientSpreadsheetEntry> {
    // Column indices for Recipient attributes
    private static final int EMAIL_INDEX = 3;
    private static final int SALUTATION_INDEX = 4;
    private static final int HAS_REPLIED_INDEX = 5;
    private static final int INITIAL_CONTACT_DATE_INDEX = 6;
    private static final String POSITIVE_REPLY_VALUE = "ja";

    /**
     * Maps spreadsheet data to a list of RecipientSpreadsheetEntry objects.
     *
     * @param valueRanges The value ranges from the spreadsheet
     * @return A list of RecipientSpreadsheetEntry objects
     */
    public List<RecipientSpreadsheetEntry> buildRecipientsFromColumns(List<ValueRange> valueRanges) {
        logger.info("Building recipients from spreadsheet data");
        return mapFromValueRanges(valueRanges, null);
    }

    @Override
    protected List<RecipientSpreadsheetEntry> mapRowToEntities(List<String> rowValues, int rowNumber, String sheetTitle) {
        RecipientSpreadsheetEntry entry = createRecipientEntryFromRow(rowValues, rowNumber);
        return entry != null ? List.of(entry) : Collections.emptyList();
    }

    private RecipientSpreadsheetEntry createRecipientEntryFromRow(List<String> rowValues, int rowNumber) {
        // Your existing logic
        String emailsString = getValueOrNull(rowValues, EMAIL_INDEX);
        if (emailsString == null || emailsString.isEmpty()) {
            return null;
        }

        try {
            List<Recipient> recipients = parseEmailsToRecipients(
                    emailsString,
                    getValueOrNull(rowValues, SALUTATION_INDEX),
                    getValueOrNull(rowValues, HAS_REPLIED_INDEX),
                    getValueOrNull(rowValues, INITIAL_CONTACT_DATE_INDEX)
            );

            if (recipients.isEmpty()) {
                logger.info("No valid recipients found in row " + rowNumber);
                return null;
            }

            return new RecipientSpreadsheetEntry(recipients, rowNumber);
        } catch (Exception e) {
            logger.warning("Failed to parse recipients from row " + rowNumber + ": " + e.getMessage());
            return null;
        }
    }
    /**
     * Parses a comma-separated string of email addresses into a list of Recipient objects.
     *
     * @param emailsString Comma-separated string of email addresses
     * @param salutation Salutation for the recipients
     * @param hasRepliedValue String indicating if recipients have replied
     * @param initialContactDate Initial contact date as string
     * @return List of Recipient objects
     */
    private List<Recipient> parseEmailsToRecipients(
            String emailsString,
            String salutation,
            String hasRepliedValue,
            String initialContactDate) {

        if (emailsString == null || emailsString.isEmpty()) {
            return new ArrayList<>();
        }

        boolean hasReplied = POSITIVE_REPLY_VALUE.equalsIgnoreCase(hasRepliedValue);
        LocalDate contactDate = TimeUtils.parseDateToLocalDate(initialContactDate);

        // Parse email addresses
        List<String> emailAddresses = parseCommaDelimitedValues(emailsString);
        if (emailAddresses.isEmpty()) {
            return Collections.emptyList();
        }

        // Create recipients based on available data
        List<Recipient> recipients = new ArrayList<>();

        // One salutation for all emails
        for (String emailAddress : emailAddresses) {
            Recipient recipient = createRecipient(emailAddress, salutation, hasReplied, contactDate);

            if (recipient != null) {
                recipients.add(recipient);
            }
        }

        return recipients;
    }

    /**
     * Parses a comma-delimited string into a list of trimmed values.
     *
     * @param input The comma-delimited string
     * @return List of trimmed values
     */
    private List<String> parseCommaDelimitedValues(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Creates a single Recipient object from the provided attributes.
     *
     * @param email Email address string
     * @param salutation Salutation string
     * @param hasReplied Boolean indicating if the recipient has replied
     * @param initialContactDate Initial contact date string
     * @return A new Recipient object or null if invalid
     */
    private Recipient createRecipient(
            String email,
            String salutation,
            boolean hasReplied,
            LocalDate initialContactDate
    ) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        try {
            return new Recipient.Builder()
                    .setEmailAddress(EmailAddress.of(email))
                    .setSalutation(salutation)
                    .setHasReplied(hasReplied)
                    .setInitialContactDate(initialContactDate)
                    .build();
        } catch (Exception e) {
            logger.warning("Failed to create recipient with email " + email + ": " + e.getMessage());
            return null;
        }
    }
}
