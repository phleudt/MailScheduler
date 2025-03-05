package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.application.email.factory.EmailFactory;
import com.mailscheduler.common.exception.EmailTemplateManagerException;
import com.mailscheduler.common.exception.PlaceholderException;
import com.mailscheduler.common.exception.SpreadsheetOperationException;
import com.mailscheduler.application.template.TemplateManager;
import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.email.EmailId;
import com.mailscheduler.domain.email.EmailStatus;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.domain.email.EmailCategory;
import com.mailscheduler.domain.schedule.ScheduleId;
import com.mailscheduler.domain.template.PlaceholderManager;
import com.mailscheduler.domain.template.Template;
import com.mailscheduler.common.exception.service.EmailSchedulingException;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;
import com.mailscheduler.application.spreadsheet.SpreadsheetService;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteEmailRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailSchedulingManager {
    private final Logger LOGGER = Logger.getLogger(EmailSchedulingManager.class.getName());

    private final SQLiteEmailRepository emailRepository;
    private final TemplateManager templateManager;
    private final EmailSchedulingService emailSchedulingService;
    private final SpreadsheetService spreadsheetService;
    private final EmailAddress defaultSenderEmail;
    private final EmailFactory emailFactory;

    public EmailSchedulingManager(
            SQLiteEmailRepository emailRepository,
            TemplateManager templateManager,
            SpreadsheetService spreadsheetService,
            EmailAddress defaultSenderEmail
    ) {
        this.emailRepository = emailRepository;
        this.templateManager = templateManager;
        this.emailSchedulingService = new EmailSchedulingService(emailRepository);
        this.spreadsheetService = spreadsheetService;
        this.defaultSenderEmail = defaultSenderEmail;
        this.emailFactory = new EmailFactory(defaultSenderEmail);
    }

    public ScheduledEmailsResult scheduleEmailsForRecipients(List<Recipient> recipients) {
        if (!validateRecipientInput(recipients)) {
            return null;
        }

        List<Email> initialEmails = new ArrayList<>();
        List<Email> followupEmails = new ArrayList<>();

        for (Recipient recipient : recipients) {
            try {
                ScheduledRecipientResult result = scheduleEmailsForRecipient(recipient);
                initialEmails.addAll(result.initialEmails());
                followupEmails.addAll(result.followupEmails());
            } catch (EmailSchedulingException e) {
                LOGGER.log(Level.WARNING, "Failed to schedule emails for recipient: " + recipient.getName().value(), e);
            }
        }

        return new ScheduledEmailsResult(initialEmails, followupEmails);
    }

    private ScheduledRecipientResult scheduleEmailsForRecipient(Recipient recipient) throws EmailSchedulingException {
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
                    "Unexpected error scheduling emails for recipient: " + recipient.getName().value(), e);
        }
    }

    /**
     * Create the first set of emails when no emails have been scheduled previously
     */
    private ScheduledRecipientResult scheduleInitialEmailPack(Recipient recipient)
            throws EmailTemplateManagerException, EmailSchedulingException {
        Email initialEmail = createInitialEmail(recipient);
        initialEmail = emailSchedulingService.scheduleInitialEmail(initialEmail);

        Email firstFollowUpEmail = createFollowUpEmail(recipient, 1, initialEmail.getId());
        emailSchedulingService.scheduleFollowUpEmail(firstFollowUpEmail);

        return new ScheduledRecipientResult(
                List.of(initialEmail),
                List.of(firstFollowUpEmail)
        );
    }

    private ScheduledRecipientResult scheduleOneFollowUpEmail(
            Recipient recipient,
            EmailSchedulingContext context
    ) throws EmailTemplateManagerException, EmailSchedulingException {
        Optional<EmailId> initialEmailId = context.getInitialEmailId();
        Optional<String> threadId = context.getThreadId();
        if (initialEmailId.isEmpty() || threadId.isEmpty()) return new ScheduledRecipientResult(List.of(), List.of());
        Email firstFollowUpEmail = createFollowUpEmail(
                recipient,
                context.getCurrentFollowupNumber() + 1,
                initialEmailId.get()
        );
        firstFollowUpEmail.setThreadId(threadId.get());

        emailSchedulingService.scheduleFollowUpEmail(firstFollowUpEmail);

        return new ScheduledRecipientResult(
                List.of(),
                List.of(firstFollowUpEmail)
        );
    }

    private ScheduledRecipientResult scheduleAdditionalFollowUpEmails(
            Recipient recipient,
            EmailSchedulingContext context
    ) throws EmailTemplateManagerException, EmailSchedulingException {
        Optional<EmailId> initialEmailId = context.getInitialEmailId();
        Optional<String> threadId = context.getThreadId();
        if (initialEmailId.isEmpty() || threadId.isEmpty()) return new ScheduledRecipientResult(List.of(), List.of());

        List<Email> followUpEmails = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            Email followUpEmail = createFollowUpEmail(
                    recipient,
                    context.getCurrentFollowupNumber() + i,
                    initialEmailId.get()
            );
            followUpEmail.setThreadId(threadId.get());

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
                        if (values.get(index).isEmpty()) throw new PlaceholderException("Cell value is empty");
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
    private Email createInitialEmail(Recipient recipient) throws EmailTemplateManagerException {
        Optional<Template> template = templateManager.getDefaultInitialEmailTemplate();
        if (template.isEmpty()) throw new EmailTemplateManagerException("Failed to load default initial template");
        try {
            // Resolve spreadsheet placeholders
            PlaceholderManager resolvedPlaceholders = resolveSpreadsheetReference(
                    template.get().getPlaceholderManager(),
                    recipient.getSpreadsheetRow()
            );
            template.get().setPlaceholderManager(resolvedPlaceholders);

            return emailFactory.createInitialEmail(recipient, template.get());
        } catch (PlaceholderException e) {
            throw new EmailTemplateManagerException("Failed to update the placeholder manager", e);
        }
    }

    private Email createFollowUpEmail(Recipient recipient, int followUpNumber, EmailId initialEmailId)
            throws EmailTemplateManagerException, EmailSchedulingException {
        try {
            int intervalDays = emailRepository.getIntervalDaysForFollowupNumber(followUpNumber);
            ZonedDateTime lastEmailDate = emailRepository.getLastEmailDateForRecipient(recipient.getId());
            ZonedDateTime followUpEmailDate = lastEmailDate.plusDays(intervalDays);

            Optional<Template> template = templateManager.getDefaultFollowUpEmailTemplate(followUpNumber);

            if (template.isEmpty()) throw new EmailTemplateManagerException("Failed to load follow-up template for follow-up number " + followUpNumber);

            try {
                template.get().setPlaceholderManager(
                        resolveSpreadsheetReference(template.get().getPlaceholderManager(), recipient.getSpreadsheetRow())
                );

                return emailFactory.createFollowUpEmail(recipient, template.get(), followUpEmailDate, followUpNumber, initialEmailId);
            } catch (PlaceholderException e) {
                throw new EmailTemplateManagerException("Failed to update placeholder manager", e);
            }
        } catch (RepositoryException e) {
            throw new EmailSchedulingException("Failed to create follow-up email", e);
        }
    }

    private boolean validateRecipientInput(List<Recipient> recipients) {
        return recipients != null && !recipients.isEmpty();
    }

    private void validateRecipientInput(Recipient recipient) throws EmailSchedulingValidationException {
        if (recipient == null) {
            throw new EmailSchedulingValidationException("Recipient cannot be null");
        }
        if (recipient.getInitialEmailDate() == null) {
            throw new EmailSchedulingValidationException("Initial email date must be set for recipient " + recipient.getName());
        }
    }

    private EmailSchedulingContext createEmailSchedulingContext(Recipient recipient)
            throws RepositoryException {
        List<Email> emails = emailRepository.getByRecipientId(recipient.getId());
        int currentFollowupNumber = emailRepository.getCurrentFollowupNumberForRecipient(recipient.getId());
        int maxFollowupNumber = emailRepository.getMaxFollowupNumberForSchedule(ScheduleId.of(1));

        return new EmailSchedulingContext(emails, currentFollowupNumber, maxFollowupNumber);
    }

    public static class EmailSchedulingContext {
        public enum SchedulingStatus {
            NO_EMAILS_SCHEDULED,
            INITIAL_EMAIL_SCHEDULED,
            FIRST_FOLLOWUP_SCHEDULED,
            MAX_FOLLOWUPS_REACHED,
            NO_SCHEDULING_REQUIRED
        }

        private final List<Email> emails;
        private final int currentFollowupNumber;
        private final int maxFollowupNumber;

        public EmailSchedulingContext(
                List<Email> emails,
                int currentFollowupNumber,
                int maxFollowupNumber
        ) {
            this.emails = emails;
            this.currentFollowupNumber = currentFollowupNumber;
            this.maxFollowupNumber = maxFollowupNumber;
        }

        public SchedulingStatus getSchedulingStatus() {
            if (isMaxFollowupsReached()) {
                return SchedulingStatus.MAX_FOLLOWUPS_REACHED;
            }

            if (emails.isEmpty()) {
                return SchedulingStatus.NO_EMAILS_SCHEDULED;
            }

            Optional<Email> initialEmail = getInitialEmail();
            Optional<Email> lastEmail = getLastEmail();
            Optional<Email> secondToLastEmail = getSecondToLastEmail();

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

        private boolean isEmailPending(Optional<Email> email) {
            return email.filter(email1 -> EmailStatus.PENDING.equals(email1.getStatus())).isPresent();
        }

        private boolean isEmailSent(Optional<Email> email) {
            return email.filter(email1 -> EmailStatus.SENT.equals(email1.getStatus())).isPresent();
        }

        private boolean isMaxFollowupsReached() {
            return currentFollowupNumber >= maxFollowupNumber;
        }

        public int getCurrentFollowupNumber() {
            return currentFollowupNumber;
        }

        public Optional<EmailId> getInitialEmailId() {
            Optional<Email> lastEmail = getLastEmail();
            if (lastEmail.isPresent()) {
                return lastEmail.get().getInitialEmailId();
            }
            return Optional.empty();
        }

        public Optional<String> getThreadId() {
            Optional<Email> lastEmail = getLastEmail();
            if (lastEmail.isPresent()) {
                return lastEmail.get().getThreadId();
            }
            return Optional.empty();
        }

        private Optional<Email> getInitialEmail() {
            return emails.stream()
                    .filter(email -> EmailCategory.INITIAL.equals(email.getCategory()))
                    .findFirst();
        }

        private Optional<Email> getLastEmail() {
            return emails.isEmpty()
                    ? Optional.empty()
                    : Optional.of(emails.get(emails.size() - 1));
        }

        private Optional<Email> getSecondToLastEmail() {
            return emails.size() < 2
                    ? Optional.empty()
                    : Optional.of(emails.get(emails.size() - 2));
        }
    }

    public record ScheduledRecipientResult(List<Email> initialEmails, List<Email> followupEmails) {
        public static ScheduledRecipientResult empty() {
            return new ScheduledRecipientResult(List.of(), List.of());
        }
    }

    public record ScheduledEmailsResult(List<Email> initialEmails, List<Email> followupEmails) {}

    public static class EmailSchedulingValidationException extends EmailSchedulingException {
        public EmailSchedulingValidationException(String message) {
            super(message);
        }
    }
}