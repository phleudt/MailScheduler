package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.common.config.ApplicationConfiguration;

import java.util.Optional;

/**
 * Repository interface for managing ApplicationConfiguration entities.
 * <p>
 *     The configuration repository handles system-wide settings including API connections,
 *     scheduling parameters, and data source references.
 * </p>
 */
public interface ConfigurationRepository extends Repository<ApplicationConfiguration, NoMetadata> {
    /**
     * Returns the current application configuration.
     * There should be only one active configuration at a time.
     */
    ApplicationConfiguration getActiveConfiguration();

    ApplicationConfiguration save(ApplicationConfiguration configuration);

    /**
     * Finds a configuration by its associated Google Spreadsheet ID.
     *
     * @param spreadsheetId The ID of the Google Spreadsheet
     * @return An Optional containing the configuration if found, empty otherwise
     */
    Optional<ApplicationConfiguration> findBySpreadsheetId(String spreadsheetId);
}
