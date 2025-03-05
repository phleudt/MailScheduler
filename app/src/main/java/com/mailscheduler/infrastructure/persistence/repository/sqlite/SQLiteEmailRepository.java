package com.mailscheduler.infrastructure.persistence.repository.sqlite;

import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.email.*;
import com.mailscheduler.domain.recipient.RecipientId;
import com.mailscheduler.domain.schedule.ScheduleId;
import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.infrastructure.persistence.entities.EmailEntity;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.List;

public class SQLiteEmailRepository extends AbstractSQLiteRepository<Email, EmailEntity> {
    private static final String INSERT_EMAIL = "INSERT INTO Emails (subject, body, status, scheduled_date, email_category, followup_number, thread_id, schedule_id, initial_email_id, recipient_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_BY_ID = "UPDATE Emails SET subject = ?, body = ?, status = ?, scheduled_date = ?, email_category = ?, followup_number = ?, thread_id = ?, schedule_id = ?, initial_email_id = ?, recipient_id = ? WHERE id = ?";
    private static final String DELETE_BY_ID = "DELETE FROM Emails WHERE id = ?";
    private static final String FIND_BY_ID = "SELECT * FROM Emails WHERE id = ?";
    private static final String GET_CURRENT_FOLLOWUP_NUMBER_FOR_RECIPIENT_QUERY = "SELECT MAX(followup_number) FROM Emails WHERE recipient_id = ?";
    private static final String GET_MAX_FOLLOWUP_NUMBER_FOR_SCHEDULE_QUERY = "SELECT MAX(followup_number) FROM Schedules WHERE schedule_id = ?";
    private static final String GET_INTERVAL_DAYS_FOR_FOLLOWUP_NUMBER_QUERY = "SELECT interval_days FROM Schedules WHERE followup_number = ?";
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

    public SQLiteEmailRepository(DatabaseManager databaseManager) {
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

    @Override
    protected Email mapToDomainEntity(EmailEntity entity) {
        return new Email.Builder()
                .setId(entity.getId())
                .setSender("phleudt@protonmail.com") // TODO: ###!!!###
                .setRecipientId(entity.getRecipient_id())
                .setSubject(entity.getSubject())
                .setBody(entity.getBody())
                .setStatus(entity.getStatus())
                .setScheduledDate(entity.getScheduled_date() != null ?
                    entity.getScheduled_date().toInstant().atZone(ZoneId.systemDefault()) : null)
                .setCategory(EmailCategory.fromString(entity.getEmail_category()))
                .setFollowupNumber(entity.getFollowup_number())
                .setThreadId(entity.getThread_id())
                .setInitialEmailId(entity.getInitial_email_id())
                .build();
    }

    @Override
    protected EmailEntity mapFromDomainEntity(Email domain) {
        return new EmailEntity(
                domain.getId() != null ? domain.getId().value() : -1,
                domain.getSubject().value(),
                domain.getBody().value(),
                domain.getStatus().toString(),
                domain.getScheduledDate() != null ?
                        Timestamp.from(domain.getScheduledDate().toInstant()) : null,
                domain.getCategory().toString(),
                domain.getFollowupNumber(),
                domain.getThreadId().orElse(""),
                1,
                domain.getInitialEmailId().orElse(EmailId.of(0)).value(),
                domain.getRecipientId().value()
        );
    }

    @Override
    protected Object[] extractParameters(EmailEntity entity, Object... additionalParams) {
        Object[] baseParams = new Object[] {
                entity.getSubject(),
                entity.getBody(),
                entity.getStatus(),
                entity.getScheduled_date(),
                entity.getEmail_category(),
                entity.getFollowup_number(),
                entity.getThread_id(),
                entity.getSchedule_id(),
                entity.getInitial_email_id(),
                entity.getRecipient_id()
        };

        // If additional parameters (like ID for UPDATE) are provided, add them
        if (additionalParams.length > 0) {
            Object[] allParams = new Object[baseParams.length + additionalParams.length];
            System.arraycopy(baseParams, 0, allParams, 0, baseParams.length);
            System.arraycopy(additionalParams, 0, allParams, baseParams.length, additionalParams.length);
            return allParams;
        }

        return baseParams;
    }

    @Override
    public Optional<Email> findById(int id) throws RepositoryException {
        try {
            Optional<EmailEntity> entity = executeQueryForSingleResult(FIND_BY_ID, id);
            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find email with ID: " + id, e);
        }
    }

    public Email save(Email email) throws RepositoryException {
        try {
            return save(INSERT_EMAIL, email);
        } catch (RepositoryException e) {
            throw new RepositoryException("Failed to save email", e);
        }
    }

    public boolean update(Email email) throws RepositoryException {
        try {
            return update(UPDATE_BY_ID, email, email.getId().value());
        } catch (RepositoryException e) {
            throw new RepositoryException("Failed to update email", e);
        }
    }

    public boolean delete(EmailId id) throws RepositoryException {
        try {
            return delete(DELETE_BY_ID, id);
        } catch (RepositoryException e) {
            throw new RepositoryException("Failed to delete email", e);
        }
    }


    public List<Email> getByRecipientId(RecipientId recipientId) throws RepositoryException {
        try {
            return executeQueryForList(GET_EMAILS_FROM_RECIPIENT_ID_QUERY, recipientId.value()).stream()
                    .map(this::mapToDomainEntity)
                    .toList();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to retrieve the initial email for recipient with ID: " + recipientId, e);
        }
    }

    public int getCurrentFollowupNumberForRecipient(RecipientId recipientId) throws RepositoryException {
        try {
            return executeQueryForInt(GET_CURRENT_FOLLOWUP_NUMBER_FOR_RECIPIENT_QUERY, recipientId.value());
        } catch (SQLException e) {
            throw new RepositoryException("Failed to get follow-up number for recipient with ID: " + recipientId, e);
        }
    }

    public int getMaxFollowupNumberForSchedule(ScheduleId scheduleId) throws RepositoryException {
        try {
            return executeQueryForInt(GET_MAX_FOLLOWUP_NUMBER_FOR_SCHEDULE_QUERY, scheduleId.value());
        } catch (SQLException e) {
            throw new RepositoryException("Failed to get max follow-up number for schedule ID: " + scheduleId.value(), e);
        }
    }

    public int getIntervalDaysForFollowupNumber(int followupNumber) throws RepositoryException {
        try {
            if (followupNumber == 0) return 0;
            return executeQueryForInt(GET_INTERVAL_DAYS_FOR_FOLLOWUP_NUMBER_QUERY, followupNumber);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to get interval days for follow-up number: " + followupNumber, e);
        }
    }

    public ZonedDateTime getLastEmailDateForRecipient(RecipientId recipientId) throws RepositoryException {
        try {
            return executeQueryForTimestamp(GET_LAST_EMAIL_DATE_FOR_RECIPIENT_QUERY, recipientId.value());
        } catch (SQLException e) {
            throw new RepositoryException("Failed to get last email date for recipient with ID: " + recipientId, e);
        }
    }

    public int hasEmailReferenceForRecipient(RecipientId recipientId) throws RepositoryException {
        String query = "SELECT COUNT(*) FROM Emails WHERE recipient_id = ?";
        try {
            return executeQueryForInt(query, recipientId.value());
        } catch (SQLException e) {
            throw new RepositoryException("Failed to check if recipient with ID: " + recipientId + " has a reference in the Emails table", e);
        }
    }

    public RecipientId getRecipientIdByEmail(EmailAddress emailAddress) throws RepositoryException {
        try {
            String query = "SELECT id FROM Recipients WHERE email_address = ?";
            return RecipientId.of(executeQueryForInt(query, emailAddress.value()));
        } catch (SQLException e) {
            throw new RepositoryException("Error finding recipient ID for email address: " + emailAddress.value(), e);
        }
    }

    public List<Email> findPendingEmailsToSend() throws RepositoryException {
        try {
            return executeQueryForList(FIND_PENDING_EMAILS_TO_SEND_QUERY).stream().map(this::mapToDomainEntity).toList();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find pending emails to send", e);
        }
    }

    public boolean markRecipientAsReplied(RecipientId recipientId) throws RepositoryException {
        try {
            return executeUpdate(MARK_RECIPIENT_AS_REPLIED, recipientId.value()) > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to mark recipient as replied with ID: " + recipientId.value(), e);
        }

    }

    public List<Email> getFollowUpEmailsByInitialEmailId(EmailId initialEmailId) throws RepositoryException {
        try {
            return executeQueryForList(GET_FOLLOWUP_EMAILS_BY_INITIAL_EMAIL_ID_QUERY, initialEmailId.value()).stream().map(this::mapToDomainEntity).toList();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to retrieve follow-up emails for initial email ID: " + initialEmailId, e);
        }
    }

    public Optional<Email> getInitialEmailFromRecipientId(RecipientId recipientId) throws RepositoryException {
        try {
            return executeQueryForSingleResult(GET_INITIAL_EMAIL_FROM_RECIPIENT_ID_QUERY, recipientId).map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to retrieve the initial email for recipient with ID: " + recipientId, e);
        }
    }

    public boolean updateEmailStatus(EmailId emailId, EmailStatus newStatus) throws RepositoryException {
        try {
            return executeUpdate(UPDATE_EMAIL_STATUS_BY_ID_QUERY, newStatus.toString(), emailId.value()) > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update email status for ID: " + emailId.value() + " to status: " + newStatus, e);
        }
    }
}
