package com.mailscheduler.infrastructure.persistence.repository;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.EntityMetadata;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.Repository;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.TableEntity;
import com.mailscheduler.infrastructure.persistence.repository.exception.DataAccessException;
import com.mailscheduler.infrastructure.persistence.repository.exception.EntityNotFoundException;
import com.mailscheduler.infrastructure.persistence.repository.exception.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for SQL-based repository implementations.
 * Provides common CRUD operations and error handling.
 *
 * @param <T> The domain entity type
 * @param <M> The metadata type
 * @param <E> The database entity type
 */
public abstract class AbstractSqlRepository
        <T extends IdentifiableEntity<T>, M extends EntityMetadata, E extends TableEntity>
        implements Repository<T, M> {

    private static final Logger LOGGER = Logger.getLogger(AbstractSqlRepository.class.getName());

    protected final DatabaseFacade db;

    protected AbstractSqlRepository(DatabaseFacade db) {
        this.db = db;
    }

    /**
     * Gets the name of the database table this repository operates on.
     */
    protected abstract String tableName();

    /**
     * Maps a ResultSet row to a database entity.
     *
     * @param rs The ResultSet containing the current row
     * @return The mapped database entity
     * @throws SQLException if a database access error occurs
     */
    protected abstract E mapResultSetToEntity(ResultSet rs) throws SQLException;

    /**
     * Converts a domain entity and its metadata to a database entity
     */
    protected abstract E toTableEntity(T domainEntity, M metadata);

    /**
     * Converts a database entity to a domain entity.
     */
    protected abstract T toDomainEntity(E tableEntity);

    /**
     * Extracts metadata from a database entity
     */
    protected abstract M toMetadata(E tableEntity);

    /**
     * @return The SQL for inserting a new entity
     */
    protected abstract String createInsertSql();

    /**
     * @return The SQL for updating an existing entity
     */
    protected abstract String createUpdateSql();

    /**
     * Sets parameters on a PreparedStatement for insert or update operations.
     *
     * @param stmt The PreparedStatement to set parameters on
     * @param entity The database entity containing the values
     * @throws SQLException if a database access error occurs
     */
    protected abstract void setStatementParameters(PreparedStatement stmt, E entity) throws SQLException;

    /**
     * Creates a new entity with its metadata in the repository.
     *
     * @param entity the domain entity to create
     * @param metadata the metadata associated with the entity
     * @return the created entity data (entity with metadata)
     * @throws RepositoryException if the entity already has an ID or if a database error occurs
     */
    @Override
    public EntityData<T, M> createWithMetadata(T entity, M metadata) throws RepositoryException {
        if (entity.getId() != null) {
            throw new RepositoryException("Entity already has an ID");
        }

        E tableEntity = toTableEntity(entity, metadata);
        String sql = createInsertSql();

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setStatementParameters(stmt, tableEntity);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    E savedEntity = mapResultSetToEntity(rs);
                    LOGGER.log(Level.FINE, "Created entity in table {0} with ID {1}",
                            new Object[]{tableName(), savedEntity.getId()});
                    return EntityData.of(
                            toDomainEntity(savedEntity),
                            toMetadata(savedEntity)
                    );
                }
                throw new DataAccessException("No data returned after insert");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during create operation in " + tableName(), e);
            throw new DataAccessException("Database error during create operation", e);
        }
    }

    /**
     * Updates an existing entity with its metadata in the repository.
     *
     * @param entity the domain entity to update
     * @param metadata the metadata associated with the entity
     * @return the updated entity data (entity with metadata)
     * @throws RepositoryException if the entity has no ID or if a database error occurs
     */
    @Override
    public EntityData<T, M> updateWithMetadata(T entity, M metadata) throws RepositoryException {
        if (entity.getId() == null) {
            throw new RepositoryException("Entity has no ID");
        }

        E tableEntity = toTableEntity(entity, metadata);
        String sql = createUpdateSql();

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setStatementParameters(stmt, tableEntity);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    E savedEntity = mapResultSetToEntity(rs);
                    LOGGER.log(Level.FINE, "Updated entity in table {0} with ID {1}",
                            new Object[]{tableName(), savedEntity.getId()});
                    return EntityData.of(
                            toDomainEntity(savedEntity),
                            toMetadata(savedEntity)
                    );
                }
                throw new EntityNotFoundException("No data returned after update. Entity might not exist: " + entity.getId());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during update operation in " + tableName(), e);
            throw new DataAccessException("Database error during update operation", e);
        }
    }

    /**
     * Finds an entity by its ID along with its metadata.
     *
     * @param id the ID of the entity to find
     * @return an Optional containing the entity data if found, or empty if not found
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public Optional<EntityData<T, M>> findByIdWithMetadata(EntityId<T> id) {
        String sql = String.format("SELECT * FROM %s WHERE id = ?", tableName());

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id.value());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    E entity = mapResultSetToEntity(rs);
                    LOGGER.log(Level.FINE, "Found entity in table {0} with ID {1}",
                            new Object[]{tableName(), id.value()});
                    return Optional.of(EntityData.of(
                            toDomainEntity(entity),
                            toMetadata(entity)
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding entity by ID in " + tableName(), e);
            throw new DataAccessException("Failed to find entity by ID: " + id, e);
        }
        LOGGER.log(Level.FINE, "Entity not found in table {0} with ID {1}",
                new Object[]{tableName(), id.value()});
        return Optional.empty();
    }

    /**
     * Finds all entities in the repository along with their metadata.
     *
     * @return a list of all entity data (entities with metadata)
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public List<EntityData<T, M>> findAllWithMetadata() {
        String sql = String.format("SELECT * FROM %s", tableName());
        List<EntityData<T, M>> result = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                E entity = mapResultSetToEntity(rs);
                result.add(EntityData.of(
                        toDomainEntity(entity),
                        toMetadata(entity)
                ));
            }
            LOGGER.log(Level.FINE, "Found {0} entities in table {1}", new Object[]{result.size(), tableName()});
            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all entities in " + tableName(), e);
            throw new DataAccessException("Failed to find all entities", e);
        }
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id the ID of the entity to delete
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public void delete(EntityId<T> id) {
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName());

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id.value());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.log(Level.FINE, "Deleted entity from table {0} with ID {1}",
                        new Object[]{tableName(), id.value()});
            } else {
                LOGGER.log(Level.WARNING, "No entity found to delete in table {0} with ID {1}",
                        new Object[]{tableName(), id.value()});
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting entity in " + tableName(), e);
            throw new DataAccessException("Failed to delete entity with ID: " + id, e);
        }
    }

    /**
     * Checks if an entity with the specified ID exists.
     *
     * @param id the ID to check
     * @return true if the entity exists, false otherwise
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public boolean exists(EntityId<T> id) {
        String sql = String.format("SELECT 1 FROM %s WHERE id = ?", tableName());

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id.value());

            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                LOGGER.log(Level.FINE, "Entity with ID {0} {1} in table {2}",
                        new Object[]{id.value(), exists ? "exists" : "does not exist", tableName()});
                return exists;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking entity existence in " + tableName(), e);
            throw new DataAccessException("Failed to check entity existence for ID: " + id, e);
        }
    }
}
