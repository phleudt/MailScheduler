package com.mailscheduler.infrastructure.persistence.database;

import com.mailscheduler.infrastructure.persistence.database.config.DatabaseConfig;
import com.mailscheduler.infrastructure.persistence.database.exception.ConnectionException;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connections and ensures database directory exists.
 * Responsible for establishing and providing database connections.
 */
public class ConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());

    private final DatabaseConfig config;
    private boolean driverInitialized = false;

    /**
     * Creates a new connection manager with the specified configuration.
     *
     * @param config the database configuration
     * @throws ConnectionException if the database driver cannot be loaded
     */
    public ConnectionManager(DatabaseConfig config) throws ConnectionException {
        this.config = config;
        initializeDriver();
        ensureDatabaseDirectoryExists();
    }

    /**
     * Initializes the JDBC driver.
     *
     * @throws ConnectionException if the driver cannot be loaded
     */
    private void initializeDriver() throws ConnectionException {
        if (driverInitialized) {
            return;
        }

        try {
            Class.forName(config.getDriverClassName());
            driverInitialized = true;
            LOGGER.fine("Database driver loaded successfully: " + config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load database driver: " + config.getDriverClassName(), e);
            throw new ConnectionException("Failed to load database driver: " + config.getDriverClassName(), e);
        }
    }

    /**
     * Creates a new database connection.
     *
     * @return a new database connection
     * @throws ConnectionException if a database access error occurs
     */
    public Connection getConnection() throws ConnectionException {
        try {
            Connection connection = DriverManager.getConnection(config.getConnectionUrl());
            LOGGER.fine("Established database connection to: " + config.getConnectionUrl());
            return connection;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to establish database connection", e);
            throw new ConnectionException("Failed to establish database connection to " + config.getConnectionUrl(), e);
        }
    }

    /**
     * Ensures the database directory exists, creating it if necessary.
     *
     * @throws ConnectionException if the directory cannot be created
     */
    private void ensureDatabaseDirectoryExists() throws ConnectionException {
        File dbDir = new File(config.getDbDirectory());

        if (!dbDir.exists()) {
            LOGGER.info("Creating database directory: " + dbDir.getAbsolutePath());
            if (!dbDir.mkdirs()) {
                LOGGER.severe("Failed to create database directory: " + dbDir.getAbsolutePath());
                throw new ConnectionException("Failed to create database directory: " + dbDir.getAbsolutePath());
            }
        }
    }
}