package com.mailscheduler.config;

import com.mailscheduler.exception.validation.CriterionValidationException;
import com.mailscheduler.exception.service.EmailValidationException;
import com.mailscheduler.model.SpreadsheetReference;
import com.mailscheduler.service.AbstractUserConsoleInteractionService;
import com.mailscheduler.service.EmailValidationService;
import com.mailscheduler.service.SpreadsheetService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ConfigurationInitializationService extends AbstractUserConsoleInteractionService {
    private final ConfigurationService configurationService;

    /**
     * Constructor for ConfigurationInitializationService
     *
     * @param configurationService The configuration service to use
     */
    public ConfigurationInitializationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Initializes the configuration through an interactive console process
     *
     * @return Fully configured Configuration object
     * @throws ConfigurationService.ConfigurationException If configuration is invalid
     */
    public Configuration initializeConfiguration() throws ConfigurationService.ConfigurationException {
        System.out.println("=== Mail Scheduler Configuration Wizard ===\n");
        System.out.println("Welcome to MailScheduler Configuration Wizard!");
        System.out.println("Are you using the standard template? (y/n)");

        while (true) {
            String useTemplate = scanner.next().trim().toLowerCase();

            if (useTemplate.equals("yes") || useTemplate.equals("y")) {
                return initializeTemplateConfiguration();
            } else if (useTemplate.equals("no") || useTemplate.equals("n")){
                return initializeCustomConfiguration();
            } else {
                System.out.println("Invalid input. Please enter 'yes/y' or 'no/n'");
            }
        }
    }

    private Configuration initializeTemplateConfiguration() throws ConfigurationService.ConfigurationException {
        System.out.println("\nUsing standard template configuration...");

        String spreadsheetId = configureSpreadsheetId();
        boolean saveMode = configureSaveMode();
        String defaultSender = configureDefaultSender();
        List<SendingCriterion> sendingCriteria = configureSendingCriteria();

        Configuration config = TemplateConfiguration.createTemplateConfiguration(
                spreadsheetId,
                saveMode,
                sendingCriteria,
                defaultSender
        );

        System.out.println("\nTemplate configuration completed successfully!");
        printConfigurationSummary(config);

        return config;
    }

    private Configuration initializeCustomConfiguration() throws ConfigurationService.ConfigurationException {
        System.out.println("\nProceeding with custom configuration...");

        boolean saveMode = configureSaveMode();
        String spreadsheetId = configureSpreadsheetId();
        int numberOfFollowUps = configureNumberOfFollowUps();
        String defaultSender = configureDefaultSender();

        // Column Configurations
        Map<String, SpreadsheetReference> contactColumns = configureContactColumns();
        Map<String, SpreadsheetReference> markEmailColumns = configureMarkEmailColumns(numberOfFollowUps);
        Map<String, SpreadsheetReference> markScheduleForEmailColumns = configureMarkScheduleEmailColumns(numberOfFollowUps);

        List<SendingCriterion> sendingCriteria = configureSendingCriteria();

        // Create Configuration
        Configuration configuration = new Configuration(
                contactColumns,
                markEmailColumns,
                markScheduleForEmailColumns,
                spreadsheetId,
                saveMode,
                numberOfFollowUps,
                sendingCriteria,
                defaultSender
        );

        System.out.println("=== Configuration Completed Successfully ===");
        return configuration;
    }

    private void printConfigurationSummary(Configuration config) {
        System.out.println("\nConfiguration Summary:");
        System.out.println("Spreadsheet ID: " + config.getSpreadsheetId());
        System.out.println("Default Sender: " + config.getDefaultSender());
        System.out.println("Number of Follow-ups: " + config.getNumberOfFollowUps());
        System.out.println("Save Mode: " + config.isSaveMode());

        System.out.println("\nContact Columns:");
        config.getContactColumns().forEach((key, value) ->
                System.out.println("  " + key + ": Column " + value.getReference())
        );

        System.out.println("\nMark Email Columns:");
        config.getMarkEmailColumns().forEach((key, value) ->
                System.out.println("  " + key + ": Column " + value.getReference())
        );

        System.out.println("\nMark Schedule Columns:");
        config.getMarkSchedulesForEmailColumns().forEach((key, value) ->
                System.out.println("  " + key + ": Column " + value.getReference())
        );
    }

    /**
     * Configures save mode through user interaction
     *
     * @return Selected save mode
     */
    private boolean configureSaveMode() {
        System.out.println("\nConfigure Save Mode:");
        System.out.println("1. Enable Save Mode (Recommended)");
        System.out.println("2. Disable Save Mode");

        int choice = getValidatedIntegerInput("Select save mode:", 2);
        return choice == 1;
    }

    private int configureNumberOfFollowUps() {
        System.out.println("\nEnter the number of follow-ups:");
        return getValidatedIntegerInput("Number of follow-ups", Integer.MAX_VALUE);
    }

    private String configureDefaultSender() {
        System.out.println("\nEnter the default sender email address:");
        while (true) {
            String defaultSender = scanner.next().trim();
            try {
                EmailValidationService.validateEmail(defaultSender);
                return defaultSender;
            } catch (EmailValidationException e) {
                System.out.println("Invalid email address. Please try again.");
            }
        }
    }

    /**
     * Configures spreadsheet ID through user interaction
     *
     * @return Entered spreadsheet ID
     */
    private String configureSpreadsheetId() {
        while (true) {
            System.out.println("\nEnter Google Spreadsheet ID:");
            String input = scanner.next().trim();

            String spreadsheetId;

            if (input.isEmpty()) {
                System.out.println("Spreadsheet ID cannot be empty. Please try again.");
            }

            // Check if input looks like a full URL
            if (input.contains("docs.google.com/spreadsheets")) {
                spreadsheetId = SpreadsheetService.extractSpreadsheetIdFromUrl(input);
                if (spreadsheetId == null) {
                    System.out.println("Could not extract spreadsheet ID from the provided URL.");
                    continue;
                }
            } else {
                // Assume input is directly the spreadsheet ID
                spreadsheetId = input;
            }

            try {
                // Validate the spreadsheet ID
                if (SpreadsheetService.validateSpreadsheetId(spreadsheetId)) {
                    System.out.println("Spreadsheet validated successfully!");
                    return spreadsheetId;
                } else {
                    System.out.println("Invalid spreadsheet ID or access denied. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Error validating spreadsheet: " + e.getMessage());
                System.out.println("Please ensure you have:");
                System.out.println("1. A valid spreadsheet ID");
                System.out.println("2. Proper Google Sheets API credentials");
                System.out.println("3. Appropriate access permissions");
            }

        }
    }

    /**
     * Configures contact columns through user interaction
     *
     * @return Map of configured contact columns
     */
    private Map<String, SpreadsheetReference> configureContactColumns() {
        System.out.println("\nConfiguring Contact Columns:");
        Map<String, SpreadsheetReference> contactColumns = new HashMap<>();

        String[] contactColumnKeys = {
                "domain", "emailAddress", "name", "salutation", "phoneNumber", "initialEmailDate"
        };

        String[] exampleColumns = {
                "A", "B", "C", "D", "S", "I"
        };

        for (int i = 0; i < contactColumnKeys.length; i++) {
            String key = contactColumnKeys[i];
            String exampleColumn = exampleColumns[i];
            SpreadsheetReference column = configureSpreadsheetReference(
                    "Enter column letter for " + key + " (e.g., " + exampleColumn + "):"
            );
            contactColumns.put(key, column);
        }

        return contactColumns;
    }

    /**
     * Configures mark email columns through user interaction
     *
     * @return Map of configured mark email columns
     */
    private Map<String, SpreadsheetReference> configureMarkEmailColumns(int numberOfFollowUps) {
        return configureMultiColumnMapping(
                "Mark Email Columns",
                createColumnKeys(numberOfFollowUps)
        );
    }

    /**
     * Configures mark schedule email columns through user interaction
     *
     * @return Map of configured mark schedule email columns
     */
    private Map<String, SpreadsheetReference> configureMarkScheduleEmailColumns(int numberOfFollowUps) {
        return configureMultiColumnMapping(
                "Mark Schedule Email Columns",
                createColumnKeys(numberOfFollowUps)
        );
    }

    private String[] createColumnKeys(int numberOfFollowUps) {
        String[] columnNames = new String[1 + numberOfFollowUps];
        columnNames[0] = "INITIAL";
        for (int i = 1; i <= numberOfFollowUps; i++) {
            columnNames[i] = "FOLLOW_UP_" + i;
        }
        return columnNames;
    }

    /**
     * Generic method to configure multiple column mappings
     *
     * @param configName Name of the configuration being set up
     * @param keys Array of keys for the columns
     * @return Map of configured columns
     */
    private Map<String, SpreadsheetReference> configureMultiColumnMapping(
            String configName,
            String[] keys
    ) {
        System.out.println("\nConfiguring " + configName + ":");
        Map<String, SpreadsheetReference> columns = new HashMap<>();

        for (String key : keys) {
            SpreadsheetReference column = configureSpreadsheetReference(
                    "Enter column letter for " + key + " (e.g., E):"
            );
            columns.put(key, column);
        }

        return columns;
    }

    /**
     * Configures a single SpreadsheetReference through user interaction
     *
     * @param prompt Prompt to display to the user
     * @return Configured SpreadsheetReference
     */
    private SpreadsheetReference configureSpreadsheetReference(String prompt) {
        while (true) {
            System.out.println(prompt);
            String columnLetter = scanner.next().trim().toUpperCase();

            try {
                return SpreadsheetReference.ofColumn(columnLetter);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid column letter. Please enter a valid column (A-Z).");
            }
        }
    }

    private List<SendingCriterion> configureSendingCriteria() throws ConfigurationService.ConfigurationException {
        try {
            List<SendingCriterion> sendingCriteria = new ArrayList<>();

            while (true) {
                System.out.println("\nConfigure Sending Criteria:");
                System.out.println("1. Add New Sending Criteria");
                System.out.println("2. Finish Configuring");

                int choice = getValidatedIntegerInput("Select an option:", 2);

                if (choice == 1) {
                    // Configure individual sending criteria
                    SendingCriterion criteria = configureSingleSendingCriteria(sendingCriteria.size() + 1);
                    sendingCriteria.add(criteria);
                } else {
                    break;
                }
            }

            return sendingCriteria;
        } catch (CriterionValidationException e) {
            throw new ConfigurationService.ConfigurationException("Failed to configure sending criteria");
        }
    }

    /**
     * Configures a single sending criteria
     *
     * @param criteriaNumber Number of the current criteria being configured
     * @return Configured SendingCriteria
     */
    private SendingCriterion configureSingleSendingCriteria(int criteriaNumber) throws CriterionValidationException {
        System.out.println("\nConfiguring Sending Criteria #" + criteriaNumber);

        // Select criteria type
        List<String> criteriaTypes = Arrays.stream(CriterionType.values())
                .map(CriterionType::name)
                .toList();

        System.out.println("Select criteria type:");
        System.out.println("INFO: Currently, only COLUMN_FILLED works");
        for (int i = 0; i < criteriaTypes.size(); i++) {
            System.out.println((i + 1) + ". " + criteriaTypes.get(i));
        }

        int criteriaTypeChoice = getValidatedIntegerInput("Select criteria type:", criteriaTypes.size()) - 1;
        CriterionType criterionType = CriterionType.valueOf(criteriaTypes.get(criteriaTypeChoice));

        // Configure criteria builder
        SendingCriterion.Builder criteriaBuilder = new SendingCriterion.Builder()
                .withType(criterionType);

        // Configure target column
        SpreadsheetReference targetColumn = configureSpreadsheetReference("Enter column letter for criteria:");
        criteriaBuilder.withTargetColumn(targetColumn);

        // Additional configuration based on criteria type
        switch (criterionType) {
            case COLUMN_VALUE_MATCH:
                System.out.println("Enter exact value to match:");
                String exactValue = scanner.next().trim();
                criteriaBuilder.withExpectedValue(exactValue);
                break;
            case COLUMN_PATTERN_MATCH:
                System.out.println("Enter regex pattern to match:");
                String regex = scanner.next().trim();
                criteriaBuilder.withPattern(regex);
                break;
            case COLUMN_FILLED, STATUS_CHECK:
                // These types don't require additional configuration
                break;
        }

        // Build and return the criteria
        return criteriaBuilder.build();
    }

    /**
     * Provides a method to modify existing configuration
     *
     * @param existingConfiguration The current configuration to modify
     * @return Updated Configuration
     * @throws IOException If there's an error saving the configuration
     * @throws ConfigurationService.ConfigurationException If configuration is invalid
     */
    public Configuration modifyConfiguration(Configuration existingConfiguration)
            throws IOException, ConfigurationService.ConfigurationException {

        System.out.println("=== Configuration Modification Wizard ===");

        List<String> modificationOptions = List.of(
                "Save Mode",
                "Spreadsheet ID",
                "Contact Columns",
                "Mark Email Columns",
                "Mark Schedule Email Columns",
                "Number of follow-ups",
                "Sending criteria",
                "Default sender"
        );

        System.out.println("\nSelect what you want to modify:");
        modificationOptions.forEach((option) ->
                System.out.println((modificationOptions.indexOf(option) + 1) + ". " + option)
        );

        int choice = getValidatedIntegerInput("Enter your choice:", modificationOptions.size()) - 1;

        Configuration updatedConfiguration = switch (choice) {
            case 0 -> {
                boolean newSaveMode = configureSaveMode();
                yield new Configuration(
                        existingConfiguration.getContactColumns(),
                        existingConfiguration.getMarkEmailColumns(),
                        existingConfiguration.getMarkSchedulesForEmailColumns(),
                        existingConfiguration.getSpreadsheetId(),
                        newSaveMode,
                        existingConfiguration.getNumberOfFollowUps(),
                        existingConfiguration.getSendingCriteria(),
                        existingConfiguration.getDefaultSender()
                );
            }
            case 1 -> {
                String newSpreadsheetId = configureSpreadsheetId();
                yield new Configuration(
                        existingConfiguration.getContactColumns(),
                        existingConfiguration.getMarkEmailColumns(),
                        existingConfiguration.getMarkSchedulesForEmailColumns(),
                        newSpreadsheetId,
                        existingConfiguration.isSaveMode(),
                        existingConfiguration.getNumberOfFollowUps(),
                        existingConfiguration.getSendingCriteria(),
                        existingConfiguration.getDefaultSender()
                );
            }
            case 2 -> {
                Map<String, SpreadsheetReference> newContactColumns = configureContactColumns();
                yield new Configuration(
                        newContactColumns,
                        existingConfiguration.getMarkEmailColumns(),
                        existingConfiguration.getMarkSchedulesForEmailColumns(),
                        existingConfiguration.getSpreadsheetId(),
                        existingConfiguration.isSaveMode(),
                        existingConfiguration.getNumberOfFollowUps(),
                        existingConfiguration.getSendingCriteria(),
                        existingConfiguration.getDefaultSender()
                );
            }
            case 3 -> {
                Map<String, SpreadsheetReference> newMarkEmailColumns = configureMarkEmailColumns(existingConfiguration.getNumberOfFollowUps());
                yield new Configuration(
                        existingConfiguration.getContactColumns(),
                        newMarkEmailColumns,
                        existingConfiguration.getMarkSchedulesForEmailColumns(),
                        existingConfiguration.getSpreadsheetId(),
                        existingConfiguration.isSaveMode(),
                        existingConfiguration.getNumberOfFollowUps(),
                        existingConfiguration.getSendingCriteria(),
                        existingConfiguration.getDefaultSender()
                );
            }
            case 4 -> {
                Map<String, SpreadsheetReference> newMarkScheduleColumns = configureMarkScheduleEmailColumns(existingConfiguration.getNumberOfFollowUps());
                yield new Configuration(
                        existingConfiguration.getContactColumns(),
                        existingConfiguration.getMarkEmailColumns(),
                        newMarkScheduleColumns,
                        existingConfiguration.getSpreadsheetId(),
                        existingConfiguration.isSaveMode(),
                        existingConfiguration.getNumberOfFollowUps(),
                        existingConfiguration.getSendingCriteria(),
                        existingConfiguration.getDefaultSender()
                );
            }
            case 5 -> {
                System.out.println("Currently not able to change the number of follow-ups");
                yield existingConfiguration;
            }
            case 6 -> {
                List<SendingCriterion> newSendingCriteria = configureSendingCriteria();
                yield new Configuration(
                        existingConfiguration.getContactColumns(),
                        existingConfiguration.getMarkEmailColumns(),
                        existingConfiguration.getMarkSchedulesForEmailColumns(),
                        existingConfiguration.getSpreadsheetId(),
                        existingConfiguration.isSaveMode(),
                        existingConfiguration.getNumberOfFollowUps(),
                        newSendingCriteria,
                        existingConfiguration.getDefaultSender()
                );
            }
            case 7 -> {
                String newDefaultSender = configureDefaultSender();
                yield new Configuration(
                        existingConfiguration.getContactColumns(),
                        existingConfiguration.getMarkEmailColumns(),
                        existingConfiguration.getMarkSchedulesForEmailColumns(),
                        existingConfiguration.getSpreadsheetId(),
                        existingConfiguration.isSaveMode(),
                        existingConfiguration.getNumberOfFollowUps(),
                        existingConfiguration.getSendingCriteria(),
                        newDefaultSender
                );
            }
            default -> throw new IllegalStateException("Unexpected modification choice");
        };

        configurationService.saveConfiguration(updatedConfiguration);
        return updatedConfiguration;
    }
}
