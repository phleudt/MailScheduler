package com.mailscheduler.database.dao;

import com.mailscheduler.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class GenericDao<T> {
    private static final Logger LOGGER = Logger.getLogger(GenericDao.class.getName());
    protected final DatabaseManager databaseManager;
    private Connection currentTransaction = null;

    public GenericDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    protected abstract T mapResultSetToEntity(ResultSet resultSet) throws SQLException;

    public int insert(String query, Object... params) throws SQLException {
        return executeUpdate(query, params);
    }

    public boolean update(String query, Object... params) throws SQLException {
        return executeUpdate(query, params) > 0;
    }

    public boolean delete(String query, Object... params) throws SQLException {
        return executeUpdate(query, params) > 0;
    }

    public T findById(String query, Object... params) throws SQLException {
        return executeQueryWithSingleResult(query, this::mapResultSetToEntity, params);
    }

    public List<T> findAll(String query) throws SQLException {
        return executeQueryWithListResult(query, this::mapResultSetToEntity);
    }

    // --- Transaction Methods ---
    public void beginTransaction() throws SQLException {
        if (currentTransaction != null) {
            throw new SQLException("A transaction is already in progress");
        }

        currentTransaction = databaseManager.getConnection();
        databaseManager.beginTransaction(currentTransaction);
    }

    public void commitTransaction() throws SQLException {
        if (currentTransaction == null) {
            throw new SQLException("No active transaction to commit");
        }

        try {
            databaseManager.commitTransaction(currentTransaction);
        } finally {
            databaseManager.closeConnection(currentTransaction);
            currentTransaction = null;
        }
    }

    public void rollbackTransaction() throws SQLException {
        if (currentTransaction == null) {
            throw new SQLException("No active transaction to rollback");
        }

        try {
            databaseManager.rollbackTransaction(currentTransaction);
        } finally {
            databaseManager.closeConnection(currentTransaction);
            currentTransaction = null;
        }
    }


    // --- Helper Methods ---

    protected int executeUpdate(String query, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            setParameters(statement, params);
            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
            return -1; // Return -1 if no ID was generated
        }
    }

    protected <T> T executeQueryWithSingleResult(String query, ResultSetMapper<T> mapper, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapper.map(resultSet);
                }
                return null;
            }
        }
    }

    protected <T> List<T> executeQueryWithListResult(String query, ResultSetMapper<T> mapper, Object... params) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapper.map(resultSet));
                }
            }
        }
        return results;
    }

    protected boolean executeQueryForBoolean(String query, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    protected int executeQueryForInt(String query, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        }
    }

    protected String executeQueryForString(String query, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
                return null;
            }
        }
    }

    protected ZonedDateTime executeQueryForTimestamp(String query, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getTimestamp(1).toInstant().atZone(ZoneId.systemDefault());
                }
                return null;
            }
        }
    }

    protected void setParameters(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
