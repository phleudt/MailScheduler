package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.common.exception.service.EmailSchedulingException;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;
import com.mailscheduler.domain.email.Email;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteEmailRepository;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for the actual scheduling of emails in the system.
 * Handles database interactions and email persistence.
 */
public class EmailSchedulingService {
    private static final Logger LOGGER = Logger.getLogger(EmailSchedulingService.class.getName());

    private final SQLiteEmailRepository emailRepository;

    public EmailSchedulingService(SQLiteEmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    /**
     * Schedules an initial email and returns its database ID.
     *
     * @param initialEmail The initial email to be scheduled
     * @return The database ID of the scheduled email
     * @throws EmailSchedulingException If there are issues scheduling the email
     */
    public Email scheduleInitialEmail(Email initialEmail) throws EmailSchedulingException {
        LOGGER.info("Scheduling initial email for recipient: " + initialEmail.getRecipientId());

        try {
            return emailRepository.save(initialEmail);
        } catch (RepositoryException e) {
            LOGGER.log(Level.SEVERE, "Failed to schedule initial email", e);
            throw new EmailSchedulingException("Failed to insert initial email to database", e);
        }
    }

    /**
     * Schedules a follow-up email.
     *
     * @param followUpEmail The follow-up email to be scheduled
     * @throws EmailSchedulingException If there are issues scheduling the email
     */
    public void scheduleFollowUpEmail(Email followUpEmail) throws EmailSchedulingException {
        LOGGER.info("Scheduling follow-up email for recipient: " + followUpEmail.getRecipientId().toString());

        try {
            emailRepository.save(followUpEmail);
            LOGGER.info("Follow-up email scheduled successfully");
        } catch (RepositoryException e) {
            LOGGER.log(Level.SEVERE, "Failed to schedule follow-up email", e);
            throw new EmailSchedulingException("Failed to insert follow-up email to database", e);
        }
    }
}
