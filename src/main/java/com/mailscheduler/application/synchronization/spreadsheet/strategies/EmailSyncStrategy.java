package com.mailscheduler.application.synchronization.spreadsheet.strategies;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.application.service.ColumnMappingService;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.repository.EmailRepository;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.RecipientRepository;
import com.mailscheduler.infrastructure.google.sheet.mapper.SpreadsheetEmailMapper;

import java.util.*;
import java.util.logging.Level;

/**
 * Strategy for synchronizing email data between a spreadsheet and the local database.
 * Handles importing emails from Google Sheets and linking them to recipients and follow-up sequences.
 */
public class EmailSyncStrategy extends AbstractSpreadsheetSynchronizationStrategy {
    private final EmailRepository emailRepository;
    private final RecipientRepository recipientRepository;
    private final ColumnMappingService mappingService;
    private final SpreadsheetEmailMapper emailMapper;

    /**
     * Creates a new EmailSyncStrategy with the required dependencies.
     */
    public EmailSyncStrategy(
            SpreadsheetGateway spreadsheetGateway,
            EmailRepository emailRepository,
            RecipientRepository recipientRepository,
            ColumnMappingService mappingService
    ) {
        super(spreadsheetGateway);
        this.emailRepository = Objects.requireNonNull(emailRepository, "EmailRepository cannot be null");
        this.recipientRepository = Objects.requireNonNull(recipientRepository, "RecipientRepository cannot be null");
        this.mappingService = Objects.requireNonNull(mappingService, "ColumnMappingService cannot be null");
        this.emailMapper = new SpreadsheetEmailMapper();
    }

    @Override
    public String getStrategyName() {
        return "Email Synchronization Strategy";
    }

    @Override
    protected void processSheet(String spreadsheetId, SheetConfiguration sheetConfiguration) {
        logger.info("Processing emails for sheet: " + sheetConfiguration.title());

        List<SpreadsheetReference> references = mappingService.getSpreadsheetReferences(
                ColumnMapping.MappingType.EMAIL,
                sheetConfiguration.title()
        );

        if (references.isEmpty()) {
            logger.warning("No column mappings found for EMAIL type. Skipping sheet: " +
                    sheetConfiguration.title());
            return;
        }

        List<ValueRange> valueRanges = fetchSpreadsheetData(spreadsheetId, references);

        if (valueRanges.isEmpty()) {
            logger.info("No email data found in sheet: " + sheetConfiguration.title());
            return;
        }

        // Map the spreadsheet data to domain objects
        List<EntityData<Email, EmailMetadata>> emails = emailMapper.buildEmailsFromColumns(valueRanges);
        if (emails.isEmpty()) {
            logger.info("No valid email data mapped from sheet: " + sheetConfiguration.title());
            return;
        }

        // Link emails to their recipients and save to repository
        linkEmailsToRecipients(emails);
    }

    @Override
    protected void doPostProcessing(SpreadsheetConfiguration configuration) {
        // After all sheets are processed, link follow-up emails
        linkFollowUpEmails();
    }

    /**
     * Links emails to their corresponding recipients based on email addresses.
     *
     * @param emails The list of email data to process
     */
    private void linkEmailsToRecipients(List<EntityData<Email, EmailMetadata>> emails) {
        // Create a mapping from email address to recipient ID
        Map<EmailAddress, Long> emailAddressToRecipientIdMap = createRecipientEmailAddressMap();

        int linkedCount = 0;
        int skippedCount = 0;

        // Link emails to recipients and save to repository
        for (EntityData<Email, EmailMetadata> emailData : emails) {
            Email email = emailData.entity();
            EmailMetadata metadata = emailData.metadata();
            int curFollowupNumber = metadata.followupNumber();

            // Find the recipient ID for this email's recipient address
            Long recipientId = emailAddressToRecipientIdMap.get(email.getRecipient());
            if (recipientId == null) {
                logger.warning("No recipient found for email address: " + email.getRecipient());
                skippedCount++;
                continue;
            }

            // If the recipient already has linked emails, skip linking
            var existingEmails = emailRepository.findByRecipientId(EntityId.of(recipientId));
            if (curFollowupNumber < existingEmails.size()) {
                logger.info(String.format(
                        "Skipping email %s for recipient %s because it is not the next follow-up email",
                        email.getId(), recipientId));
                skippedCount++;
                continue;
            }


            // Update the metadata with the recipient ID
            EmailMetadata updatedMetadata = new EmailMetadata.Builder()
                    .from(metadata)
                    .recipientId(EntityId.of(recipientId))
                    .build();

            // Save the email with updated metadata
            emailRepository.saveWithMetadata(email, updatedMetadata);
            linkedCount++;
        }

        logger.info(String.format("Linked %d emails to recipients (skipped %d)", linkedCount, skippedCount));
    }

    /**
     * Creates a mapping from recipient email addresses to their entity IDs.
     *
     * @return Map of email addresses to recipient IDs
     */
    private Map<EmailAddress, Long> createRecipientEmailAddressMap() {
        List<EntityData<Recipient, RecipientMetadata>> recipients = recipientRepository.findAllWithMetadata();
        Map<EmailAddress, Long> emailAddressToIdMap = new HashMap<>();

        for (EntityData<Recipient, RecipientMetadata> recipientData : recipients) {
            Recipient recipient = recipientData.entity();
            if (recipient != null && recipient.getEmailAddress() != null) {
                emailAddressToIdMap.put(
                        recipient.getEmailAddress(),
                        recipient.getId().value()
                );
            }
        }

        return emailAddressToIdMap;
    }

    /**
     * Links follow-up emails to their initial emails based on recipient email address.
     * This creates the connection between initial emails and their follow-ups.
     */
    private void linkFollowUpEmails() {
        List<EntityData<Email, EmailMetadata>> allEmails = emailRepository.findAllWithMetadata();
        if (allEmails.isEmpty()) {
            logger.info("No emails found for follow-up linking");
            return;
        }

        // First pass: Process initial emails and create mapping
        Map<EntityId<Recipient>, EntityId<Email>> recipientIdToInitialEmailMap = mapInitialEmails(allEmails);

        // Second pass: Process follow-up emails
        linkFollowUpEmailsToInitial(allEmails, recipientIdToInitialEmailMap);
    }

    /**
     * Creates a mapping from recipient email addresses to their initial email IDs.
     * Also updates the initial emails to reference themselves.
     *
     * @param allEmails List of all emails with metadata
     * @return Map of recipient email addresses to initial email IDs
     */
    private Map<EntityId<Recipient>, EntityId<Email>> mapInitialEmails(List<EntityData<Email, EmailMetadata>> allEmails) {
        Map<EntityId<Recipient>, EntityId<Email>> recipientToInitialEmailMap = new HashMap<>();
        int initialEmailCount = 0;

        for (EntityData<Email, EmailMetadata> emailData : allEmails) {
            Email email = emailData.entity();
            EmailMetadata metadata = emailData.metadata();

            if (email == null || metadata == null) {
                continue;
            }

            if (EmailType.EXTERNALLY_INITIAL.equals(email.getType())) {
                // Update the metadata to set the initialEmailId to itself
                EmailMetadata updatedMetadata = new EmailMetadata.Builder()
                        .from(metadata)
                        .initialEmailId(email.getId())
                        .build();

                emailRepository.saveWithMetadata(email, updatedMetadata);

                // Add to map for follow-up processing
                recipientToInitialEmailMap.put(
                        metadata.recipientId(),
                        email.getId()
                );
                initialEmailCount++;
            }
        }

        logger.info("Processed " + initialEmailCount + " initial emails");
        return recipientToInitialEmailMap;
    }

    /**
     * Links follow-up emails to their corresponding initial emails.
     *
     * @param allEmails List of all emails with metadata
     * @param recipientToInitialEmailMap Map of recipient email addresses to initial email IDs
     */
    private void linkFollowUpEmailsToInitial(
            List<EntityData<Email, EmailMetadata>> allEmails,
            Map<EntityId<Recipient>, EntityId<Email>> recipientToInitialEmailMap) {
        int followUpCount = 0;

        for (EntityData<Email, EmailMetadata> emailData : allEmails) {
            Email email = emailData.entity();
            EmailMetadata metadata = emailData.metadata();

            if (email == null || metadata == null) {
                continue;
            }

            // Only process follow-up emails (not initial emails)
            if (EmailType.EXTERNALLY_FOLLOW_UP.equals(email.getType())) {
                EntityId<Email> initialEmailId = recipientToInitialEmailMap.get(metadata.recipientId());

                if (initialEmailId == null) {
                    logger.warning("No initial email found for follow-up to: " + email.getRecipient());
                    continue;
                }

                EmailMetadata updatedMetadata = new EmailMetadata.Builder()
                        .from(metadata)
                        .initialEmailId(initialEmailId)
                        .build();

                emailRepository.saveWithMetadata(email, updatedMetadata);
                followUpCount++;
            }
        }

        logger.info("Linked " + followUpCount + " follow-up emails");
    }

    @Override
    protected void handleSynchronizationError(Exception exception) {
        logger.log(Level.SEVERE, "Email synchronization failed", exception);
        // Additional recovery logic could be added here
    }
}
