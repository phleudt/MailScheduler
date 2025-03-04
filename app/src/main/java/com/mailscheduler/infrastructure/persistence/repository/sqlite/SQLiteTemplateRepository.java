package com.mailscheduler.infrastructure.persistence.repository.sqlite;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mailscheduler.common.exception.PlaceholderException;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.template.*;
import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.infrastructure.persistence.entities.TemplateEntity;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;

import java.io.IOException;
import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SQLiteTemplateRepository extends AbstractSQLiteRepository<Template, TemplateEntity> {

    // SQL Queries
    private static final String INSERT_TEMPLATE =
            "INSERT INTO Templates (draft_id, template_category, subject_template, body_template, " +
                    "placeholder_symbols, placeholders, followup_number) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_TEMPLATE_BY_ID =
            "UPDATE Templates SET draft_id = ?, template_category = ?, subject_template = ?, " +
                    "body_template = ?, placeholder_symbols = ?, placeholders = ?, followup_number = ? WHERE id = ?";
    private static final String DELETE_TEMPLATE_BY_ID =
            "DELETE FROM Templates WHERE id = ?";
    private static final String FIND_BY_ID =
            "SELECT * FROM Templates WHERE id = ?";
    private static final String FIND_ALL_TEMPLATES =
            "SELECT * FROM Templates";
    private static final String FIND_BY_CATEGORY =
            "SELECT * FROM Templates WHERE template_category = ?";
    private static final String FIND_BY_CATEGORY_AND_FOLLOWUP =
            "SELECT * FROM Templates WHERE template_category = ? AND followup_number = ?";
    private static final String DELETE_BY_CATEGORY =
            "DELETE FROM Templates WHERE template_category = ?";
    private static final String FIND_DEFAULT_INITIAL_TEMPLATE =
            "SELECT * FROM Templates WHERE template_category = 'DEFAULT_INITIAL'";
    private static final String FIND_DEFAULT_FOLLOW_UP_TEMPLATE_BY_NUMBER =
            "SELECT * FROM Templates WHERE template_category = 'DEFAULT_FOLLOW_UP' AND followup_number = ?";
    private static final String FIND_DEFAULT_FOLLOW_UP_TEMPLATES =
            "SELECT * FROM Templates WHERE template_category = 'DEFAULT_FOLLOW_UP'";
    private static final String COUNT_DEFAULT_FOLLOW_UP_TEMPLATES =
            "SELECT COUNT(*) FROM Templates WHERE template_category = 'DEFAULT_FOLLOW_UP'";
    private static final String GET_SCHEDULE_COUNT =
            "SELECT COUNT(*) FROM Schedules WHERE schedule_id = ?";
    private static final String FIND_BY_DRAFT_ID =
            "SELECT * FROM Templates WHERE draft_id = ?";

    public SQLiteTemplateRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected TemplateEntity mapResultSetToEntity(ResultSet resultSet) throws SQLException {
        return new TemplateEntity(
                resultSet.getInt("id"),
                resultSet.getString("draft_id"),
                resultSet.getString("template_category"),
                resultSet.getString("subject_template"),
                resultSet.getString("body_template"),
                resultSet.getString("placeholder_symbols"),
                resultSet.getString("placeholders"),
                resultSet.getInt("followup_number")
        );
    }

    @Override
    protected Template mapToDomainEntity(TemplateEntity entity) {
        PlaceholderManager placeholderManager = deserializePlaceholders(entity.getPlaceholders());

        return new Template.Builder()
                .setId(entity.getId())
                .setDraftId(entity.getDraft_id())
                .setCategory(TemplateCategory.fromString(entity.getTemplate_category()))
                .setSubject(entity.getSubject_template())
                .setBody(entity.getBody_template())
                // .setPlaceholderSymbols(entity.getPlaceholder_symbols().toCharArray()) // TODO
                .setPlaceholderManager(placeholderManager)
                .setFollowUpNumber(entity.getFollowup_number())
                .build();
    }

    @Override
    protected TemplateEntity mapFromDomainEntity(Template domain) {
        return new TemplateEntity(
                domain.getId() != null ? domain.getId().value() : -1,
                domain.getDraftId(),
                domain.getCategory().toString(),
                domain.getContent().subject(),
                domain.getContent().body(),
                new String(domain.getPlaceholderManager().getDelimiters()),
                serializePlaceholders(domain.getPlaceholderManager()),
                domain.getFollowUpNumber()
        );
    }

    @Override
    protected Object[] extractParameters(TemplateEntity entity, Object... additionalParams) {
        Object[] parameters = {
                entity.getDraft_id(),
                entity.getTemplate_category(),
                entity.getSubject_template(),
                entity.getBody_template(),
                entity.getPlaceholder_symbols(),
                entity.getPlaceholders(),
                entity.getFollowup_number()
        };

        if (additionalParams.length > 0 && additionalParams[0] instanceof Integer) {
            Object[] updatedParams = new Object[parameters.length + 1];
            System.arraycopy(parameters, 0, updatedParams, 0, parameters.length);
            updatedParams[parameters.length] = additionalParams[0];
            return updatedParams;
        }

        return parameters;
    }

    @Override
    public Optional<Template> findById(int id) throws RepositoryException {
        try {
            Optional<TemplateEntity> entity = executeQueryForSingleResult(FIND_BY_ID, id);
            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find template with ID: " + id, e);
        }
    }

    public Optional<Template> findDefaultTemplate(TemplateCategory category, int followupNumber) throws RepositoryException {
        try {
            Optional<TemplateEntity> entity = executeQueryForSingleResult(
                    FIND_BY_CATEGORY_AND_FOLLOWUP,
                    category.toString(),
                    followupNumber
            );
            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException(
                    String.format("Failed to find template for category %s and followup %d",
                            category, followupNumber),
                    e
            );
        }
    }

    public List<Template> findByCategory(TemplateCategory category) throws RepositoryException {
        try {
            return findAll(FIND_BY_CATEGORY, category.toString());
        } catch (RepositoryException e) {
            throw new RepositoryException(
                    "Failed to find templates for category: " + category,
                    e
            );
        }
    }

    public Template save(Template template) throws RepositoryException {
        try {
            return save(INSERT_TEMPLATE, template);
        } catch (RepositoryException e) {
            throw new RepositoryException("Failed to save template", e);
        }
    }

    public void deleteByCategory(TemplateCategory category) throws RepositoryException {
        try {
            delete(DELETE_BY_CATEGORY, category.toString());
        } catch (RepositoryException e) {
            throw new RepositoryException(
                    "Failed to delete templates for category: " + category,
                    e
            );
        }
    }

    public Optional<Template> findDefaultInitialEmailTemplate() throws RepositoryException {
        try {
            Optional<TemplateEntity> entity = executeQueryForSingleResult(FIND_DEFAULT_INITIAL_TEMPLATE);
            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Error finding default initial email template", e);
        }
    }

    public Optional<Template> findDefaultFollowUpEmailTemplateByNumber(int followupNumber) throws RepositoryException {
        try {
            Optional<TemplateEntity> entity = executeQueryForSingleResult(FIND_DEFAULT_FOLLOW_UP_TEMPLATE_BY_NUMBER, followupNumber);
            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Error finding follow-up email template for follow-up number: " + followupNumber, e);
        }
    }

    public boolean doesDefaultInitialTemplateExist() throws RepositoryException {
        try {
            return executeQueryForSingleResult(FIND_DEFAULT_INITIAL_TEMPLATE).isPresent();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find default initial template");
        }
    }

    public boolean doesDefaultFollowUpTemplateExist(int followupNumber) throws RepositoryException {
        try {
            return executeQueryForSingleResult(FIND_DEFAULT_FOLLOW_UP_TEMPLATE_BY_NUMBER, followupNumber).isPresent();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to check for follow-up email template for follow-up number: " + followupNumber, e);
        }
    }

    public int countDefaultFollowUpTemplates() throws RepositoryException {
        try {
            return executeQueryForList(FIND_DEFAULT_FOLLOW_UP_TEMPLATES).size();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to count default follow-up number");
        }
    }

    public int getScheduleCount(int scheduleId) throws RepositoryException {
        try {
            return executeQueryForInt(GET_SCHEDULE_COUNT, scheduleId);
        } catch (SQLException e) {
            throw new RepositoryException("Error getting schedule count for schedule ID: " + scheduleId, e);
        }
    }

    public Optional<Template> findByDraftId(String draftId) throws RepositoryException {
        try {
            Optional<TemplateEntity> entity = executeQueryForSingleResult(FIND_BY_DRAFT_ID, draftId);

            return entity.map(this::mapToDomainEntity);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find email template by draft ID: " + draftId, e);
        }
    }

    public boolean update(Template template) throws RepositoryException {
        try {
            return executeUpdate(UPDATE_TEMPLATE_BY_ID,
                    template.getDraftId(),
                    template.getCategory(),
                    template.getBody(),
                    template.getSubject(),
                    template.getPlaceholderManager().getDelimiters(),
                    template.getPlaceholderManager(),
                    template.getFollowUpNumber(),
                    template.getId().value()) > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Error updating email template with ID: " + template.getId().value(), e);
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(PlaceholderManager.class, new SQLiteTemplateRepository.PlaceholderValueSerializer());
        module.addDeserializer(PlaceholderManager.class, new SQLiteTemplateRepository.PlaceholderValueDeserializer());
        objectMapper.registerModule(module);
    }

    // Helper methods for placeholder serialization
    private static String serializePlaceholders(PlaceholderManager placeholderManager) {
        try {
            return objectMapper.writeValueAsString(placeholderManager);
        } catch (IOException e) {
            return "{" + e.getMessage() + "}";
        }
    }

    private static PlaceholderManager deserializePlaceholders(String json) {
        try {
            return json == null || json.isEmpty()
                    ? new PlaceholderManager()
                    : objectMapper.readValue(json, PlaceholderManager.class);
        } catch (IOException e) {
            return new PlaceholderManager();
        }
    }

    private static class PlaceholderValueSerializer extends JsonSerializer<PlaceholderManager> {
        @Override
        public void serialize(PlaceholderManager manager, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            for (Map.Entry<String, PlaceholderManager.PlaceholderValue> entry : manager.getAllPlaceholders().entrySet()) {
                gen.writeObjectFieldStart(entry.getKey());

                PlaceholderManager.PlaceholderValue placeholderValue = entry.getValue();
                gen.writeStringField("type", placeholderValue.type().name());

                switch (placeholderValue.type()) {
                    case STRING -> gen.writeStringField("value", placeholderValue.getStringValue());
                    case SPREADSHEET_REFERENCE -> {
                        SpreadsheetReference reference = placeholderValue.getSpreadsheetReference();
                        gen.writeObjectFieldStart("value");

                        switch (reference.getType()) {
                            case COLUMN -> gen.writeStringField("column", reference.getReference());
                            case ROW -> gen.writeStringField("row", reference.getReference());
                            case CELL -> gen.writeStringField("cell", reference.getReference());
                        }

                        gen.writeEndObject();
                    }
                }

                gen.writeEndObject();
            }

            gen.writeEndObject();
        }
    }

    private static class PlaceholderValueDeserializer extends JsonDeserializer<PlaceholderManager> {
        @Override
        public PlaceholderManager deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            PlaceholderManager placeholderManager = new PlaceholderManager();
            JsonNode rootNode = p.getCodec().readTree(p);

            for (Iterator<Map.Entry<String, JsonNode>> it = rootNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = entry.getKey();
                JsonNode valueNode = entry.getValue();

                String typeStr = valueNode.get("type").asText();
                JsonNode dataNode = valueNode.get("value");

                try {
                    if ("STRING".equals(typeStr)) {
                        placeholderManager.addStringPlaceholder(key, dataNode.asText());
                    } else if ("SPREADSHEET_REFERENCE".equals(typeStr)) {
                        SpreadsheetReference reference;
                        if (dataNode.has("column")) {
                            reference = SpreadsheetReference.ofColumn(dataNode.get("column").asText());
                        } else if (dataNode.has("row")) {
                            reference = SpreadsheetReference.ofRow(dataNode.get("row").asText());
                        } else if (dataNode.has("cell")) {
                            reference = SpreadsheetReference.ofCell(dataNode.get("cell").asText());
                        } else {
                            throw new IOException("Invalid spreadsheet reference");
                        }

                        placeholderManager.addSpreadsheetPlaceholder(key, reference);
                    }
                } catch (PlaceholderException e) {
                    throw new IOException("Failed to add placeholder to placeholder manger", e);
                }
            }

            return placeholderManager;
        }
    }
}
