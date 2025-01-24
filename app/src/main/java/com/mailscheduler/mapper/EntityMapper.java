package com.mailscheduler.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mailscheduler.database.entities.*;
import com.mailscheduler.dto.*;
import com.mailscheduler.exception.validation.InvalidTemplateException;
import com.mailscheduler.exception.PlaceholderException;
import com.mailscheduler.exception.MappingException;
import com.mailscheduler.model.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntityMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOGGER = Logger.getLogger(EntityMapper.class.getName());

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(PlaceholderManager.class, new PlaceholderValueSerializer());
        module.addDeserializer(PlaceholderManager.class, new PlaceholderValueDeserializer());
        objectMapper.registerModule(module);
    }

    /**
     * Converts an EmailEntity to an EmailDto.
     *
     * @param entity the EmailEntity to convert
     * @return the converted EmailDto
     * @throws MappingException if the conversion fails
     */
    public static EmailDto toEmailDto(EmailEntity entity) throws MappingException {
        try {
            ZonedDateTime sendDate = toZonedDateTime(entity.getScheduled_date());
            return new EmailDto(
                    entity.getId(),
                    entity.getSubject(),
                    entity.getBody(),
                    entity.getStatus(),
                    sendDate,
                    EmailCategory.fromString(entity.getEmail_category()),
                    entity.getFollowup_number(),
                    entity.getThread_id(),
                    entity.getSchedule_id(),
                    entity.getInitial_email_id(),
                    entity.getRecipient_id()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map EmailEntity to EmailDto", e);
            throw new MappingException("Failed to map EmailEntity to EmailDto", e);
        }
    }

    /**
     * Converts an Email to an EmailDto.
     *
     * @param email the Email to convert
     * @param scheduleId the schedule ID
     * @return the converted EmailDto
     */
    public static EmailDto toEmailDto(Email email, int scheduleId) {
        return new EmailDto(
                email.getId(),
                email.getSubject(),
                email.getBody(),
                email.getStatus(),
                email.getScheduledDate(),
                email.getEmailCategory(),
                email.getFollowupNumber(),
                email.getThreadId(),
                scheduleId,
                email.getInitialEmailId(),
                email.getRecipientId()
        );
    }

    /**
     * Converts an EmailDto to an EmailEntity.
     *
     * @param dto the EmailDto to convert
     * @return the converted EmailEntity
     * @throws MappingException if the conversion fails
     */
    public static EmailEntity toEmailEntity(EmailDto dto) throws MappingException {
        try {
            Timestamp sendDate = toTimestamp(dto.getScheduledDate());
            return new EmailEntity(
                    dto.getId(),
                    dto.getSubject(),
                    dto.getBody(),
                    dto.getStatus(),
                    sendDate,
                    EmailCategory.toString(dto.getEmailCategory()),
                    dto.getFollowupNumber(),
                    dto.getThreadId(),
                    dto.getScheduleId(),
                    dto.getInitialEmailId(),
                    dto.getRecipientId()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map EmailDto to EmailEntity", e);
            throw new MappingException("Failed to map EmailDto to EmailEntity", e);
        }
    }

    /**
     * Converts an EmailDto to an Email.
     *
     * @param dto the EmailDto to convert
     * @return the converted Email
     */
    public static Email toEmail(EmailDto dto) {
        return new Email.Builder()
                .setId(dto.getId())
                .setSender("me")
                .setRecipientId(dto.getRecipientId())
                .setSubject(dto.getSubject())
                .setBody(dto.getBody())
                .setStatus(dto.getStatus())
                .setScheduledDate(dto.getScheduledDate())
                .setEmailCategory(dto.getEmailCategory())
                .setFollowupNumber(dto.getFollowupNumber())
                .setThreadId(dto.getThreadId())
                .setInitialEmailId(dto.getInitialEmailId())
                .build();
    }

    /**
     * Converts an EmailDto to an Email with a specified recipient.
     *
     * @param dto the EmailDto to convert
     * @param recipient the recipient email address
     * @return the converted Email
     */
    public static Email toEmail(EmailDto dto, String recipient) {
        return new Email.Builder()
                .setSender("me")
                .setRecipientEmail(recipient)
                .setSubject(dto.getSubject())
                .setBody(dto.getBody())
                .setStatus(dto.getStatus())
                .setScheduledDate(dto.getScheduledDate())
                .setEmailCategory(dto.getEmailCategory())
                .setFollowupNumber(dto.getFollowupNumber())
                .setThreadId(dto.getThreadId())
                .setInitialEmailId(dto.getInitialEmailId())
                .build();
    }

    /**
     * Converts a RecipientEntity to a RecipientDto.
     *
     * @param entity the RecipientEntity to convert
     * @return the converted RecipientDto
     * @throws MappingException if the conversion fails
     */
    public static RecipientDto toRecipientDto(RecipientEntity entity) throws MappingException {
        try {
            ZonedDateTime initialEmailDate = toZonedDateTime(entity.getInitial_email_date());
            return new RecipientDto(
                    entity.getId(),
                    entity.getName(),
                    entity.getEmail_address(),
                    entity.getDomain(),
                    entity.getPhone_number(),
                    initialEmailDate,
                    entity.has_replied(),
                    entity.getSpreadsheet_row()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map RecipientEntity to RecipientDto", e);
            throw new MappingException("Failed to map RecipientEntity to RecipientDto", e);
        }
    }

    /**
     * Converts a Contact to a RecipientDto.
     *
     * @param contact the Contact to convert
     * @return the converted RecipientDto
     */
    public static RecipientDto toRecipientDto(Contact contact) {
        return new RecipientDto(
                -1,
                contact.getName(),
                contact.getEmailAddress(),
                contact.getDomain(),
                contact.getPhoneNumber(),
                contact.getInitialEmailDate(),
                contact.hasReplied(),
                contact.getSpreadsheetRow()
        );
    }

    /**
     * Converts a RecipientDto to a RecipientEntity.
     *
     * @param dto the RecipientDto to convert
     * @return the converted RecipientEntity
     * @throws MappingException if the conversion fails
     */
    public static RecipientEntity toRecipientEntity(RecipientDto dto) throws MappingException {
        try {
            Timestamp initialEmailDate = toTimestamp(dto.getInitialEmailDate());
            return new RecipientEntity(
                    dto.getId(),
                    dto.getName(),
                    dto.getEmailAddress(),
                    dto.getDomain(),
                    dto.getPhoneNumber(),
                    initialEmailDate,
                    dto.hasReplied(),
                    dto.getSpreadsheetRow()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map RecipientDto to RecipientEntity", e);
            throw new MappingException("Failed to map RecipientDto to RecipientEntity", e);
        }
    }

    /**
     * Converts a RecipientDto to a Contact.
     *
     * @param dto the RecipientDto to convert
     * @return the converted Contact
     */
    public static Contact toContact(RecipientDto dto) {
        return new Contact.Builder()
                .setName(dto.getName())
                .setEmailAddress(dto.getEmailAddress())
                .setDomain(dto.getDomain())
                .setPhoneNumber(dto.getPhoneNumber())
                .build();
    }

    /**
     * Converts a FollowUpScheduleEntity to a FollowUpScheduleDto.
     *
     * @param entity the FollowUpScheduleEntity to convert
     * @return the converted FollowUpScheduleDto
     * @throws MappingException if the conversion fails
     */
    public static FollowUpScheduleDto toFollowUpScheduleDto(FollowUpScheduleEntity entity) throws MappingException {
        try {
            return new FollowUpScheduleDto(
                    entity.getId(),
                    entity.getFollowup_number(),
                    entity.getInterval_days(),
                    entity.getSchedule_id(),
                    ScheduleCategory.fromString(entity.getSchedule_category())
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map FollowUpScheduleEntity to FollowUpScheduleDto", e);
            throw new MappingException("Failed to map FollowUpScheduleEntity to FollowUpScheduleDto", e);
        }
    }

    public static List<FollowUpScheduleDto> toFollowUpScheduleDtos(Schedule schedule) throws MappingException {
        try {
            List<FollowUpScheduleDto> dtos = new ArrayList<>();
            for (ScheduleEntry entry : schedule.getEntries()) {
                FollowUpScheduleDto dto = new FollowUpScheduleDto(
                        entry.getId(),
                        entry.getNumber(),
                        entry.getIntervalDays(),
                        entry.getScheduleId(),
                        entry.getScheduleCategory()
                );
                dtos.add(dto);
            }
            return dtos;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map Schedule to FollowUpScheduleDto", e);
            throw new MappingException("Failed to map Schedule to FollowUpScheduleDto", e);
        }
    }

    public static Schedule toSchedule(List<FollowUpScheduleDto> followUpScheduleDtos) throws MappingException {
        try {
            List<ScheduleEntry> entries = new ArrayList<>();
            int scheduleId = followUpScheduleDtos.get(0).getScheduleId();
            for (FollowUpScheduleDto dto : followUpScheduleDtos) {
                ScheduleEntry entry = new ScheduleEntry(
                        dto.getId(),
                        dto.getFollowupNumber(),
                        dto.getIntervalDays(),
                        dto.getScheduleId(),
                        dto.getScheduleCategory()
                );
                entries.add(entry);
            }
            return new Schedule.Builder()
                    .setScheduleId(scheduleId)
                    .addEntries(entries)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map FollowUpScheduleDto to Schedule", e);
            throw new MappingException("Failed to map FollowUpScheduleDto to Schedule", e);
        }
    }

    /**
     * Converts a FollowUpScheduleDto to a FollowUpScheduleEntity.
     *
     * @param dto the FollowUpScheduleDto to convert
     * @return the converted FollowUpScheduleEntity
     * @throws MappingException if the conversion fails
     */
    public static FollowUpScheduleEntity toFollowUpScheduleEntity(FollowUpScheduleDto dto) throws MappingException {
        try {
            return new FollowUpScheduleEntity(
                    dto.getId(),
                    dto.getFollowupNumber(),
                    dto.getIntervalDays(),
                    dto.getScheduleId(),
                    ScheduleCategory.toString(dto.getScheduleCategory())
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to map FollowUpScheduleDto to FollowUpScheduleEntity", e);
            throw new MappingException("Failed to map FollowUpScheduleDto to FollowUpScheduleEntity", e);
        }
    }

    /**
     * Converts an EmailTemplate to an EmailTemplateEntity.
     *
     * @param emailTemplate the EmailTemplate to convert
     * @return the converted EmailTemplateEntity
     * @throws MappingException if the conversion fails
     */
    public static EmailTemplateEntity toEmailTemplateEntity(EmailTemplate emailTemplate) throws MappingException {
        try {
            return new EmailTemplateEntity(
                    emailTemplate.getId(),
                    "",
                    emailTemplate.getDraftId(),
                    TemplateCategory.toString(emailTemplate.getTemplateCategory()),
                    emailTemplate.getSubjectTemplate(),
                    emailTemplate.getBodyTemplate(),
                    new String(emailTemplate.getPlaceholderSymbols()),
                    serializePlaceholderManager(emailTemplate.getPlaceholderManager()),
                    emailTemplate.getFollowupNumber()
            );
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to map EmailTemplate to EmailTemplateEntity", e);
            throw new MappingException("Failed to map EmailTemplate to EmailTemplateEntity", e);
        }
    }

    /**
     * Converts an EmailTemplateEntity to an EmailTemplate.
     *
     * @param entity the EmailTemplateEntity to convert
     * @return the converted EmailTemplate
     * @throws MappingException if the conversion fails
     */
    public static EmailTemplate toEmailTemplate(EmailTemplateEntity entity) throws MappingException {
        try {
            return new EmailTemplate.Builder()
                    .setId(entity.getId())
                    .setTemplateCategory(TemplateCategory.fromString(entity.getTemplate_category()))
                    .setSubjectTemplate(entity.getSubject_template())
                    .setBodyTemplate(entity.getBody_template())
                    .setPlaceholderSymbols(entity.getPlaceholder_symbols().toCharArray())
                    .setPlaceholderManager(deserializePlaceholderManager(entity.getPlaceholders()))
                    .setFollowupNumber(entity.getFollowup_number())
                    .build();
        } catch (IOException | InvalidTemplateException | PlaceholderException e) {
            LOGGER.log(Level.SEVERE, "Failed to map EmailTemplateEntity to EmailTemplate", e);
            throw new MappingException("Failed to map EmailTemplateEntity to EmailTemplate", e);
        }
    }

    // Helper methods
    private static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant().atZone(ZoneId.systemDefault()) : null;
    }

    private static Timestamp toTimestamp(ZonedDateTime dateTime) {
        return dateTime != null ? Timestamp.from(dateTime.toInstant()) : null;
    }

    private static String serializePlaceholderManager(PlaceholderManager placeholderManager) throws IOException {
        return objectMapper.writeValueAsString(placeholderManager);
    }

    private static PlaceholderManager deserializePlaceholderManager(String json) throws IOException, PlaceholderException {
        return objectMapper.readValue(json, PlaceholderManager.class);
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