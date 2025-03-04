package com.mailscheduler.infrastructure.persistence.repository.sqlite;

import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Base repository class for SQLite operations.
 * @param <T> Domain entity type
 * @param <E> Database entity type
 */
public abstract class AbstractSQLiteRepository<T, E> {
    protected final DatabaseManager databaseManager;
    private Connection currentTransaction = null;

    protected AbstractSQLiteRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Abstract methods
    protected abstract E mapResultSetToEntity(ResultSet resultSet) throws SQLException;
    protected abstract T mapToDomainEntity(E entity);
    protected abstract E mapFromDomainEntity(T domain);
    protected abstract Object[] extractParameters(E entity, Object... additionalParams);

    public abstract Optional<T> findById(int id) throws RepositoryException;

    // Basic CRUD operations with domain entities
    protected T save(String query, T domain, Object... additionalParams) throws RepositoryException {
        try {
            E entity = mapFromDomainEntity(domain);
            int id = executeUpdate(query, extractParameters(entity, additionalParams));
            if (id > 0) {
                return findById(id).orElse(null);
            }
            return null;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save entity: " + e.getMessage(), e);
        }
    }

    protected boolean update(String query, T domain, Object... additionalParams) throws RepositoryException {
        try {
            E entity = mapFromDomainEntity(domain);
            return executeUpdate(query, extractParameters(entity, additionalParams)) > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update entity: " + e.getMessage(), e);
        }
    }

    protected boolean delete(String query, Object... params) throws RepositoryException {
        try {
            return executeUpdate(query, params) > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete entity: " + e.getMessage(), e);
        }
    }

    protected List<T> findAll(String query, Object... params) throws RepositoryException {
        try {
            return executeQueryForList(query, params).stream()
                    .map(this::mapToDomainEntity)
                    .toList();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find entities: " + e.getMessage(), e);
        }
    }

    // Protected helper methods for database operations
    protected int executeUpdate(String query, Object... params) throws SQLException {
        try (Connection connection = currentTransaction != null ? currentTransaction : databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(statement, params);
            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0 ? 1 : -1;
        }
    }

    protected Optional<E> executeQueryForSingleResult(String query, Object... params) throws SQLException {
        try (Connection connection = currentTransaction != null ? currentTransaction : databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapResultSetToEntity(resultSet)) : Optional.empty();
            }
        }
    }

    protected List<E> executeQueryForList(String query, Object... params) throws SQLException {
        List<E> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapResultSetToEntity(resultSet));
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
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    protected String executeQueryForString(String query, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    protected ZonedDateTime executeQueryForTimestamp(String query, Object... params) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp(1);
                    return timestamp != null ? timestamp.toInstant().atZone(ZoneId.systemDefault()) : null;
                }
                return null;
            }
        }
    }

    // Protected utility methods
    protected void setParameters(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    // Transaction management
    public void beginTransaction() throws SQLException {
        if (currentTransaction != null) {
            throw new SQLException("Transaction already in progress");
        }
        currentTransaction = databaseManager.getConnection();
        currentTransaction.setAutoCommit(false);
    }

    public void commitTransaction() throws SQLException {
        if (currentTransaction == null) {
            throw new SQLException("No active transaction");
        }
        try {
            currentTransaction.commit();
        } finally {
            closeTransaction();
        }
    }

    public void rollbackTransaction() throws SQLException {
        if (currentTransaction == null) {
            throw new SQLException("No active transaction");
        }
        try {
            currentTransaction.rollback();
        } finally {
            closeTransaction();
        }
    }

    private void closeTransaction() throws SQLException {
        if (currentTransaction != null) {
            currentTransaction.setAutoCommit(true);
            currentTransaction.close();
            currentTransaction = null;
        }
    }
}
