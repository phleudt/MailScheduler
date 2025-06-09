package com.mailscheduler.application.synchronization.spreadsheet.strategies;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.mailscheduler.application.synchronization.spreadsheet.gateway.SpreadsheetGateway;
import com.mailscheduler.domain.factory.FollowUpPlanFactory;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.config.ApplicationConfiguration;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpStepMetadata;
import com.mailscheduler.domain.model.template.Template;
import com.mailscheduler.domain.model.template.TemplateMetadata;
import com.mailscheduler.domain.model.template.placeholder.PlaceholderException;
import com.mailscheduler.domain.model.template.placeholder.PlaceholderManager;
import com.mailscheduler.domain.repository.ConfigurationRepository;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.TemplateRepository;
import com.mailscheduler.infrastructure.service.FollowUpManagementService;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Strategy for synchronizing configuration data between a spreadsheet and the local database.
 * Handles application settings, templates, follow-up plans, column mappings, and placeholders.
 */
public class ConfigSheetSyncStrategy extends AbstractSpreadsheetSynchronizationStrategy {
    private static final Logger LOGGER = Logger.getLogger(ConfigSheetSyncStrategy.class.getName());
    private static final String CONFIG_SHEET_NAME = "Configuration";
    private static final int TEMPLATE_START_ROW = 19;
    private static final int PLACEHOLDER_START_ROW = 19;
    private static final char[] DEFAULT_DELIMITERS = new char[]{'{', '}'};

    private final ConfigurationRepository configurationRepository;
    private final TemplateRepository templateRepository;
    private final FollowUpManagementService followUpManagementService;

    // Collected data during synchronization
    private ApplicationConfiguration applicationConfiguration;
    private List<ColumnMapping> columnMappings;
    private List<TemplateSheetDto> templateSheetDtos;
    private Character[] delimiters;
    private PlaceholderManager placeholderManager;
    private ValueRange followUpValueRange;

    /**
     * Creates a new ConfigSheetSyncStrategy with the required dependencies.
     *
     * @param spreadsheetGateway Gateway for interacting with spreadsheets
     * @param configurationRepository Repository for application configuration
     * @param templateRepository Repository for email templates
     * @param followUpManagementService Service for managing follow-up plans
     */
    public ConfigSheetSyncStrategy(
            SpreadsheetGateway spreadsheetGateway,
            ConfigurationRepository configurationRepository,
            TemplateRepository templateRepository,
            FollowUpManagementService followUpManagementService
    ) {
        super(spreadsheetGateway);
        this.configurationRepository = Objects.requireNonNull(configurationRepository, "ConfigurationRepository cannot be null");
        this.templateRepository = Objects.requireNonNull(templateRepository, "TemplateRepository cannot be null");
        this.followUpManagementService = Objects.requireNonNull(followUpManagementService, "FollowUpManagementService cannot be null");

        // Initialize collections
        resetCollectedData();
    }

    @Override
    public String getStrategyName() {
        return "Configuration Sheet Synchronization Strategy";
    }

    @Override
    public boolean shouldProcessSheet(SheetConfiguration sheetConfiguration) {
        // This strategy only processes the Configuration sheet
        return sheetConfiguration != null && CONFIG_SHEET_NAME.equals(sheetConfiguration.title());
    }

    @Override
    protected void processSheetsInSpreadsheet(SpreadsheetConfiguration configuration) {
        // Reset collected data for a new sync
        resetCollectedData();

        // Find configuration sheet
        Optional<SheetConfiguration> configSheetOpt = configuration.sheetConfigurations().stream()
                .filter(sheet -> CONFIG_SHEET_NAME.equals(sheet.title()))
                .findFirst();

        if (configSheetOpt.isEmpty()) {
            logger.info("Configuration sheet not found. Creating initial configuration is not yet implemented.");
            return;
        }

        // Process the configuration sheet
        processSheet(configuration.spreadsheetId(), configSheetOpt.get());
    }

    @Override
    protected void processSheet(String spreadsheetId, SheetConfiguration sheetConfiguration) {
        // Fetch all configuration data
        List<ValueRange> valueRanges = fetchConfigurationData(spreadsheetId);
        if (valueRanges.isEmpty()) {
            logger.warning("No configuration data found in spreadsheet: " + spreadsheetId);
            return;
        }

        // Process each configuration section
        for (ValueRange valueRange : valueRanges) {
            if (valueRange == null || valueRange.getValues() == null) {
                continue;
            }

            ConfigReferences.ConfigTyp configType = identifyConfigType(valueRange);
            if (configType == null) {
                continue;
            }

            try {
                processConfigSection(spreadsheetId, valueRange, configType);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing " + configType + " configuration", e);
            }
        }
    }

    @Override
    protected void doPostProcessing(SpreadsheetConfiguration configuration) {
        try {
            // Process follow-ups after templates are loaded
            if (followUpValueRange != null) {
                processFollowUpPlan(followUpValueRange, templateSheetDtos);
            }

            // Save placeholders from templates back to sheet
            syncNewPlaceholdersToSheet(configuration.spreadsheetId(), placeholderManager);

            // Update templates with delimiters and placeholders
            updateTemplatesWithDelimitersAndPlaceholders(placeholderManager, delimiters);

            // Save application configuration
            saveApplicationConfiguration(configuration.spreadsheetId(), applicationConfiguration, columnMappings);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in configuration post-processing", e);
        }
    }

    /**
     * Resets all collected data fields to prepare for a new synchronization.
     */
    private void resetCollectedData() {
        applicationConfiguration = null;
        columnMappings = new ArrayList<>();
        templateSheetDtos = new ArrayList<>();
        delimiters = null;
        placeholderManager = new PlaceholderManager();
        followUpValueRange = null;
    }

    /**
     * Fetches all configuration data sections from the spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet
     * @return List of ValueRange objects containing the configuration data
     */
    private List<ValueRange> fetchConfigurationData(String spreadsheetId) {
        List<SpreadsheetReference> references = ConfigReferences.getSpreadsheetReferences();
        return fetchSpreadsheetData(spreadsheetId, references);
    }

    /**
     * Processes a single configuration section based on its type.
     *
     * @param spreadsheetId ID of the spreadsheet
     * @param valueRange Value range containing configuration data
     * @param configType Type of configuration data
     */
    private void processConfigSection(String spreadsheetId, ValueRange valueRange, ConfigReferences.ConfigTyp configType) {
        switch (configType) {
            case SETTINGS -> applicationConfiguration = processSettings(valueRange);
            case TEMPLATES -> templateSheetDtos = processTemplates(spreadsheetId, valueRange);
            case FOLLOW_UPS -> followUpValueRange = valueRange;
            case CONTACT_COLUMNS -> columnMappings.addAll(processColumnMappings(ColumnMapping.MappingType.CONTACT, valueRange));
            case RECIPIENT_COLUMNS -> columnMappings.addAll(processColumnMappings(ColumnMapping.MappingType.RECIPIENT, valueRange));
            case EMAIL_COLUMNS -> columnMappings.addAll(processColumnMappings(ColumnMapping.MappingType.EMAIL, valueRange));
            case DELIMITERS -> delimiters = processDelimiters(valueRange);
            case PLACEHOLDERS -> processPlaceholders(placeholderManager, valueRange);
        }
    }

    /**
     * Identifies the configuration type for a given value range.
     *
     * @param valueRange Value range to identify
     * @return The configuration type, or null if not recognized
     */
    private ConfigReferences.ConfigTyp identifyConfigType(ValueRange valueRange) {
        if (valueRange == null || valueRange.getRange() == null) {
            return null;
        }

        String returnedRange = valueRange.getRange();

        for (Map.Entry<ConfigReferences.ConfigTyp, SpreadsheetReference> entry : ConfigReferences.references.entrySet()) {
            String configRange = entry.getValue().getGoogleSheetsReference();

            // Check for exact match of the range
            if (configRange.equals(returnedRange) || entry.getValue().getReference().equals(returnedRange)) {
                return entry.getKey();
            }

        }

        return null;
    }

    /**
     * Processes the settings section of the configuration sheet.
     *
     * @param valueRange Value range containing settings data
     * @return ApplicationConfiguration with settings values
     */
    private ApplicationConfiguration processSettings(ValueRange valueRange) {
        if (valueRange == null || valueRange.getValues() == null) {
            return null;
        }

        ApplicationConfiguration.Builder configBuilder = new ApplicationConfiguration.Builder();
        List<List<Object>> values = valueRange.getValues();

        for (List<Object> row : values) {
            if (row.size() < 2 || row.get(0) == null || row.get(1) == null) {
                continue;
            }

            String key = row.get(0).toString();
            String value = row.get(1).toString();

            switch (key) {
                case "Sender Email Address" -> {
                    try {
                        configBuilder.senderEmailAddress(EmailAddress.of(value));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid sender email address: " + value);

                    }
                }
                case "Save Mode" -> configBuilder.saveMode("TRUE".equalsIgnoreCase(value));
                case "Sending Criteria Column" -> configBuilder.sendingCriteriaColumn(
                        SpreadsheetReference.ofRange(value));
                default -> LOGGER.fine("Unknown setting key: " + key);
            }
        }

        return configBuilder.build();
    }

    /**
     * Processes the templates section of the configuration sheet.
     *
     * @param spreadsheetId ID of the spreadsheet
     * @param valueRange Value range containing template data
     * @return List of template data objects
     */
    private List<TemplateSheetDto> processTemplates(String spreadsheetId, ValueRange valueRange) {
        if (valueRange == null || valueRange.getValues() == null) {
            return Collections.emptyList();
        }

        // Parse templates from sheet
        Map<String, TemplateSheetDto> templatesFromSheet = parseTemplatesFromSheet(valueRange);
        if (templatesFromSheet.isEmpty()) {
            LOGGER.info("No templates found in configuration sheet");
        }

        // Prepare for sheet updates
        int nextAvailableRow = TEMPLATE_START_ROW + templatesFromSheet.size();
        List<EntityData<Template, TemplateMetadata>> templatesFromRepository = templateRepository.findAllWithMetadata();

        // Prepare data for updates to the sheet
        List<SpreadsheetReference> referenceRows = new ArrayList<>();
        List<List<Object>> rowsToWrite = new ArrayList<>();

        // Sync templates from repository to sheet
        syncTemplatesToSheet(
                spreadsheetId,
                templatesFromRepository,
                templatesFromSheet,
                nextAvailableRow,
                referenceRows,
                rowsToWrite
        );

        return new ArrayList<>(templatesFromSheet.values());
    }

    /**
     * Parses template data from the value range.
     *
     * @param valueRange Value range containing template data
     * @return Map of template draft IDs to template data objects
     */
    private Map<String, TemplateSheetDto> parseTemplatesFromSheet(ValueRange valueRange) {
        List<TemplateSheetDto> templates = new ArrayList<>();
        List<List<Object>> values = valueRange.getValues();

        // Skip header row
        List<List<Object>> dataRows = values.subList(1, values.size());

        for (List<Object> row : dataRows) {
            if (row.size() < 3 || row.get(0) == null || row.get(0).toString().isEmpty()) {
                continue;
            }

            try {
                Integer templateNum = Integer.parseInt(row.get(0).toString());
                String subject = row.size() > 1 && row.get(1) != null ? row.get(1).toString() : "";
                String draftId = row.size() > 2 && row.get(2) != null ? row.get(2).toString() : "";

                if (!draftId.isEmpty()) {
                    templates.add(new TemplateSheetDto(templateNum, subject, draftId));
                }
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid template number: " + row.get(0));
            }
        }

        return templates.stream()
                .filter(dto -> dto.draftId() != null && !dto.draftId().isEmpty())
                .collect(Collectors.toMap(
                        TemplateSheetDto::draftId,
                        Function.identity(),
                        (existing, replacement) -> existing // Keep first in case of duplicates
                ));
    }

    /**
     * Synchronizes template data between repository and sheet.
     *
     * @param spreadsheetId ID of the spreadsheet
     * @param templatesFromRepository Templates from the repository
     * @param templatesFromSheet Templates parsed from the sheet
     * @param nextAvailableRow Next available row for new templates
     * @param referenceRows Accumulates references for sheet updates
     * @param rowsToWrite Accumulates data for sheet updates
     */
    private void syncTemplatesToSheet(
            String spreadsheetId,
            List<EntityData<Template, TemplateMetadata>> templatesFromRepository,
            Map<String, TemplateSheetDto> templatesFromSheet,
            int nextAvailableRow,
            List<SpreadsheetReference> referenceRows,
            List<List<Object>> rowsToWrite
    ) {
        int sheetTemplateCount = templatesFromSheet.size();

        // Add templates from repository to sheet if not already present
        for (EntityData<Template, TemplateMetadata> templateData : templatesFromRepository) {
            Template template = templateData.entity();
            TemplateMetadata metadata = templateData.metadata();

            if (template == null || metadata == null || metadata.draftId() == null) {
                continue;
            }

            String draftId = metadata.draftId();
            String subject = template.getSubject() != null ? template.getSubject().value() : "";

            if (!templatesFromSheet.containsKey(draftId)) {
                // Add new template to sheet
                rowsToWrite.add(List.of(sheetTemplateCount, subject, draftId));
                referenceRows.add(
                        SpreadsheetReference.ofRange(CONFIG_SHEET_NAME,
                                String.format("A%d:C%d", nextAvailableRow, nextAvailableRow))
                );
                sheetTemplateCount++;
                nextAvailableRow++;
            } else {
                // Update existing template subject if changed
                TemplateSheetDto existingTemplate = templatesFromSheet.get(draftId);
                if (!existingTemplate.subject().equals(subject)) {
                    rowsToWrite.add(List.of(existingTemplate.number(), subject, existingTemplate.draftId()));
                    referenceRows.add(
                            SpreadsheetReference.ofRange(CONFIG_SHEET_NAME,
                                    String.format("A%d:C%d", existingTemplate.number() + TEMPLATE_START_ROW - 1,
                                            existingTemplate.number() + TEMPLATE_START_ROW - 1))
                    );
                }
            }
        }

        // Write updates to sheet
        if (!rowsToWrite.isEmpty()) {
            try {
                spreadsheetGateway.writeData(spreadsheetId, referenceRows, rowsToWrite);
                LOGGER.info(String.format("Updated %d templates in configuration sheet", rowsToWrite.size()));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to write template updates to sheet", e);
            }
        }
    }

    /**
     * Processes column mappings for different entity types.
     *
     * @param mappingType Type of mapping (CONTACT, RECIPIENT, EMAIL)
     * @param valueRange Value range containing mapping data
     * @return List of column mappings
     */
    private List<ColumnMapping> processColumnMappings(ColumnMapping.MappingType mappingType, ValueRange valueRange) {
        List<ColumnMapping> columnMappings = new ArrayList<>();

        if (valueRange == null || valueRange.getValues() == null) {
            return columnMappings;
        }

        for (List<Object> row : valueRange.getValues()) {
            if (row.size() < 2 || row.get(0) == null || row.get(1) == null) {
                continue;
            }

            String fieldName = row.get(0).toString();
            String columnReference = row.get(1).toString();

            if (!fieldName.isEmpty() && !columnReference.isEmpty()) {
                columnMappings.add(new ColumnMapping(mappingType, fieldName,
                        SpreadsheetReference.ofColumn(CONFIG_SHEET_NAME, columnReference)));
            }
        }

        LOGGER.info(String.format("Processed %d %s column mappings", columnMappings.size(), mappingType));
        return columnMappings;
    }

    /**
     * Processes delimiter data from the configuration sheet.
     *
     * @param valueRange Value range containing delimiter data
     * @return Array of delimiter characters
     */
    private Character[] processDelimiters(ValueRange valueRange) {
        if (valueRange == null || valueRange.getValues() == null || valueRange.getValues().isEmpty() ||
                valueRange.getValues().get(0).isEmpty()) {
            LOGGER.info("No delimiters found, using defaults");
            return new Character[]{'{', '}'};
        }

        String delimitersStr = valueRange.getValues().get(0).get(0).toString().trim();
        if (delimitersStr.isEmpty()) {
            return new Character[]{'{', '}'};
        }

        String[] delimitersList = delimitersStr.split(",");
        Character[] charDelimiters = new Character[Math.min(delimitersList.length, 2)]; // Ensure we only use at most 2

        for (int i = 0; i < charDelimiters.length; i++) {
            if (!delimitersList[i].isEmpty()) {
                charDelimiters[i] = delimitersList[i].charAt(0);
            } else {
                charDelimiters[i] = DEFAULT_DELIMITERS[i];
            }
        }

        LOGGER.info("Using delimiters: " + Arrays.toString(charDelimiters));
        return charDelimiters;
    }

    /**
     * Processes placeholder definitions from the configuration sheet.
     *
     * @param placeholderManager Placeholder manager to update
     * @param valueRange Value range containing placeholder data
     */
    private void processPlaceholders(PlaceholderManager placeholderManager, ValueRange valueRange) {
        if (valueRange == null || valueRange.getValues() == null || valueRange.getValues().isEmpty()) {
            LOGGER.info("No placeholders found in configuration sheet");
            return;
        }
        List<List<Object>> values = valueRange.getValues();

        // Skip header row
        var dataRows = values.subList(1, values.size());

        int placeholderCount = 0;
        for (List<Object> row : dataRows) {
            if (row.isEmpty() || row.get(0) == null || row.get(0).toString().isEmpty()) {
                continue;
            }

            String placeholder = row.get(0).toString().trim();
            String replacement = row.size() >= 2 && row.get(1) != null ? row.get(1).toString().trim() : "";

            try {
                if (!placeholder.isEmpty() && !replacement.isEmpty()) {
                    placeholderManager.addPlaceholder(placeholder,
                            SpreadsheetReference.ofColumn(replacement));
                    placeholderCount++;
                }
            } catch (PlaceholderException e) {
                LOGGER.log(Level.WARNING, "Invalid placeholder: " + placeholder, e);
            }
        }

        LOGGER.info("Processed " + placeholderCount + " placeholders");
    }

    /**
     * Processes follow-up plan data from the configuration sheet.
     *
     * @param followUpsValueRange Value range containing follow-up data
     * @param templateSheetDtos Template data from the sheet
     */
    private void processFollowUpPlan(ValueRange followUpsValueRange, List<TemplateSheetDto> templateSheetDtos) {
        if (followUpsValueRange == null || followUpsValueRange.getValues() == null ||
                followUpsValueRange.getValues().isEmpty() || templateSheetDtos.isEmpty()) {
            LOGGER.info("No follow-up data or templates found for follow-up plan creation");
            return;
        }

        // Extract waiting periods and template associations
        List<Integer> waitingPeriods = new ArrayList<>();
        List<Integer> associatedTemplates = new ArrayList<>();

        for (List<Object> row : followUpsValueRange.getValues()) {
            if (row.size() < 2 || row.get(0) == null || row.get(1) == null || row.get(1).toString().isEmpty()) {
                continue;
            }

            try {
                Integer waitingPeriod = Integer.parseInt(row.get(1).toString());
                waitingPeriods.add(waitingPeriod);

                // Associate template if available
                if (row.size() > 2 && row.get(2) != null && !row.get(2).toString().isEmpty()) {
                    Integer templateNum = Integer.parseInt(row.get(2).toString());
                    associatedTemplates.add(templateNum);
                }
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid number in follow-up plan: " + row.get(1));
            }
        }

        if (waitingPeriods.isEmpty()) {
            LOGGER.warning("No valid waiting periods found for follow-up plan");
            return;
        }

        // Create a map for template lookup
        Map<Integer, TemplateSheetDto> templateMap = templateSheetDtos.stream()
                .collect(Collectors.toMap(TemplateSheetDto::number, Function.identity(), (a, b) -> a));

        // Create follow-up steps metadata
        List<FollowUpStepMetadata> stepsMetadata = createFollowUpStepsMetadata(associatedTemplates, templateMap);

        // Create and save the follow-up plan
        if (!stepsMetadata.isEmpty()) {
            createFollowUpPlan(waitingPeriods, stepsMetadata);
        }
    }

    /**
     * Creates follow-up steps metadata from template associations.
     *
     * @param associatedTemplates Template numbers associated with follow-up steps
     * @param templateMap Map of template numbers to template data objects
     * @return List of follow-up step metadata objects
     */
    private List<FollowUpStepMetadata> createFollowUpStepsMetadata(
            List<Integer> associatedTemplates,
            Map<Integer, TemplateSheetDto> templateMap) {

        List<FollowUpStepMetadata> stepsMetadata = new ArrayList<>();

        for (Integer templateNum : associatedTemplates) {
            TemplateSheetDto templateDto = templateMap.get(templateNum);
            if (templateDto == null || templateDto.draftId() == null || templateDto.draftId().isEmpty()) {
                LOGGER.warning("No template found for number: " + templateNum);
                continue;
            }

            Optional<EntityData<Template, TemplateMetadata>> templateOpt =
                    templateRepository.findByDraftId(templateDto.draftId());

            if (templateOpt.isPresent()) {
                Template template = templateOpt.get().entity();
                FollowUpStepMetadata metadata = new FollowUpStepMetadata(EntityId.of(1L), template.getId());
                stepsMetadata.add(metadata);
            } else {
                LOGGER.warning("Template not found in repository for draft ID: " + templateDto.draftId());
            }
        }

        return stepsMetadata;
    }

    /**
     * Creates and saves a follow-up plan with the specified waiting periods and step metadata.
     *
     * @param waitingPeriods Waiting periods between follow-up steps
     * @param stepsMetadata Metadata for follow-up steps
     */
    private void createFollowUpPlan(List<Integer> waitingPeriods, List<FollowUpStepMetadata> stepsMetadata) {
        try {
            // Create the plan with waiting periods
            FollowUpPlan plan = FollowUpPlanFactory.createDefaultPlan(waitingPeriods);

            // Save the plan with step associations
            FollowUpPlan updatedPlan = followUpManagementService.createPlanWithSteps(plan, stepsMetadata);
            LOGGER.info("Created follow-up plan with " + updatedPlan.getSteps().size() + " steps");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create follow-up plan", e);
        }
    }

    /**
     * Synchronizes new placeholders found in templates back to the configuration sheet.
     *
     * @param spreadsheetId ID of the spreadsheet
     * @param placeholderManager Placeholder manager with current placeholders
     */
    private void syncNewPlaceholdersToSheet(String spreadsheetId, PlaceholderManager placeholderManager) {
        // Get all templates from repository
        List<EntityData<Template, TemplateMetadata>> templates = templateRepository.findAllWithMetadata();

        // Extract all placeholders from all templates
        Set<String> allPlaceholders = extractAllPlaceholdersFromTemplates(templates);
        if (allPlaceholders.isEmpty()) {
            return;
        }

        // Filter out placeholders already in the manager
        Set<String> newPlaceholders = allPlaceholders.stream()
                .filter(placeholder -> !placeholderManager.getPlaceholders().containsKey(placeholder))
                .collect(Collectors.toSet());

        if (newPlaceholders.isEmpty()) {
            return;
        }

        // Prepare data for sheet update
        List<SpreadsheetReference> references = new ArrayList<>();
        List<List<Object>> cellValues = new ArrayList<>();

        int currentRow = PLACEHOLDER_START_ROW + placeholderManager.getPlaceholders().size();

        for (String placeholder : newPlaceholders) {
            references.add(SpreadsheetReference.ofRange(
                    CONFIG_SHEET_NAME,
                    "E" + currentRow + ":" + "F" + currentRow)
            );
            cellValues.add(List.of(placeholder, ""));
            currentRow++;
        }

        // Write new placeholders to sheet
        if (!cellValues.isEmpty()) {
            try {
                spreadsheetGateway.writeData(spreadsheetId, references, cellValues);
                LOGGER.info("Added " + newPlaceholders.size() + " new placeholders to configuration sheet");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to write new placeholders to sheet", e);
                throw new RuntimeException("Failed to write placeholders to sheet", e);
            }
        }
    }

    /**
     * Extracts all placeholders from all templates in the repository.
     *
     * @param templates List of templates with metadata
     * @return Set of all placeholder keys found in templates
     */
    private Set<String> extractAllPlaceholdersFromTemplates(List<EntityData<Template, TemplateMetadata>> templates) {
        Set<String> allPlaceholders = new HashSet<>();
        for (EntityData<Template, TemplateMetadata> templateData : templates) {
            Template template = templateData.entity();
            if (template != null) {
                allPlaceholders.addAll(template.getPlaceholdersInBody());
            }
        }
        return allPlaceholders;
    }

    /**
     * Updates all templates with the current delimiters and placeholder definitions.
     *
     * @param placeholderManager Placeholder manager with current placeholders
     * @param delimiters Array of delimiter characters
     */
    private void updateTemplatesWithDelimitersAndPlaceholders(
            PlaceholderManager placeholderManager,
            Character[] delimiters) {

        // Use default delimiters if none provided
        char[] delimiterChars;
        if (delimiters == null || delimiters.length < 2 || delimiters[0] == null || delimiters[1] == null) {
            delimiterChars = DEFAULT_DELIMITERS;
        } else {
            delimiterChars = new char[]{delimiters[0], delimiters[1]};
        }

        // Get all templates from repository
        List<EntityData<Template, TemplateMetadata>> templates = templateRepository.findAllWithMetadata();
        int updatedCount = 0;

        for (EntityData<Template, TemplateMetadata> templateData : templates) {
            Template template = templateData.entity();
            TemplateMetadata metadata = templateData.metadata();

            if (template == null || metadata == null) {
                continue;
            }

            try {
                // Create updated template with new placeholder manager
                Template updatedTemplate = updateTemplateWithPlaceholders(
                        template, placeholderManager, delimiterChars);

                // Save updated template
                templateRepository.saveWithMetadata(updatedTemplate, metadata);
                updatedCount++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to update template: " + template.getId(), e);
            }
        }

        LOGGER.info("Updated " + updatedCount + " templates with delimiters and placeholders");
    }

    /**
     * Creates an updated template with new placeholder manager.
     *
     * @param template Original template
     * @param placeholderManager Placeholder manager with current placeholders
     * @param delimiterChars Array of delimiter characters
     * @return Updated template
     */
    private Template updateTemplateWithPlaceholders(
            Template template,
            PlaceholderManager placeholderManager,
            char[] delimiterChars) {

        // Create a deep copy of the placeholder manager for this template
        PlaceholderManager templatePlaceholderManager = new PlaceholderManager(
                delimiterChars,
                new HashMap<>(placeholderManager.getPlaceholders())
        );

        // Update template with new placeholder manager
        return new Template.Builder()
                .from(template)
                .setPlaceholderManager(templatePlaceholderManager)
                .build();
    }

    /**
     * Saves the application configuration to the repository.
     *
     * @param spreadsheetId ID of the spreadsheet
     * @param appConfig Application configuration data
     * @param columnMappings List of column mappings
     */
    private void saveApplicationConfiguration(
            String spreadsheetId,
            ApplicationConfiguration appConfig,
            List<ColumnMapping> columnMappings) {

        if (appConfig == null) {
            LOGGER.warning("Cannot save null application configuration");
            return;
        }

        // Create updated configuration with spreadsheet ID and column mappings
        ApplicationConfiguration updatedConfig = new ApplicationConfiguration.Builder()
                .from(appConfig)
                .spreadsheetId(spreadsheetId)
                .columnMappings(columnMappings)
                .build();

        // Check if configuration already exists
        Optional<ApplicationConfiguration> existingConfigOpt = configurationRepository.findBySpreadsheetId(spreadsheetId);

        if (existingConfigOpt.isPresent()) {
            if (!existingConfigOpt.get().equals(updatedConfig)) {
                // Update existing configuration
                updatedConfig = new ApplicationConfiguration.Builder()
                        .from(existingConfigOpt.get())
                        .merge(updatedConfig)
                        .build();
                LOGGER.info("Updating existing application configuration for spreadsheet: " + spreadsheetId);

                configurationRepository.save(updatedConfig);
            }

        } else {
            LOGGER.info("Creating new application configuration for spreadsheet: " + spreadsheetId);
            // Save to repository
            try {
                configurationRepository.save(updatedConfig);
                LOGGER.info("Saved application configuration with " + columnMappings.size() + " column mappings");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to save application configuration", e);
            }
        }

    }

    /**
     * Template data from the configuration sheet.
     */
    private record TemplateSheetDto(Integer number, String subject, String draftId) {
        @Override
        public String toString() {
            return "Template #" + number + ": " + subject + " (Draft ID: " + draftId + ")";
        }
    }

    /**
     * References for configuration sheet sections.
     */
    private static class ConfigReferences {
        private static final String CONFIG_SHEET_NAME = "Configuration";

        public static final Map<ConfigTyp, SpreadsheetReference> references = Map.of(
                ConfigTyp.SETTINGS, SpreadsheetReference.ofRange(CONFIG_SHEET_NAME, "A4:B6"),
                ConfigTyp.TEMPLATES, SpreadsheetReference.ofRange(CONFIG_SHEET_NAME, "A18:C40"),
                ConfigTyp.FOLLOW_UPS, SpreadsheetReference.ofRange(CONFIG_SHEET_NAME, "F4:H12"),
                ConfigTyp.CONTACT_COLUMNS, SpreadsheetReference.ofRange(CONFIG_SHEET_NAME, "K4:L6"),
                ConfigTyp.RECIPIENT_COLUMNS, SpreadsheetReference.ofRange(CONFIG_SHEET_NAME, "K11:L14"),
                ConfigTyp.EMAIL_COLUMNS, SpreadsheetReference.ofRange(CONFIG_SHEET_NAME, "K19:L36"),
                ConfigTyp.DELIMITERS, SpreadsheetReference.ofCell(CONFIG_SHEET_NAME, "B7"),
                ConfigTyp.PLACEHOLDERS, SpreadsheetReference.ofRange(CONFIG_SHEET_NAME, "E18:F40")
        );

        public static List<SpreadsheetReference> getSpreadsheetReferences() {
            return new ArrayList<>(references.values());
        }

        public enum ConfigTyp {
            SETTINGS,
            TEMPLATES,
            FOLLOW_UPS,
            CONTACT_COLUMNS,
            RECIPIENT_COLUMNS,
            EMAIL_COLUMNS,
            DELIMITERS,
            PLACEHOLDERS
        }
    }
}