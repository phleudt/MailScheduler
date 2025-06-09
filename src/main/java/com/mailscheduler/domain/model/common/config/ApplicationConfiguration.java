package com.mailscheduler.domain.model.common.config;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;

import java.util.*;

/**
 * Entity representing the application's configuration settings.
 * <p>
 *     This entity stores all settings needed for the mail scheduler application to function,
 *     including spreadsheet integration settings, email configuration, and column mappings.
 *     It follows the builder pattern for flexible construction.
 * </p>
 */
public class ApplicationConfiguration extends IdentifiableEntity<ApplicationConfiguration> {
    private final String spreadsheetId;
    private final EmailAddress senderEmailAddress;
    private final Boolean saveMode;
    private final SpreadsheetReference sendingCriteriaColumn;
    private final List<ColumnMapping> columnMappings;

    /**
     * Private constructor used by the Builder.
     *
     * @param builder The builder containing configuration values
     */
    private ApplicationConfiguration(Builder builder) {
        setId(builder.id);
        this.spreadsheetId = builder.spreadsheetId;
        this.senderEmailAddress = builder.senderEmailAddress;
        this.saveMode = builder.saveMode;
        this.sendingCriteriaColumn = builder.sendingCriteriaColumn;
        this.columnMappings = builder.columnMappings != null ?
                List.copyOf(builder.columnMappings) : Collections.emptyList();
    }

    /**
     * Gets the Google Spreadsheet ID for data synchronization.
     *
     * @return The spreadsheet ID
     */
    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    /**
     * Gets the email address used for sending emails.
     *
     * @return The sender email address
     */
    public EmailAddress getSenderEmailAddress() {
        return senderEmailAddress;
    }

    /**
     * Gets the save mode setting.
     * <p>
     *     When true, emails are saved rather than sent (testing mode).
     *     When false, emails are actually sent to recipients.
     * </p>
     *
     * @return The save mode setting
     */
    public Boolean getSaveMode() {
        return saveMode;
    }

    /**
     * Gets the spreadsheet column used to determine which rows should have emails sent.
     *
     * @return The sending criteria column reference
     */
    public SpreadsheetReference getSendingCriteriaColumn() {
        return sendingCriteriaColumn;
    }

    /**
     * Gets all column mappings defined in the configuration.
     *
     * @return An unmodifiable list of column mappings
     */
    public List<ColumnMapping> getColumnMappings() {
        return columnMappings;
    }

    /**
     * Gets column mappings filtered by a specific mapping type.
     *
     * @param mappingType The mapping type to filter by
     * @return A list of column mappings of the specified type
     */
    public List<ColumnMapping> getColumnMappings(ColumnMapping.MappingType mappingType) {
        Objects.requireNonNull(mappingType, "Mapping type cannot be null");

        return columnMappings.stream()
                .filter(mapping -> mappingType.equals(mapping.type()))
                .toList();
    }

    /**
     * Groups column mappings by their type.
     *
     * @return A map of mapping types to lists of corresponding column mappings
     */
    public Map<ColumnMapping.MappingType, List<ColumnMapping>> groupColumnMappingsByType() {
        Map<ColumnMapping.MappingType, List<ColumnMapping>> groupedMappings = new HashMap<>();

        // Initialize with empty lists for all types
        for (ColumnMapping.MappingType mappingType : ColumnMapping.MappingType.values()) {
            groupedMappings.put(mappingType, new ArrayList<>());
        }

        // Add each mapping to its corresponding type list
        for (ColumnMapping columnMapping : columnMappings) {
            groupedMappings.put(columnMapping.type(), columnMappings);
        }

        return groupedMappings;
    }

    /**
     * Checks if the configuration has all required fields for operation.
     *
     * @return true if the configuration is complete, false otherwise
     */
    public boolean isComplete() {
        return spreadsheetId != null && !spreadsheetId.isBlank() &&
                senderEmailAddress != null &&
                saveMode != null &&
                sendingCriteriaColumn != null &&
                !columnMappings.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationConfiguration that)) return false;

        if (!Objects.equals(spreadsheetId, that.spreadsheetId)) return false;
        if (!Objects.equals(senderEmailAddress, that.senderEmailAddress)) return false;
        if (saveMode != that.saveMode) return false;
        if (!Objects.equals(sendingCriteriaColumn, that.sendingCriteriaColumn)) return false;
        return Objects.equals(columnMappings, that.columnMappings);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (spreadsheetId != null ? spreadsheetId.hashCode() : 0);
        result = 31 * result + (senderEmailAddress != null ? senderEmailAddress.hashCode() : 0);
        result = 31 * result + (saveMode != null ? saveMode.hashCode() : 0);
        result = 31 * result + (sendingCriteriaColumn != null ? sendingCriteriaColumn.hashCode() : 0);
        result = 31 * result + (columnMappings != null ? columnMappings.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ApplicationConfiguration{" +
                "id=" + getId() +
                ", spreadsheetId='" + spreadsheetId + '\'' +
                ", senderEmailAddress=" + senderEmailAddress +
                ", saveMode=" + saveMode +
                ", sendingCriteriaColumn=" + sendingCriteriaColumn +
                ", columnMappings=" + columnMappings.size() +
                '}';
    }

    /**
     * Builder for creating ApplicationConfiguration instances.
     */
    public static class Builder {
        private EntityId<ApplicationConfiguration> id;
        private String spreadsheetId;
        private EmailAddress senderEmailAddress;
        private Boolean saveMode;
        private SpreadsheetReference sendingCriteriaColumn;
        private List<ColumnMapping> columnMappings = new ArrayList<>();

        public Builder setId(EntityId<ApplicationConfiguration> id) {
            this.id = id;
            return this;
        }

        public Builder spreadsheetId(String spreadsheetId) {
            this.spreadsheetId = spreadsheetId;
            return this;
        }

        public Builder senderEmailAddress(EmailAddress senderEmailAddress) {
            this.senderEmailAddress = senderEmailAddress;
            return this;
        }

        public Builder saveMode(Boolean saveMode) {
            this.saveMode = saveMode;
            return this;
        }

        public Builder sendingCriteriaColumn(SpreadsheetReference sendingCriteriaColumn) {
            this.sendingCriteriaColumn = sendingCriteriaColumn;
            return this;
        }

        public Builder columnMappings(List<ColumnMapping> columnMappings) {
            this.columnMappings = columnMappings;
            return this;
        }

        public ApplicationConfiguration build() {
            return new ApplicationConfiguration(this);
        }

        /**
         * Creates a new builder with values copied from an existing configuration.
         *
         * @param config The configuration to copy from
         * @return A new builder with copied values
         */
        public Builder from(ApplicationConfiguration config) {
            return new Builder()
                    .setId(config.getId())
                    .spreadsheetId(config.getSpreadsheetId())
                    .senderEmailAddress(config.getSenderEmailAddress())
                    .saveMode(config.getSaveMode())
                    .sendingCriteriaColumn(config.getSendingCriteriaColumn())
                    .columnMappings(config.getColumnMappings());
        }

        /**
         * Merges the fields of the given ApplicationConfiguration into this builder.
         * Non-null fields from the provided config will overwrite current builder values.
         *
         * @param config The configuration to merge from
         * @return This builder with merged values
         */
        public Builder merge(ApplicationConfiguration config) {
            if (config.getSpreadsheetId() != null) {
                this.spreadsheetId = config.getSpreadsheetId();
            }
            if (config.getSenderEmailAddress() != null) {
                this.senderEmailAddress = config.getSenderEmailAddress();
            }
            if (config.getSaveMode() != null) {
                this.saveMode = config.getSaveMode();
            }
            if (config.getSendingCriteriaColumn() != null) {
                this.sendingCriteriaColumn = config.getSendingCriteriaColumn();
            }
            if (config.getColumnMappings() != null) {
                this.columnMappings = config.getColumnMappings();
            }
            return this;
        }
    }

}
