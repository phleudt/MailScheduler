package com.mailscheduler.service;

import com.mailscheduler.config.Configuration;
import com.mailscheduler.database.DatabaseManager;
import com.mailscheduler.database.dao.EmailDao;
import com.mailscheduler.database.dao.EmailTemplateDao;
import com.mailscheduler.database.entities.EmailEntity;
import com.mailscheduler.dto.EmailDto;
import com.mailscheduler.dto.RecipientDto;
import com.mailscheduler.email.EmailSchedulingManager;
import com.mailscheduler.email.EmailSender;
import com.mailscheduler.email.EmailTemplateManager;
import com.mailscheduler.exception.*;
import com.mailscheduler.exception.dao.EmailDaoException;
import com.mailscheduler.exception.service.EmailSchedulingException;
import com.mailscheduler.exception.service.EmailServiceException;
import com.mailscheduler.exception.service.EmailValidationException;
import com.mailscheduler.exception.validation.EmailNotScheduledException;
import com.mailscheduler.exception.validation.EmailNotSentException;
import com.mailscheduler.google.GmailService;
import com.mailscheduler.mapper.EntityMapper;
import com.mailscheduler.model.Email;
import com.mailscheduler.model.EmailCategory;
import com.mailscheduler.model.SpreadsheetReference;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service responsible for managing email operations including sending, scheduling, and synchronization.
 * This service handles:
 * - Email sending and scheduling
 * - Email status tracking and updates
 * - Synchronization with spreadsheet data
 * - Management of follow-up emails
 *
 * @author phleudt
 * @version 1.0
 */
public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private final EmailSender emailSender;
    private final EmailSchedulingManager schedulingManager;
    private final SpreadsheetService spreadsheetService;
    private final EmailDao emailDao;
    private final EmailValidationService emailValidationService;
    private final Configuration configuration;
    private final SpreadsheetReference emailAddressColumn;

    /**
     * Constructs a new EmailService with the necessary dependencies.
     *
     * @param gmailService Service for interacting with Gmail API
     * @param spreadsheetService Service for spreadsheet operations
     * @param configuration Service configuration parameters
     * @param emailTemplateDao DAO for email template operations
     * @throws RuntimeException if service initialization fails
     */
    public EmailService(
            GmailService gmailService,
            SpreadsheetService spreadsheetService,
            Configuration configuration,
            EmailTemplateDao emailTemplateDao
    ) {
        try {
            this.emailDao = new EmailDao(DatabaseManager.getInstance());
            this.emailSender = new EmailSender(gmailService, configuration.isSaveMode());
            this.schedulingManager = new EmailSchedulingManager(emailDao, new EmailTemplateManager(gmailService, emailTemplateDao), spreadsheetService, configuration.getDefaultSender());
            this.spreadsheetService = spreadsheetService;
            this.emailValidationService = new EmailValidationService();

            this.configuration = configuration;
            this.emailAddressColumn = configuration.getContactColumns().get("emailAddress");
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize EmailService: " + e.getMessage());
            throw new RuntimeException("EmailService initialization failed", e);
        }
    }

    /**
     * Sends a single email and updates its status in the database.
     * Handles different sending outcomes including successful delivery,
     * recipient replies, and sending failures.
     *
     * @param email The email to send
     * @throws EmailNotSentException if the email cannot be sent
     */
    public void sendEmail(Email email) throws EmailNotSentException {
        try {
            emailValidationService.validateSending(email);
            EmailSender.EmailSendResult sendResult = emailSender.sendEmail(email);

            switch (sendResult.status()) {
                case SENT -> handleSuccessfulSend(email, sendResult);
                case ALREADY_REPLIED -> markRecipientAsReplied(email);
                case SENDING_ERROR -> throw new EmailNotSentException("Failed to send email");
            }
        } catch (EmailNotSentException e) {
            LOGGER.severe("Email sending failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.severe("Unexpected error during email send: " + e.getMessage());
            throw new EmailNotSentException("Unexpected error while sending email", e);
        }
    }

    /**
     * Sends a batch of emails, handling initial emails and follow-ups separately.
     * Updates spreadsheet tracking information after sending.
     *
     * @param emails List of emails to send
     */
    public void sendEmails(List<Email> emails) {
        LOGGER.info("Processing batch of " + emails.size() + " emails for sending");

        Map<EmailCategory, List<Email>> categorizedEmails = categorizeEmails(emails);

        sendInitialEmails(categorizedEmails.get(EmailCategory.INITIAL));
        sendFollowUpEmails(categorizedEmails.get(EmailCategory.FOLLOW_UP));

        updateSpreadsheetTracking(categorizedEmails);

    }

    /**
     * Schedules emails for a list of recipients, including follow-up emails if configured.
     * Updates spreadsheet with scheduling information.
     *
     * @param recipients List of recipients to schedule emails for
     * @return Number of emails scheduled (including follow-ups)
     * @throws EmailNotScheduledException if scheduling fails
     */
    public int scheduleEmailsForRecipients(List<RecipientDto> recipients) throws EmailNotScheduledException {
        try {
            var result = schedulingManager.scheduleEmailsForRecipients(recipients);
            if (result == null) {
                return 0;
            }

            validateScheduledEmails(result);
            updateSpreadsheetWithSchedulingInfo(result, recipients);

            return result.initialEmails().size() + result.followupEmails().size();
        } catch (EmailSchedulingException e) {
            throw new EmailNotScheduledException("Email scheduling failed", e);
        }
    }

    /**
     * Retrieves all pending emails that are ready to be sent.
     * Filters and sorts emails to ensure proper sending order.
     *
     * @return List of email DTOs ready for sending
     * @throws EmailServiceException if retrieval fails
     */
    public List<EmailDto> getPendingEmailsReadyToSend() throws EmailServiceException {
        try {
            List<EmailEntity> pendingEntities = emailDao.findPendingEmailsToSend();
            List<EmailDto> emailDtos = mapToEmailDtos(pendingEntities);

            List<List<EmailDto>> sortedEmails = sortEmailsByRecipientIdAndFollowUpNumber(emailDtos);
            List<EmailDto> pendingEmails = selectNextEmailsToSend(sortedEmails);

            return filterEmailsByPastDate(pendingEmails);
        } catch (EmailDaoException | MappingException e) {
            throw new EmailServiceException("Failed to retrieve pending emails", e);
        }
    }

    private List<EmailDto> mapToEmailDtos(List<EmailEntity> emailEntities) throws MappingException {
        List<EmailDto> emailDtos = new ArrayList<>(emailEntities.size());
        for (EmailEntity entity : emailEntities) {
            emailDtos.add(EntityMapper.toEmailDto(entity));
        }
        return emailDtos;
    }

    private List<EmailDto> selectNextEmailsToSend(List<List<EmailDto>> listOfEmailDtos) {
        List<EmailDto> pendingEmails = new ArrayList<>();

        for (List<EmailDto> pendingDtos : listOfEmailDtos) {
            // Checking, if the email before has been already send, or is still pending
            // Filter emails to ensure that an email is only sent if the previous follow-up email has been sent
            // -> if this case occurs then update the send date for the now being sent one and the one after that to be correct again
            pendingEmails.add(pendingDtos.get(0));
        }

        return pendingEmails;
    }

    /**
     * Synchronizes email status between spreadsheet and database.
     * Retrieves and updates information about already scheduled and sent emails.
     *
     * @throws EmailServiceException if synchronization fails
     */
    public void retrieveAndSyncAlreadyScheduledAndSentEmails() throws EmailServiceException {
        List<Email> spreadsheetEmails = retrieveAlreadyScheduledAndSentEmailsFromSpreadsheet();
        if (spreadsheetEmails.isEmpty()) return;

        try {
            updateRecipientsForRetrievedEmails(spreadsheetEmails);
            List<Email> newEmails = filterNewEmails(spreadsheetEmails);
            saveNewEmailsToDatabase(newEmails);
        } catch (Exception e) {
            throw new EmailServiceException("Failed to sync email status", e);
        }
    }

    // Helper methods

    private void handleSuccessfulSend(Email email, EmailSender.EmailSendResult result) throws EmailServiceException {
        email.setThreadId(result.message().getThreadId());
        email.setStatus("SENT");
        updateEmailInDatabase(email);
        updateThreadIdForScheduledFollowUps(email);
    }

    private Map<EmailCategory, List<Email>> categorizeEmails(List<Email> emails) {
        return emails.stream()
                .collect(Collectors.groupingBy(Email::getEmailCategory));
    }

    private void sendInitialEmails(List<Email> initialEmails) {
        for (Email initialEmail : initialEmails) {
            try {
                sendEmail(initialEmail);
            } catch (EmailNotSentException e) {
                LOGGER.severe("Failed to send email: " + e.getMessage());
            }
        }
    }

    private void sendFollowUpEmails(List<Email> followUpEmails) {
        if (followUpEmails == null) return;

        for (Email email : followUpEmails) {
            try {
                Email emailToSend = email.getThreadId() == null ?
                        reloadEmailFromDatabase(email.getId()) : email;
                emailToSend.setRecipientEmail(email.getRecipientEmail());
                sendEmail(emailToSend);
            } catch (EmailServiceException | EmailNotSentException e) {
                LOGGER.severe("Failed to send follow-up email: " + e.getMessage());
            }
        }
    }

    private void updateSpreadsheetTracking(Map<EmailCategory, List<Email>> categorizedEmails) {
        updateInitialEmailTracking(categorizedEmails.get(EmailCategory.INITIAL));
        updateFollowUpEmailTracking(categorizedEmails.get(EmailCategory.FOLLOW_UP));

    }

    private void updateInitialEmailTracking(List<Email> initialEmails) {
        try {
            spreadsheetService.markEmailsAsSent(emailAddressColumn, initialEmails, getInitialEmailColumnToMark());
        } catch (SpreadsheetOperationException e) {
            LOGGER.severe("Failed to mark initial emails as sent: " + e.getMessage());
        }
    }

    private void updateFollowUpEmailTracking(List<Email> followUpEmails) {
        try {
            if (followUpEmails.isEmpty()) return;
            for (int i = 1; i <= getMaxFollowupNumber(); i++) {
                final int followUpNumber = i;
                List<Email> followUpEmailsToMark = followUpEmails.stream()
                        .filter(email -> email.getFollowupNumber() == followUpNumber)
                        .toList();
                try {
                    spreadsheetService.markEmailsAsSent(emailAddressColumn, followUpEmailsToMark, getFollowUpEmailColumnToMark(followUpNumber));
                } catch (SpreadsheetOperationException e) {
                    LOGGER.severe("Failed to mark " + followUpNumber + ". followup emails as sent: " + e.getMessage());
                }
            }
        } catch (EmailServiceException e) {
            LOGGER.severe("Failed to markFollowup email: " + e.getMessage());
        }
    }

    private void validateScheduledEmails(EmailSchedulingManager.ScheduledEmailsResult result)
            throws EmailNotScheduledException {
        try {
            for (Email email : result.initialEmails()) {
                emailValidationService.validateScheduling(email);
            }
            for (Email email : result.followupEmails()) {
                emailValidationService.validateScheduling(email);
            }
        } catch (EmailValidationException e) {
            throw new EmailNotScheduledException("Email validation failed during scheduling", e);
        }
    }

    private void updateSpreadsheetWithSchedulingInfo(
            EmailSchedulingManager.ScheduledEmailsResult result,
            List<RecipientDto> recipients
    ) throws EmailNotScheduledException {
        try {
            List<SpreadsheetReference> cellIndicesContainingEmailAddresses = spreadsheetService.markEmailsAsScheduled(emailAddressColumn, result.initialEmails(), getInitialEmailColumnToMark());
            List<String> schedules = recipients.stream()
                    .map(RecipientDto::getInitialEmailDateAsString)
                    .toList();
            spreadsheetService.markSchedule(cellIndicesContainingEmailAddresses, getInitialScheduleColumnToMark(), schedules);
        } catch (SpreadsheetOperationException e) {
            LOGGER.severe("Failed to mark initial emails as scheduled: " + e.getMessage());
        }

        try {
            List<Email> followUpEmails = result.followupEmails();
            for (Email email : followUpEmails) {
                emailValidationService.validateScheduling(email);
            }
            if (!followUpEmails.isEmpty()) {
                for (int i = 1; i <= getMaxFollowupNumber(); i++) {
                    final int followUpNumber = i;
                    List<Email> followUpEmailsToMark = followUpEmails.stream()
                            .filter(email -> email.getFollowupNumber() == followUpNumber)
                            .toList();
                    List<SpreadsheetReference> cellIndicesContainingEmailAddresses = spreadsheetService.markEmailsAsScheduled(emailAddressColumn, followUpEmailsToMark, getFollowUpEmailColumnToMark(followUpNumber));

                    SpreadsheetReference referenceColumn = getFollowUpReferenceColumnToMark(i);
                    spreadsheetService.markSchedule(cellIndicesContainingEmailAddresses, getFollowUpScheduleColumnToMark(followUpNumber), referenceColumn, emailDao.getIntervalDaysForFollowupNumber(i));
                }
            }
        } catch (EmailValidationException e) {
            throw new EmailNotScheduledException("Failed to schedule email", e);
        } catch (EmailServiceException | SpreadsheetOperationException e) {
            LOGGER.severe("Failed to mark follow-up email: " + e.getMessage());
        } catch (EmailDaoException e) {
            throw new EmailNotScheduledException("Failed to retrieve follow-up interval days", e);
        }
    }

    private void saveNewEmailsToDatabase(List<Email> emails) throws SQLException, MappingException, EmailDaoException {
        emailDao.beginTransaction();
        try {
            for (Email email : emails) {
                emailDao.insertEmail(EntityMapper.toEmailEntity(EntityMapper.toEmailDto(email, 1)));
            }
            emailDao.commitTransaction();
        } catch (Exception e) {
            emailDao.rollbackTransaction();
            throw e;
        }
    }

    private List<Email> filterNewEmails(List<Email> emails) throws EmailDaoException {
        return emails.stream()
                .filter(email -> {
                    try {
                        return !hasEmailReferenceForRecipient(email.getRecipientId());
                    } catch (EmailDaoException e) {
                        LOGGER.warning("Failed to check email reference: " + e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private void markRecipientAsReplied(Email email) throws EmailServiceException {
        try {
            LOGGER.info("Updating recipient status for email: " + email.toString() + " in database");
            emailDao.markRecipientAsReplied(email.getRecipientId());
        } catch (EmailDaoException e) {
            LOGGER.severe("Failed to mark recipient as replied in database: " + e.getMessage());
            throw new EmailServiceException("Failed to mark recipient as replied in database", e);
        }

    }


    private List<List<EmailDto>> sortEmailsByRecipientIdAndFollowUpNumber(List<EmailDto> emailDtos) {
        Map<Integer, List<EmailDto>> mappedEmailDtos = new HashMap<>();
        for (EmailDto emailDto : emailDtos) {
            Integer recipientId = emailDto.getRecipientId();
            mappedEmailDtos.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(emailDto);
        }

        List<List<EmailDto>> sortedEmailDtos = new ArrayList<>();
        for (List<EmailDto> toBeSorted : mappedEmailDtos.values()) {
            toBeSorted.sort(Comparator.comparingInt(EmailDto::getFollowupNumber));
            sortedEmailDtos.add(toBeSorted);
        }

        return sortedEmailDtos;
    }

    private void updateEmailInDatabase(Email email) throws EmailServiceException {
        // This methods updates the email by the id
        try {
            LOGGER.info("Updating email: " + email.toString() + " in database");
            // EmailDto emailDto = EntityMapper.toEmailDto(email, scheduleId, initialEmailId, recipientId);
            EmailDto emailDto = EntityMapper.toEmailDto(email, 1);

            emailDao.updateEmailById(email.getId(), EntityMapper.toEmailEntity(emailDto));
        } catch (EmailDaoException | MappingException e) {
            LOGGER.info("Failed to save email to database: " + e.getMessage());
            throw new EmailServiceException("Failed to save email to database: " + e.getMessage(), e);
        }
    }

    private void updateThreadIdForScheduledFollowUps(Email email) throws EmailServiceException {
        // This method updates the thread_id by all followups that are scheduled, when the initial email is sent
        try {
            LOGGER.info("Updating thread_id for all followups in database for email: " + email.toString());
            List<EmailEntity> followUpEmails = emailDao.getFollowUpEmailsByInitialEmailId(email.getId());
            for (EmailEntity followUpEmail : followUpEmails) {
                followUpEmail.setThread_id(email.getThreadId());
                emailDao.updateEmailById(followUpEmail.getId(), followUpEmail);
            }
        } catch (EmailDaoException e) {
            LOGGER.severe("Failed to update thread_id for followups: " + e.getMessage());
            throw new EmailServiceException("Failed to update thread_id for followups", e);
        }
    }

    private Email reloadEmailFromDatabase(int emailId) throws EmailServiceException {
        try {
            EmailEntity emailEntity = emailDao.findById(emailId);
            return EntityMapper.toEmail(EntityMapper.toEmailDto(emailEntity));
        } catch (EmailDaoException | MappingException e) {
            LOGGER.severe("Failed to reload email from database: " + e.getMessage());
            throw new EmailServiceException("Failed to reload email from database", e);
        }
    }

    public Email getInitialEmailByRecipientId(int recipientId) {
        try {
            EmailEntity emailEntity = emailDao.getInitialEmailFromRecipientId(recipientId);
            return emailEntity != null ? EntityMapper.toEmail(EntityMapper.toEmailDto(emailEntity)) : null;
        } catch (EmailDaoException | MappingException e) {
            // throw new EmailServiceException("Failed to retrieve initial email for recipient ID: " + recipientId, e);
            System.out.println("ERROR HIER");
            LOGGER.severe("Failed to retrieve initial email for recipient ID: ");
        }
        return null;
    }

    private List<EmailDto> filterEmailsByPastDate(List<EmailDto> emailDtos) {
        return emailDtos.stream()
                .filter(emailDto -> emailDto.getScheduledDate().isBefore(ZonedDateTime.now()))
                .collect(Collectors.toList());
    }

    private SpreadsheetReference getInitialEmailColumnToMark() {
        return configuration.getMarkEmailColumns().get("INITIAL");
    }

    private SpreadsheetReference getFollowUpEmailColumnToMark(int followupNumber) {
        return configuration.getMarkEmailColumns().get("FOLLOW_UP_" + followupNumber);
    }

    private SpreadsheetReference getFollowUpReferenceColumnToMark(int followupNumber) {
        return followupNumber - 1 == 0 ?
                configuration.getMarkSchedulesForEmailColumns().get("INITIAL") :
                configuration.getMarkSchedulesForEmailColumns().get("FOLLOW_UP_" + (followupNumber - 1));
    }

    private SpreadsheetReference getInitialScheduleColumnToMark() {
        return configuration.getMarkSchedulesForEmailColumns().get("INITIAL");
    }

    private SpreadsheetReference getFollowUpScheduleColumnToMark(int followupNumber) {
        return configuration.getMarkSchedulesForEmailColumns().get("FOLLOW_UP_" + followupNumber);
    }

    private int getMaxFollowupNumber() throws EmailServiceException {
        try {
            return emailDao.getMaxFollowupNumberForSchedule(1);
        } catch (EmailDaoException e) {
            throw new EmailServiceException("Failed to get the maximum followup number", e);
        }
    }


    private List<Email> retrieveAlreadyScheduledAndSentEmailsFromSpreadsheet() throws EmailServiceException {
        try {
            return spreadsheetService.retrieveScheduledAndSentEmails(
                    getColumnsFromMap(configuration.getMarkEmailColumns()),
                    getColumnsFromMap(configuration.getMarkSchedulesForEmailColumns()),
                    configuration.getContactColumns().get("emailAddress")
            );
        } catch (SpreadsheetOperationException e) {
            throw new EmailServiceException("Failed to retrieve already scheduled and sent emails from spreadsheet", e);
        }
    }

    private List<SpreadsheetReference> getColumnsFromMap(Map<String, SpreadsheetReference> map) {
        return map.values().stream().toList();
    }

    private boolean hasEmailReferenceForRecipient(int recipientId) throws EmailDaoException {
        return emailDao.hasEmailReferenceForRecipient(recipientId) > 0;
    }

    /**
     * Updates the recipientId for emails retrieved from the spreadsheet by matching email addresses.
     *
     * @param emails the list of emails to update
     * @return the list of emails with updated recipientIds
     * @throws EmailServiceException if an error occurs during recipient ID retrieval
     */
    private List<Email> updateRecipientsForRetrievedEmails(List<Email> emails) throws EmailServiceException {
        for (Email email : emails) {
            try {
                // Retrieve the recipient ID based on the email address
                int recipientId = getRecipientIdByEmail(email.getRecipientEmail());

                email.setRecipientId(recipientId);
            } catch (Exception e) {
                LOGGER.severe("Failed to retrieve recipient ID for email address: " + email.getRecipientEmail());
                throw new EmailServiceException("Failed to update recipient ID for retrieved emails", e);
            }
        }

        return emails;
    }

    /**
     * Retrieves the recipient ID based on the email address.
     *
     * @param emailAddress the email address to look up
     * @return the corresponding recipient ID
     * @throws EmailServiceException if the recipient ID cannot be found
     */
    private int getRecipientIdByEmail(String emailAddress) throws EmailServiceException {
        try {
            return emailDao.getRecipientIdByEmail(emailAddress);
        } catch (Exception e) {
            LOGGER.severe("Failed to retrieve recipient ID for email: " + emailAddress);
            throw new EmailServiceException("Could not find recipient ID for email address", e);
        }
    }
}