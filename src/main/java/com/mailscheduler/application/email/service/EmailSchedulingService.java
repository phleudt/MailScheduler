package com.mailscheduler.application.email.service;

import com.mailscheduler.application.email.scheduling.EmailScheduler;
import com.mailscheduler.application.email.scheduling.PlanWithTemplatesRecipientsMap;
import com.mailscheduler.application.email.scheduling.RecipientScheduledEmailsMap;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailStatus;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.repository.EmailRepository;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for scheduling and managing emails.
 * Provides higher-level operations on top of the email scheduler.
 */
public class EmailSchedulingService {
    private final static Logger LOGGER = Logger.getLogger(EmailSchedulingService.class.getName());

    private final EmailScheduler emailScheduler;
    private final EmailRepository emailRepository;

    public EmailSchedulingService(EmailScheduler scheduler, EmailRepository emailRepository) {
        this.emailScheduler = scheduler;
        this.emailRepository = emailRepository;
    }

    /**
     * Schedules emails for recipients based on the provided plan.
     *
     * @param recipientsMap mapping of plans to recipients
     * @return mapping of recipients to their scheduled emails
     */
    public RecipientScheduledEmailsMap scheduleForRecipients(PlanWithTemplatesRecipientsMap recipientsMap) {
        if (recipientsMap == null || recipientsMap.isEmpty()) {
            LOGGER.info("No recipients to schedule emails for");
            return RecipientScheduledEmailsMap.empty();
        }

        try {
            LOGGER.info("Scheduling emails for " + countRecipients(recipientsMap) + " recipients");
            return emailScheduler.scheduleEmailsAfterPlans(recipientsMap);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error scheduling emails", e);
            return RecipientScheduledEmailsMap.empty();
        }
    }

    private int countRecipients(PlanWithTemplatesRecipientsMap recipientsMap) {
        return recipientsMap.getMapping().values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Retrieves emails that are due to be sent.
     * Returns at most one email per recipient, prioritizing by follow-up number.     *
     *
     * @return list of pending emails with their metadata
     */
    public List<EntityData<Email, EmailMetadata>> getPendingEmails() {
        try {
            LocalDate cutoff = LocalDate.now().plusDays(1);
            LOGGER.info("Finding pending emails scheduled before " + cutoff);

            // Get emails scheduled for today or earlier
            List<EntityData<Email, EmailMetadata>> pendingEmails =
                    emailRepository.findPendingScheduledBefore(cutoff);

            LOGGER.info("Found " + pendingEmails.size() + " pending emails");

            // Filter out externally managed emails
            List<EntityData<Email, EmailMetadata>> filteredEmails = filterInternalEmails(pendingEmails);
            LOGGER.info("After filtering: " + filteredEmails.size() + " internal emails");

            // Group by recipient and get next email for each
            Map<EntityId<Recipient>, List<EntityData<Email, EmailMetadata>>> emailsByRecipient =
                    groupEmailsByRecipient(filteredEmails);

            List<EntityData<Email, EmailMetadata>> nextEmails = selectNextEmailsToSend(emailsByRecipient);
            LOGGER.info("Selected " + nextEmails.size() + " emails to send (one per recipient)");

            return nextEmails;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving pending emails", e);
            return List.of();
        }
    }

    /**
     * Filters out emails that are managed externally.
     */
    private List<EntityData<Email, EmailMetadata>> filterInternalEmails(
            List<EntityData<Email, EmailMetadata>> emails) {
        return emails.stream()
                .filter(email -> !isExternalEmail(email.entity().getType()))
                .collect(Collectors.toList());
    }

    private boolean isExternalEmail(EmailType type) {
        return EmailType.EXTERNALLY_INITIAL.equals(type) ||
                EmailType.EXTERNALLY_FOLLOW_UP.equals(type);
    }

    /**
     * Groups emails by recipient ID.
     */
    private Map<EntityId<Recipient>, List<EntityData<Email, EmailMetadata>>> groupEmailsByRecipient(
            List<EntityData<Email, EmailMetadata>> emails) {

        Map<EntityId<Recipient>, List<EntityData<Email, EmailMetadata>>> result = new HashMap<>();

        for (EntityData<Email, EmailMetadata> email : emails) {
            EntityId<Recipient> recipientId = email.metadata().recipientId();
            if (recipientId != null) {
                result.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(email);
            }
        }

        // Sort each recipient's emails by follow-up number
        for (List<EntityData<Email, EmailMetadata>> recipientEmails : result.values()) {
            recipientEmails.sort(Comparator.comparing(email -> email.metadata().followupNumber()));
        }

        return result;
    }

    /**
     * Selects the next email to send for each recipient.
     * For each recipient, we take the email with the lowest follow-up number.
     */
    private List<EntityData<Email, EmailMetadata>> selectNextEmailsToSend(
            Map<EntityId<Recipient>, List<EntityData<Email, EmailMetadata>>> emailsByRecipient) {

        List<EntityData<Email, EmailMetadata>> result = new ArrayList<>();

        // For each recipient, take the first email (lowest follow-up number)
        for (List<EntityData<Email, EmailMetadata>> recipientEmails : emailsByRecipient.values()) {
            if (!recipientEmails.isEmpty()) {
                result.add(recipientEmails.get(0));
            }
        }

        return result;
    }

    /**
     * Updates an email's status.
     *
     * @param emailId       The ID of the email to update
     * @param newStatus     The new status to set
     * @param failureReason Optional reason if the status is FAILED
     * @return true if the update was successful, false otherwise
     */
    public boolean updateEmailStatus(EntityId<Email> emailId, EmailStatus newStatus, String failureReason) {
        try {
            Optional<EntityData<Email, EmailMetadata>> emailData = emailRepository.findByIdWithMetadata(emailId);

            if (emailData.isEmpty()) {
                LOGGER.warning("Cannot update email status: Email not found: " + emailId);
                return false;
            }

            Email email = emailData.get().entity();
            EmailMetadata metadata = emailData.get().metadata();

            // Create updated metadata
            EmailMetadata updatedMetadata = new EmailMetadata.Builder()
                    .initialEmailId(metadata.initialEmailId())
                    .recipientId(metadata.recipientId())
                    .followupNumber(metadata.followupNumber())
                    .status(newStatus)
                    .failureReason(failureReason)
                    .scheduledDate(metadata.scheduledDate())
                    .sentDate(newStatus == EmailStatus.SENT ? LocalDate.now() : metadata.sentDate())
                    .build();

            // Save the updated metadata
            emailRepository.saveWithMetadata(email, updatedMetadata);
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating email status", e);
            return false;
        }
    }
}
