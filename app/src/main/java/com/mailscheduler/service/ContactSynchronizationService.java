package com.mailscheduler.service;

import com.mailscheduler.config.Configuration;
import com.mailscheduler.config.SendingCriterion;
import com.mailscheduler.database.DatabaseManager;
import com.mailscheduler.database.dao.RecipientDao;
import com.mailscheduler.database.entities.RecipientEntity;
import com.mailscheduler.dto.EmailDto;
import com.mailscheduler.dto.RecipientDto;
import com.mailscheduler.exception.*;
import com.mailscheduler.exception.dao.RecipientDaoException;
import com.mailscheduler.exception.service.RecipientServiceException;
import com.mailscheduler.mapper.EntityMapper;
import com.mailscheduler.model.Contact;
import com.mailscheduler.model.Email;
import com.mailscheduler.model.SpreadsheetReference;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for synchronizing contact information between spreadsheets and the database.
 * This service handles the following operations:
 * - Loading contacts from configured spreadsheet columns
 * - Synchronizing contact data with the database
 * - Managing recipient email information
 * - Handling contact updates and merges
 *
 * @author phleudt
 * @version 1.0
 */
public class ContactSynchronizationService {
    private static final Logger LOGGER = Logger.getLogger(ContactSynchronizationService.class.getName());
    private static final String EMAIL_VALIDATION_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final RecipientDao recipientDao;
    private final SpreadsheetService spreadsheetService;
    private final UserInteractionService userInteractionService;
    private final Map<String, SpreadsheetReference> contactSpreadsheetReference; // Currently only SpreadsheetReference.Type.COLUMN supported
    private final List<SendingCriterion> sendingCriteria;

    /**
     * Constructs a new ContactSynchronizationService with the specified dependencies.
     *
     * @param spreadsheetService Service for interacting with spreadsheets
     * @param configuration Contains configuration settings for contact columns and sending criteria
     */
    public ContactSynchronizationService(SpreadsheetService spreadsheetService, Configuration configuration) {
        this.recipientDao = new RecipientDao(DatabaseManager.getInstance());
        this.spreadsheetService = spreadsheetService;
        this.userInteractionService = new UserInteractionService();
        this.contactSpreadsheetReference = configuration.getContactColumns();
        this.sendingCriteria = configuration.getSendingCriteria();

    }

    /**
     * Synchronizes contact data from configured spreadsheet columns to the database.
     * This method performs the following steps:
     * 1. Collects all relevant spreadsheet references
     * 2. Fetches contacts from the spreadsheet
     * 3. Processes and updates the database with contact information
     *
     * @throws RecipientServiceException if synchronization fails
     */
    public void syncContacts() throws RecipientServiceException {
        try {
            List<SpreadsheetReference> spreadsheetReferences = collectSpreadsheetReferences();
            LOGGER.info("Synchronizing contacts from spreadsheet columns: " + spreadsheetReferences);

            List<Contact> contacts = fetchContactsFromSpreadsheet(spreadsheetReferences);
            processContactUpdates(contacts);
        } catch (RecipientServiceException e) {
            LOGGER.severe("Contact synchronization failed: " + e.getMessage());
            throw new RecipientServiceException("Failed to synchronize contacts", e);
        }
    }

    /**
     * Processes a list of contacts, updating existing contacts or creating new ones as needed.
     * Handles database transactions and rollback in case of errors.
     *
     * @param contacts List of contacts to process
     * @throws RecipientServiceException if contact processing fails
     */
    private void processContactUpdates(List<Contact> contacts) throws RecipientServiceException {
        try {
            recipientDao.beginTransaction();

            for (Contact contact : contacts) {
                Optional<RecipientEntity> existingRecipients = findExistingRecipient(contact);

                if (existingRecipients.isPresent()) {
                    updateExistingContact(contact, existingRecipients.get());
                } else if (isValidContact(contact)){
                    persistNewContact(contact);
                }
            }
            recipientDao.commitTransaction();
        } catch (SQLException | RecipientDaoException | MappingException e) {
            handleTransactionError(e);
        }
    }

    /**
     * Retrieves a list of all recipients who have their initial email date set.
     *
     * @return List of RecipientDto objects with initial email dates
     * @throws RecipientServiceException if retrieval fails
     */
    public List<RecipientDto> getRecipientsWithInitialEmailDate() throws RecipientServiceException {
        try {
            List<RecipientEntity> recipientEntities = recipientDao.getRecipientsWithInitialEmailDate();

            List<RecipientDto> recipientDtos = new ArrayList<>(recipientEntities.size());
            for (RecipientEntity recipientEntity : recipientEntities) {
                recipientDtos.add(EntityMapper.toRecipientDto(recipientEntity));
            }
            return recipientDtos;
        } catch (RecipientDaoException | MappingException e) {
            throw new RecipientServiceException("Failed to retrieve recipients with initial email date", e);
        }
    }

    /**
     * Updates the initial email date for a recipient if it's in the past.
     *
     * @param recipient The recipient to update
     * @return Updated RecipientDto with new initial email date
     * @throws RecipientServiceException if update fails
     */
    public RecipientDto updateInitialEmailDateIfPast(RecipientDto recipient) throws RecipientServiceException {
        if (!isEmailDateInPast(recipient)) {
            return recipient;
        }
        try {
            RecipientDto updatedRecipient = recipient.withUpdatedInitialEmailDate(ZonedDateTime.now());
            recipientDao.updateRecipientById(
                    updatedRecipient.getId(),
                    EntityMapper.toRecipientEntity(updatedRecipient)
            );
            return updatedRecipient;
        } catch (RecipientDaoException | MappingException e) {
            throw new RecipientServiceException("Initial email initial email date", e);
        }
    }

    /**
     * Prepares a list of emails for sending by replacing recipient IDs with email addresses.
     *
     * @param emailDtos List of email DTOs to prepare
     * @return List of prepared Email objects
     * @throws EmailPreparationException if preparation fails
     */
    public List<Email> prepareEmailsForSending(List<EmailDto> emailDtos) throws EmailPreparationException {
        List<Email> emails = new ArrayList<>(emailDtos.size());
        for (EmailDto emailDto : emailDtos) {
            emails.add(prepareEmailForSending(emailDto));
        }
        return emails;
    }

    private List<SpreadsheetReference> collectSpreadsheetReferences() {
        List<SpreadsheetReference> references = new ArrayList<>(List.of(
                contactSpreadsheetReference.get("domain"),
                contactSpreadsheetReference.get("emailAddress"),
                contactSpreadsheetReference.get("name"),
                contactSpreadsheetReference.get("gender"),
                contactSpreadsheetReference.get("phoneNumber"),
                contactSpreadsheetReference.get("initialEmailDate")
        ));

        references.addAll(sendingCriteria.stream()
                .map(SendingCriterion::getTargetColumn)
                .toList());
        return references;
    }

    private boolean isValidContact(Contact contact) {
        return contact.getEmailAddress() != null &&
                contact.getEmailAddress().matches(EMAIL_VALIDATION_REGEX);
    }

    private void handleTransactionError(Exception e) throws RecipientServiceException {
        LOGGER.log(Level.SEVERE, "Failed to process contacts", e);
        try {
            recipientDao.rollbackTransaction();
        } catch (SQLException rollbackEx) {
            LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
        }
        throw new RecipientServiceException("Contact processing failed", e);
    }

    private void updateExistingContact(Contact newContact, RecipientEntity existingRecipient)
            throws RecipientDaoException, MappingException {
        RecipientDto existingDto = EntityMapper.toRecipientDto(existingRecipient);
        RecipientDto newDto = EntityMapper.toRecipientDto(newContact);

        if (existingDto.equals(newDto)) {
            return;
        }

        if (userInteractionService.promptContactUpdate(existingDto, newDto)) {
            RecipientDto mergeRecipient = mergeRecipients(existingDto, newDto);
            recipientDao.updateRecipientById(
                    mergeRecipient.getId(),
                    EntityMapper.toRecipientEntity(mergeRecipient)
            );
        }
    }

    private void persistNewContact(Contact contact) throws RecipientDaoException, MappingException {
        RecipientDto recipientDto = EntityMapper.toRecipientDto(contact);
        recipientDao.insertRecipient(EntityMapper.toRecipientEntity(recipientDto));
    }

    private Optional<RecipientEntity> findExistingRecipient(Contact contact) throws RecipientDaoException {
        return Optional.ofNullable(
                recipientDao.findRecipientByUniqueIdentifiers(
                        contact.getEmailAddress(),
                        contact.getName(),
                        contact.getPhoneNumber(),
                        contact.getDomain()
                )
        );
    }

    private boolean isEmailDateInPast(RecipientDto recipient) {
        return recipient.getInitialEmailDate().isBefore(ZonedDateTime.now());
    }

    private RecipientDto mergeRecipients(RecipientDto existingRecipient, RecipientDto newRecipient) {
        return new RecipientDto.Builder()
                .fromExisting(existingRecipient)
                .mergeWith(newRecipient)
                .build();
    }

    private List<Contact> fetchContactsFromSpreadsheet(List<SpreadsheetReference> spreadsheetReferences) throws RecipientServiceException {
        try {
            return spreadsheetService.retrieveContacts(spreadsheetReferences);
        } catch (IOException |SpreadsheetOperationException e) {
            throw new RecipientServiceException("Spreadsheet contact retrieval failed", e);
        }
    }


    private String getEmailAddressById(int recipientId) throws RecipientServiceException {
        try {
            LOGGER.info("Getting email address by id: " + recipientId);
            return recipientDao.findEmailAddressById(recipientId);
        } catch (RecipientDaoException e) {
            throw new RecipientServiceException("Failed to get email address with id: " + recipientId, e);
        }
    }

    /**
     * Prepares a single email for sending by replacing recipientId with emailAddress.
     *
     * @param emailDto the email DTO to prepare
     * @return the prepared email
     * @throws EmailPreparationException if an error occurs during email preparation
     */
    private Email prepareEmailForSending(EmailDto emailDto) throws EmailPreparationException {
        try {
            String emailAddress = getEmailAddressById(emailDto.getRecipientId());
            Email email = EntityMapper.toEmail(emailDto);
            email.setRecipientEmail(emailAddress);
            return email;
        } catch (RecipientServiceException e)  {
            LOGGER.log(Level.SEVERE, "Failed to retrieve email address for recipient ID:" + emailDto.getRecipientId(), e);
            throw new EmailPreparationException("Failed to prepare email for sending", e);
        }
    }
}

class UserInteractionService extends AbstractUserConsoleInteractionService {

    /**
     * This method interacts with the user to prompt them, whether to change the data from the existing recipient to new recipient
     */
    boolean promptContactUpdate(RecipientDto existingRecipient, RecipientDto newRecipient) {
        displayDifferenceBetweenRecipients(existingRecipient, newRecipient);

        System.out.println("Do you want to update this contact? (y/n)");
        String response = scanner.next().trim().toLowerCase();

        return response.equals("y") || response.equals("yes");
    }

    private void displayDifferenceBetweenRecipients(RecipientDto existingRecipient, RecipientDto newRecipient) {
        System.out.println("Existing Contact Found:");

        if (!existingRecipient.getName().equals(newRecipient.getName())) {
            System.out.println("Name: " + existingRecipient.getName() + " -> " + newRecipient.getName());
        }
        if (!existingRecipient.getEmailAddress().equals(newRecipient.getEmailAddress())) {
            System.out.println("Email Address: " + existingRecipient.getEmailAddress() + " -> " + newRecipient.getEmailAddress());
        }
        if (!existingRecipient.getDomain().equals(newRecipient.getDomain())) {
            System.out.println("Domain: " + existingRecipient.getDomain() + " -> " + newRecipient.getDomain());
        }
        if (!existingRecipient.getPhoneNumber().equals(newRecipient.getPhoneNumber())) {
            System.out.println("Phone Number: " + existingRecipient.getPhoneNumber() + " -> " + newRecipient.getPhoneNumber());
        }
        if (existingRecipient.hasReplied() != newRecipient.hasReplied()) {
            System.out.println("Has Replied: " + existingRecipient.hasReplied() + " -> " + newRecipient.hasReplied());
        }
        if (existingRecipient.getSpreadsheetRow() != newRecipient.getSpreadsheetRow()) {
            System.out.println("Spreadsheet Row: " + existingRecipient.getSpreadsheetRow() + " -> " + newRecipient.getSpreadsheetRow());
        }
    }
}