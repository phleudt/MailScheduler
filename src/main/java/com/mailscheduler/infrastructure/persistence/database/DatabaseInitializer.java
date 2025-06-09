package com.mailscheduler.infrastructure.persistence.database;

import com.mailscheduler.infrastructure.persistence.database.exception.SchemaException;
import com.mailscheduler.infrastructure.persistence.database.schema.TableDefinitions;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles database initialization, creating tables if they don't exist.
 */
public class DatabaseInitializer {
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    private final ConnectionManager connectionManager;

    public DatabaseInitializer(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Initializes the database, creating tables if they don't exist.
     *
     * @throws SchemaException if the database cannot be initialized
     */
    public void initializeDatabase() {
        LOGGER.info("Initializing database schema...");

        try (Connection connection = connectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                createTables(statement);
                connection.commit();
                LOGGER.info("Database schema initialized successfully.");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error creating database tables", e);
                connection.rollback();
                throw new SchemaException("Failed to initialize database schema", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error during initialization", e);
            throw new SchemaException("Failed to establish database connection for initialization", e);
        }
    }

    /**
     * Executes the table creation statements.
     *
     * @param statement the SQL statement to use
     * @throws SQLException if a database error occurs
     */
    private void createTables(Statement statement) throws SQLException {
        for (String tableStatement : TableDefinitions.getAllTableStatements()) {
            statement.execute(tableStatement);
        }
    }

    /**
     * Validates the database schema by checking if all required tables exist.
     *
     * @return true if the schema is valid, false otherwise
     */
    public boolean validateSchema() {
        LOGGER.info("Validating database schema...");

        try (Connection connection = connectionManager.getConnection()) {
            String[] requiredTables = TableDefinitions.TABLE_NAMES;

            for (String tableName : requiredTables) {
                if (!tableExists(connection, tableName)) {
                    LOGGER.warning("Required table missing from schema: " + tableName);
                    return false;
                }
            }

            LOGGER.info("Database schema validation successful");
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error validating schema", e);
            return false;
        }
    }


    /**
     * Checks if a table exists in the database.
     *
     * @param connection the database connection
     * @param tableName the name of the table to check
     * @return true if the table exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}