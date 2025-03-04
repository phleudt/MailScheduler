package com.mailscheduler.common.config;

import com.mailscheduler.common.exception.validation.CriterionValidationException;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Default implementation of SendingCriteria
 */
public class SendingCriterion {
    private static final Logger logger = LoggerFactory.getLogger(SendingCriterion.class);

    private final CriterionType type;
    private final SpreadsheetReference targetColumn;
    private final String expectedValue;
    private final Pattern pattern;

    /**
     * Private constructor to enforce builder usage
     */
    private SendingCriterion(
            CriterionType type,
            SpreadsheetReference targetColumn,
            String expectedValue,
            Pattern pattern
    ) {
        this.type = type;
        this.targetColumn = targetColumn;
        this.expectedValue = expectedValue;
        this.pattern = pattern;
    }

    public void validate(Map<String, SpreadsheetReference> columnMappings) throws CriterionValidationException {
        if (targetColumn == null) {
            throw new CriterionValidationException("Target column must be specified");
        }

        // Check if the target column exists in the provided column mappings
        boolean columnExists = columnMappings.values().stream()
                .anyMatch(ref -> ref.equals(targetColumn));

        if (!columnExists) {
            throw new CriterionValidationException("Target column " + targetColumn + " not found in column mappings");
        }

        // Additional type-specific validations
        switch (type) {
            case COLUMN_VALUE_MATCH:
                if (expectedValue == null || expectedValue.isEmpty()) {
                    throw new CriterionValidationException("Expected value must be specified for value match");
                }
                break;
            case COLUMN_PATTERN_MATCH:
                if (pattern == null) {
                    throw new CriterionValidationException("Regex pattern must be specified for pattern match");
                }
                break;
        }
    }

    public boolean meetsCriterion(Map<String, String> rowData) {
        if (rowData == null) {
            logger.warn("Row data is null. Skipping criteria evaluation.");
            return false;
        }

        String columnValue = rowData.get(targetColumn.getReference());

        try {
            return switch (type) {
                case COLUMN_FILLED -> columnValue != null && !columnValue.trim().isEmpty();
                case COLUMN_VALUE_MATCH -> Objects.equals(columnValue, expectedValue);
                case COLUMN_PATTERN_MATCH -> pattern != null && pattern.matcher(columnValue).matches();
                case STATUS_CHECK ->
                    // Add specific status checking logic
                        columnValue != null &&
                                (columnValue.equalsIgnoreCase("active") ||
                                        columnValue.equalsIgnoreCase("pending"));
                default -> {
                    logger.warn("Unsupported criteria type: {}", type);
                    yield false;
                }
            };
        } catch (Exception e) {
            logger.error("Error evaluating sending criteria", e);
            return false;
        }
    }

    public CriterionType getType() {
        return type;
    }

    public SpreadsheetReference getTargetColumn() {
        return targetColumn;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Builder implementation for DefaultSendingCriteria
     */
    public static class Builder {
        private CriterionType type;
        private SpreadsheetReference targetColumn;
        private String expectedValue;
        private Pattern pattern;

        public Builder withType(CriterionType type) {
            this.type = type;
            return this;
        }

        public Builder withTargetColumn(SpreadsheetReference column) {
            if (SpreadsheetReference.ReferenceType.COLUMN.equals(column.getType())) {
                this.targetColumn = column;
                return this;
            }
            throw new IllegalArgumentException("Invalid spreadsheetReference type");
        }

        public Builder withExpectedValue(String value) {
            this.expectedValue = value;
            return this;
        }

        public Builder withPattern(String regex) {
            this.pattern = Pattern.compile(regex);
            return this;
        }

        public SendingCriterion build() throws CriterionValidationException {
            if (type == null) {
                throw new CriterionValidationException("Criteria type must be specified");
            }
            return new SendingCriterion(type, targetColumn, expectedValue, pattern);
        }
    }
}