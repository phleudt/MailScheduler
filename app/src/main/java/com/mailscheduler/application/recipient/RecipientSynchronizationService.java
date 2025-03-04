package com.mailscheduler.application.recipient;

import com.mailscheduler.common.config.Configuration;
import com.mailscheduler.common.config.SendingCriterion;
import com.mailscheduler.common.exception.EmailPreparationException;
import com.mailscheduler.common.exception.SpreadsheetOperationException;
import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.recipient.RecipientId;
import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.common.exception.service.RecipientServiceException;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteRecipientRepository;
import com.mailscheduler.interfaces.cli.AbstractUserConsoleInteractionService;
import com.mailscheduler.application.spreadsheet.SpreadsheetService;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for synchronizing recipient information between spreadsheets and the database.
 * This service handles the following operations:
 * - Loading recipient from configured spreadsheet columns
 * - Synchronizing recipient data with the database
 * - Managing recipient email information
 * - Handling recipient updates and merges
 *
 * @author phleudt
 * @version 1.0
 */
public class RecipientSynchronizationService {
    private static final Logger LOGGER = Logger.getLogger(RecipientSynchronizationService.class.getName());

    private final SQLiteRecipientRepository recipientRepository;
    private final SpreadsheetService spreadsheetService;
    private final UserInteractionService userInteractionService;
    private final Map<String, SpreadsheetReference> recipientSpreadsheetReference; // Currently only SpreadsheetReference.Type.COLUMN supported
    private final List<SendingCriterion> sendingCriteria;

    public RecipientSynchronizationService(SpreadsheetService spreadsheetService, Configuration configuration) {
        recipientRepository = new SQLiteRecipientRepository(DatabaseManager.getInstance());
        this.spreadsheetService = spreadsheetService;
        this.userInteractionService = new UserInteractionService();
        this.recipientSpreadsheetReference = configuration.getRecipientColumns();
        this.sendingCriteria = configuration.getSendingCriteria();

    }

    public void syncRecipients() throws RecipientServiceException {
        try {
            List<SpreadsheetReference> spreadsheetReferences = collectSpreadsheetReferences();
            LOGGER.info("Synchronizing recipient from spreadsheet columns: " + spreadsheetReferences);

            List<Recipient> recipients = fetchRecipientsFromSpreadsheet(spreadsheetReferences);
            processRecipientUpdates(recipients);
        } catch (RecipientServiceException e) {
            LOGGER.severe("recipient synchronization failed: " + e.getMessage());
            throw new RecipientServiceException("Failed to synchronize recipient", e);
        }
    }

    private void processRecipientUpdates(List<Recipient> recipients) throws RecipientServiceException {
        try {
            // recipientRepository.beginTransaction();

            for (Recipient recipient : recipients) {
                Optional<Recipient> existingRecipients = findExistingRecipient(recipient);

                if (existingRecipients.isPresent()) {
                    updateExistingRecipient(recipient, existingRecipients.get());
                } else {
                    recipientRepository.save(recipient);
                }
            }
            // recipientRepository.commitTransaction();
        } catch (RepositoryException e) {
            handleTransactionError(e);
        }
    }

    public List<Recipient> getRecipientsWithInitialEmailDate() throws RecipientServiceException {
        try {
            return recipientRepository.getRecipientsWithInitialEmailDate();
        } catch (RepositoryException e) {
            throw new RecipientServiceException("Failed to retrieve recipients with initial email date", e);
        }
    }

    public Recipient updateInitialEmailDateIfPast(Recipient recipient) throws RecipientServiceException {
        if (!isEmailDateInPast(recipient)) {
            return recipient;
        }
        try {
            Recipient updatedRecipient = new Recipient.Builder()
                    .fromExisting(recipient)
                    .setInitialEmailDate(ZonedDateTime.now())
                    .setPreserveInitialEmailDate(true)
                    .build();

            recipientRepository.update(updatedRecipient);
            return updatedRecipient;
        } catch (RepositoryException e) {
            throw new RecipientServiceException("Initial email initial email date", e);
        }
    }

    public List<Email> prepareEmailsForSending(List<Email> emailsToPrepare) throws EmailPreparationException {
        List<Email> emails = new ArrayList<>(emailsToPrepare.size());
        for (Email email : emailsToPrepare) {
            emails.add(prepareEmailForSending(email));
        }
        return emails;
    }

    private List<SpreadsheetReference> collectSpreadsheetReferences() {
        // TODO: make variable, in case a new reference is added
        List<SpreadsheetReference> references = new ArrayList<>(List.of(
                recipientSpreadsheetReference.get("domain"),
                recipientSpreadsheetReference.get("emailAddress"),
                recipientSpreadsheetReference.get("name"),
                recipientSpreadsheetReference.get("salutation"),
                recipientSpreadsheetReference.get("phoneNumber"),
                recipientSpreadsheetReference.get("initialEmailDate")
        ));

        references.addAll(sendingCriteria.stream()
                .map(SendingCriterion::getTargetColumn)
                .toList());
        return references;
    }

    private void handleTransactionError(Exception e) throws RecipientServiceException {
        LOGGER.log(Level.SEVERE, "Failed to process recipient", e);
        try {
            recipientRepository.rollbackTransaction();
        } catch (SQLException rollbackEx) {
            LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
        }
        throw new RecipientServiceException("Recipient processing failed", e);
    }

    private void updateExistingRecipient(Recipient newRecipient, Recipient existingRecipient)
            throws RepositoryException {
        if (existingRecipient.equals(newRecipient)) {
            return;
        }

        // Check if the only difference is the initial email date
        if (existingRecipient.equalsWithoutInitialEmailDate(newRecipient)) {
            Recipient updatedRecipient = new Recipient.Builder()
                    .fromExisting(existingRecipient)
                    .setInitialEmailDate(newRecipient.getInitialEmailDate())
                    .setPreserveInitialEmailDate(true)
                    .build();
            recipientRepository.update(updatedRecipient);
            return;
        }

        if (userInteractionService.promptRecipientUpdate(existingRecipient, newRecipient)) {
            Recipient mergeRecipient = mergeRecipients(existingRecipient, newRecipient);
            recipientRepository.update(mergeRecipient);
        }
    }

    private Optional<Recipient> findExistingRecipient(Recipient recipient) throws RepositoryException {
        return recipientRepository.findRecipientByUniqueIdentifiers(
                recipient.getEmailAddress(),
                recipient.getName(),
                recipient.getPhoneNumber(),
                recipient.getDomain()
            );
    }

    private boolean isEmailDateInPast(Recipient recipient) {
        return recipient.getInitialEmailDate().isBefore(ZonedDateTime.now());
    }

    private Recipient mergeRecipients(Recipient existingRecipient, Recipient newRecipient) {
        ZonedDateTime emailDate = determineInitialEmailDate(existingRecipient, newRecipient);

        return new Recipient.Builder()
                .fromExisting(existingRecipient)
                // Update fields from new recipient
                .setName(newRecipient.getName() != null ? newRecipient.getName().toString() : null)
                .setEmailAddress(newRecipient.getEmailAddress() != null ? newRecipient.getEmailAddress().toString() : null)
                .setSalutation(newRecipient.getSalutation())
                .setDomain(newRecipient.getDomain())
                .setPhoneNumber(newRecipient.getPhoneNumber())
                .setSpreadsheetReference(newRecipient.getSpreadsheetRow())
                .setInitialEmailDate(emailDate)
                .setPreserveInitialEmailDate(true)
                .build();
    }

    /**
     * Determines which initial email date to use when merging recipients
     *
     * @param existingRecipient The existing recipient
     * @param newRecipient The new recipient
     * @return The appropriate initial email date to use
     */
    private ZonedDateTime determineInitialEmailDate(Recipient existingRecipient, Recipient newRecipient) {
        // If existing has date but new doesn't, keep existing
        if (existingRecipient.getInitialEmailDate() != null && newRecipient.getInitialEmailDate() == null) {
            return existingRecipient.getInitialEmailDate();
        }

        // If new has date but existing doesn't, use new
        if (existingRecipient.getInitialEmailDate() == null && newRecipient.getInitialEmailDate() != null) {
            return newRecipient.getInitialEmailDate();
        }

        // If both have dates, use the earlier one
        if (existingRecipient.getInitialEmailDate() != null && newRecipient.getInitialEmailDate() != null) {
            return existingRecipient.getInitialEmailDate().isBefore(newRecipient.getInitialEmailDate()) ?
                    existingRecipient.getInitialEmailDate() : newRecipient.getInitialEmailDate();
        }

        // If neither has a date, return null
        return null;
    }


    private List<Recipient> fetchRecipientsFromSpreadsheet(List<SpreadsheetReference> spreadsheetReferences) throws RecipientServiceException {
        try {
            return spreadsheetService.retrieveRecipient(spreadsheetReferences);
        } catch (SpreadsheetOperationException e) {
            throw new RecipientServiceException("Spreadsheet recipient retrieval failed", e);
        }
    }


    private EmailAddress getEmailAddressById(RecipientId recipientId) throws RecipientServiceException {
        try {
            LOGGER.info("Getting email address by id: " + recipientId);
            return recipientRepository.findEmailAddressById(recipientId);
        } catch (RepositoryException e) {
            throw new RecipientServiceException("Failed to get email address with id: " + recipientId, e);
        }
    }

    private Email prepareEmailForSending(Email email) throws EmailPreparationException {
        try {
            EmailAddress emailAddress = getEmailAddressById(email.getRecipientId());
            email.setRecipient(emailAddress);
            return email;
        } catch (RecipientServiceException e)  {
            LOGGER.log(Level.SEVERE, "Failed to retrieve email address for recipient ID:" + email.getRecipientId().value(), e);
            throw new EmailPreparationException("Failed to prepare email for sending", e);
        }
    }
}

class UserInteractionService extends AbstractUserConsoleInteractionService {

    /**
     * This method interacts with the user to prompt them, whether to change the data from the existing recipient to new recipient
     */
    boolean promptRecipientUpdate(Recipient existingRecipient, Recipient newRecipient) {
        displayDifferenceBetweenRecipients(existingRecipient, newRecipient);

        System.out.println("Do you want to update this recipient? (y/n)");
        String response = scanner.next().trim().toLowerCase();

        return response.equals("y") || response.equals("yes");
    }

    private void displayDifferenceBetweenRecipients(Recipient existingRecipient, Recipient newRecipient) {
        // TODO change what is displayed. use implemented equal method of classes
        System.out.println("Existing Recipient Found: " + existingRecipient.getName());

        if (existingRecipient.getName() != null && newRecipient.getName() != null && !existingRecipient.getName().equals(newRecipient.getName())) {
            System.out.println("Name: " + existingRecipient.getName() + " -> " + newRecipient.getName());
        }
        if (existingRecipient.getSalutation() != null && newRecipient.getSalutation() != null && !existingRecipient.getSalutation().equals(newRecipient.getSalutation())) {
            System.out.println("Salutation: " + existingRecipient.getName() + " -> " + newRecipient.getName());
        }
        if (existingRecipient.getEmailAddress() != null && newRecipient.getEmailAddress() != null && !existingRecipient.getEmailAddress().equals(newRecipient.getEmailAddress())) {
            System.out.println("Email Address: " + existingRecipient.getEmailAddress() + " -> " + newRecipient.getEmailAddress());
        }
        if (existingRecipient.getDomain() != null && newRecipient.getDomain() != null && !existingRecipient.getDomain().equals(newRecipient.getDomain())) {
            System.out.println("Domain: " + existingRecipient.getDomain() + " -> " + newRecipient.getDomain());
        }
        if (existingRecipient.getPhoneNumber() != null && newRecipient.getPhoneNumber() != null && !existingRecipient.getPhoneNumber().equals(newRecipient.getPhoneNumber())) {
            System.out.println("Phone Number: " + existingRecipient.getPhoneNumber() + " -> " + newRecipient.getPhoneNumber());
        }
        if (existingRecipient.hasReplied() != newRecipient.hasReplied()) {
            System.out.println("Has Replied: " + existingRecipient.hasReplied() + " -> " + newRecipient.hasReplied());
        }
        if (existingRecipient.getSpreadsheetRow() != newRecipient.getSpreadsheetRow()) {
            System.out.println("Spreadsheet Row: " + existingRecipient.getSpreadsheetRow() + " -> " + newRecipient.getSpreadsheetRow());
        }
        if (!Objects.equals(existingRecipient.getInitialEmailDateAsString(), newRecipient.getInitialEmailDateAsString())) {
            System.out.println("Initial Email date: " + existingRecipient.getInitialEmailDateAsString() + " -> " + newRecipient.getInitialEmailDateAsString());
        }
    }
}