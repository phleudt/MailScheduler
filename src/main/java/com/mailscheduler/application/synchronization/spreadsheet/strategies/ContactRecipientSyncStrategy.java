package com.mailscheduler.application.synchronization.spreadsheet.strategies;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.application.service.ColumnMappingService;
import com.mailscheduler.application.synchronization.spreadsheet.RecipientSpreadsheetEntry;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.model.recipient.Contact;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.repository.ContactRepository;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.RecipientRepository;
import com.mailscheduler.infrastructure.google.sheet.mapper.SpreadsheetContactMapper;
import com.mailscheduler.infrastructure.google.sheet.mapper.SpreadsheetRecipientMapper;

import java.util.*;
import java.util.logging.Level;

/**
 * Strategy for synchronizing contact and recipient data between a spreadsheet and the local database.
 * Handles importing contacts, creating recipients, and establishing relationships between them.
 */
public class ContactRecipientSyncStrategy extends AbstractSpreadsheetSynchronizationStrategy {
    private static final long DEFAULT_FOLLOWUP_PLAN_ID = 1L;

    private final ContactRepository contactRepository;
    private final RecipientRepository recipientRepository;
    private final ColumnMappingService mappingService;
    private final SpreadsheetContactMapper contactMapper;
    private final SpreadsheetRecipientMapper recipientMapper;

    /**
     * Creates a new ContactRecipientSyncStrategy with the necessary dependencies.
     */
    public ContactRecipientSyncStrategy(
            SpreadsheetGateway spreadsheetGateway,
            ContactRepository contactRepository,
            RecipientRepository recipientRepository,
            ColumnMappingService mappingService
    ) {
        super(spreadsheetGateway);
        this.contactRepository = Objects.requireNonNull(contactRepository, "ContactRepository cannot be null");
        this.recipientRepository = Objects.requireNonNull(recipientRepository, "RecipientRepository cannot be null");
        this.mappingService = Objects.requireNonNull(mappingService, "ColumnMappingService cannot be null");
        this.contactMapper = new SpreadsheetContactMapper();
        this.recipientMapper = new SpreadsheetRecipientMapper();
    }

    @Override
    public String getStrategyName() {
        return "Contact and Recipient Synchronization Strategy";
    }

    @Override
    protected void processSheet(String spreadsheetId, SheetConfiguration sheetConfiguration) {
        logger.info("Processing contacts and recipients for sheet: " + sheetConfiguration.title());

        // Get references for both contact and recipient data using the column mapping service
        List<SpreadsheetReference> references = collectSpreadsheetReferences(sheetConfiguration.title());

        if (references.isEmpty()) {
            logger.warning("No column mappings found for contacts or recipients. Skipping sheet: " +
                    sheetConfiguration.title());
            return;
        }

        List<ValueRange> valueRanges = fetchSpreadsheetData(spreadsheetId, references);

        if (valueRanges.isEmpty()) {
            logger.info("No data found in sheet: " + sheetConfiguration.title());
            return;
        }

        // Process contacts first
        List<Contact> contacts = contactMapper.buildContactsFromColumns(valueRanges, sheetConfiguration.title());
        int contactCount = processContacts(contacts);
        logger.info("Processed " + contactCount + " contacts from sheet: " + sheetConfiguration.title());

        // Then process recipients
        List<RecipientSpreadsheetEntry> recipientEntries = recipientMapper.buildRecipientsFromColumns(valueRanges);
        int recipientCount = processRecipients(recipientEntries);
        logger.info("Processed " + recipientCount + " recipient entries from sheet: " + sheetConfiguration.title());
    }

    @Override
    protected void doPostProcessing(SpreadsheetConfiguration configuration) {
        // No post-processing needed for contacts/recipients
        logger.fine("No post-processing required for contacts and recipients");
    }

    /**
     * Collects the spreadsheet references needed for contacts and recipients.
     *
     * @param sheetTitle Title of the sheet to collect references for
     * @return List of SpreadsheetReference objects for both contacts and recipients
     */
    private List<SpreadsheetReference> collectSpreadsheetReferences(String sheetTitle) {

        // Get contact references from column mapping service
        List<SpreadsheetReference> contactRefs =
                mappingService.getSpreadsheetReferences(
                        ColumnMapping.MappingType.CONTACT,
                        sheetTitle
                );
        List<SpreadsheetReference> references = new ArrayList<>(contactRefs);

        // Get recipient references from column mapping service
        List<SpreadsheetReference> recipientRefs =
                mappingService.getSpreadsheetReferences(
                        ColumnMapping.MappingType.RECIPIENT,
                        sheetTitle
                );
        references.addAll(recipientRefs);

        return references;    }

    /**
     * Processes contact data, updating existing contacts or creating new ones as needed.
     *
     * @param contacts List of contacts parsed from the spreadsheet
     * @return Number of contacts processed
     */
    private int processContacts(List<Contact> contacts) {
        if (contacts.isEmpty()) {
            return 0;
        }

        int createdCount = 0;
        int updatedCount = 0;

        for (Contact contact : contacts) {
            if (contact == null) {
                continue;
            }

            // Find existing contact by row number
            Optional<Contact> existingContactOpt = contactRepository.findBySpreadsheetRow(
                    contact.getSpreadsheetRow().extractRowNumber());

            if (existingContactOpt.isPresent()) {
                if (updateExistingContact(existingContactOpt.get(), contact)) {
                    updatedCount++;
                }
            } else {
                contactRepository.save(contact);
                createdCount++;
            }
        }

        logger.info(String.format("Contacts: created %d, updated %d", createdCount, updatedCount));
        return createdCount + updatedCount;
    }

    /**
     * Updates an existing contact with new data if needed.
     *
     * @param existingContact The existing contact from the database
     * @param newContact The new contact data from the spreadsheet
     * @return true if the contact was updated, false if no changes were needed
     */
    private boolean updateExistingContact(Contact existingContact, Contact newContact) {
        if (existingContact.equals(newContact)) {
            return false; // No changes needed
        }

        // Preserve the existing ID
        newContact.setId(existingContact.getId());
        contactRepository.save(newContact);
        return true;
    }

    /**
     * Processes recipient entries, linking them to their contacts and creating or updating as needed.
     *
     * @param recipientEntries List of recipient entries parsed from the spreadsheet
     * @return Number of recipient entries processed
     */
    private int processRecipients(List<RecipientSpreadsheetEntry> recipientEntries) {
        if (recipientEntries.isEmpty()) {
            return 0;
        }

        // Create mapping from row number to contact ID
        Map<Integer, Long> rowToContactIdMap = createRowToContactIdMap();

        int totalRecipients = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int skippedDueToMissingContact = 0;
        int skippedNoChanges = 0;

        // Process each recipient entry
        for (RecipientSpreadsheetEntry entry : recipientEntries) {
            if (entry == null || entry.recipients() == null || entry.recipients().isEmpty()) {
                continue;
            }

            Long contactId = rowToContactIdMap.get(entry.spreadsheetRow());
            if (contactId == null) {
                logger.warning("No contact found for row: " + entry.spreadsheetRow());
                skippedDueToMissingContact += entry.recipients().size();
                continue;
            }

            // Process all recipients for this entry
            RecipientMetadata metadata = createRecipientMetadata(contactId);
            EntityId<Contact> contactEntityId = EntityId.of(contactId);

            for (Recipient recipient : entry.recipients()) {
                if (recipient == null) {
                    continue;
                }

                totalRecipients++;

                RecipientProcessResult result = processRecipient(recipient, contactEntityId, metadata);
                switch (result) {
                    case CREATED -> createdCount++;
                    case UPDATED -> updatedCount++;
                    case NO_CHANGES -> skippedNoChanges++;
                }
            }
        }

        logger.info(String.format("Recipients: created %d, updated %d, unchanged %d, skipped due to missing contact %d (total: %d)",
                createdCount, updatedCount, skippedNoChanges, skippedDueToMissingContact, totalRecipients));
        return totalRecipients - skippedDueToMissingContact;
    }

    /**
     * Represents the possible outcomes of processing a recipient
     */
    private enum RecipientProcessResult {
        CREATED,
        UPDATED,
        NO_CHANGES
    }


    /**
     * Creates a mapping from spreadsheet row numbers to contact IDs.
     *
     * @return Map of row numbers to contact IDs
     */
    private Map<Integer, Long> createRowToContactIdMap() {
        List<EntityData<Contact, NoMetadata>> allSavedContacts = contactRepository.findAllWithMetadata();
        Map<Integer, Long> rowToContactIdMap = new HashMap<>();

        for (EntityData<Contact, NoMetadata> contactData : allSavedContacts) {
            Contact contact = contactData.entity();
            if (contact != null && contact.getSpreadsheetRow() != null) {
                rowToContactIdMap.put(
                        contact.getSpreadsheetRow().extractRowNumber(),
                        contact.getId().value()
                );
            }
        }

        return rowToContactIdMap;
    }

    /**
     * Creates metadata for a recipient.
     *
     * @param contactId The ID of the contact associated with the recipient
     * @return RecipientMetadata object
     */
    private RecipientMetadata createRecipientMetadata(Long contactId) {
        return new RecipientMetadata.Builder()
                .contactId(EntityId.of(contactId))
                .followupPlanId(EntityId.of(DEFAULT_FOLLOWUP_PLAN_ID))
                .build();
    }

    /**
     * Processes a single recipient, updating if it exists or creating if it doesn't.
     *
     * @param recipient The recipient to process
     * @param contactId The ID of the contact associated with the recipient
     * @param metadata The metadata to associate with the recipient
     * @return Result indicating whether recipient was created, updated, or unchanged
     */
    private RecipientProcessResult processRecipient(Recipient recipient, EntityId<Contact> contactId, RecipientMetadata metadata) {
        List<EntityData<Recipient, RecipientMetadata>> existingRecipients =
                recipientRepository.findByContactId(contactId);

        // Check if recipient already exists
        Optional<EntityData<Recipient, RecipientMetadata>> existingRecipientOpt = existingRecipients.stream()
                .filter(r -> r.entity().getEmailAddress().equals(recipient.getEmailAddress()))
                .findFirst();

        if (existingRecipientOpt.isPresent()) {
            // Recipient exists - check if update is needed
            EntityData<Recipient, RecipientMetadata> existingData = existingRecipientOpt.get();
            Recipient existingRecipient = existingData.entity();

            if (existingRecipient.equals(recipient)) {
                // No changes needed
                return RecipientProcessResult.NO_CHANGES;
            } else {
                Recipient updatedRecipient = new Recipient.Builder()
                        .from(existingRecipient)
                        .setSalutation(recipient.getSalutation())
                        .setHasReplied(recipient.hasReplied())
                        .setInitialContactDate(recipient.getInitialContactDate())
                        .build();

                recipientRepository.updateWithMetadata(updatedRecipient, existingData.metadata());
                return RecipientProcessResult.UPDATED;
            }
        } else {
            // Create new recipient
            recipientRepository.saveWithMetadata(recipient, metadata);
            return RecipientProcessResult.CREATED;
        }
    }

    @Override
    protected void handleSynchronizationError(Exception exception) {
        logger.log(Level.SEVERE, "Contact and recipient synchronization failed", exception);
        // Additional recovery logic could be added here
    }
}
