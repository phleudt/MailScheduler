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
import com.mailscheduler.model.Recipient;
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
    private static final String EMAIL_VALIDATION_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final RecipientDao recipientDao;
    private final SpreadsheetService spreadsheetService;
    private final UserInteractionService userInteractionService;
    private final Map<String, SpreadsheetReference> recipientSpreadsheetReference; // Currently only SpreadsheetReference.Type.COLUMN supported
    private final List<SendingCriterion> sendingCriteria;

    /**
     * Constructs a new RecipientSynchronizationService with the specified dependencies.
     *
     * @param spreadsheetService Service for interacting with spreadsheets
     * @param configuration Contains configuration settings for recipient columns and sending criteria
     */
    public RecipientSynchronizationService(SpreadsheetService spreadsheetService, Configuration configuration) {
        this.recipientDao = new RecipientDao(DatabaseManager.getInstance());
        this.spreadsheetService = spreadsheetService;
        this.userInteractionService = new UserInteractionService();
        this.recipientSpreadsheetReference = configuration.getRecipientColumns();
        this.sendingCriteria = configuration.getSendingCriteria();

    }

    /**
     * Synchronizes recipient data from configured spreadsheet columns to the database.
     * This method performs the following steps:
     * 1. Collects all relevant spreadsheet references
     * 2. Fetches recipients from the spreadsheet
     * 3. Processes and updates the database with recipient information
     *
     * @throws RecipientServiceException if synchronization fails
     */
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

    /**
     * Processes a list of recipient, updating existing recipient or creating new ones as needed.
     * Handles database transactions and rollback in case of errors.
     *
     * @param recipients List of recipient to process
     * @throws RecipientServiceException if recipient processing fails
     */
    private void processRecipientUpdates(List<Recipient> recipients) throws RecipientServiceException {
        try {
            recipientDao.beginTransaction();

            for (Recipient recipient : recipients) {
                Optional<RecipientEntity> existingRecipients = findExistingRecipient(recipient);

                if (existingRecipients.isPresent()) {
                    updateExistingRecipient(recipient, existingRecipients.get());
                } else if (isValidRecipient(recipient)){
                    persistNewRecipient(recipient);
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

    private boolean isValidRecipient(Recipient recipient) {
        return recipient.getEmailAddress() != null &&
                recipient.getEmailAddress().matches(EMAIL_VALIDATION_REGEX);
    }

    private void handleTransactionError(Exception e) throws RecipientServiceException {
        LOGGER.log(Level.SEVERE, "Failed to process recipient", e);
        try {
            recipientDao.rollbackTransaction();
        } catch (SQLException rollbackEx) {
            LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
        }
        throw new RecipientServiceException("Recipient processing failed", e);
    }

    private void updateExistingRecipient(Recipient newRecipient, RecipientEntity existingRecipient)
            throws RecipientDaoException, MappingException {
        RecipientDto existingDto = EntityMapper.toRecipientDto(existingRecipient);
        RecipientDto newDto = EntityMapper.toRecipientDto(newRecipient);

        if (existingDto.equals(newDto)) {
            return;
        }

        // Check if the only difference is the initial email date
        if (existingDto.equalsWithoutInitialEmailDate(newDto)) {
            RecipientDto updatedRecipient = new RecipientDto.Builder()
                    .fromExisting(existingDto)
                    .setInitialEmailDate(newDto.getInitialEmailDate())
                    .build();
            recipientDao.updateRecipientById(
                    updatedRecipient.getId(),
                    EntityMapper.toRecipientEntity(updatedRecipient)
            );
            return;
        }

        if (userInteractionService.promptRecipientUpdate(existingDto, newDto)) {
            RecipientDto mergeRecipient = mergeRecipients(existingDto, newDto);
            recipientDao.updateRecipientById(
                    mergeRecipient.getId(),
                    EntityMapper.toRecipientEntity(mergeRecipient)
            );
        }
    }

    private void persistNewRecipient(Recipient recipient) throws RecipientDaoException, MappingException {
        RecipientDto recipientDto = EntityMapper.toRecipientDto(recipient);
        recipientDao.insertRecipient(EntityMapper.toRecipientEntity(recipientDto));
    }

    private Optional<RecipientEntity> findExistingRecipient(Recipient recipient) throws RecipientDaoException {
        return Optional.ofNullable(
                recipientDao.findRecipientByUniqueIdentifiers(
                        recipient.getEmailAddress(),
                        recipient.getName(),
                        recipient.getPhoneNumber(),
                        recipient.getDomain()
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


    private List<Recipient> fetchRecipientsFromSpreadsheet(List<SpreadsheetReference> spreadsheetReferences) throws RecipientServiceException {
        try {
            return spreadsheetService.retrieveRecipient(spreadsheetReferences);
        } catch (IOException |SpreadsheetOperationException e) {
            throw new RecipientServiceException("Spreadsheet recipient retrieval failed", e);
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
    boolean promptRecipientUpdate(RecipientDto existingRecipient, RecipientDto newRecipient) {
        displayDifferenceBetweenRecipients(existingRecipient, newRecipient);

        System.out.println("Do you want to update this recipient? (y/n)");
        String response = scanner.next().trim().toLowerCase();

        return response.equals("y") || response.equals("yes");
    }

    private void displayDifferenceBetweenRecipients(RecipientDto existingRecipient, RecipientDto newRecipient) {
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
        if (existingRecipient.getInitialEmailDateAsString() != newRecipient.getInitialEmailDateAsString()) {
            System.out.println("Initial Email date: " + existingRecipient.getInitialEmailDateAsString() + " -> " + newRecipient.getInitialEmailDateAsString());
        }
    }
}