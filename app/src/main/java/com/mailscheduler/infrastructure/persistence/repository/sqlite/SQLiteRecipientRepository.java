package com.mailscheduler.infrastructure.persistence.repository.sqlite;

import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.recipient.FullName;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.domain.recipient.RecipientId;
import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.infrastructure.persistence.entities.RecipientEntity;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class SQLiteRecipientRepository extends AbstractSQLiteRepository<Recipient, RecipientEntity> {
    private static final String FIND_BY_ID = "SELECT * FROM Recipients WHERE id = ?";
    private static final String INSERT_RECIPIENT_QUERY =
            "INSERT INTO Recipients (name, email_address, salutation, domain, phone_number, initial_email_date, has_replied, spreadsheet_row) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_RECIPIENT_BY_ID_QUERY =
            "UPDATE Recipients SET name = ?, email_address = ?, salutation = ?, domain = ?, phone_number = ?, " +
                    "initial_email_date = ?, has_replied = ?, spreadsheet_row = ? WHERE id = ?";
    private static final String DELETE_RECIPIENT_BY_ID_QUERY = "DELETE FROM Recipients WHERE id = ?";
    private static final String FIND_RECIPIENT_BY_ID_QUERY = "SELECT * FROM Recipients WHERE id = ?";
    private static final String FIND_EMAIL_ADDRESS_BY_ID_QUERY = "SELECT email_address FROM Recipients WHERE id = ?";
    private static final String FIND_RECIPIENT_BY_EMAIL_ADDRESS_QUERY = "SELECT * FROM Recipients WHERE email_address = ?";
    private static final String IS_INITIAL_EMAIL_DATE_SET_QUERY = "SELECT initial_email_date FROM Recipients WHERE id = ? AND initial_email_date IS NOT NULL";

    private static final String GET_RECIPIENTS_WITH_INITIAL_EMAIL_DATE_QUERY =
            "SELECT r.* " +
                    "FROM Recipients r " +
                    "LEFT JOIN Emails e " +
                    "ON r.id = e.recipient_id " +
                    "WHERE r.initial_email_date IS NOT NULL " +
                    "AND r.has_replied = FALSE " +
                    "AND (e.email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP') OR e.recipient_id IS NULL)";

    private static final String GET_ALL_RECIPIENTS_QUERY = "SELECT r.* FROM Recipients r " +
            "JOIN Emails e ON r.id = e.recipient_id " +
            "AND e.email_category NOT IN ('EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')";

    public SQLiteRecipientRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected RecipientEntity mapResultSetToEntity(ResultSet resultSet) throws SQLException {
        return new RecipientEntity(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("email_address"),
                resultSet.getString("salutation"),
                resultSet.getString("domain"),
                resultSet.getString("phone_number"),
                resultSet.getTimestamp("initial_email_date"),
                resultSet.getBoolean("has_replied"),
                resultSet.getInt("spreadsheet_row")
        );
    }

    @Override
    protected Recipient mapToDomainEntity(RecipientEntity entity) {
        ZonedDateTime initialEmailDate = toZonedDateTime(entity.getInitial_email_date());
        return new Recipient.Builder()
                .setId(entity.getId())
                .setName(entity.getName())
                .setEmailAddress(entity.getEmail_address())
                .setSalutation(entity.getSalutation())
                .setDomain(entity.getDomain())
                .setPhoneNumber(entity.getPhone_number())
                .setSpreadsheetRow(entity.getSpreadsheet_row())
                .setInitialEmailDate(initialEmailDate)
                .setPreserveInitialEmailDate(true)
                .setHasReplied(entity.has_replied())
                .build();
    }

    @Override
    protected RecipientEntity mapFromDomainEntity(Recipient recipient) {
        Timestamp timestamp = toTimestamp(recipient.getInitialEmailDate());
        return new RecipientEntity(
                recipient.getId() != null ? recipient.getId().value() : -1,
                recipient.getName() != null ? recipient.getName().value() : "",
                recipient.getEmailAddress().value(),
                recipient.getSalutation(),
                recipient.getDomain(),
                recipient.getPhoneNumber(),
                timestamp,
                recipient.hasReplied(),
                recipient.getSpreadsheetRow().toRowIndex()
        );
    }

    @Override
    protected Object[] extractParameters(RecipientEntity entity, Object... additionalParams) {
        Object[] baseParams = new Object[] {
                entity.getName(),
                entity.getEmail_address(),
                entity.getSalutation(),
                entity.getDomain(),
                entity.getPhone_number(),
                entity.getInitial_email_date(),
                entity.has_replied(),
                entity.getSpreadsheet_row()
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

    private static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant().atZone(ZoneId.systemDefault()) : null;
    }

    private static Timestamp toTimestamp(ZonedDateTime dateTime) {
        return dateTime != null ? Timestamp.from(dateTime.toInstant()) : null;
    }

    @Override
    public Optional<Recipient> findById(int id) throws RepositoryException {
        try {
            Optional<RecipientEntity> entity = executeQueryForSingleResult(FIND_BY_ID, id);
            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Error finding recipient with ID: " + id, e);
        }
    }

    public Recipient save(Recipient recipient) throws RepositoryException {
        try {
            return save(INSERT_RECIPIENT_QUERY, recipient);
        } catch (RepositoryException e) {
            throw new RepositoryException("Failed to save recipient: " + recipient.getName().value(), e);
        }
    }

    public boolean update(Recipient recipient) throws RepositoryException {
        try {
            return update(UPDATE_RECIPIENT_BY_ID_QUERY, recipient, recipient.getId().value());
        } catch (RepositoryException e) {
            throw new RepositoryException("Error updating recipient with id: " + recipient.getId().value(), e);
        }
    }

    public boolean delete(RecipientId id) throws RepositoryException {
        try {
            return delete(DELETE_RECIPIENT_BY_ID_QUERY, id.value());
        } catch (RepositoryException e) {
            throw new RepositoryException("Error deleting recipient with ID: " + id.value(), e);
        }
    }

    public List<Recipient> getRecipientsWithInitialEmailDate() throws RepositoryException {
        try {
            return findAll(GET_RECIPIENTS_WITH_INITIAL_EMAIL_DATE_QUERY);
        } catch (RepositoryException e) {
            throw new RepositoryException("Error getting recipients with initial email date set", e);
        }
    }

    public EmailAddress findEmailAddressById(RecipientId id) throws RepositoryException {
        try {
            String emailAddress = executeQueryForString(FIND_EMAIL_ADDRESS_BY_ID_QUERY, id.value());
            if (emailAddress == null) {
                throw new RepositoryException("Email address not found for ID: " + id.value());
            }
            return EmailAddress.of(emailAddress);
        } catch (SQLException e) {
            throw new RepositoryException("Error finding email address with ID: " + id, e);
        }
    }

    public Optional<Recipient> findRecipientByUniqueIdentifiers(
            EmailAddress emailAddress,
            FullName name,
            String phoneNumber,
            String domain
    ) throws RepositoryException {
        try {
            String findByUniqueIdentifiersQuery = "SELECT * FROM Recipients " +
                    "WHERE email_address = ? AND " +
                    "name = ? AND " +
                    "phone_number = ? AND " +
                    "domain = ? " +
                    "LIMIT 1";

            return executeQueryForSingleResult(
                    findByUniqueIdentifiersQuery,
                    emailAddress.value(),
                    name.value(),
                    phoneNumber,
                    domain
            ).map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Error finding recipient by unique identifiers", e);
        }
    }

    public Optional<Recipient> findByEmailAddress(EmailAddress emailAddress) throws RepositoryException {
        try {
            Optional<RecipientEntity> entity = executeQueryForSingleResult(
                    FIND_RECIPIENT_BY_EMAIL_ADDRESS_QUERY,
                    emailAddress.value()
            );
            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Error finding recipient with email address: " + emailAddress.value(), e);
        }
    }

    public boolean isInitialEmailDateSet(RecipientId id) throws RepositoryException {
        try {
            return executeQueryForBoolean(IS_INITIAL_EMAIL_DATE_SET_QUERY, id.value());
        } catch (SQLException e) {
            throw new RepositoryException("Error checking if initial email date is set for recipient with ID: " + id.value(), e);
        }
    }

    public List<Recipient> getAllRecipients() throws RepositoryException {
        try {
            return findAll(GET_ALL_RECIPIENTS_QUERY);
        } catch (RepositoryException e) {
            throw new RepositoryException("Error getting all recipients", e);
        }
    }
}
