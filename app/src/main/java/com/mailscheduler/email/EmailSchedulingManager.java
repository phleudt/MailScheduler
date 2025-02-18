package com.mailscheduler.email;

import com.mailscheduler.database.dao.EmailDao;
import com.mailscheduler.database.entities.EmailEntity;
import com.mailscheduler.dto.EmailDto;
import com.mailscheduler.dto.RecipientDto;
import com.mailscheduler.exception.*;
import com.mailscheduler.exception.dao.EmailDaoException;
import com.mailscheduler.exception.service.EmailSchedulingException;
import com.mailscheduler.mapper.EntityMapper;
import com.mailscheduler.model.*;
import com.mailscheduler.service.SpreadsheetService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailSchedulingManager {
    private final Logger LOGGER = Logger.getLogger(EmailSchedulingManager.class.getName());

    private final EmailDao emailDao;
    private final EmailTemplateManager templateManager;
    private final EmailSchedulingService emailSchedulingService;
    private final SpreadsheetService spreadsheetService;
    private final String defaultSenderEmail;

    public EmailSchedulingManager(
            EmailDao emailDao,
            EmailTemplateManager templateManager,
            SpreadsheetService spreadsheetService,
            String defaultSenderEmail
    ) {
        this.emailDao = emailDao;
        this.templateManager = templateManager;
        this.emailSchedulingService = new EmailSchedulingService(emailDao);
        this.spreadsheetService = spreadsheetService;
        this.defaultSenderEmail = defaultSenderEmail;

    }

    /**
     * Schedule emails for multiple recipients.
     *
     * @param recipientDtos List of recipients to schedule emails for
     * @return Scheduled emails result
     * @throws EmailSchedulingException If email scheduling fails for any recipient
     */
    public ScheduledEmailsResult scheduleEmailsForRecipients(List<RecipientDto> recipientDtos)
            throws EmailSchedulingException {
        if (!validateRecipientInput(recipientDtos)) {
            return null;
        }

        List<Email> initialEmails = new ArrayList<>();
        List<Email> followupEmails = new ArrayList<>();

        for (RecipientDto recipient : recipientDtos) {
            try {
                ScheduledRecipientResult result = scheduleEmailsForRecipient(recipient);
                initialEmails.addAll(result.initialEmails());
                followupEmails.addAll(result.followupEmails());
            } catch (EmailSchedulingException e) {
                LOGGER.log(Level.WARNING, "Failed to schedule emails for recipient: " + recipient.getName(), e);
            }
        }

        return new ScheduledEmailsResult(initialEmails, followupEmails);
    }

    /**
     * Schedule emails for a single recipient.
     *
     * @param recipient Recipient to schedule emails for
     * @return Scheduled emails result
     * @throws EmailSchedulingException If email scheduling fails
     */
    private ScheduledRecipientResult scheduleEmailsForRecipient(RecipientDto recipient) throws EmailSchedulingException {
        validateRecipientInput(recipient);

        try {
            EmailSchedulingContext context = createEmailSchedulingContext(recipient);

            // Different strategies based on current email scheduling status
            return switch (context.getSchedulingStatus()) {
                case NO_EMAILS_SCHEDULED -> scheduleInitialEmailPack(recipient);
                case INITIAL_EMAIL_SCHEDULED -> scheduleOneFollowUpEmail(recipient, context);
                case FIRST_FOLLOWUP_SCHEDULED -> scheduleAdditionalFollowUpEmails(recipient, context);
                case MAX_FOLLOWUPS_REACHED, NO_SCHEDULING_REQUIRED -> ScheduledRecipientResult.empty();
            };
        } catch (Exception e) {
            throw new EmailSchedulingException(
                    "Unexpected error scheduling emails for recipient: " + recipient.getName(), e);
        }
    }

    /**
     * Create the first set of emails when no emails have been scheduled previously
     */
    private ScheduledRecipientResult scheduleInitialEmailPack(RecipientDto recipient)
            throws EmailTemplateManagerException, EmailSchedulingException {
        Email initialEmail = createInitialEmail(recipient);
        int initialEmailId = emailSchedulingService.scheduleInitialEmail(initialEmail);

        Email firstFollowUpEmail = createFollowUpEmail(recipient, 1, initialEmailId);
        emailSchedulingService.scheduleFollowUpEmail(firstFollowUpEmail);

        return new ScheduledRecipientResult(
                List.of(initialEmail),
                List.of(firstFollowUpEmail)
        );
    }

    /**
     * Create follow-up emails when an initial email has been scheduled
     */
    private ScheduledRecipientResult scheduleOneFollowUpEmail(
            RecipientDto recipient,
            EmailSchedulingContext context
    ) throws EmailTemplateManagerException, EmailSchedulingException {
        Email firstFollowUpEmail = createFollowUpEmail(
                recipient,
                context.getCurrentFollowupNumber() + 1,
                context.getInitialEmailId()
        );
        firstFollowUpEmail.setThreadId(context.getThreadId());

        emailSchedulingService.scheduleFollowUpEmail(firstFollowUpEmail);

        return new ScheduledRecipientResult(
                List.of(),
                List.of(firstFollowUpEmail)
        );
    }

    /**
     * Create additional follow-up emails when initial and first follow-ups are already sent
     */
    private ScheduledRecipientResult scheduleAdditionalFollowUpEmails(
            RecipientDto recipient,
            EmailSchedulingContext context
    ) throws EmailTemplateManagerException, EmailSchedulingException {
        List<Email> followUpEmails = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            Email followUpEmail = createFollowUpEmail(
                    recipient,
                    context.getCurrentFollowupNumber() + i,
                    context.getInitialEmailId()
            );
            followUpEmail.setThreadId(context.getThreadId());

            emailSchedulingService.scheduleFollowUpEmail(followUpEmail);
            followUpEmails.add(followUpEmail);
        }

        return new ScheduledRecipientResult(List.of(), followUpEmails);
    }

    private PlaceholderManager resolveSpreadsheetReference(PlaceholderManager manager, SpreadsheetReference row) throws PlaceholderException {
        PlaceholderManager updatedPlaceholderManager = new PlaceholderManager();

        List<SpreadsheetReference> cellsToRetrieveValuesFrom = new ArrayList<>();
        for (Map.Entry<String, PlaceholderManager.PlaceholderValue> entry : manager.getAllPlaceholders().entrySet()) {
            PlaceholderManager.PlaceholderValue value = entry.getValue();
            PlaceholderManager.ValueType type = value.type();
            switch (type) {
                case STRING -> updatedPlaceholderManager.addStringPlaceholder(entry.getKey(), value.getStringValue());
                case SPREADSHEET_REFERENCE -> {
                    SpreadsheetReference column = value.getSpreadsheetReference();

                    // Combine column with row to create a cell
                    SpreadsheetReference cell = SpreadsheetReference.ofCell(column.getReference() + "" + row.getReference());
                    cellsToRetrieveValuesFrom.add(cell);
                }
            }
        }
        // Retrieve data from cells and add them to the updated placeholder manager
        List<String> values;
        try {
            if (!cellsToRetrieveValuesFrom.isEmpty()) {
                values = spreadsheetService.readSpreadsheetBatch(cellsToRetrieveValuesFrom);
                int index = 0;
                for (Map.Entry<String, PlaceholderManager.PlaceholderValue> entry : manager.getAllPlaceholders().entrySet()) {
                    if (entry.getValue().type() == PlaceholderManager.ValueType.SPREADSHEET_REFERENCE) {
                        updatedPlaceholderManager.addStringPlaceholder(entry.getKey(), values.get(index));
                        index++;
                    }
                }
            }
        } catch (SpreadsheetOperationException e) {
            throw new PlaceholderException("Failed to retrieve values from spreadsheet: " + e.getMessage());
        }


        return updatedPlaceholderManager;
    }

    // Helper methods for email creation
    private Email createInitialEmail(RecipientDto recipient) throws EmailTemplateManagerException {
        ZonedDateTime initialEmailDate = recipient.getInitialEmailDate();
        EmailTemplate template = templateManager.getDefaultInitialEmailTemplate();
        try {
            // Resolve spreadsheet placeholders
            PlaceholderManager resolvedPlaceholders = resolveSpreadsheetReference(
                    template.getPlaceholderManager(),
                    SpreadsheetReference.ofRow(recipient.getSpreadsheetRow())
            );
            template.setPlaceholderManager(resolvedPlaceholders);

            return new Email.Builder()
                    .setSender(defaultSenderEmail)
                    .setRecipientEmail(recipient.getEmailAddress())
                    .setRecipientId(recipient.getId())
                    .setStatus("PENDING")
                    .setScheduledDate(initialEmailDate)
                    .setEmailCategory(EmailCategory.INITIAL)
                    .setTemplate(template)
                    .build();
        } catch (PlaceholderException e) {
            throw new EmailTemplateManagerException("Failed to update the placeholder manager", e);
        }
    }

    private Email createFollowUpEmail(RecipientDto recipient, int followUpNumber, int initialEmailId)
            throws EmailTemplateManagerException, EmailSchedulingException {
        try {
            int intervalDays = emailDao.getIntervalDaysForFollowupNumber(followUpNumber);
            ZonedDateTime lastEmailDate = emailDao.getLastEmailDateForRecipient(recipient.getId());
            ZonedDateTime followUpEmailDate = lastEmailDate.plusDays(intervalDays);

            EmailTemplate template = templateManager.getDefaultFollowUpEmailTemplate(followUpNumber);

            try {
                template.setPlaceholderManager(
                        resolveSpreadsheetReference(template.getPlaceholderManager(), SpreadsheetReference.ofRow(recipient.getSpreadsheetRow()))
                );
                return new Email.Builder()
                        .setSender(defaultSenderEmail)
                        .setRecipientEmail(recipient.getEmailAddress())
                        .setRecipientId(recipient.getId())
                        .setStatus("PENDING")
                        .setScheduledDate(followUpEmailDate)
                        .setFollowupNumber(followUpNumber)
                        .setInitialEmailId(initialEmailId)
                        .setEmailCategory(EmailCategory.FOLLOW_UP)
                        .setTemplate(template)
                        .build();
            } catch (PlaceholderException e) {
                throw new EmailTemplateManagerException("Failed to update placeholder manager", e);
            }
        } catch (EmailDaoException e) {
            throw new EmailSchedulingException("Failed to create follow-up email", e);
        }
    }

    /**
     * Validates that the recipient list is not null or empty
     */
    private boolean validateRecipientInput(List<RecipientDto> recipientDtos) {
        return recipientDtos != null && !recipientDtos.isEmpty();
    }

    /**
     * Validates individual recipient data
     */
    private void validateRecipientInput(RecipientDto recipient) throws EmailSchedulingValidationException {
        if (recipient == null) {
            throw new EmailSchedulingValidationException("Recipient cannot be null");
        }
        if (recipient.getInitialEmailDate() == null) {
            throw new EmailSchedulingValidationException("Initial email date must be set for recipient " + recipient.getName());
        }
    }

    private int scheduleInitialEmail(Email initialEmail, RecipientDto recipientDto) throws EmailSchedulingException {
        LOGGER.info("Scheduling email for recipient: " + recipientDto.getName());

        if (recipientDto.getInitialEmailDate() == null) {
            LOGGER.warning("Initial email date is not set for recipient: " + recipientDto.getName());
            return -1;
        }

        EmailDto emailDto = EntityMapper.toEmailDto(initialEmail, 1);
        try {
            return emailDao.insertEmail(EntityMapper.toEmailEntity(emailDto));
        } catch (EmailDaoException | MappingException e) {
            throw new EmailSchedulingException("Failed to insert email generated from template to database", e);
        }
    }

    private void scheduleFollowUpEmail(Email followUpEmail, RecipientDto recipientDto) throws EmailSchedulingException {
        LOGGER.info("Scheduling followup email for recipient: " + recipientDto.getName());

        EmailDto emailDto = EntityMapper.toEmailDto(followUpEmail, 1);
        try {
            emailDao.insertEmail(EntityMapper.toEmailEntity(emailDto));
        } catch (EmailDaoException | MappingException e) {
            throw new EmailSchedulingException("Failed to insert email generated from template to database", e);
        }
    }


    /**
     * Create context for email scheduling based on current email status
     */
    private EmailSchedulingContext createEmailSchedulingContext(RecipientDto recipient)
            throws EmailDaoException {
        List<EmailEntity> emailEntities = emailDao.getEmailsByRecipientId(recipient.getId());
        int currentFollowupNumber = emailDao.getCurrentFollowupNumberForRecipient(recipient.getId());
        int maxFollowupNumber = emailDao.getMaxFollowupNumberForSchedule(1);

        return new EmailSchedulingContext(emailEntities, currentFollowupNumber, maxFollowupNumber);
    }

    /**
     * Represents the current context of email scheduling for a recipient
     */
    public static class EmailSchedulingContext {
        public enum SchedulingStatus {
            NO_EMAILS_SCHEDULED,
            INITIAL_EMAIL_SCHEDULED,
            FIRST_FOLLOWUP_SCHEDULED,
            MAX_FOLLOWUPS_REACHED,
            NO_SCHEDULING_REQUIRED
        }

        private final List<EmailEntity> emailEntities;
        private final int currentFollowupNumber;
        private final int maxFollowupNumber;

        public EmailSchedulingContext(
                List<EmailEntity> emailEntities,
                int currentFollowupNumber,
                int maxFollowupNumber
        ) {
            this.emailEntities = emailEntities;
            this.currentFollowupNumber = currentFollowupNumber;
            this.maxFollowupNumber = maxFollowupNumber;
        }

        public SchedulingStatus getSchedulingStatus() {
            if (isMaxFollowupsReached()) {
                return SchedulingStatus.MAX_FOLLOWUPS_REACHED;
            }

            if (emailEntities.isEmpty()) {
                return SchedulingStatus.NO_EMAILS_SCHEDULED;
            }

            Optional<EmailEntity> initialEmail = getInitialEmail();
            Optional<EmailEntity> lastEmail = getLastEmail();
            Optional<EmailEntity> secondToLastEmail = getSecondToLastEmail();

            if (initialEmail.isPresent() && isEmailPending(initialEmail)) {
                if (secondToLastEmail.equals(initialEmail)) { // Only initial email and first follow-up are in emailEntities
                    if (lastEmail.isPresent() && isEmailPending(lastEmail)) {
                        return SchedulingStatus.NO_SCHEDULING_REQUIRED;
                    }
                }
                return SchedulingStatus.INITIAL_EMAIL_SCHEDULED;
            }
            if (initialEmail.isPresent() && isEmailSent(initialEmail)) {
                if (secondToLastEmail.equals(initialEmail) || isEmailSent(secondToLastEmail)) {
                    return SchedulingStatus.INITIAL_EMAIL_SCHEDULED;
                }
                if (secondToLastEmail.isPresent() && isEmailPending(secondToLastEmail)) {
                    return SchedulingStatus.NO_SCHEDULING_REQUIRED;
                }
            }

            return SchedulingStatus.FIRST_FOLLOWUP_SCHEDULED;
        }

        private boolean isEmailPending(Optional<EmailEntity> emailEntity) {
            return emailEntity.filter(entity -> "PENDING".equals(entity.getStatus())).isPresent();
        }

        private boolean isEmailSent(Optional<EmailEntity> emailEntity) {
            return emailEntity.filter(entity -> "SENT".equals(entity.getStatus())).isPresent();
        }

        private boolean isMaxFollowupsReached() {
            return currentFollowupNumber >= maxFollowupNumber;
        }

        public int getCurrentFollowupNumber() {
            return currentFollowupNumber;
        }

        public int getInitialEmailId() {
            return getLastEmail()
                    .map(EmailEntity::getInitial_email_id)
                    .orElse(-1);
        }

        public String getThreadId() {
            return getLastEmail()
                    .map(EmailEntity::getThread_id)
                    .orElse(null);
        }

        private Optional<EmailEntity> getInitialEmail() {
            return emailEntities.stream()
                    .filter(email -> "INITIAL".equals(email.getEmail_category()))
                    .findFirst();
        }

        private Optional<EmailEntity> getLastEmail() {
            return emailEntities.isEmpty()
                    ? Optional.empty()
                    : Optional.of(emailEntities.get(emailEntities.size() - 1));
        }

        private Optional<EmailEntity> getSecondToLastEmail() {
            return emailEntities.size() < 2
                    ? Optional.empty()
                    : Optional.of(emailEntities.get(emailEntities.size() - 2));
        }
    }

    public record ScheduledRecipientResult(List<Email> initialEmails, List<Email> followupEmails) {
        public static ScheduledRecipientResult empty() {
            return new ScheduledRecipientResult(List.of(), List.of());
        }
    }

    public record ScheduledEmailsResult(List<Email> initialEmails, List<Email> followupEmails) {}

    /**
     * Exception for validation errors during email scheduling.
     */
    public static class EmailSchedulingValidationException extends EmailSchedulingException {
        public EmailSchedulingValidationException(String message) {
            super(message);
        }
    }
}
