package com.mailscheduler.infrastructure.persistence.database;

import com.mailscheduler.infrastructure.persistence.database.config.DatabaseConfig;
import com.mailscheduler.infrastructure.persistence.database.exception.ConnectionException;
import com.mailscheduler.infrastructure.persistence.database.exception.MaintenanceException;
import com.mailscheduler.infrastructure.persistence.database.exception.SchemaException;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a unified facade for database operations.
 * This class coordinates between the connection, initialization, and maintenance components.
 */
public class DatabaseFacade {
    private static final Logger LOGGER = Logger.getLogger(DatabaseFacade.class.getName());

    private final ConnectionManager connectionManager;
    private final DatabaseInitializer initializer;
    private final DatabaseMaintenance maintenance;
    private final DatabaseConfig config;

    public DatabaseFacade() {
        this.config = new DatabaseConfig();
        this.connectionManager = new ConnectionManager(config);
        this.initializer = new DatabaseInitializer(connectionManager);
        this.maintenance = new DatabaseMaintenance(connectionManager, config);

        // Initialize database on startup
        initialize();
    }


    /**
     * Initializes the database schema.
     *
     * @throws SchemaException if the schema cannot be initialized
     */
    public void initialize() {
        try {
            initializer.initializeDatabase();
        } catch (SchemaException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database schema", e);
            throw e;
        }
    }

    /**
     * Gets a new database connection.
     * The caller is responsible for closing this connection.
     *
     * @return a new database connection
     * @throws ConnectionException if connection cannot be established
     */
    public Connection getConnection() throws ConnectionException {
        return connectionManager.getConnection();
    }

    /**
     * Optimizes the database by running VACUUM.
     *
     * @throws MaintenanceException if the operation fails
     */
    public void vacuum() throws MaintenanceException {
        maintenance.vacuumDatabase();
    }

    /**
     * Truncates a table by deleting all rows and resetting auto-increment.
     *
     * @param tableName the name of the table to truncate
     * @throws MaintenanceException if the operation fails
     */
    public void truncateTable(String tableName) throws MaintenanceException {
        maintenance.truncateTable(tableName);
    }

    /**
     * Drops a table if it exists.
     *
     * @param tableName the name of the table to drop
     * @throws MaintenanceException if the operation fails
     */
    public void dropTable(String tableName) throws MaintenanceException {
        maintenance.dropTable(tableName);
    }

    /**
     * Deletes the database file.
     *
     * @return true if the file was deleted or didn't exist, false otherwise
     */
    public boolean deleteDatabase() {
        return maintenance.deleteDatabase();
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName the name of the table to check
     * @return true if the table exists, false otherwise
     * @throws MaintenanceException if the operation fails
     */
    public boolean tableExists(String tableName) throws MaintenanceException {
        return maintenance.checkTableExists(tableName);
    }

    /**
     * Validates the database schema.
     *
     * @return true if the schema is valid, false otherwise
     */
    public boolean validateSchema() {
        return initializer.validateSchema();
    }

    /**
     * Gets the database configuration.
     *
     * @return the database configuration
     */
    public DatabaseConfig getConfig() {
        return config;
    }
}
