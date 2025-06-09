package com.mailscheduler.infrastructure.persistence.database.config;

import java.io.File;

/**
 * Configuration for the database connection.
 * Provides settings for database location, name, and connection parameters.
 */
public class DatabaseConfig {
    private final String dbDirectory;
    private final String dbName;
    private final String driverClass;

    /**
     * Creates a default database configuration.
     */
    public DatabaseConfig() {
        this.dbDirectory = "data";
        this.dbName = "mailscheduler.db";
        this.driverClass = "org.sqlite.JDBC";
    }


    public String getDbDirectory() {
        return dbDirectory;
    }

    public String getDbName() {
        return dbName;
    }

    /**
     * Gets the full path to the database file.
     */
    public String getDbFilePath() {
        return dbDirectory + File.separator + dbName;
    }

    /**
     * Gets the JDBC connection URL.
     */
    public String getConnectionUrl() {
        return "jdbc:sqlite:" + getDbFilePath();
    }

    /**
     * Gets the JDBC driver class name.
     */
    public String getDriverClassName() {
        return driverClass;
    }
}
