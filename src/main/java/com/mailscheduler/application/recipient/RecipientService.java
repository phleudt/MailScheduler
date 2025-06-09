package com.mailscheduler.application.recipient;

import com.mailscheduler.application.email.scheduling.RecipientScheduledEmailsMap;
import com.mailscheduler.application.email.service.RowToScheduledEmailsMap;
import com.mailscheduler.application.recipient.exception.RecipientOperationException;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.recipient.Contact;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.recipient.RecipientMetadata;
import com.mailscheduler.domain.model.common.vo.ThreadId;
import com.mailscheduler.domain.repository.ContactRepository;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.RecipientRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service responsible for managing recipients and their interactions with contacts.
 */
public class RecipientService {
    private static final Logger LOGGER = Logger.getLogger(RecipientService.class.getName());
    private static final String CONFIGURATION_SHEET_NAME = "Configuration";
    private static final int DEFAULT_STARTING_ROW = 6;

    private final RecipientRepository recipientRepository;
    private final ContactRepository contactRepository;
    private final SpreadsheetGateway spreadsheetGateway;

    public RecipientService(
            RecipientRepository recipientRepository,
            ContactRepository contactRepository,
            SpreadsheetGateway spreadsheetGateway
    ) {
        this.recipientRepository = Objects.requireNonNull(recipientRepository, "Recipient repository cannot be null");
        this.contactRepository = Objects.requireNonNull(contactRepository, "Contact repository cannot be null");
        this.spreadsheetGateway = Objects.requireNonNull(spreadsheetGateway, "Spreadsheet gateway cannot be null");
    }

    /**
     * Finds recipients eligible for email communication based on spreadsheet data.
     *
     * @param spreadsheetConfig Configuration of the spreadsheet
     * @param sendingCriterionColumn Column that indicates eligibility for sending
     * @return List of eligible recipients with their metadata
     * @throws RecipientOperationException if finding eligible recipients fails
     */
    public List<EntityData<Recipient, RecipientMetadata>> findEligibleRecipients(
            SpreadsheetConfiguration spreadsheetConfig,
            SpreadsheetReference sendingCriterionColumn) throws RecipientOperationException {

        validateFindEligibleRecipientsParams(spreadsheetConfig, sendingCriterionColumn);

        try {
            List<EntityData<Recipient, RecipientMetadata>> allEligibleRecipients = new ArrayList<>();
            LOGGER.info("Finding eligible recipients from spreadsheet");

            // Process each sheet configuration
            for (SheetConfiguration sheetConfig : spreadsheetConfig.sheetConfigurations()) {
                if (isConfigurationSheet(sheetConfig.title())) {
                    continue;
                }

                List<EntityData<Recipient, RecipientMetadata>> sheetRecipients =
                        processSheetForEligibleRecipients(
                                spreadsheetConfig.spreadsheetId(),
                                sheetConfig,
                                sendingCriterionColumn);

                allEligibleRecipients.addAll(sheetRecipients);
            }

            LOGGER.info("Found " + allEligibleRecipients.size() + " eligible recipients");
            return allEligibleRecipients;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read spreadsheet data", e);
            throw new RecipientOperationException("Failed to find eligible recipients: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding eligible recipients", e);
            throw new RecipientOperationException("Unexpected error finding eligible recipients", e);
        }
    }

    /**
     * Maps recipients to their corresponding spreadsheet rows along with scheduled emails.
     *
     * @param recipients List of recipients
     * @param scheduledEmailsMap Map of scheduled emails by recipient
     * @return Map of row numbers to scheduled emails
     */
    public RowToScheduledEmailsMap mapRecipientsToRows(
            List<EntityData<Recipient, RecipientMetadata>> recipients,
            RecipientScheduledEmailsMap scheduledEmailsMap) {
        if (recipients == null || recipients.isEmpty()) {
            LOGGER.info("No recipients to map to rows");
            return new RowToScheduledEmailsMap(Map.of());
        }

        if (scheduledEmailsMap == null || scheduledEmailsMap.isEmpty()) {
            LOGGER.info("No scheduled emails to map");
            return new RowToScheduledEmailsMap(Map.of());
        }

        try {
            LOGGER.info("Mapping " + recipients.size() + " recipients to spreadsheet rows");
            Map<Integer, List<EntityData<Email, EmailMetadata>>> rowsToScheduledEmails = new HashMap<>();
            var emailMap = scheduledEmailsMap.map();

            for (var recipientData : recipients) {
                mapRecipientToRow(recipientData, emailMap, rowsToScheduledEmails);
            }

            LOGGER.info("Mapped " + rowsToScheduledEmails.size() + " rows to scheduled emails");
            return new RowToScheduledEmailsMap(rowsToScheduledEmails);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error mapping recipients to rows", e);
            return new RowToScheduledEmailsMap(Map.of());
        }
    }

    /**
     * Creates a mapping of recipient IDs to their spreadsheet row numbers.
     *
     * @param recipientIds The recipients to map
     * @return Map of recipient IDs to row numbers
     */
    public Map<EntityId<Recipient>, Integer> mapRecipientIdsToRows(
            List<EntityId<Recipient>> recipientIds) {

        if (recipientIds == null || recipientIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<EntityId<Recipient>, Integer> recipientIdToRowMap = new HashMap<>();

        for (var recipientId : recipientIds) {
            findRowForRecipient(recipientId)
                    .ifPresent(row -> recipientIdToRowMap.put(recipientId, row));
        }

        return recipientIdToRowMap;
    }

    /**
     * Retrieves a recipient by ID with metadata.
     *
     * @param id The ID of the recipient to retrieve
     * @return The recipient with metadata
     * @throws IllegalArgumentException if recipient is not found
     */
    public EntityData<Recipient, RecipientMetadata> getRecipient(EntityId<Recipient> id) {
        Objects.requireNonNull(id, "Recipient ID cannot be null");

        return recipientRepository.findByIdWithMetadata(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + id));
    }

    /**
     * Marks a recipient as having replied to communications.
     *
     * @param id The ID of the recipient to mark
     * @return true if the update was successful, false otherwise
     */
    public boolean markAsReplied(EntityId<Recipient> id) {
        Objects.requireNonNull(id, "Recipient ID cannot be null");

        try {
            Optional<EntityData<Recipient, RecipientMetadata>> recipientOpt =
                    recipientRepository.findByIdWithMetadata(id);

            if (recipientOpt.isEmpty()) {
                LOGGER.warning("Cannot mark as replied: Recipient not found: " + id);
                return false;
            }

            var recipient = recipientOpt.get();
            if (recipient.entity().hasReplied()) {
                // Already marked as replied
                return true;
            }

            Recipient updatedRecipient = new Recipient.Builder()
                    .from(recipient.entity())
                    .setHasReplied(true)
                    .build();

            recipientRepository.updateWithMetadata(updatedRecipient, recipient.metadata());
            LOGGER.info("Marked recipient " + id + " as replied");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error marking recipient " + id + " as replied", e);
            return false;
        }
    }

    /**
     * Updates the thread ID for a recipient.
     *
     * @param id The ID of the recipient to update
     * @param threadId The new thread ID
     * @return true if the update was successful, false otherwise
     */
    public boolean updateThreadId(EntityId<Recipient> id, ThreadId threadId) {
        Objects.requireNonNull(id, "Recipient ID cannot be null");
        Objects.requireNonNull(threadId, "Thread ID cannot be null");

        try {
            Optional<EntityData<Recipient, RecipientMetadata>> recipientOpt =
                    recipientRepository.findByIdWithMetadata(id);

            if (recipientOpt.isEmpty()) {
                LOGGER.warning("Cannot update thread ID: Recipient not found: " + id);
                return false;
            }

            var recipient = recipientOpt.get();
            RecipientMetadata updatedMetadata = RecipientMetadata.Builder
                    .from(recipient.metadata())
                    .threadId(threadId)
                    .build();

            recipientRepository.saveWithMetadata(recipient.entity(), updatedMetadata);
            LOGGER.info("Updated thread ID for recipient " + id);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating thread ID for recipient " + id, e);
            return false;
        }
    }

    /**
     * Sets the initial contact date for a recipient if not already set.
     *
     * @param id The ID of the recipient to update
     * @param contactDate The contact date to set, or null to use current date
     * @return updatedRecipient
     */
    public Recipient setInitialContactDate(EntityId<Recipient> id, LocalDate contactDate) {
        Objects.requireNonNull(id, "Recipient ID cannot be null");

        try {
            Optional<EntityData<Recipient, RecipientMetadata>> recipientOpt =
                    recipientRepository.findByIdWithMetadata(id);

            if (recipientOpt.isEmpty()) {
                LOGGER.warning("Cannot set contact date: Recipient not found: " + id);
                return null;
            }

            var recipientData = recipientOpt.get();
            Recipient recipient = recipientData.entity();

            // Only update if not already set
            if (recipient.getInitialContactDate() == null) {
                LocalDate finalDate = contactDate != null ?
                        contactDate :
                        LocalDate.now();

                Recipient updatedRecipient = new Recipient.Builder()
                        .from(recipient)
                        .setInitialContactDate(finalDate)
                        .build();

                recipientRepository.saveWithMetadata(updatedRecipient, recipientData.metadata());
                LOGGER.info("Set initial contact date for recipient " + id + " to " + finalDate);
                return updatedRecipient;
            }

            return recipient;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error setting initial contact date for recipient " + id, e);
            return null;
        }
    }

    // Private helper methods

    private void validateFindEligibleRecipientsParams(
            SpreadsheetConfiguration spreadsheetConfig,
            SpreadsheetReference sendingCriterionColumn) {

        if (spreadsheetConfig == null) {
            throw new IllegalArgumentException("Spreadsheet configuration cannot be null");
        }

        if (sendingCriterionColumn == null) {
            throw new IllegalArgumentException("Sending criterion column cannot be null");
        }

        if (spreadsheetConfig.sheetConfigurations().isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet configuration must have at least one sheet");
        }
    }

    private boolean isConfigurationSheet(String sheetTitle) {
        return CONFIGURATION_SHEET_NAME.equals(sheetTitle);
    }

    private List<EntityData<Recipient, RecipientMetadata>> processSheetForEligibleRecipients(
            String spreadsheetId,
            SheetConfiguration sheetConfig,
            SpreadsheetReference sendingCriterionColumn) throws IOException {

        // Set sheet title for sending criterion column
        SpreadsheetReference sheetSendingCriterion = sendingCriterionColumn.withSheetTitle(sheetConfig.title());

        // Read data and identify qualifying rows
        List<List<Object>> data = spreadsheetGateway.readData(spreadsheetId, sheetSendingCriterion);
        if (data == null || data.isEmpty()) {
            return List.of();
        }

        Set<Integer> qualifyingRows = identifyQualifyingRows(data, DEFAULT_STARTING_ROW);

        // Find contacts that fulfill sending criterion
        List<EntityData<Contact, NoMetadata>> allContacts = contactRepository.findAllWithMetadata();
        List<Contact> qualifyingContacts = filterContactsMatchingCriteria(
                allContacts, sheetConfig.title(), qualifyingRows);

        // Find and update eligible recipients
        return findAndUpdateEligibleRecipients(qualifyingContacts);
    }

    private Set<Integer> identifyQualifyingRows(List<List<Object>> data, int startRow) {
        final Set<Integer> qualifyingRows = new HashSet<>();
        int currentRow = startRow;

        for (List<Object> row : data) {
            if (row != null && !row.isEmpty() && hasNonEmptyValue(row)) {
                qualifyingRows.add(currentRow);
            }
            currentRow++;
        }

        return qualifyingRows;
    }

    private boolean hasNonEmptyValue(List<Object> row) {
        return row.stream()
                .anyMatch(value -> value != null && !value.toString().trim().isEmpty());
    }

    private List<Contact> filterContactsMatchingCriteria(
            List<EntityData<Contact, NoMetadata>> contacts,
            String sheetTitle,
            Set<Integer> qualifyingRows) {

        return contacts.stream()
                .map(EntityData::entity)
                .filter(contact -> sheetTitle.equals(contact.getSheetTitle()))
                .filter(contact -> {
                    int rowNumber = contact.getSpreadsheetRow().extractRowNumber();
                    return qualifyingRows.contains(rowNumber);
                })
                .collect(Collectors.toList());
    }

    private List<EntityData<Recipient, RecipientMetadata>> findAndUpdateEligibleRecipients(List<Contact> contacts) {
        List<EntityData<Recipient, RecipientMetadata>> allRecipients = new ArrayList<>();

        // Find recipients for qualifying contacts
        for (Contact contact : contacts) {
            List<EntityData<Recipient, RecipientMetadata>> contactRecipients =
                    recipientRepository.findByContactId(contact.getId());

            if (contactRecipients.isEmpty()) {
                LOGGER.info("No recipients found for contact: " + contact.getId());
            }

            allRecipients.addAll(contactRecipients);
        }

        // Filter and update recipients without an initial contact date
        List<EntityData<Recipient, RecipientMetadata>> eligibleRecipients = new ArrayList<>();

        for (EntityData<Recipient, RecipientMetadata> recipientData : allRecipients) {
            if (isEligibleForContact(recipientData.entity())) {
                Recipient updatedRecipient = setInitialContactDate(recipientData.entity().getId(), null);
                if (updatedRecipient != null) {
                    eligibleRecipients.add(new EntityData<>(updatedRecipient, recipientData.metadata()));
                }
            }
        }

        return eligibleRecipients;
    }

    private boolean isEligibleForContact(Recipient recipient) {
        return recipient.getInitialContactDate() == null && !recipient.hasReplied();
    }

    private void mapRecipientToRow(
            EntityData<Recipient, RecipientMetadata> recipientData,
            Map<EntityId<Recipient>, List<EntityData<Email, EmailMetadata>>> emailMap,
            Map<Integer, List<EntityData<Email, EmailMetadata>>> rowsToScheduledEmails) {

        List<EntityData<Email, EmailMetadata>> scheduledEmails =
                emailMap.get(recipientData.entity().getId());

        if (scheduledEmails == null || scheduledEmails.isEmpty()) {
            return;
        }

        findRowForRecipient(recipientData.entity().getId()).ifPresent(
                rowNumber -> rowsToScheduledEmails.put(rowNumber, scheduledEmails)
        );
    }

    private Optional<Integer> findRowForRecipient(EntityId<Recipient> recipientId) {
        try {
            Optional<EntityData<Recipient, RecipientMetadata>> recipientOpt =
                    recipientRepository.findByIdWithMetadata(recipientId);

            if (recipientOpt.isEmpty()) {
                return Optional.empty();
            }

            EntityId<Contact> contactId = recipientOpt.get().metadata().contactId();
            Optional<EntityData<Contact, NoMetadata>> contactOpt =
                    contactRepository.findByIdWithMetadata(contactId);

            if (contactOpt.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(contactOpt.get().entity().getSpreadsheetRow().extractRowNumber());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding row for recipient " + recipientId, e);
            return Optional.empty();
        }
    }
}