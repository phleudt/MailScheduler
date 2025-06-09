package com.mailscheduler.infrastructure.google.sheet.mapper;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailStatus;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.util.TimeUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper for converting spreadsheet data to Email domain objects with metadata.
 * This mapper handles extraction of email addresses, scheduling dates,
 * and status information from spreadsheet rows.
 */
public class SpreadsheetEmailMapper extends AbstractSpreadsheetMapper<EntityData<Email, EmailMetadata>> {
    // Column indices for Email attributes
    private static final int EMAIL_ADDRESS_INDEX = 0;
    private static final int INITIAL_CONTACT_DATE_INDEX = 1;
    private static final int MAX_FOLLOWUP_PAIRS = 8; // Maximum number of follow-up date/status pairs

    // Status string constants
    private static final String STATUS_PENDING = "Offen";
    private static final String STATUS_SENT = "Gesendet";
    private static final String STATUS_CANCELLED = "Nicht erforderlich";
    private static final String STATUS_FAILED = "Failed";

    /**
     * Maps spreadsheet data to a list of Email domain objects with their metadata.
     *
     * @param valueRanges The value ranges from the spreadsheet
     * @return A list of Email objects with metadata
     */
    public List<EntityData<Email, EmailMetadata>> buildEmailsFromColumns(List<ValueRange> valueRanges) {
        logger.info("Building emails from spreadsheet data");
        return mapFromValueRanges(valueRanges, null);
    }

    @Override
    protected List<EntityData<Email, EmailMetadata>> mapRowToEntities(List<String> rowValues, int rowNumber, String sheetTitle) {
        String emailsString = getValueOrNull(rowValues, EMAIL_ADDRESS_INDEX);
        String initialContactDateStr = getValueOrNull(rowValues, INITIAL_CONTACT_DATE_INDEX);

        // Skip if no email or initial contact date
        if (emailsString == null || initialContactDateStr == null) {
            return Collections.emptyList();
        }

        try {
            // Parse email addresses
            List<EmailAddress> recipientEmails = parseEmailAddresses(emailsString);
            if (recipientEmails.isEmpty()) {
                return Collections.emptyList();
            }

            // Convert data to emails with metadata
            LocalDate initialContactDate = TimeUtils.parseDateToLocalDate(initialContactDateStr);
            if (initialContactDate == null) {
                logger.warning("Invalid initial contact date in row " + rowNumber + ": " + initialContactDateStr);
                return Collections.emptyList();
            }

            List<EntityData<Email, EmailMetadata>> allEmails = new ArrayList<>();

            // Create emails for each recipient
            for (EmailAddress recipientEmail : recipientEmails) {
                List<EntityData<Email, EmailMetadata>> emailsForRecipient =
                        createEmailsForRecipient(rowValues, recipientEmail, initialContactDate, rowNumber);
                allEmails.addAll(emailsForRecipient);
            }

            return allEmails;
        } catch (Exception e) {
            logger.warning("Failed to parse emails from row " + rowNumber + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parses a comma-separated string of email addresses into a list of EmailAddress objects.
     *
     * @param emailAddressesString Comma-separated string of email addresses
     * @return List of EmailAddress objects
     */
    private List<EmailAddress> parseEmailAddresses(String emailAddressesString) {
        if (emailAddressesString == null || emailAddressesString.isEmpty()) {
            return Collections.emptyList();
        }

        String[] emailStrings = emailAddressesString.split(",");
        return Arrays.stream(emailStrings)
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> {
                    try {
                        return new EmailAddress(email);
                    } catch (Exception e) {
                        logger.warning("Invalid email address: " + email);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Creates a list of Email objects with metadata for a single recipient.
     *
     * @param rowValues The row values from the spreadsheet
     * @param recipientEmail The recipient's email address
     * @param initialContactDate The initial contact date
     * @return A list of Email objects with metadata
     */
    private List<EntityData<Email, EmailMetadata>> createEmailsForRecipient(
            List<String> rowValues,
            EmailAddress recipientEmail,
            LocalDate initialContactDate,
            int rowNumber
    ) {

        List<EntityData<Email, EmailMetadata>> emails = new ArrayList<>();

        // Create initial email
        Email initialEmail = createEmail(recipientEmail, EmailType.EXTERNALLY_INITIAL);
        EmailMetadata initialMetadata = new EmailMetadata.Builder()
                .followupNumber(0)
                .status(EmailStatus.SENT)
                .scheduledDate(initialContactDate)
                .sentDate(initialContactDate)
                .build();

        emails.add(EntityData.of(initialEmail, initialMetadata));
        logger.fine("Created initial email for " + recipientEmail + " on row " + rowNumber);

        // Create follow-up emails
        for (int followUpNum = 1; followUpNum <= MAX_FOLLOWUP_PAIRS; followUpNum++) {
            int dateIndex = 2 * followUpNum;
            int statusIndex = dateIndex + 1;

            // Stop if we've reached the end of the data
            if (dateIndex >= rowValues.size() || getValueOrNull(rowValues, dateIndex) == null) {
                break;
            }

            LocalDate followUpDate = TimeUtils.parseDateToLocalDate(getValueOrNull(rowValues, dateIndex));
            if (followUpDate == null) {
                continue;
            }

            EmailStatus status = extractEmailStatus(getValueOrNull(rowValues, statusIndex));

            Email followUpEmail = createEmail(recipientEmail, EmailType.EXTERNALLY_FOLLOW_UP);
            EmailMetadata followUpMetadata = new EmailMetadata.Builder()
                    .followupNumber(followUpNum)
                    .status(status != null ? status : EmailStatus.PENDING)
                    .scheduledDate(followUpDate)
                    .sentDate(followUpDate)
                    .build();

            emails.add(EntityData.of(followUpEmail, followUpMetadata));
            logger.fine("Created follow-up #" + followUpNum + " for " + recipientEmail + " on row " + rowNumber);
        }

        return emails;
    }

    /**
     * Creates an Email object with the basic properties set.
     *
     * @param recipientEmail The recipient's email address
     * @param emailType The type of email
     * @return A new Email object
     */
    private Email createEmail(EmailAddress recipientEmail, EmailType emailType) {
        return new Email.Builder()
                .setRecipientEmail(recipientEmail)
                .setSubject("") // Subject will be populated from template
                .setBody("") // Body will be populated from template
                .setType(emailType)
                .build();
    }

    /**
     * Extracts an EmailStatus from a status string.
     *
     * @param status The status string
     * @return The corresponding EmailStatus or null if not recognized
     */
    private EmailStatus extractEmailStatus(String status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case STATUS_PENDING -> EmailStatus.PENDING;
            case STATUS_SENT -> EmailStatus.SENT;
            case STATUS_CANCELLED -> EmailStatus.CANCELLED;
            case STATUS_FAILED -> EmailStatus.FAILED;
            default -> {
                logger.warning("Unknown email status: " + status + ", defaulting to FAILED");
                yield EmailStatus.FAILED;
            }
        };
    }
}
