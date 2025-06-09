package com.mailscheduler.infrastructure.persistence.database;

import com.mailscheduler.infrastructure.persistence.database.config.DatabaseConfig;
import com.mailscheduler.infrastructure.persistence.database.exception.MaintenanceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides database maintenance operations such as vacuum, truncate, and delete.
 */
public class DatabaseMaintenance {
    private static final Logger LOGGER = Logger.getLogger(DatabaseMaintenance.class.getName());

    private final ConnectionManager connectionManager;
    private final DatabaseConfig config;


    public DatabaseMaintenance(ConnectionManager connectionManager, DatabaseConfig config) {
        this.connectionManager = connectionManager;
        this.config = config;
    }

    /**
     * Deletes the database file.
     *
     * @return true if the file was deleted or didn't exist, false otherwise
     */
    public boolean deleteDatabase() {
        LOGGER.warning("Deleting database file: " + config.getDbFilePath());
        File dbFile = new File(config.getDbFilePath());

        if (dbFile.exists()) {
            boolean deleted = dbFile.delete();
            if (!deleted) {
                LOGGER.severe("Failed to delete database file: " + dbFile.getAbsolutePath());
                return false;
            }
            LOGGER.info("Database file deleted successfully");
        } else {
            LOGGER.info("Database file does not exist, nothing to delete");
        }
        return true;
    }

    /**
     * Runs VACUUM on the database to optimize storage and performance.
     *
     * @throws MaintenanceException if the operation fails
     */
    public void vacuumDatabase() throws MaintenanceException {
        LOGGER.info("Running VACUUM on database");

        try (Connection connection = connectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("VACUUM;");
            LOGGER.info("VACUUM completed successfully");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to vacuum database", e);
            throw new MaintenanceException("Failed to vacuum database", e);
        }
    }

    /**
     * Truncates a table by deleting all rows and resetting auto-increment.
     *
     * @param table the name of the table to truncate
     * @throws MaintenanceException if the operation fails
     */
    public void truncateTable(String table) throws MaintenanceException {
        LOGGER.info("Truncating table: " + table);

        if (!checkTableExists(table)) {
            LOGGER.info("Table does not exist, nothing to truncate: " + table);
            return;
        }

        try (Connection connection = connectionManager.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute("DELETE FROM " + table);
                resetAutoIncrement(connection, table);
                connection.commit();
                LOGGER.info("Table truncated successfully: " + table);
            } catch (SQLException e) {
                connection.rollback();
                LOGGER.log(Level.SEVERE, "Failed to truncate table: " + table, e);
                throw new MaintenanceException("Failed to truncate table: " + table, e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error during truncate operation", e);
            throw new MaintenanceException("Failed to establish database connection for truncate operation", e);
        }
    }

    /**
     * Resets the auto-increment counter for a table.
     *
     * @param connection the database connection
     * @param table the name of the table
     * @throws SQLException if a database error occurs
     */
    private void resetAutoIncrement(Connection connection, String table) throws SQLException {
        String resetAutoincrement = "UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME=?";
        try (PreparedStatement resetStatement = connection.prepareStatement(resetAutoincrement)) {
            resetStatement.setString(1, table);
            resetStatement.execute();
        }
    }

    /**
     * Drops a table if it exists.
     *
     * @param tableName the name of the table to drop
     * @throws MaintenanceException if the operation fails
     */
    public void dropTable(String tableName) throws MaintenanceException {
        LOGGER.warning("Dropping table if exists: " + tableName);

        if (!checkTableExists(tableName)) {
            LOGGER.info("Table does not exist, nothing to drop: " + tableName);
            return;
        }

        try (Connection connection = connectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + tableName);
            LOGGER.info("Table dropped successfully: " + tableName);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to drop table: " + tableName, e);
            throw new MaintenanceException("Failed to drop table: " + tableName, e);
        }
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName the name of the table to check
     * @return true if the table exists, false otherwise
     * @throws MaintenanceException if the operation fails
     */
    public boolean checkTableExists(String tableName) {
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, tableName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if table exists: " + tableName, e);
            throw new MaintenanceException("Failed to check if table exists: " + tableName, e);
        }
    }
}