package com.mailscheduler.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.template.Template;
import com.mailscheduler.domain.model.template.TemplateMetadata;
import com.mailscheduler.domain.model.template.TemplateType;
import com.mailscheduler.domain.model.template.placeholder.PlaceholderManager;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.TemplateRepository;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.TemplateEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * SQL implementation of the TemplateRepository.
 * Handles persistence of Template entities in a relational database.
 */
public class TemplateSqlRepository extends AbstractSqlRepository<Template, TemplateMetadata, TemplateEntity>
        implements TemplateRepository {

    public TemplateSqlRepository(DatabaseFacade db) {
        super(db);
    }

    @Override
    protected String tableName() {
        return "templates";
    }

    @Override
    protected TemplateEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new TemplateEntity(
                rs.getLong("id"),
                rs.getString("template_type"),
                rs.getString("subject_template"),
                rs.getString("body_template"),
                rs.getString("delimiters"),
                rs.getString("placeholders"),
                rs.getString("draft_id")
        );
    }

    @Override
    protected TemplateEntity toTableEntity(Template domainEntity, TemplateMetadata metadata) {
        PlaceholderManager placeholderManager = domainEntity.getPlaceholderManager();

        if (placeholderManager != null) {
            ObjectMapper mapper = new ObjectMapper();

            String json;
            try {
                json = mapper.writeValueAsString(placeholderManager.getPlaceholders());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            return new TemplateEntity(
                    domainEntity.getId() != null ? domainEntity.getId().value() : null,
                    domainEntity.getType().toString(),
                    domainEntity.getSubject().value(),
                    domainEntity.getBody().value(),
                    new String(placeholderManager.getDelimiters()),
                    json,
                    metadata.draftId()
            );
        } else {
            return new TemplateEntity(
                    domainEntity.getId() != null ? domainEntity.getId().value() : null,
                    domainEntity.getType().toString(),
                    domainEntity.getSubject().value(),
                    domainEntity.getBody().value(),
                    null,
                    null,
                    metadata.draftId()
            );
        }
    }

    @Override
    protected Template toDomainEntity(TemplateEntity tableEntity) {
        String delimitersString = tableEntity.getDelimiters();
        if (delimitersString != null) {
            char[] delimiters = tableEntity.getDelimiters().toCharArray();
            ObjectMapper mapper = new ObjectMapper();

            Map<String, SpreadsheetReference> loadedMap;
            try {
                loadedMap = mapper.readValue(tableEntity.getPlaceholders(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            PlaceholderManager placeholderManager = new PlaceholderManager(delimiters, loadedMap);

            return new Template.Builder()
                    .setId(EntityId.of(tableEntity.getId()))
                    .setType(TemplateType.valueOf(tableEntity.getTemplateType()))
                    .setSubject(Subject.of(tableEntity.getSubjectTemplate()))
                    .setBody(Body.of(tableEntity.getBodyTemplate()))
                    .setPlaceholderManager(placeholderManager)
                    .build();
        }

        return new Template.Builder()
                .setId(EntityId.of(tableEntity.getId()))
                .setType(TemplateType.valueOf(tableEntity.getTemplateType()))
                .setSubject(Subject.of(tableEntity.getSubjectTemplate()))
                .setBody(Body.of(tableEntity.getBodyTemplate()))
                .build();
    }

    @Override
    protected TemplateMetadata toMetadata(TemplateEntity tableEntity) {
        return new TemplateMetadata(tableEntity.getDraftId());
    }

    @Override
    protected String createInsertSql() {
        return String.format(
                """
                INSERT INTO %s
                    (template_type, subject_template, body_template, delimiters, placeholders, draft_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    RETURNING *
                """, tableName());
    }

    @Override
    protected String createUpdateSql() {
        return String.format(
                """
                UPDATE %s SET
                    template_type = ?,
                    subject_template = ?,
                    body_template = ?,
                    delimiters = ?,
                    placeholders = ?,
                    draft_id = ?
                WHERE id = ?
                RETURNING *
                """, tableName());
    }

    @Override
    protected void setStatementParameters(PreparedStatement stmt, TemplateEntity entity) throws SQLException {
        stmt.setString(1, entity.getTemplateType());
        stmt.setString(2, entity.getSubjectTemplate());
        stmt.setString(3, entity.getBodyTemplate());
        stmt.setString(4, entity.getDelimiters());
        stmt.setString(5, entity.getPlaceholders());
        stmt.setString(6, entity.getDraftId());

        // For updates, we need to set the ID as the 7th parameter
        if (entity.getId() != null) {
            stmt.setLong(7, entity.getId());
        }
    }

    @Override
    public Optional<EntityData<Template, TemplateMetadata>> findByDraftId(String draftId) {
        String sql = String.format("SELECT * FROM %s WHERE draft_id = ?", tableName());
        try (var conn = db.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, draftId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TemplateEntity entity = mapResultSetToEntity(rs);
                    return Optional.of(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return Optional.empty();
    }

    @Override
    public List<EntityData<Template, TemplateMetadata>> findByType(TemplateType type) {
        String sql = String.format("SELECT * FROM %s WHERE template_type = ?", tableName());
        List<EntityData<Template, TemplateMetadata>> result = new ArrayList<>();
        try (var conn = db.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.toString());
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TemplateEntity entity = mapResultSetToEntity(rs);
                    result.add(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return result;
    }

    @Override
    public List<EntityData<Template, TemplateMetadata>> findDefaultTemplates(TemplateType type) {
        String sql = String.format("SELECT * FROM %s WHERE template_type = ? AND draft_id IS NULL", tableName());
        List<EntityData<Template, TemplateMetadata>> result = new ArrayList<>();
        try (var conn = db.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.toString());
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TemplateEntity entity = mapResultSetToEntity(rs);
                    result.add(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception as needed
        }
        return result;
    }

    @Override
    public Optional<EntityData<Template, TemplateMetadata>> findBySubject(Subject subject) {
        String sql = String.format("SELECT * FROM %s WHERE subject_template = ?", tableName());
        try (var conn = db.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.value());
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TemplateEntity entity = mapResultSetToEntity(rs);
                    return Optional.of(
                            EntityData.of(toDomainEntity(entity), toMetadata(entity))
                    );
                }
            }
        } catch (SQLException e) {
            // Handle/log exception
        }
        return Optional.empty();
    }
}
