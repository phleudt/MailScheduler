package com.mailscheduler.database.dao;

import com.mailscheduler.database.DatabaseManager;
import com.mailscheduler.database.entities.EmailTemplateEntity;
import com.mailscheduler.exception.dao.EmailTemplateDaoException;
import com.mailscheduler.model.TemplateCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Data Access Object for managing email templates in the database.
 * Provides CRUD operations for email templates with robust error handling.
 */
public class EmailTemplateDao extends GenericDao<EmailTemplateEntity> {
    private static final Logger LOGGER = Logger.getLogger(EmailTemplateDao.class.getName());

    // SQL Queries
    private static final String INSERT_EMAIL_TEMPLATE_QUERY = "INSERT INTO EmailTemplates (name, draft_id, template_category, subject_template, body_template, placeholder_symbols, placeholders, followup_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_EMAIL_TEMPLATE_BY_ID_QUERY = "UPDATE EmailTemplates SET name = ?, draft_id = ?, template_category = ?, subject_template = ?, body_template = ?, placeholder_symbols = ?, placeholders = ?, followup_number = ? WHERE id = ?";
    private static final String DELETE_EMAIL_TEMPLATE_BY_ID_QUERY = "DELETE FROM EmailTemplates WHERE id = ?";
    private static final String FIND_EMAIL_TEMPLATE_BY_ID_QUERY = "SELECT * FROM EmailTemplates WHERE id = ?";
    private static final String GET_ALL_EMAIL_TEMPLATES_QUERY = "SELECT * FROM EmailTemplates";
    private static final String FIND_DEFAULT_INITIAL_EMAIL_TEMPLATE_QUERY = "SELECT * FROM EmailTemplates WHERE  template_category = 'DEFAULT_INITIAL'";
    private static final String FIND_DEFAULT_DEFAULT_FOLLOW_UP_EMAIL_TEMPLATE_BY_NUMBER_QUERY = "SELECT * FROM EmailTemplates WHERE  template_category = 'DEFAULT_FOLLOW_UP' AND followup_number = ?";
    private static final String FIND_BY_DRAFT_ID_QUERY = "SELECT * FROM EmailTemplates WHERE draft_id = ?";
    private static final String COUNT_DEFAULT_FOLLOW_UP_TEMPLATES_QUERY = "SELECT COUNT(*) FROM EmailTemplates WHERE template_category = 'DEFAULT_FOLLOW_UP'";
    private static final String GET_SCHEDULE_COUNT_QUERY = "SELECT COUNT(*) FROM FollowUpSchedules WHERE schedule_id = ?";
    private static final String DELETE_BY_CATEGORY_QUERY = "DELETE FROM EmailTemplates WHERE template_category = ?";
    private static final String FIND_BY_CATEGORY_QUERY = "SELECT * FROM EmailTemplates WHERE template_category = ?";

    /**
     * Constructor for EmailTemplateDao.
     *
     * @param databaseManager The database manager to handle database connections
     */
    public EmailTemplateDao(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    /**
     * Maps a ResultSet to an EmailTemplateEntity.
     *
     * @param resultSet The ResultSet to map
     * @return An EmailTemplateEntity populated with data from the ResultSet
     * @throws SQLException if there's an error accessing ResultSet data
     */
    @Override
    protected EmailTemplateEntity mapResultSetToEntity(ResultSet resultSet) throws SQLException {
        return new EmailTemplateEntity(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("draft_id"),
                resultSet.getString("template_category"),
                resultSet.getString("subject_template"),
                resultSet.getString("body_template"),
                resultSet.getString("placeholder_symbols"),
                resultSet.getString("placeholders"),
                resultSet.getInt("followup_number")
        );
    }

    /**
     * Inserts a new email template into the database.
     *
     * @param emailTemplate The email template to insert
     * @return The ID of the inserted email template
     * @throws EmailTemplateDaoException if there's an error during insertion
     */
    public int insertEmailTemplate(EmailTemplateEntity emailTemplate) throws EmailTemplateDaoException {
        try {
            LOGGER.info("Inserting email template into Database: " + emailTemplate.getName());
            return insert(INSERT_EMAIL_TEMPLATE_QUERY,
                    emailTemplate.getName(),
                    emailTemplate.getDraft_id(),
                    emailTemplate.getTemplate_category(),
                    emailTemplate.getSubject_template(),
                    emailTemplate.getBody_template(),
                    emailTemplate.getPlaceholder_symbols(),
                    emailTemplate.getPlaceholders(),
                    emailTemplate.getFollowup_number()
            );
        } catch (SQLException e) {
            throw new EmailTemplateDaoException.Insertion("Error inserting email template: " + emailTemplate.getName(), e);
        }
    }

    /**
     * Updates an existing email template.
     *
     * @param id The ID of the email template to update
     * @param emailTemplate The updated email template data
     * @return true if the update was successful, false otherwise
     * @throws EmailTemplateDaoException if there's an error during update
     */
    public boolean updateEmailTemplateById(int id, EmailTemplateEntity emailTemplate) throws EmailTemplateDaoException {
        try {
            LOGGER.info("Updating email template with ID: " + id);
            boolean updated = update(UPDATE_EMAIL_TEMPLATE_BY_ID_QUERY,
                    emailTemplate.getName(),
                    emailTemplate.getDraft_id(),
                    emailTemplate.getTemplate_category(),
                    emailTemplate.getSubject_template(),
                    emailTemplate.getBody_template(),
                    emailTemplate.getPlaceholder_symbols(),
                    emailTemplate.getPlaceholders(),
                    emailTemplate.getFollowup_number(),
                    id);

            if (!updated) {
                throw new EmailTemplateDaoException.NotFound(id);
            }
            return true;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error updating email template with ID: " + id, e);
        }
    }

    /**
     * Deletes an email template by its ID.
     *
     * @param id The ID of the email template to delete
     * @return true if the deletion was successful, false otherwise
     * @throws EmailTemplateDaoException if there's an error during deletion
     */
    public boolean deleteEmailTemplateById(int id) throws EmailTemplateDaoException {
        try {
            LOGGER.info("Deleting email template with ID: " + id);
            boolean deleted = delete(DELETE_EMAIL_TEMPLATE_BY_ID_QUERY, id);
            if (!deleted) {
                throw new EmailTemplateDaoException.NotFound(id);
            }
            return true;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error deleting email template with ID: " + id, e);
        }
    }

    /**
     * Finds an email template by its ID.
     *
     * @param id The ID of the email template to find
     * @return The found EmailTemplateEntity
     * @throws EmailTemplateDaoException if the template is not found or there's an error
     */
    public EmailTemplateEntity findById(int id) throws EmailTemplateDaoException {
        try {
            LOGGER.info("Finding email template with ID: " + id);
            EmailTemplateEntity template = findById(FIND_EMAIL_TEMPLATE_BY_ID_QUERY, id);
            if (template == null) {
                throw new EmailTemplateDaoException.NotFound(id);
            }
            return template;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error finding email template with ID: " + id, e);
        }
    }

    /**
     * Retrieves all email templates.
     *
     * @return A list of all EmailTemplateEntities
     * @throws EmailTemplateDaoException if there's an error retrieving templates
     */
    public List<EmailTemplateEntity> getAllEmailTemplates() throws EmailTemplateDaoException {
        try {
            LOGGER.info("Getting all email templates");
            return findAll(GET_ALL_EMAIL_TEMPLATES_QUERY);
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error getting all email templates", e);
        }
    }

    /**
     * Finds the default initial email template.
     *
     * @return The default initial EmailTemplateEntity
     * @throws EmailTemplateDaoException if the template is not found or there's an error
     */
    public EmailTemplateEntity findDefaultInitialEmailTemplate() throws EmailTemplateDaoException {
        try {
            EmailTemplateEntity template = executeQueryWithSingleResult(FIND_DEFAULT_INITIAL_EMAIL_TEMPLATE_QUERY, this::mapResultSetToEntity);
            if (template == null) {
                throw new EmailTemplateDaoException.NotFound("Default initial email template not found");
            }
            return template;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error finding default initial email template", e);
        }
    }

    /**
     * Finds a default follow-up email template by follow-up number.
     *
     * @param followupNumber The follow-up number
     * @return The corresponding EmailTemplateEntity
     * @throws EmailTemplateDaoException if the template is not found or there's an error
     */
    public EmailTemplateEntity findDefaultFollowUpEmailTemplateByNumber(int followupNumber) throws EmailTemplateDaoException {
        try {
            EmailTemplateEntity template = executeQueryWithSingleResult(FIND_DEFAULT_DEFAULT_FOLLOW_UP_EMAIL_TEMPLATE_BY_NUMBER_QUERY, this::mapResultSetToEntity, followupNumber);

            if (template == null) {
                throw new EmailTemplateDaoException.NotFound("Default follow-up email template not found for number: " + followupNumber);
            }
            return template;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error finding follow-up email template for follow-up number: " + followupNumber, e);
        }
    }

    /**
     * Finds an email template by draft ID.
     *
     * @param draftId The draft ID to search for
     * @return The corresponding EmailTemplateEntity
     * @throws EmailTemplateDaoException if the template is not found or there's an error
     */
    public EmailTemplateEntity findByDraftId(String draftId) throws EmailTemplateDaoException {
        try {
            EmailTemplateEntity template = executeQueryWithSingleResult(FIND_BY_DRAFT_ID_QUERY, this::mapResultSetToEntity, draftId);

            if (template == null) {
                return null;
                // throw new EmailTemplateDaoException.NotFound("Email template not found for draft ID: " + draftId);
            }
            return template;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Failed to find email template by draft ID: " + draftId, e);
        }
    }

    public boolean doesDefaultInitialTemplateExist() throws EmailTemplateDaoException {
        try {
            EmailTemplateEntity template = executeQueryWithSingleResult(FIND_DEFAULT_INITIAL_EMAIL_TEMPLATE_QUERY, this::mapResultSetToEntity);
            return template != null;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Failed to check for default initial email template", e);
        }
    }

    public boolean doesDefaultFollowUpTemplateExist(int followupNumber) throws EmailTemplateDaoException {
        try {
            EmailTemplateEntity template = executeQueryWithSingleResult(FIND_DEFAULT_DEFAULT_FOLLOW_UP_EMAIL_TEMPLATE_BY_NUMBER_QUERY, this::mapResultSetToEntity, followupNumber);
            return template != null;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Failed to check for follow-up email template for follow-up number: " + followupNumber, e);
        }
    }

    /**
     * Delete all email templates of a specific category.
     *
     * @param category The template category to delete
     * @throws EmailTemplateDaoException if there's an error during deletion
     */
    public void deleteByCategory(TemplateCategory category) throws EmailTemplateDaoException {
        try {
            LOGGER.info("Deleting email templates with category: " + category);
            boolean deleted = delete(DELETE_BY_CATEGORY_QUERY, category.name());

            // Note: We don't throw NotFound exception here because deleting non-existent
            // templates is considered a success case when clearing a category

        } catch (SQLException e) {
            throw new EmailTemplateDaoException(
                    "Error deleting email templates with category: " + category,
                    e
            );
        }
    }

    /**
     * Find an email template by its category.
     * If multiple templates exist for the category, returns the first one found.
     *
     * @param category The template category to search for
     * @return The found EmailTemplateEntity
     * @throws EmailTemplateDaoException if the template is not found or there's an error
     */
    public EmailTemplateEntity findByCategory(TemplateCategory category) throws EmailTemplateDaoException {
        try {
            LOGGER.info("Finding email template with category: " + category);
            EmailTemplateEntity template = executeQueryWithSingleResult(
                    FIND_BY_CATEGORY_QUERY,
                    this::mapResultSetToEntity,
                    category.name()
            );

            if (template == null) {
                throw new EmailTemplateDaoException.NotFound(
                        "Email template not found for category: " + category
                );
            }
            return template;
        } catch (SQLException e) {
            throw new EmailTemplateDaoException(
                    "Error finding email template with category: " + category,
                    e
            );
        }
    }

    /**
     * Counts the number of default follow-up templates.
     *
     * @return The count of default follow-up templates
     * @throws EmailTemplateDaoException if there's an error during the count
     */
    public int countDefaultFollowUpTemplates() throws EmailTemplateDaoException {
        try {
            return executeQueryWithSingleResult(COUNT_DEFAULT_FOLLOW_UP_TEMPLATES_QUERY, resultSet -> resultSet.getInt(1));
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error counting default follow-up templates", e);
        }
    }

    /**
     * Gets the count of schedules for a given schedule ID.
     *
     * @param scheduleId The schedule ID
     * @return The count of schedules
     * @throws EmailTemplateDaoException if there's an error during the count
     */
    public int getScheduleCount(int scheduleId) throws EmailTemplateDaoException {
        try {
            return executeQueryWithSingleResult(GET_SCHEDULE_COUNT_QUERY, resultSet -> resultSet.getInt(1), scheduleId);
        } catch (SQLException e) {
            throw new EmailTemplateDaoException("Error getting schedule count for schedule ID: " + scheduleId, e);
        }
    }
}