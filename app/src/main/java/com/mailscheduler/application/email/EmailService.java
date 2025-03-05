package com.mailscheduler.application.email;

import com.mailscheduler.application.email.scheduling.EmailSchedulingService;
import com.mailscheduler.application.email.sending.EmailSendingService;
import com.mailscheduler.application.email.validation.EmailValidationService;
import com.mailscheduler.common.config.Configuration;
import com.mailscheduler.common.exception.SpreadsheetOperationException;
import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.email.EmailId;
import com.mailscheduler.domain.email.EmailStatus;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.domain.recipient.RecipientId;
import com.mailscheduler.domain.schedule.ScheduleId;
import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.application.template.TemplateManager;
import com.mailscheduler.common.exception.service.EmailServiceException;
import com.mailscheduler.common.exception.service.EmailValidationException;
import com.mailscheduler.common.exception.validation.EmailNotScheduledException;
import com.mailscheduler.common.exception.validation.EmailNotSentException;
import com.mailscheduler.infrastructure.google.gmail.GmailService;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.domain.email.EmailCategory;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.application.spreadsheet.SpreadsheetService;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteEmailRepository;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteTemplateRepository;

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

    private final EmailSendingService emailSendingService;
    private final EmailSchedulingService schedulingManager;
    private final SpreadsheetService spreadsheetService;
    private final SQLiteEmailRepository emailRepository;
    private final EmailValidationService emailValidationService;
    private final Configuration configuration;
    private final SpreadsheetReference emailAddressColumn;

    public EmailService(
            GmailService gmailService,
            SpreadsheetService spreadsheetService,
            Configuration configuration,
            SQLiteTemplateRepository templateRepository
    ) {
        try {
            this.emailRepository = new SQLiteEmailRepository(DatabaseManager.getInstance());
            this.emailSendingService = new EmailSendingService(gmailService, configuration.isSaveMode());
            this.schedulingManager = new EmailSchedulingService(
                    emailRepository,
                    new TemplateManager(gmailService, templateRepository),
                    spreadsheetService,
                    EmailAddress.of(configuration.getDefaultSender())
            );
            this.spreadsheetService = spreadsheetService;
            this.emailValidationService = new EmailValidationService();

            this.configuration = configuration;
            this.emailAddressColumn = configuration.getRecipientColumns().get("emailAddress");
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize EmailService: " + e.getMessage());
            throw new RuntimeException("EmailService initialization failed", e);
        }
    }

    public void sendEmail(Email email) throws EmailNotSentException {
        try {
            emailValidationService.validateSending(email);
            EmailSendingService.EmailSendResult sendResult = emailSendingService.sendEmail(email);

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

    public void sendEmails(List<Email> emails) {
        LOGGER.info("Processing batch of " + emails.size() + " emails for sending");

        Map<EmailCategory, List<Email>> categorizedEmails = categorizeEmails(emails);

        sendInitialEmails(categorizedEmails.get(EmailCategory.INITIAL));
        sendFollowUpEmails(categorizedEmails.get(EmailCategory.FOLLOW_UP));

        updateSpreadsheetTracking(categorizedEmails);

    }

    public int scheduleEmailsForRecipients(List<Recipient> recipients) throws EmailNotScheduledException {
        var result = schedulingManager.scheduleEmailsForRecipients(recipients);
        if (result.isEmpty()) {
            return 0;
        }

        validateScheduledEmails(result);
        updateSpreadsheetWithSchedulingInfo(result, recipients);

        return result.initialEmails().size() + result.followupEmails().size();
    }

    public List<Email> getPendingEmailsReadyToSend() throws EmailServiceException {
        try {
            List<Email> pendingEmails = emailRepository.findPendingEmailsToSend();

            List<List<Email>> sortedEmails = sortEmailsByRecipientIdAndFollowUpNumber(pendingEmails);
            List<Email> pendingEmails1 = selectNextEmailsToSend(sortedEmails);

            return filterEmailsByPastDate(pendingEmails1);
        } catch (RepositoryException e) {
            throw new EmailServiceException("Failed to retrieve pending emails", e);
        }
    }

    private List<Email> selectNextEmailsToSend(List<List<Email>> listOfEmails) {
        List<Email> pendingEmails = new ArrayList<>();

        for (List<Email> pendingEmails1 : listOfEmails) {
            // Checking, if the email before has been already send, or is still pending
            // Filter emails to ensure that an email is only sent if the previous follow-up email has been sent
            // -> if this case occurs then update the send date for the now being sent one and the one after that to be correct again
            pendingEmails.add(pendingEmails1.get(0));
        }

        return pendingEmails;
    }

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
    private void handleSuccessfulSend(Email email, EmailSendingService.EmailSendResult result) throws EmailServiceException {
        email.setThreadId(result.message().getThreadId());
        email.setStatus(EmailStatus.SENT);
        updateEmailInDatabase(email);
        updateThreadIdForScheduledFollowUps(email);
    }

    private Map<EmailCategory, List<Email>> categorizeEmails(List<Email> emails) {
        return emails.stream()
                .collect(Collectors.groupingBy(Email::getCategory));
    }

    private void sendInitialEmails(List<Email> initialEmails) {
        if (initialEmails == null || initialEmails.isEmpty()) return;
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
                Optional<Email> emailToSend = email.getThreadId().isEmpty() ?
                        reloadEmailFromDatabase(email.getId()) : Optional.of(email);
                if (emailToSend.isPresent()) {
                    emailToSend.get().setRecipient(email.getRecipient());
                    sendEmail(emailToSend.get());
                }
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
            if (initialEmails == null || initialEmails.isEmpty()) return;
            spreadsheetService.markEmailsAsSent(emailAddressColumn, initialEmails, getInitialEmailColumnToMark());
        } catch (SpreadsheetOperationException e) {
            LOGGER.severe("Failed to mark initial emails as sent: " + e.getMessage());
        }
    }

    private void updateFollowUpEmailTracking(List<Email> followUpEmails) {
        try {
            if (followUpEmails == null || followUpEmails.isEmpty()) return;
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

    private void validateScheduledEmails(EmailSchedulingService.EmailSchedulingResult result)
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
            EmailSchedulingService.EmailSchedulingResult result,
            List<Recipient> recipients
    ) throws EmailNotScheduledException {
        try {
            List<SpreadsheetReference> cellIndicesContainingEmailAddresses = spreadsheetService.markEmailsAsScheduled(
                    emailAddressColumn, result.initialEmails(), getInitialEmailColumnToMark());

            Set<EmailAddress> initialEmailAddresses = result.initialEmails().stream()
                    .map(Email::getRecipient)
                    .collect(Collectors.toSet());

            // Filter schedules to only include recipients that have initial emails
            List<String> schedules = recipients.stream()
                    .filter(recipient -> initialEmailAddresses.contains(recipient.getEmailAddress()))
                    .map(Recipient::getInitialEmailDateAsString)
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
                    List<SpreadsheetReference> cellIndicesContainingEmailAddresses =
                            spreadsheetService.markEmailsAsScheduled(
                                    emailAddressColumn, followUpEmailsToMark, getFollowUpEmailColumnToMark(followUpNumber)
                            );

                    SpreadsheetReference referenceColumn = getFollowUpReferenceColumnToMark(i);
                    spreadsheetService.markSchedule(
                            cellIndicesContainingEmailAddresses,
                            getFollowUpScheduleColumnToMark(followUpNumber),
                            referenceColumn, emailRepository.getIntervalDaysForFollowupNumber(i)
                    );
                }
            }
        } catch (EmailValidationException e) {
            throw new EmailNotScheduledException("Failed to schedule email", e);
        } catch (EmailServiceException | SpreadsheetOperationException e) {
            LOGGER.severe("Failed to mark follow-up email: " + e.getMessage());
        } catch (RepositoryException e) {
            throw new EmailNotScheduledException("Failed to retrieve follow-up interval days", e);
        }
    }

    private void saveNewEmailsToDatabase(List<Email> emails) {
        for (Email email : emails) {
            try {
                emailRepository.save(email);
            } catch (RepositoryException ignored) {
                // TODO: Log
            }
        }
    }

    private List<Email> filterNewEmails(List<Email> emails) {
        return emails.stream()
                .filter(email -> {
                    try {
                        return !hasEmailReferenceForRecipient(email.getRecipientId());
                    } catch (RepositoryException e) {
                        LOGGER.warning("Failed to check email reference: " + e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private void markRecipientAsReplied(Email email) throws EmailServiceException {
        try {
            LOGGER.info("Updating recipient status for email: " + email.toString() + " in database");
            emailRepository.markRecipientAsReplied(email.getRecipientId());
        } catch (RepositoryException e) {
            LOGGER.severe("Failed to mark recipient as replied in database: " + e.getMessage());
            throw new EmailServiceException("Failed to mark recipient as replied in database", e);
        }
    }

    private List<List<Email>> sortEmailsByRecipientIdAndFollowUpNumber(List<Email> emails) {
        Map<RecipientId, List<Email>> mappedEmails = new HashMap<>();
        for (Email email : emails) {
            RecipientId recipientId = email.getRecipientId();
            mappedEmails.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(email);
        }

        List<List<Email>> sortedEmails = new ArrayList<>();
        for (List<Email> toBeSorted : mappedEmails.values()) {
            toBeSorted.sort(Comparator.comparingInt(Email::getFollowupNumber));
            sortedEmails.add(toBeSorted);
        }

        return sortedEmails;
    }

    private void updateEmailInDatabase(Email email) throws EmailServiceException {
        // This methods updates the email by the id
        try {
            LOGGER.info("Updating email: " + email.toString() + " in database");
            emailRepository.update(email);
        } catch (RepositoryException e) {
            LOGGER.info("Failed to save email to database: " + e.getMessage());
            throw new EmailServiceException("Failed to save email to database: " + e.getMessage(), e);
        }
    }

    private void updateThreadIdForScheduledFollowUps(Email email) throws EmailServiceException {
        // This method updates the thread_id by all followups that are scheduled, when the initial email is sent
        try {
            LOGGER.info("Updating thread_id for all followups in database for email: " + email.toString());
            List<Email> followUpEmails = emailRepository.getFollowUpEmailsByInitialEmailId(email.getId());
            for (Email followUpEmail : followUpEmails) {
                Optional<String> threadId = email.getThreadId();
                if (threadId.isPresent()) {
                    followUpEmail.setThreadId(threadId.get());
                    emailRepository.update(followUpEmail);
                }
            }
        } catch (RepositoryException e) {
            LOGGER.severe("Failed to update thread_id for followups: " + e.getMessage());
            throw new EmailServiceException("Failed to update thread_id for followups", e);
        }
    }

    private Optional<Email> reloadEmailFromDatabase(EmailId emailId) throws EmailServiceException {
        try {
            return emailRepository.findById(emailId.value());
        } catch (RepositoryException e) {
            LOGGER.severe("Failed to reload email from database: " + e.getMessage());
            throw new EmailServiceException("Failed to reload email from database", e);
        }
    }

    public Optional<Email> getInitialEmailByRecipientId(RecipientId recipientId) {
        try {
            return emailRepository.getInitialEmailFromRecipientId(recipientId);
        } catch (RepositoryException e) {
            LOGGER.severe("Failed to retrieve initial email for recipient ID: ");
        }
        return Optional.empty();
    }

    private List<Email> filterEmailsByPastDate(List<Email> emailDtos) {
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
            return emailRepository.getMaxFollowupNumberForSchedule(ScheduleId.of(1));
        } catch (RepositoryException e) {
            throw new EmailServiceException("Failed to get the maximum followup number", e);
        }
    }

    private List<Email> retrieveAlreadyScheduledAndSentEmailsFromSpreadsheet() throws EmailServiceException {
        try {
            return spreadsheetService.retrieveScheduledAndSentEmails(
                    getColumnsFromMap(configuration.getMarkEmailColumns()),
                    getColumnsFromMap(configuration.getMarkSchedulesForEmailColumns()),
                    configuration.getRecipientColumns().get("emailAddress")
            );
        } catch (SpreadsheetOperationException e) {
            throw new EmailServiceException("Failed to retrieve already scheduled and sent emails from spreadsheet", e);
        }
    }

    private List<SpreadsheetReference> getColumnsFromMap(Map<String, SpreadsheetReference> map) {
        return map.values().stream().toList();
    }

    private boolean hasEmailReferenceForRecipient(RecipientId recipientId) throws RepositoryException {
        return emailRepository.hasEmailReferenceForRecipient(recipientId) > 0;
    }

    private List<Email> updateRecipientsForRetrievedEmails(List<Email> emails) throws EmailServiceException {
        for (Email email : emails) {
            try {
                // Retrieve the recipient ID based on the email address
                RecipientId recipientId = getRecipientIdByEmail(email.getRecipient());

                email.setRecipientId(recipientId);
            } catch (Exception e) {
                LOGGER.severe("Failed to retrieve recipient ID for email address: " + email.getRecipient());
                throw new EmailServiceException("Failed to update recipient ID for retrieved emails", e);
            }
        }

        return emails;
    }

    private RecipientId getRecipientIdByEmail(EmailAddress emailAddress) throws EmailServiceException {
        try {
            return emailRepository.getRecipientIdByEmail(emailAddress);
        } catch (Exception e) {
            LOGGER.severe("Failed to retrieve recipient ID for email: " + emailAddress);
            throw new EmailServiceException("Could not find recipient ID for email address", e);
        }
    }
}