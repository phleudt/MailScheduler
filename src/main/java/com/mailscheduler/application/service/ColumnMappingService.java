package com.mailscheduler.application.service;

import com.mailscheduler.domain.model.common.config.ApplicationConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.repository.ConfigurationRepository;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for retrieving column mappings and converting them to spreadsheet references.
 * Provides a clean interface for sync strategies to access column mappings from the database.
 */
public class ColumnMappingService {
    private static final Logger LOGGER = Logger.getLogger(ColumnMappingService.class.getName());
    private final ConfigurationRepository configurationRepository;

    // Default column mappings as fallback when database configuration is unavailable
    private static final Map<ColumnMapping.MappingType, Map<String, String>> DEFAULT_MAPPINGS = Map.of(
            ColumnMapping.MappingType.CONTACT, Map.of(
                    "website", "D",
                    "name", "E",
                    "phoneNumber", "I"
            ),
            ColumnMapping.MappingType.RECIPIENT, Map.of(
                    "emailAddress", "H",
                    "salutation", "G",
                    "hasReplied", "M",
                    "initialContactDate", "J"
            ),
            ColumnMapping.MappingType.EMAIL, Map.of(
                    "emailAddress", "H",
                    "initialContactDate", "J",
                    "followUp1Date", "O",
                    "followUp1State", "P",
                    "followUp2Date", "Q",
                    "followUp2State", "R",
                    "followUp3Date", "S",
                    "followUp3State", "T"
            )
    );

    public ColumnMappingService(ConfigurationRepository configurationRepository) {
        this.configurationRepository = Objects.requireNonNull(configurationRepository,
                "ConfigurationRepository cannot be null");
    }

    /**
     * Gets spreadsheet references for a specific entity type.
     * Retrieves mappings from the database configuration or falls back to defaults.
     *
     * @param mappingType The type of entity mapping to retrieve
     * @param sheetTitle The title of the sheet to create references for
     * @return List of spreadsheet references
     */
    public List<SpreadsheetReference> getSpreadsheetReferences(
            ColumnMapping.MappingType mappingType, String sheetTitle) {

        // Get active configuration
        ApplicationConfiguration config = configurationRepository.getActiveConfiguration();

        // If configuration exists, use its column mappings
        if (config != null && config.getColumnMappings() != null && !config.getColumnMappings().isEmpty()) {
            return getReferencesFromConfiguration(config, mappingType, sheetTitle);
        } else {
            // Fall back to default mappings
            LOGGER.info("No configuration found in database. Using default column mappings for " + mappingType);
            return getDefaultReferences(mappingType, sheetTitle);
        }
    }

    /**
     * Retrieves column mappings from the database configuration.
     */
    private List<SpreadsheetReference> getReferencesFromConfiguration(
            ApplicationConfiguration config, ColumnMapping.MappingType mappingType, String sheetTitle) {

        List<SpreadsheetReference> references = new ArrayList<>();

        // Filter mappings by type and convert to SpreadsheetReference objects
        List<ColumnMapping> typedMappings = config.getColumnMappings().stream()
                .filter(mapping -> mapping.type() == mappingType)
                .collect(Collectors.toList());

        if (typedMappings.isEmpty()) {
            LOGGER.warning("No column mappings found for type: " + mappingType +
                    ". Falling back to defaults.");
            return getDefaultReferences(mappingType, sheetTitle);
        }

        // Convert each mapping to a SpreadsheetReference with the provided sheet title
        for (ColumnMapping mapping : typedMappings) {
            SpreadsheetReference reference = SpreadsheetReference.ofColumn(
                    sheetTitle,
                    extractColumnLetter(mapping.columnReference().getGoogleSheetsReference())
            );
            references.add(reference);

            LOGGER.fine("Using database mapping: " + mapping.columnName() +
                    " -> " + reference.getGoogleSheetsReference());
        }

        return references;
    }

    /**
     * Creates default references as a fallback when database configuration is unavailable.
     */
    private List<SpreadsheetReference> getDefaultReferences(
            ColumnMapping.MappingType mappingType, String sheetTitle) {

        List<SpreadsheetReference> references = new ArrayList<>();
        Map<String, String> typeDefaults = DEFAULT_MAPPINGS.get(mappingType);

        if (typeDefaults == null) {
            LOGGER.warning("No default mappings defined for type: " + mappingType);
            return Collections.emptyList();
        }

        for (Map.Entry<String, String> entry : typeDefaults.entrySet()) {
            references.add(SpreadsheetReference.ofColumn(sheetTitle, entry.getValue()));
            LOGGER.fine("Using default mapping: " + entry.getKey() +
                    " -> " + entry.getValue() + " for sheet " + sheetTitle);
        }

        return references;
    }

    /**
     * Extracts just the column letter from a Google Sheets reference.
     */
    private String extractColumnLetter(String reference) {
        // Extract column letter (e.g., from "Sheet1!A:A" get "A")
        if (reference != null && reference.contains("!")) {
            String[] parts = reference.split("!");
            if (parts.length > 1 && parts[1].contains(":")) {
                return parts[1].split(":")[0].replaceAll("[0-9]", "");
            } else if (parts.length > 1) {
                return parts[1].replaceAll("[0-9]", "");
            }
        }
        return reference;
    }
}