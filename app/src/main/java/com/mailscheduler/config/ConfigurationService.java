package com.mailscheduler.config;

import com.mailscheduler.model.SpreadsheetReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigurationService provides comprehensive configuration management
 * with improved separation of concerns, dependency injection,
 * and robust error handling.
 */
public class ConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    private final Path configFilePath;
    private final ConfigurationValidator validator;
    private final ConfigurationInitializationService initializationService;

    /**
     * Constructor
     * @param configFilePath Path to the configuration file
     */
    public ConfigurationService(Path configFilePath) {
        this.configFilePath = configFilePath;
        this.validator = new ConfigurationValidator();
        this.initializationService = new ConfigurationInitializationService(this);
    }

    /**
     * Loads configuration from properties file, initializing if no configuration exists
     *
     * @return Loaded or newly created configuration
     * @throws ConfigurationException If configuration is invalid
     */
    public Configuration loadConfiguration() throws ConfigurationException {
        if (!Files.exists(configFilePath)) {
            logger.warn("Configuration file does not exist. Initializing configuration.");
            return initializationService.initializeConfiguration();
        }

        try (InputStream input = Files.newInputStream(configFilePath)) {
            Properties properties = new Properties();
            properties.load(input);

            Configuration configuration = parsePropertiesToConfiguration(properties);
            validator.validate(configuration);
            return configuration;
        } catch (IOException e) {
            logger.error("Failed to read configuration file", e);
            logger.warn("Attempting to reinitialize configuration.");
            return initializationService.initializeConfiguration();
        }
    }

    /**
     * Saves configuration to properties file
     *
     * @param configuration Configuration to save
     * @throws IOException If saving fails
     * @throws ConfigurationException If configuration is invalid
     */
    public void saveConfiguration(Configuration configuration) throws IOException, ConfigurationException {
        validator.validate(configuration);

        Properties properties = convertConfigurationToProperties(configuration);

        // Ensure parent directory exists
        Files.createDirectories(configFilePath.getParent());

        // Save properties
        try (OutputStream output = Files.newOutputStream(configFilePath)) {
            properties.store(output, "Mail Scheduler Configuration");
        }
    }

    /**
     * Modify existing configuration through interactive wizard
     *
     * @param existingConfiguration Current configuration to modify
     * @return Updated configuration
     * @throws IOException If file operations fail
     * @throws ConfigurationException If configuration is invalid
     */
    public Configuration modifyConfiguration(Configuration existingConfiguration)
            throws IOException, ConfigurationException {
        return initializationService.modifyConfiguration(existingConfiguration);
    }

    public void deleteConfiguration() throws IOException {
        if (Files.exists(configFilePath)) {
            Files.delete(configFilePath);
            System.out.println("Configuration file deleted successfully.");
        } else {
            System.out.println("Configuration file does not exist");
        }
    }

    /**
     * Converts Properties to Configuration object
     *
     * @param properties Properties to parse
     * @return Parsed Configuration
     */
    private Configuration parsePropertiesToConfiguration(Properties properties) {
        Configuration.Builder builder = new Configuration.Builder()
                .recipientColumns(parseRecipientColumns(properties))
                .markEmailColumns(parseMarkEmailColumns(properties))
                .markSchedulesForEmailColumns(parseMarkScheduleEmailColumns(properties))
                .spreadsheetId(properties.getProperty("spreadsheet.id"))
                .saveMode(Boolean.parseBoolean(properties.getProperty("save.mode", "true")))
                .numberOfFollowUps(Integer.parseInt(properties.getProperty("number.of.followups", "0")))
                .setDefaultSender(properties.getProperty("default.sender"));

        parseSendingCriteria(properties, builder);

        return builder.build();
    }

    /**
     * Parse recipient columns from properties
     *
     * @param properties Properties object containing configuration
     * @return Map of recipient columns
     */
    private Map<String, SpreadsheetReference> parseRecipientColumns(Properties properties) {
        Map<String, SpreadsheetReference> recipientColumns = new HashMap<>();
        String[] recipientColumnKeys = {
                "domain", "emailAddress", "name", "salutation", "phoneNumber", "initialEmailDate"
        };

        for (String key : recipientColumnKeys) {
            String propertyKey = "recipient.columns." + key;
            String columnLetter = properties.getProperty(propertyKey);

            if (columnLetter != null && !columnLetter.isEmpty()) {
                recipientColumns.put(key, SpreadsheetReference.ofColumn(columnLetter));
            } else {
                logger.warn("Recipient column '{}' not found in configuration", key);
            }
        }

        return recipientColumns;
    }

    /**
     * Parse mark email columns from properties
     *
     * @param properties Properties object containing configuration
     * @return Map of mark email columns
     */
    private Map<String, SpreadsheetReference> parseMarkEmailColumns(Properties properties) {
        Map<String, SpreadsheetReference> markEmailColumns = new HashMap<>();
        int numberOfFollowUps = Integer.parseInt(properties.getProperty("number.of.followups", "0"));

        // Add INITIAL column
        String initialColumnProperty = properties.getProperty("mark.email.columns.INITIAL");
        if (initialColumnProperty != null) {
            markEmailColumns.put("INITIAL", SpreadsheetReference.ofColumn(initialColumnProperty));
        }

        // Add follow-up columns based on number of follow-ups
        for (int i = 1; i <= numberOfFollowUps; i++) {
            String followUpKey = "FOLLOW_UP_" + i;
            String followUpColumnProperty = properties.getProperty("mark.email.columns." + followUpKey);

            if (followUpColumnProperty != null) {
                markEmailColumns.put(followUpKey, SpreadsheetReference.ofColumn(followUpColumnProperty));
            }
        }

        return markEmailColumns;
    }

    /**
     * Parse mark schedule email columns from properties
     *
     * @param properties Properties object containing configuration
     * @return Map of mark schedule email columns
     */
    private Map<String, SpreadsheetReference> parseMarkScheduleEmailColumns(Properties properties) {
        Map<String, SpreadsheetReference> markScheduleColumns = new HashMap<>();
        int numberOfFollowUps = Integer.parseInt(properties.getProperty("number.of.followups", "0"));

        // Add INITIAL column
        String initialColumnProperty = properties.getProperty("mark.schedule.columns.INITIAL");
        if (initialColumnProperty != null) {
            markScheduleColumns.put("INITIAL", SpreadsheetReference.ofColumn(initialColumnProperty));
        }

        // Add follow-up columns based on number of follow-ups
        for (int i = 1; i <= numberOfFollowUps; i++) {
            String followUpKey = "FOLLOW_UP_" + i;
            String followUpColumnProperty = properties.getProperty("mark.schedule.columns." + followUpKey);

            if (followUpColumnProperty != null) {
                markScheduleColumns.put(followUpKey, SpreadsheetReference.ofColumn(followUpColumnProperty));
            }
        }

        return markScheduleColumns;
    }

    /**
     * Parse sending criteria from properties
     */
    private void parseSendingCriteria(Properties properties, Configuration.Builder builder) {
        int criteriaCount = Integer.parseInt(properties.getProperty("sending.criteria.count", "0"));

        for (int i = 0; i < criteriaCount; i++) {
            try {
                CriterionType type = CriterionType.valueOf(
                        properties.getProperty("sending.criteria." + i + ".type",
                                CriterionType.COLUMN_FILLED.name())
                );

                SpreadsheetReference targetColumn = SpreadsheetReference.ofColumn(
                        properties.getProperty("sending.criteria." + i + ".target.column")
                );

                SendingCriterion.Builder criteriaBuilder = new SendingCriterion.Builder()
                        .withType(type)
                        .withTargetColumn(targetColumn);

                // Add optional properties based on type
                if (type == CriterionType.COLUMN_VALUE_MATCH) {
                    criteriaBuilder.withExpectedValue(
                            properties.getProperty("sending.criteria." + i + ".expectedValue")
                    );
                } else if (type == CriterionType.COLUMN_PATTERN_MATCH) {
                    criteriaBuilder.withPattern(
                            properties.getProperty("sending.criteria." + i + ".pattern")
                    );
                }

                builder.addSendingCriterion(criteriaBuilder.build());
            } catch (Exception e) {
                // Log and skip invalid criteria
                logger.warn("Failed to parse sending criteria {}: {}", i, e.getMessage());
            }
        }
    }

    /**
     * Converts Configuration to Properties object
     *
     * @param configuration Configuration to convert
     * @return Converted Properties
     */
    private Properties convertConfigurationToProperties(Configuration configuration) {
        Properties properties = new Properties();

        // Spreadsheet ID and Save Mode
        properties.setProperty("spreadsheet.id", configuration.getSpreadsheetId());
        properties.setProperty("save.mode", String.valueOf(configuration.isSaveMode()));
        properties.setProperty("number.of.followups", String.valueOf(configuration.getNumberOfFollowUps()));
        properties.setProperty("default.sender", configuration.getDefaultSender());

        // Recipient Columns
        configuration.getRecipientColumns().forEach((key, value) ->
                properties.setProperty("recipient.columns." + key, value.getReference())
        );

        // Mark Email Columns
        configuration.getMarkEmailColumns().forEach((key, value) ->
                properties.setProperty("mark.email.columns." + key, value.getReference())
        );

        // Mark Schedule Email Columns
        configuration.getMarkSchedulesForEmailColumns().forEach((key, value) ->
                properties.setProperty("mark.schedule.columns." + key, value.getReference())
        );

        List<SendingCriterion> sendingCriteria = configuration.getSendingCriteria();
        if (sendingCriteria != null && !sendingCriteria.isEmpty()) {
            for (int i = 0; i < sendingCriteria.size(); i++) {
                SendingCriterion criteria = sendingCriteria.get(i);
                String prefix = "sending.criteria." + (i) + ".";

                properties.setProperty(prefix + "type", criteria.getType().name());
                properties.setProperty(prefix + "target.column", criteria.getTargetColumn().getReference());

                // Add additional properties based on criteria type
                if (criteria.getType() == CriterionType.COLUMN_VALUE_MATCH) {
                    properties.setProperty(prefix + "expected.value", criteria.getExpectedValue());
                } else if (criteria.getType() == CriterionType.COLUMN_PATTERN_MATCH) {
                    properties.setProperty(prefix + "pattern", criteria.getPattern().pattern());
                }
            }

            // Store the total number of criteria for easy restoration
            properties.setProperty("sending.criteria.count", String.valueOf(sendingCriteria.size()));
        }


        return properties;
    }

    /**
     * Custom exception for configuration-related errors
     */
    public static class ConfigurationException extends Exception {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

/**
 * Validator for Configuration to ensure data integrity
 */
class ConfigurationValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    /**
     * Validates the configuration
     * @param configuration Configuration to validate
     * @throws ConfigurationService.ConfigurationException If configuration is invalid
     */
    public void validate(Configuration configuration) throws ConfigurationService.ConfigurationException {
        validateColumns(configuration);
        validateSpreadsheetId(configuration);
    }

    private void validateColumns(Configuration configuration) throws ConfigurationService.ConfigurationException {
        validateColumnMap(configuration.getRecipientColumns(), "Recipient");
        validateColumnMap(configuration.getMarkEmailColumns(), "Mark Email");
        validateColumnMap(configuration.getMarkSchedulesForEmailColumns(), "Mark Schedules");
    }

    private void validateColumnMap(Map<String, SpreadsheetReference> columns, String mapName)
            throws ConfigurationService.ConfigurationException {
        if (columns == null || columns.isEmpty()) {
            String errorMessage = mapName + " columns configuration is missing or empty";
            logger.error(errorMessage);
            throw new ConfigurationService.ConfigurationException(errorMessage);
        }
    }

    private void validateSpreadsheetId(Configuration configuration)
            throws ConfigurationService.ConfigurationException {
        if (configuration.getSpreadsheetId() == null ||
                configuration.getSpreadsheetId().trim().isEmpty()) {
            String errorMessage = "Spreadsheet ID is missing or empty";
            logger.error(errorMessage);
            throw new ConfigurationService.ConfigurationException(errorMessage);
        }
    }
}