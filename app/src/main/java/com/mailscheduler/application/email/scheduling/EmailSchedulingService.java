package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.application.email.factory.EmailFactory;
import com.mailscheduler.common.exception.EmailTemplateManagerException;
import com.mailscheduler.common.exception.PlaceholderException;
import com.mailscheduler.common.exception.SpreadsheetOperationException;
import com.mailscheduler.application.template.TemplateManager;
import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.email.EmailId;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.email.Email;
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

public class EmailSchedulingService {
    private final Logger LOGGER = Logger.getLogger(EmailSchedulingService.class.getName());

    private final SQLiteEmailRepository emailRepository;
    private final TemplateManager templateManager;
    private final SpreadsheetService spreadsheetService;
    private final EmailFactory emailFactory;
    private final PlaceholderResolver placeholderResolver;
    private final EmailSchedulingValidator validator;

    public EmailSchedulingService(
            SQLiteEmailRepository emailRepository,
            TemplateManager templateManager,
            SpreadsheetService spreadsheetService,
            EmailAddress defaultSenderEmail
    ) {
        this.emailRepository = emailRepository;
        this.templateManager = templateManager;
        this.spreadsheetService = spreadsheetService;
        this.emailFactory = new EmailFactory(defaultSenderEmail);
        this.placeholderResolver = new PlaceholderResolver(spreadsheetService);
        this.validator = new EmailSchedulingValidator();
    }

    public ScheduledEmailsResult scheduleEmailsForRecipients(List<Recipient> recipients) {
        if (!validator.validateRecipients(recipients)) {
            return ScheduledEmailsResult.empty();
        }

        EmailSchedulingResultCollector resultCollector = new EmailSchedulingResultCollector();

        recipients.forEach(recipient -> {
            try {
                ScheduledRecipientResult result = scheduleEmailsForRecipient(recipient);
                resultCollector.addResult(result);
            } catch (EmailSchedulingException e) {
                LOGGER.log(Level.WARNING,
                        String.format("Failed to schedule emails for recipient: %s", recipient.getName().value()),
                        e);
            }
        });

        return resultCollector.getResult();
    }

    private ScheduledRecipientResult scheduleEmailsForRecipient(Recipient recipient) throws EmailSchedulingException {
        validator.validateRecipient(recipient);

        try {
            EmailSchedulingContext context = createEmailSchedulingContext(recipient);

            // Different strategies based on current email scheduling status
            return switch (context.getSchedulingStatus()) {
                case NO_EMAILS_SCHEDULED -> scheduleCompleteEmailSequence(recipient);
                case PARTIAL_SEQUENCE_SCHEDULED -> completeRemainingSequence(recipient, context);
                case SEQUENCE_COMPLETE, NO_SCHEDULING_REQUIRED -> ScheduledRecipientResult.empty();
            };
        } catch (Exception e) {
            throw new EmailSchedulingException(
                    "Unexpected error scheduling emails for recipient: " + recipient.getName().value(), e);
        }
    }

    private ScheduledRecipientResult scheduleCompleteEmailSequence(Recipient recipient)
            throws EmailTemplateManagerException, EmailSchedulingException, RepositoryException {
        // Schedule initial email
        Email initialEmail = createInitialEmail(recipient);
        initialEmail = scheduleInitialEmail(initialEmail);

        List<Email> allFollowUps = new ArrayList<>();
        ZonedDateTime lastScheduledDate = initialEmail.getScheduledDate();
        int maxFollowUps = emailRepository.getMaxFollowupNumberForSchedule(ScheduleId.of(1));

        // Schedule all follow-ups at once with computed dates
        for (int followUpNum = 1; followUpNum <= maxFollowUps; followUpNum++) {
            int intervalDays = emailRepository.getIntervalDaysForFollowupNumber(followUpNum);
            lastScheduledDate = lastScheduledDate.plusDays(intervalDays);

            Email followUpEmail = createFollowUpEmail(
                    recipient,
                    followUpNum,
                    initialEmail.getId(),
                    lastScheduledDate
            );

            scheduleFollowUpEmail(followUpEmail);
            allFollowUps.add(followUpEmail);
        }

        return new ScheduledRecipientResult(
                List.of(initialEmail),
                allFollowUps
        );
    }

    /**
     * Completes the remaining sequence of follow-up emails for a recipient
     * when some emails have already been scheduled.
     *
     * @param recipient The recipient to schedule remaining emails for
     * @param context The current email scheduling context
     * @return ScheduledRecipientResult containing any newly scheduled follow-up emails
     * @throws EmailSchedulingException if there are issues scheduling the emails
     */
    private ScheduledRecipientResult completeRemainingSequence(
            Recipient recipient,
            EmailSchedulingContext context
    ) throws EmailSchedulingException {
        try {
            // Validate that we have the required information
            Optional<EmailId> initialEmailId = context.getInitialEmailId();
            Optional<String> threadId = context.getThreadId();
            if (initialEmailId.isEmpty()) {
                throw new EmailSchedulingException("Cannot complete sequence: Initial email ID not found");
            }

            // Get the last scheduled email's date to base our new schedules on
            ZonedDateTime lastScheduledDate = emailRepository.getLastEmailDateForRecipient(recipient.getId());
            if (lastScheduledDate == null) {
                throw new EmailSchedulingException("Cannot determine last scheduled email date");
            }

            List<Email> newFollowUps = new ArrayList<>();
            int currentFollowUpNumber = context.getCurrentFollowupNumber();
            int maxFollowUps = emailRepository.getMaxFollowupNumberForSchedule(ScheduleId.of(1));

            // Schedule remaining follow-ups starting from the next number in sequence
            for (int followUpNum = currentFollowUpNumber + 1; followUpNum <= maxFollowUps; followUpNum++) {
                try {
                    // Get the interval for this follow-up number
                    int intervalDays = emailRepository.getIntervalDaysForFollowupNumber(followUpNum);
                    // Calculate the next email date based on the last scheduled date
                    ZonedDateTime nextEmailDate = lastScheduledDate.plusDays(intervalDays);

                    // Create and schedule the follow-up email
                    Email followUpEmail = createFollowUpEmail(
                            recipient,
                            followUpNum,
                            initialEmailId.get(),
                            nextEmailDate
                    );

                    // Set thread ID if available
                    threadId.ifPresent(followUpEmail::setThreadId);

                    // Schedule the email
                    scheduleFollowUpEmail(followUpEmail);
                    newFollowUps.add(followUpEmail);

                    // Update last scheduled date for next iteration
                    lastScheduledDate = nextEmailDate;

                    LOGGER.info(String.format(
                            "Scheduled follow-up #%d for recipient %s on %s",
                            followUpNum,
                            recipient.getEmailAddress().value(),
                            nextEmailDate
                    ));

                } catch (EmailTemplateManagerException e) {
                    LOGGER.warning(String.format(
                            "Failed to create follow-up #%d for recipient %s: %s",
                            followUpNum,
                            recipient.getEmailAddress().value(),
                            e.getMessage()
                    ));
                    // Continue with next follow-up instead of failing the entire sequence
                    continue;
                }
            }

            return new ScheduledRecipientResult(List.of(), newFollowUps);

        } catch (RepositoryException e) {
            throw new EmailSchedulingException("Failed to complete email sequence", e);
        }
    }

    private Email scheduleInitialEmail(Email initialEmail) throws EmailSchedulingException {
        try {
            return emailRepository.save(initialEmail);
        } catch (RepositoryException e) {
            throw new EmailSchedulingException("Failed to insert initial email to database", e);
        }
    }

    private void scheduleFollowUpEmail(Email followUpEmail) throws EmailSchedulingException {
        try {
            emailRepository.save(followUpEmail);
        } catch (RepositoryException e) {
            throw new EmailSchedulingException("Failed to insert follow-up email to database", e);
        }
    }

    // Helper methods for email creation
    private Email createInitialEmail(Recipient recipient) throws EmailTemplateManagerException {
        Optional<Template> template = templateManager.getDefaultInitialEmailTemplate();
        if (template.isEmpty()) throw new EmailTemplateManagerException("Failed to load default initial template");
        try {
            // Resolve spreadsheet placeholders
            PlaceholderManager resolvedPlaceholders = placeholderResolver.resolvePlaceholders(
                    template.get().getPlaceholderManager(),
                    recipient.getSpreadsheetRow()
            );
            template.get().setPlaceholderManager(resolvedPlaceholders);

            return emailFactory.createInitialEmail(recipient, template.get());
        } catch (PlaceholderException e) {
            throw new EmailTemplateManagerException("Failed to update the placeholder manager", e);
        }
    }

    /**
     * Creates a follow-up email with a specific scheduled date
     */
    private Email createFollowUpEmail(Recipient recipient, int followUpNumber,
                                      EmailId initialEmailId, ZonedDateTime scheduledDate)
            throws EmailTemplateManagerException {
        Optional<Template> template = templateManager.getDefaultFollowUpEmailTemplate(followUpNumber);

        if (template.isEmpty()) {
            throw new EmailTemplateManagerException(
                    "Failed to load follow-up template for follow-up number " + followUpNumber);
        }

        try {
            template.get().setPlaceholderManager(
                    placeholderResolver.resolvePlaceholders(
                            template.get().getPlaceholderManager(),
                            recipient.getSpreadsheetRow()
                    )
            );

            return emailFactory.createFollowUpEmail(
                    recipient, template.get(), scheduledDate, followUpNumber, initialEmailId);
        } catch (PlaceholderException e) {
            throw new EmailTemplateManagerException("Failed to update placeholder manager", e);
        }
    }

    private EmailSchedulingContext createEmailSchedulingContext(Recipient recipient)
            throws RepositoryException {
        List<Email> emails = emailRepository.getByRecipientId(recipient.getId());
        int currentFollowupNumber = emailRepository.getCurrentFollowupNumberForRecipient(recipient.getId());
        int maxFollowupNumber = emailRepository.getMaxFollowupNumberForSchedule(ScheduleId.of(1));

        return new EmailSchedulingContext(emails, currentFollowupNumber, maxFollowupNumber);
    }

    public record ScheduledRecipientResult(List<Email> initialEmails, List<Email> followupEmails) {
        public static ScheduledRecipientResult empty() {
            return new ScheduledRecipientResult(List.of(), List.of());
        }
    }

    public record ScheduledEmailsResult(List<Email> initialEmails, List<Email> followupEmails) {
        public static ScheduledEmailsResult empty() {
            return new ScheduledEmailsResult(List.of(), List.of());
        }

        public boolean isEmpty() {
            return initialEmails.isEmpty() && followupEmails.isEmpty();
        }
    }
}