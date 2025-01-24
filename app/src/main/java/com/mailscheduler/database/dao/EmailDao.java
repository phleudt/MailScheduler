package com.mailscheduler.database.dao;

import com.mailscheduler.database.DatabaseManager;
import com.mailscheduler.database.entities.EmailEntity;
import com.mailscheduler.exception.dao.EmailDaoException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.logging.Logger;

public class EmailDao extends GenericDao<EmailEntity> {
    private static final Logger LOGGER = Logger.getLogger(EmailDao.class.getName());

    private static final String INSERT_EMAIL_QUERY = "INSERT INTO Emails (subject, body, status, scheduled_date, email_category, followup_number, thread_id, schedule_id, initial_email_id, recipient_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_EMAIL_BY_ID_QUERY = "UPDATE Emails SET subject = ?, body = ?, status = ?, scheduled_date = ?, email_category = ?, followup_number = ?, thread_id = ?, schedule_id = ?, initial_email_id = ?, recipient_id = ? WHERE id = ?";
    private static final String DELETE_EMAIL_BY_ID_QUERY = "DELETE FROM Emails WHERE id = ?";
    private static final String FIND_EMAIL_BY_ID_QUERY = "SELECT * FROM Emails WHERE id = ?";
    private static final String GET_CURRENT_FOLLOWUP_NUMBER_FOR_RECIPIENT_QUERY = "SELECT MAX(followup_number) AS followup_number FROM Emails WHERE recipient_id = ?";
    private static final String GET_MAX_FOLLOWUP_NUMBER_FOR_SCHEDULE_QUERY = "SELECT MAX(followup_number) AS max_followup_number FROM FollowUpSchedules WHERE schedule_id = ?";
    private static final String GET_INTERVAL_DAYS_FOR_FOLLOWUP_NUMBER_QUERY = "SELECT interval_days FROM FollowUpSchedules WHERE followup_number = ?";
    private static final String GET_LAST_EMAIL_DATE_FOR_RECIPIENT_QUERY = "SELECT MAX(scheduled_date) AS last_email_date FROM Emails WHERE recipient_id = ?";
    private static final String GET_INITIAL_EMAIL_FROM_FOLLOWUP_ID_QUERY = "SELECT e.* FROM Emails AS f JOIN Emails AS e ON f.initial_email_id = e.id WHERE f.id = ?";
    private static final String MARK_RECIPIENT_AS_REPLIED = "UPDATE Recipients SET has_replied = TRUE WHERE id = ?";
    private static final String GET_EMAIL_STATUS_BY_ID = "SELECT status FROM Emails WHERE id = ?";
    private static final String UPDATE_EMAIL_STATUS_BY_ID_QUERY = "UPDATE Emails SET status = ? WHERE id = ?";
    private static final String FIND_PENDING_EMAILS_TO_SEND_QUERY = "SELECT * FROM Emails WHERE status = 'PENDING' AND scheduled_date <= CURRENT_TIMESTAMP AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String IS_INITIAL_EMAIL_SCHEDULED_QUERY = "SELECT * FROM Emails WHERE recipient_id = ? AND status = 'PENDING' AND email_category = 'INITIAL' AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String IS_FOLLOWUP_EMAIL_SCHEDULED_QUERY = "SELECT * FROM Emails WHERE recipient_id = ? AND status = 'PENDING' AND email_category = 'FOLLOW_UP' AND followup_number = ? AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String IS_INITIAL_EMAIL_SENT_QUERY = "SELECT * FROM Emails WHERE recipient_id = ? AND status = 'SENT' AND email_category = 'INITIAL' AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String IS_FOLLOWUP_EMAIL_SENT_QUERY = "SELECT * FROM Emails WHERE recipient_id = ? AND status = 'SENT' AND email_category = 'FOLLOW_UP' AND followup_number = ? AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String GET_INITIAL_EMAIL_FROM_RECIPIENT_ID_QUERY = "SELECT * FROM Emails WHERE recipient_id = ? and email_category = 'INITIAL' AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String GET_FOLLOWUP_EMAILS_BY_INITIAL_EMAIL_ID_QUERY = "SELECT * FROM Emails WHERE initial_email_id = ? AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String GET_EMAILS_FROM_RECIPIENT_ID_QUERY = "SELECT * FROM Emails WHERE recipient_id = ? AND email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";
    private static final String GET_ALL_EMAILS_QUERY = "SELECT * FROM Emails WHERE email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";

    public EmailDao(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected EmailEntity mapResultSetToEntity(ResultSet resultSet) throws SQLException {
        return new EmailEntity(
                resultSet.getInt("id"),
                resultSet.getString("subject"),
                resultSet.getString("body"),
                resultSet.getString("status"),
                resultSet.getTimestamp("scheduled_date"),
                resultSet.getString("email_category"),
                resultSet.getInt("followup_number"),
                resultSet.getString("thread_id"),
                resultSet.getInt("schedule_id"),
                resultSet.getInt("initial_email_id"),
                resultSet.getInt("recipient_id")
        );
    }

    public int insertEmail(EmailEntity email) throws EmailDaoException {
        try {
            LOGGER.info("Inserting email into Database: " + email.getSubject());
            return insert(INSERT_EMAIL_QUERY,
                    email.getSubject(),
                    email.getBody(),
                    email.getStatus(),
                    email.getScheduled_date(),
                    email.getEmail_category(),
                    email.getFollowup_number(),
                    email.getThread_id(),
                    email.getSchedule_id(),
                    email.getInitial_email_id(),
                    email.getRecipient_id());
        } catch (SQLException e) {
            throw new EmailDaoException("Error inserting email: " + email.getSubject(), e);
        }
    }

    public boolean updateEmailById(int id, EmailEntity email) throws EmailDaoException {
        try {
            LOGGER.info("Updating email with ID: " + id);
            return update(UPDATE_EMAIL_BY_ID_QUERY,
                    email.getSubject(),
                    email.getBody(),
                    email.getStatus(),
                    email.getScheduled_date(),
                    email.getEmail_category(),
                    email.getFollowup_number(),
                    email.getThread_id(),
                    email.getSchedule_id(),
                    email.getInitial_email_id(),
                    email.getRecipient_id(),
                    id);
        } catch (SQLException e) {
            throw new EmailDaoException("Error updating email with ID: " + id, e);
        }
    }

    public boolean deleteEmailById(int id) throws EmailDaoException {
        try {
            LOGGER.info("Deleting email with ID: " + id);
            return delete(DELETE_EMAIL_BY_ID_QUERY, id);
        } catch (SQLException e) {
            throw new EmailDaoException("Error deleting email with ID: " + id, e);
        }
    }

    public EmailEntity findById(int id) throws EmailDaoException {
        try {
            LOGGER.info("Finding email with ID: " + id);
            return findById(FIND_EMAIL_BY_ID_QUERY, id);
        } catch (SQLException e) {
            throw new EmailDaoException("Error finding email with ID: " + id, e);
        }
    }

    public List<EmailEntity> findPendingEmailsToSend() throws EmailDaoException {
        try {
            LOGGER.info("Finding emails have the status pending and need to be send");
            return executeQueryWithListResult(FIND_PENDING_EMAILS_TO_SEND_QUERY, this::mapResultSetToEntity);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to find pending emails to send", e);
        }
    }

    public boolean isInitialEmailScheduled(int recipientId) throws EmailDaoException {
        try {
            LOGGER.info("Checking if the initial email is scheduled for recipient with id: " + recipientId);
            return executeQueryForBoolean(IS_INITIAL_EMAIL_SCHEDULED_QUERY, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to check if the initial email is scheduled for recipient with id: " + recipientId, e);
        }
    }

    public boolean isFollowUpEmailScheduled(int recipientId, int followUpNumber) throws EmailDaoException {
        try {
            LOGGER.info("Checking if the followup email is scheduled for recipient with id: " + recipientId);
            return executeQueryForBoolean(IS_FOLLOWUP_EMAIL_SCHEDULED_QUERY, recipientId, followUpNumber);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to check if the initial email is scheduled for recipient with id: " + recipientId, e);
        }
    }

    public boolean isInitialEmailSent(int recipientId) throws EmailDaoException {
        try {
            LOGGER.info("Checking if the initial email is scheduled for recipient with id: " + recipientId);
            return executeQueryForBoolean(IS_INITIAL_EMAIL_SENT_QUERY, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to check if the initial email is scheduled for recipient with id: " + recipientId, e);
        }
    }

    public boolean isFollowUpEmailSent(int recipientId, int followUpNumber) throws EmailDaoException {
        try {
            LOGGER.info("Checking if the followup email is scheduled for recipient with id: " + recipientId);
            return executeQueryForBoolean(IS_FOLLOWUP_EMAIL_SENT_QUERY, recipientId, followUpNumber);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to check if the initial email is scheduled for recipient with id: " + recipientId, e);
        }
    }

    public int getCurrentFollowupNumberForRecipient(int recipientId) throws EmailDaoException {
        try {
            LOGGER.info("Getting follow-up number for recipient with ID: " + recipientId);
            return executeQueryForInt(GET_CURRENT_FOLLOWUP_NUMBER_FOR_RECIPIENT_QUERY, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to get follow-up number for recipient with ID: " + recipientId, e);
        }
    }

    public int getMaxFollowupNumberForSchedule(int scheduleId) throws EmailDaoException {
        try {
            LOGGER.info("Getting max follow-up number for schedule ID: " + scheduleId);
            return executeQueryForInt(GET_MAX_FOLLOWUP_NUMBER_FOR_SCHEDULE_QUERY, scheduleId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to get max follow-up number for schedule ID: " + scheduleId, e);
        }
    }

    public int getIntervalDaysForFollowupNumber(int followupNumber) throws EmailDaoException {
        try {
            LOGGER.info("Getting interval days for follow-up number: " + followupNumber);
            if (followupNumber == 0) return 0;
            return executeQueryForInt(GET_INTERVAL_DAYS_FOR_FOLLOWUP_NUMBER_QUERY, followupNumber);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to get interval days for follow-up number: " + followupNumber, e);
        }
    }

    public ZonedDateTime getLastEmailDateForRecipient(int recipientId) throws EmailDaoException {
        try {
            LOGGER.info("Getting last email date for recipient with ID: " + recipientId);
            return executeQueryForTimestamp(GET_LAST_EMAIL_DATE_FOR_RECIPIENT_QUERY, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to get last email date for recipient with ID: " + recipientId, e);
        }
    }

    public EmailEntity getInitialEmailFromFollowUpId(int followupId) throws EmailDaoException {
        try {
            LOGGER.info("Getting initial email for follow-up with ID: " + followupId);
            return executeQueryWithSingleResult(GET_INITIAL_EMAIL_FROM_FOLLOWUP_ID_QUERY, this::mapResultSetToEntity, followupId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to retrieve the initial email for follow-up ID: " + followupId, e);
        }
    }

    public EmailEntity getInitialEmailFromRecipientId(int recipientId) throws EmailDaoException {
        try {
            LOGGER.info("Getting initial email for recipient with ID: " + recipientId);
            return executeQueryWithSingleResult(GET_INITIAL_EMAIL_FROM_RECIPIENT_ID_QUERY, this::mapResultSetToEntity, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to retrieve the initial email for recipient with ID: " + recipientId, e);
        }
    }

    public List<EmailEntity> getFollowUpEmailsByInitialEmailId(int initialEmailId) throws EmailDaoException {
        try {
            LOGGER.info("Retrieving follow-up emails for initial email ID: " + initialEmailId);
            return executeQueryWithListResult(GET_FOLLOWUP_EMAILS_BY_INITIAL_EMAIL_ID_QUERY, this::mapResultSetToEntity, initialEmailId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to retrieve follow-up emails for initial email ID: " + initialEmailId, e);
        }
    }

    public List<EmailEntity> getEmailsByRecipientId(int recipientId) throws EmailDaoException {
        try {
            LOGGER.info("Getting initial email for recipient with ID: " + recipientId);
            return executeQueryWithListResult(GET_EMAILS_FROM_RECIPIENT_ID_QUERY, this::mapResultSetToEntity, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to retrieve the initial email for recipient with ID: " + recipientId, e);
        }
    }

    public List<EmailEntity> getAllEmails() throws EmailDaoException {
        try {
            LOGGER.info("Getting all emails");
            return findAll(GET_ALL_EMAILS_QUERY);
        } catch (SQLException e) {
            throw new EmailDaoException("Error getting all emails", e);
        }
    }

    public boolean markRecipientAsReplied(int recipientId) throws EmailDaoException {
        try {
            LOGGER.info("Marking recipient as replied with ID: " + recipientId);
            return update(MARK_RECIPIENT_AS_REPLIED, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to mark recipient as replied with ID: " + recipientId, e);
        }

    }

    public String getEmailStatus(int emailId) throws EmailDaoException {
        try {
            LOGGER.info("Getting status for email with ID: " + emailId);
            return executeQueryForString(GET_EMAIL_STATUS_BY_ID, emailId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to get status for email with ID: " + emailId, e);
        }
    }

    public boolean updateEmailStatus(int emailId, String newStatus) throws EmailDaoException {
        try {
            LOGGER.info("Updating status for email with ID: " + emailId);
            return update(UPDATE_EMAIL_STATUS_BY_ID_QUERY, newStatus, emailId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to update status for email with ID: " + emailId, e);
        }
    }

    public int getRecipientIdByEmail(String emailAddress) throws EmailDaoException {
        try {
            LOGGER.info("Finding recipient ID for email address: " + emailAddress);
            String query = "SELECT id FROM Recipients WHERE email_address = ?";
            return executeQueryForInt(query, emailAddress);
        } catch (SQLException e) {
            throw new EmailDaoException("Error finding recipient ID for email address: " + emailAddress, e);
        }
    }

    public int hasEmailReferenceForRecipient(int recipientId) throws EmailDaoException {
        String query = "SELECT COUNT(*) FROM Emails WHERE recipient_id = ?";
        try {
            LOGGER.info("Checking if recipient with ID: " + recipientId + " is connected externally");
            return executeQueryForInt(query, recipientId);
        } catch (SQLException e) {
            throw new EmailDaoException("Failed to check if recipient with ID: " + recipientId + " has a reference in the Emails table", e);
        }
    }
}

