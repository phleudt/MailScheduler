package com.mailscheduler.infrastructure.persistence.repository;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.common.config.ApplicationConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.repository.ConfigurationRepository;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.ColumnMappingEntity;
import com.mailscheduler.infrastructure.persistence.entity.ConfigurationEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL implementation of the ConfigurationRepository.
 * Handles persistence of Configuration entities in a relational database.
 */
public class ConfigurationSqlRepository extends AbstractSqlRepository<ApplicationConfiguration, NoMetadata, ConfigurationEntity>
        implements ConfigurationRepository {

    public ConfigurationSqlRepository(DatabaseFacade db) {
        super(db);
    }

    @Override
    protected String tableName() {
        return "configuration";
    }

    @Override
    protected ConfigurationEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String spreadsheetId = rs.getString("spreadsheet_id");
        String senderEmail = rs.getString("sender_email");
        Boolean saveMode = rs.getBoolean("save_mode");
        String sendingCriteriaColumn = rs.getString("sending_criteria_column");

        ConfigurationEntity configEntity = new ConfigurationEntity(
                id, spreadsheetId, senderEmail, saveMode, sendingCriteriaColumn
        );

        // Load column mappings in a separate query
        loadColumnMappings(configEntity);

        return configEntity;
    }

    @Override
    protected ConfigurationEntity toTableEntity(ApplicationConfiguration domainEntity, NoMetadata metadata) {
        Long id = domainEntity.getId() != null ? domainEntity.getId().value() : null;
        String spreadsheetId = domainEntity.getSpreadsheetId();
        String senderEmail = domainEntity.getSenderEmailAddress().value();
        Boolean saveMode = domainEntity.getSaveMode();
        String sendingCriteriaColumn = domainEntity.getSendingCriteriaColumn() != null ?
                domainEntity.getSendingCriteriaColumn().getGoogleSheetsReference() : null;

        // Convert domain ColumnMappings to ColumnMappingEntities
        List<ColumnMappingEntity> mappingEntities = domainEntity.getColumnMappings().stream()
                .map(mapping -> new ColumnMappingEntity(
                        null,
                        id,
                        mapping.type().name(),
                        mapping.columnName(),
                        mapping.columnReference().getGoogleSheetsReference()
                ))
                .toList();

        // Assuming ConfigurationEntity has a method to replace its mappings
        return new ConfigurationEntity(
                id, spreadsheetId, senderEmail, saveMode, sendingCriteriaColumn, mappingEntities
        );
    }

    @Override
    protected ApplicationConfiguration toDomainEntity(ConfigurationEntity tableEntity) {
        List<ColumnMapping> columnMappings = tableEntity.getColumnMappings().stream()
                .map(entity -> new ColumnMapping(
                        entity.getMappingType(),
                        entity.getColumnName(),
                        SpreadsheetReference.fromGoogleReference(entity.getColumnReference())
                ))
                .toList();

        return new ApplicationConfiguration.Builder()
                .setId(EntityId.of(tableEntity.getId()))
                .spreadsheetId(tableEntity.getSpreadsheetId())
                .senderEmailAddress(tableEntity.getSenderEmailAddress())
                .saveMode(tableEntity.getSaveMode())
                .sendingCriteriaColumn(tableEntity.getSendingCriteriaColumn() != null ?
                        SpreadsheetReference.fromGoogleReference(tableEntity.getSendingCriteriaColumn()) : null)
                .columnMappings(columnMappings)
                .build();
    }

    @Override
    protected NoMetadata toMetadata(ConfigurationEntity tableEntity) {
        return NoMetadata.getInstance();
    }

    @Override
    protected String createInsertSql() {
        return "INSERT INTO configuration (spreadsheet_id, sender_email, save_mode, sending_criteria_column) " +
                "VALUES (?, ?, ?, ?) RETURNING *";
    }

    @Override
    protected String createUpdateSql() {
        return "UPDATE configuration SET spreadsheet_id = ?, sender_email = ?, save_mode = ?, " +
                "sending_criteria_column = ? WHERE id = ? RETURNING *";
    }

    @Override
    protected void setStatementParameters(PreparedStatement stmt, ConfigurationEntity entity) throws SQLException {
        stmt.setString(1, entity.getSpreadsheetId());
        stmt.setString(2, entity.getSenderEmailAddress().value());
        stmt.setBoolean(3, entity.getSaveMode());
        stmt.setString(4, entity.getSendingCriteriaColumn());

        if (entity.getId() != null) {
            stmt.setLong(5, entity.getId());
        }
    }

    @Override
    public ApplicationConfiguration getActiveConfiguration() {
        String sql = "SELECT * FROM configuration ORDER BY id DESC LIMIT 1";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                ConfigurationEntity entity = mapResultSetToEntity(rs);
                return toDomainEntity(entity);
            }
        } catch (SQLException e) {
            // Handle exception
        }
        return null;
    }

    @Override
    public EntityData<ApplicationConfiguration, NoMetadata> createWithMetadata(ApplicationConfiguration entity, NoMetadata metadata) {
        EntityData<ApplicationConfiguration, NoMetadata> result = super.createWithMetadata(entity, metadata);
        entity.setId(result.entity().getId());
        saveColumnMappings(entity);
        return result;
    }

    @Override
    public EntityData<ApplicationConfiguration, NoMetadata> updateWithMetadata(ApplicationConfiguration entity, NoMetadata metadata) {
        EntityData<ApplicationConfiguration, NoMetadata> result = super.updateWithMetadata(entity, metadata);
        deleteColumnMappings(entity.getId().value());
        entity.setId(result.entity().getId());
        saveColumnMappings(entity);
        return result;
    }

    public ApplicationConfiguration save(ApplicationConfiguration configuration) {
        ApplicationConfiguration config;
        if (configuration.getId() == null) {
            config = createWithMetadata(configuration, NoMetadata.getInstance()).entity();
        } else {
            config = updateWithMetadata(configuration, NoMetadata.getInstance()).entity();
        }
        return config;
    }

    @Override
    public void delete(EntityId<ApplicationConfiguration> id) {
        super.delete(id);
        deleteColumnMappings(id.value());
    }

    private void loadColumnMappings(ConfigurationEntity configEntity) {
        String sql = "SELECT * FROM column_mappings WHERE config_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, configEntity.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                List<ColumnMappingEntity> mappings = new ArrayList<>();
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    Long configId = rs.getLong("config_id");
                    String type = rs.getString("type");
                    String columnName = rs.getString("column_name");
                    String columnReference = rs.getString("column_reference");

                    mappings.add(new ColumnMappingEntity(id, configId, type, columnName, columnReference));
                }
                // Replace existing mappings with loaded ones
                configEntity.getColumnMappings().addAll(mappings);
            }
        } catch (SQLException e) {
            // Handle exception
        }
    }

    private void saveColumnMappings(ApplicationConfiguration config) {
        if (config.getId() == null || config.getColumnMappings().isEmpty()) {
            return;
        }

        String sql = "INSERT INTO column_mappings (config_id, type, column_name, column_reference) VALUES (?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (ColumnMapping mapping : config.getColumnMappings()) {
                stmt.setLong(1, config.getId().value());
                stmt.setString(2, mapping.type().name());
                stmt.setString(3, mapping.columnName());
                stmt.setString(4, mapping.columnReference().getGoogleSheetsReference());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            // Handle exception
        }
    }

    private void deleteColumnMappings(long configId) {
        String sql = "DELETE FROM column_mappings WHERE config_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, configId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Handle exception
        }
    }

    @Override
    public Optional<ApplicationConfiguration> findBySpreadsheetId(String spreadsheetId) {
        String sql = "SELECT * FROM configuration WHERE spreadsheet_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, spreadsheetId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ConfigurationEntity entity = mapResultSetToEntity(rs);
                    return Optional.of(toDomainEntity(entity));
                }
            }
        } catch (SQLException e) {
            // Handle exception
        }
        return Optional.empty();
    }
}