package com.mailscheduler.database.dao;

import com.mailscheduler.database.DatabaseManager;
import com.mailscheduler.database.entities.RecipientEntity;
import com.mailscheduler.exception.dao.RecipientDaoException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class RecipientDao extends GenericDao<RecipientEntity> {
    private static final Logger LOGGER = Logger.getLogger(RecipientDao.class.getName());

    private static final String INSERT_RECIPIENT_QUERY =
            "INSERT INTO Recipients (name, email_address, gender, domain, phone_number, initial_email_date, has_replied, spreadsheet_row) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_RECIPIENT_BY_ID_QUERY =
            "UPDATE Recipients SET name = ?, email_address = ?, gender = ?, domain = ?, phone_number = ?, " +
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

    public RecipientDao(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected RecipientEntity mapResultSetToEntity(ResultSet resultSet) throws SQLException {
        return new RecipientEntity(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("email_address"),
                resultSet.getString("gender"),
                resultSet.getString("domain"),
                resultSet.getString("phone_number"),
                resultSet.getTimestamp("initial_email_date"),
                resultSet.getBoolean("has_replied"),
                resultSet.getInt("spreadsheet_row")
        );
    }

    public int insertRecipient(RecipientEntity recipient) throws RecipientDaoException {
        try {
            LOGGER.info("Inserting recipient into Database: " + recipient.getName());
            return insert(INSERT_RECIPIENT_QUERY,
                    recipient.getName(),
                    recipient.getEmail_address(),
                    recipient.getDomain(),
                    recipient.getPhone_number(),
                    recipient.getInitial_email_date(),
                    recipient.has_replied(),
                    recipient.getSpreadsheet_row()
            );
        } catch (SQLException e) {
            throw new RecipientDaoException("Error inserting recipient: " + recipient.getName(), e);
        }
    }

    public boolean updateRecipientById(int id, RecipientEntity recipient) throws RecipientDaoException {
        try {
            LOGGER.info("Updating recipient with ID: " + id);
            return update(UPDATE_RECIPIENT_BY_ID_QUERY,
                    recipient.getName(),
                    recipient.getEmail_address(),
                    recipient.getDomain(),
                    recipient.getPhone_number(),
                    recipient.getInitial_email_date(),
                    recipient.has_replied(),
                    recipient.getSpreadsheet_row(),
                    id
            );
        } catch (SQLException e) {
            throw new RecipientDaoException("Error updating recipient with id: " + id, e);
        }
    }

    public boolean deleteRecipientById(int id) throws RecipientDaoException {
        try {
            LOGGER.info("Deleting recipient with ID: " + id);
            return delete(DELETE_RECIPIENT_BY_ID_QUERY, id);
        } catch (SQLException e) {
            throw new RecipientDaoException("Error deleting recipient with ID: " + id, e);
        }
    }

    public RecipientEntity findRecipientById(int id) throws RecipientDaoException {
        try {
            LOGGER.info("Finding recipient with ID: " + id);
            return findById(FIND_RECIPIENT_BY_ID_QUERY, id);
        } catch (SQLException e) {
            throw new RecipientDaoException("Error finding recipient with ID: " + id, e);
        }
    }

    public String findEmailAddressById(int id) throws RecipientDaoException {
        try {
            LOGGER.info("Finding email address with ID: " + id);
            return executeQueryWithSingleResult(FIND_EMAIL_ADDRESS_BY_ID_QUERY, resultSet -> resultSet.getString("email_address"), id);
        } catch (SQLException e) {
            throw new RecipientDaoException("Error finding email address with ID: " + id, e);
        }
    }

    public RecipientEntity findRecipientByEmailAddress(String emailAddress) throws RecipientDaoException {
        try {
            LOGGER.info("Finding email address: " + emailAddress + ", in database");
            return executeQueryWithSingleResult(FIND_RECIPIENT_BY_EMAIL_ADDRESS_QUERY, this::mapResultSetToEntity, emailAddress);
        } catch (SQLException e) {
            throw new RecipientDaoException("Error finding recipient with email address: " + emailAddress, e);
        }
    }

    public boolean isInitialEmailDateSet(int id) throws RecipientDaoException {
        try {
            LOGGER.info("Checking if the initial email date is set for id: " + id);
            return executeQueryForBoolean(IS_INITIAL_EMAIL_DATE_SET_QUERY, id);
        } catch (SQLException e) {
            throw new RecipientDaoException("Error checking if initial email date is set for recipient with ID: " + id, e);
        }
    }

    public List<RecipientEntity> getRecipientsWithInitialEmailDate() throws RecipientDaoException {
        try {
            LOGGER.info("Getting recipients with initial email date set");
            return findAll(GET_RECIPIENTS_WITH_INITIAL_EMAIL_DATE_QUERY);
        } catch (SQLException e) {
            throw new RecipientDaoException("Error getting recipients with initial email date set", e);
        }
    }


    public List<RecipientEntity> getAllRecipients() throws RecipientDaoException {
        try {
            LOGGER.info("Getting all recipients");
            return findAll(GET_ALL_RECIPIENTS_QUERY);
        } catch (SQLException e) {
            throw new RecipientDaoException("Error getting all recipients", e);
        }
    }

    public RecipientEntity findRecipientByUniqueIdentifiers(
            String emailAddress,
            String name,
            String phoneNumber,
            String domain
    ) throws RecipientDaoException {
        try {
            LOGGER.info("Searching for recipient with unique identifiers");

            String findByUniqueIdentifiersQuery = "SELECT * FROM Recipients " +
                    "WHERE email_address = ? OR " +
                    "name = ? OR " +
                    "phone_number = ? OR " +
                    "domain = ? " +
                    "LIMIT 1";

            return executeQueryWithSingleResult(
                    findByUniqueIdentifiersQuery,
                    this::mapResultSetToEntity,
                    emailAddress,
                    name,
                    phoneNumber,
                    domain
            );
        } catch (SQLException e) {
            throw new RecipientDaoException("Error finding recipient by unique identifiers", e);
        }
    }
}
