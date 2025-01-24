package com.mailscheduler.database;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public final class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String URL = "jdbc:sqlite:mailscheduler.db";

    private static volatile DatabaseManager instance;

    private DatabaseManager() {
        if (instance != null) {
            throw new IllegalStateException("DatabaseManager instance already exists");
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        LOGGER.info("Getting Connection to: " + URL);
        return DriverManager.getConnection(URL);
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupDatabase() throws SQLException {
        LOGGER.info("Setting up Database");
        try (Connection connection = getConnection();
            Statement statement = connection.createStatement()) {

            String emailTable = "CREATE TABLE IF NOT EXISTS Emails (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject TEXT NOT NULL," +
                "body TEXT NOT NULL," +
                "status TEXT CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')) DEFAULT 'PENDING'," +
                "scheduled_date TIMESTAMP," +
                "email_category TEXT CHECK (email_category IN ('INITIAL', 'FOLLOW_UP', 'EXTERNALLY_INITIAL', 'EXTERNALLY_FOLLOW_UP')) NOT NULL," +
                "followup_number INTEGER," +
                "thread_id TEXT," +
                "schedule_id INTEGER," +
                "initial_email_id INTEGER DEFAULT NULL," + // Links the follow-up email to the initial email
                "recipient_id INTEGER," +
                "FOREIGN KEY (schedule_id) REFERENCES FollowUpSchedules(id) ON DELETE CASCADE," +
                "FOREIGN KEY (initial_email_id) REFERENCES Emails(id) ON DELETE CASCADE," +
                "FOREIGN KEY (recipient_id) REFERENCES Recipients(id) ON DELETE CASCADE);";
            statement.execute(emailTable);

            String recipientTable = "CREATE TABLE IF NOT EXISTS Recipients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "email_address TEXT NOT NULL," +
                "domain TEXT," +
                "phone_number TEXT," +
                "initial_email_date TIMESTAMP," +
                "has_replied BOOLEAN DEFAULT FALSE," +
                "spreadsheet_row INTEGER);";
            statement.execute(recipientTable);

            String followUpScheduleTable = "CREATE TABLE IF NOT EXISTS FollowUpSchedules (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "followup_number INTEGER NOT NULL," + // Follow-up number in sequence (e.g. 1st, 2nd)
                "interval_days INTEGER NOT NULL," + // Number of days after the previous email
                "schedule_id INTEGER," +
                "schedule_category TEXT CHECK (schedule_category IN ('DEFAULT_SCHEDULE', 'SCHEDULE')) DEFAULT 'SCHEDULE'," +
                "FOREIGN KEY (schedule_id) REFERENCES FollowUpSchedules(id) ON DELETE CASCADE);";
            statement.execute(followUpScheduleTable);

            String emailTemplateTable = "CREATE TABLE IF NOT EXISTS EmailTemplates (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "draft_id TEXT," +
                    "template_category TEXT CHECK (template_category IN ('INITIAL', 'FOLLOW_UP', 'DEFAULT_INITIAL', 'DEFAULT_FOLLOW_UP')) NOT NULL," +
                    "subject_template TEXT NOT NULL," +
                    "body_template TEXT NOT NULL," +
                    "placeholder_symbols TEXT, " +
                    "placeholders TEXT," +
                    "followup_number INTEGER)";
            statement.execute(emailTemplateTable);
        }
    }

    public void deleteDatabase() {
        LOGGER.info("Deleting database");
        File dbFile = new File("mailscheduler.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    public void vacuumDatabase() throws SQLException {
        LOGGER.info("Vacuuming database");
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("VACUUM;");
        }
    }

    public void dropTable(String table) throws SQLException {
        LOGGER.info("Dropping table: " + table);
        String deleteTable = "DROP TABLE IF EXISTS " + table;
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(deleteTable);
        }
    }

    public void truncateTable(String table) throws SQLException {
        LOGGER.info("Clearing table: " + table);
        if (checkTableExists(table)) {
            String clearTable = "DELETE FROM " + table;
            String resetAutoincrement = "UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME=?";
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement();
                 PreparedStatement resetStatement = connection.prepareStatement(resetAutoincrement)) {
                statement.execute(clearTable);
                resetStatement.setString(1, table);
                resetStatement.execute();
            }
        }
    }

    public void beginTransaction(Connection connection) throws SQLException {
        if (connection != null) {
            connection.setAutoCommit(false);
        }
    }

    public void commitTransaction(Connection connection) throws SQLException {
        if (connection != null) {
            connection.commit();
            connection.setAutoCommit(false);
        }
    }

    public void rollbackTransaction(Connection connection) throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
        }
    }

    public boolean checkTableExists(String tableName) throws SQLException {
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public int getLastInsertId(Connection connection) throws SQLException {
        String query = "SELECT last_insert_rowid();";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return -1;
    }

    public void backupDatabase(String backupFilePath) throws IOException {
        File sourceFile = new File("sheetmailer.db");
        File backupFile = new File(backupFilePath);
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public void restoreDatabase(String backupFilePath) throws IOException {
        File sourceFile = new File(backupFilePath);
        File dbFile = new File("sheetmailer.db");
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(dbFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}
