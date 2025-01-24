package com.mailscheduler.email;

import com.mailscheduler.database.dao.EmailDao;
import com.mailscheduler.dto.EmailDto;
import com.mailscheduler.exception.dao.EmailDaoException;
import com.mailscheduler.exception.service.EmailSchedulingException;
import com.mailscheduler.exception.MappingException;
import com.mailscheduler.mapper.EntityMapper;
import com.mailscheduler.model.Email;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for the actual scheduling of emails in the system.
 * Handles database interactions and email persistence.
 */
public class EmailSchedulingService {
    private static final Logger LOGGER = Logger.getLogger(EmailSchedulingService.class.getName());

    private final EmailDao emailDao;

    /**
     * Constructor with dependency injection.
     *
     * @param emailDao Data Access Object for email-related database operations
     */
    public EmailSchedulingService(EmailDao emailDao) {
        this.emailDao = emailDao;
    }

    /**
     * Schedules an initial email and returns its database ID.
     *
     * @param initialEmail The initial email to be scheduled
     * @return The database ID of the scheduled email
     * @throws EmailSchedulingException If there are issues scheduling the email
     */
    public int scheduleInitialEmail(Email initialEmail) throws EmailSchedulingException {
        LOGGER.info("Scheduling initial email for recipient: " + initialEmail.getRecipientId());

        try {
            validateEmailForScheduling(initialEmail);

            EmailDto emailDto = EntityMapper.toEmailDto(initialEmail, 1);
            int emailId = emailDao.insertEmail(EntityMapper.toEmailEntity(emailDto));

            LOGGER.info("Initial email scheduled successfully with ID: " + emailId);
            return emailId;

        } catch (EmailDaoException | MappingException e) {
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
        LOGGER.info("Scheduling follow-up email for recipient: " + followUpEmail.getRecipientId());

        try {
            validateEmailForScheduling(followUpEmail);

            EmailDto emailDto = EntityMapper.toEmailDto(followUpEmail, 1);
            emailDao.insertEmail(EntityMapper.toEmailEntity(emailDto));

            LOGGER.info("Follow-up email scheduled successfully");

        } catch (EmailDaoException | MappingException e) {
            LOGGER.log(Level.SEVERE, "Failed to schedule follow-up email", e);
            throw new EmailSchedulingException("Failed to insert follow-up email to database", e);
        }
    }

    /**
     * Validates an email before scheduling.
     * Performs basic validation checks on the email.
     *
     * @param email The email to validate
     * @throws EmailSchedulingException If the email fails validation
     */
    private void validateEmailForScheduling(Email email) throws EmailSchedulingException {
        // Validate sender
        if (email.getSender() == null || email.getSender().isEmpty()) {
            throw new EmailSchedulingException("Sender email cannot be null or empty");
        }

        // Validate recipient
        if (email.getRecipientEmail() == null || email.getRecipientEmail().isEmpty()) {
            throw new EmailSchedulingException("Recipient email cannot be null or empty");
        }

        // Validate email date
        if (email.getScheduledDate() == null) {
            throw new EmailSchedulingException("Email scheduled date cannot be null");
        }
    }

    /**
     * Retrieves the status of a scheduled email.
     *
     * @param emailId The ID of the email to check
     * @return The current status of the email
     * @throws EmailSchedulingException If there are issues retrieving the email status
     */
    public String getEmailStatus(int emailId) throws EmailSchedulingException {
        try {
            return emailDao.getEmailStatus(emailId);
        } catch (EmailDaoException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve email status", e);
            throw new EmailSchedulingException("Failed to retrieve email status", e);
        }
    }

    /**
     * Updates the status of a scheduled email.
     *
     * @param emailId The ID of the email to update
     * @param newStatus The new status to set
     * @throws EmailSchedulingException If there are issues updating the email status
     */
    public void updateEmailStatus(int emailId, String newStatus) throws EmailSchedulingException {
        try {
            validateEmailStatus(newStatus);

            emailDao.updateEmailStatus(emailId, newStatus);
            LOGGER.info("Email status updated successfully for email ID: " + emailId);
        } catch (EmailDaoException e) {
            LOGGER.log(Level.SEVERE, "Failed to update email status", e);
            throw new EmailSchedulingException("Failed to update email status", e);
        }
    }

    /**
     * Validates the email status before updating.
     *
     * @param status The status to validate
     * @throws EmailSchedulingException If the status is invalid
     */
    private void validateEmailStatus(String status) throws EmailSchedulingException {
        String[] validStatuses = {"PENDING", "SENT", "FAILED", "CANCELLED"};

        for (String validStatus : validStatuses) {
            if (validStatus.equals(status)) {
                return;
            }
        }

        throw new EmailSchedulingException("Invalid email status: " + status);
    }
}
