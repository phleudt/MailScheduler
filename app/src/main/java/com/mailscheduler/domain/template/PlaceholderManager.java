package com.mailscheduler.domain.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.common.exception.PlaceholderException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderManager {
    // Validation constants
    private static final int MAX_KEY_LENGTH = 50;
    private static final int MAX_STRING_VALUE_LENGTH = 500;
    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final Map<String, PlaceholderValue> placeholders;
    private final char[] delimiters;

    // Constructor with default delimiters
    public PlaceholderManager() {
        this.placeholders = new HashMap<>();
        delimiters = new char[] {'{', '}'};
    }

    // Constructor with custom delimiters
    public PlaceholderManager(char[] delimiters) {
        if (delimiters == null || delimiters.length != 2) {
            throw new IllegalArgumentException("Delimiters must be an array of exactly 2 characters");
        }
        this.delimiters = delimiters.clone();
        this.placeholders = new HashMap<>();
    }

    /**
     * Represents a placeholder value which can be either a string or a spreadsheet reference
     */
    public record PlaceholderValue(ValueType type, Object value) {
        @JsonCreator
        public PlaceholderValue(@JsonProperty("type") ValueType type, @JsonProperty("value") Object value) {
            this.type = type;
            this.value = value;
        }

        public String getStringValue() {
            if (type != ValueType.STRING) {
                throw new IllegalStateException("Value is not a string");
            }
            return (String) value;
        }

        public SpreadsheetReference getSpreadsheetReference() {
            if (type != ValueType.SPREADSHEET_REFERENCE) {
                throw new IllegalStateException("Value is not a spreadsheet reference");
            }
            return (SpreadsheetReference) value;
        }
    }

    /**
     * Enum to define types of placeholder values
     */
    public enum ValueType {
        STRING,
        SPREADSHEET_REFERENCE
    }

    /**
     * Add a string placeholder
     * @param key Unique placeholder key
     * @param value String value for the placeholder
     * @throws PlaceholderException if validation fails
     */
    public void addStringPlaceholder(String key, String value) throws PlaceholderException {
        validateKey(key);
        validateStringValue(value);

        if (placeholders.containsKey(key)) {
            throw new PlaceholderException("Placeholder key already exists: " + key);
        }

        placeholders.put(key, new PlaceholderValue(ValueType.STRING, value));
    }

    /**
     * Add a spreadsheet reference placeholder
     * @param key Unique placeholder key
     * @param reference SpreadsheetReference for the placeholder
     * @throws PlaceholderException if validation fails
     */
    public void addSpreadsheetPlaceholder(String key, SpreadsheetReference reference) throws PlaceholderException {
        validateKey(key);

        if (placeholders.containsKey(key)) {
            throw new PlaceholderException("Placeholder key already exists: " + key);
        }

        placeholders.put(key, new PlaceholderValue(ValueType.SPREADSHEET_REFERENCE, reference));
    }

    /**
     * Update an existing placeholder value
     * @param key Placeholder key to update
     * @param value New value for the placeholder
     * @throws PlaceholderException if key doesn't exist or validation fails
     */
    public void updatePlaceholder(String key, Object value) throws PlaceholderException {
        if (!placeholders.containsKey(key)) {
            throw new PlaceholderException("Placeholder key does not exist: " + key);
        }

        if (value instanceof String) {
            validateStringValue((String) value);
            placeholders.put(key, new PlaceholderValue(ValueType.STRING, value));
        } else if (value instanceof SpreadsheetReference) {
            placeholders.put(key, new PlaceholderValue(ValueType.SPREADSHEET_REFERENCE, value));
        } else {
            throw new PlaceholderException("Unsupported placeholder value type");
        }
    }

    /**
     * Remove a placeholder by key
     * @param key Placeholder key to remove
     * @throws PlaceholderException if key doesn't exist
     */
    public void removePlaceholder(String key) throws PlaceholderException {
        if (!placeholders.containsKey(key)) {
            throw new PlaceholderException("Placeholder key does not exist: " + key);
        }
        placeholders.remove(key);
    }

    /**
     * Get a placeholder value
     * @param key Placeholder key to retrieve
     * @return PlaceholderValue for the given key
     * @throws PlaceholderException if key doesn't exist
     */
    public PlaceholderValue getPlaceholder(String key) throws PlaceholderException {
        PlaceholderValue value = placeholders.get(key);
        if (value == null) {
            throw new PlaceholderException("Placeholder key does not exist: " + key);
        }
        return value;
    }

    public Map<String, PlaceholderValue> getAllPlaceholders() {
        return placeholders;
    }

    public char[] getDelimiters() {
        return delimiters;
    }

    public Pattern createPlaceholderPattern() {
        return Pattern.compile(
                Pattern.quote(String.valueOf(delimiters[0])) +
                        "(.*?)" +
                        Pattern.quote(String.valueOf(delimiters[1]))
        );
    }

    public String wrapWithDelimiters(String key) {
        return delimiters[0] + key + delimiters[1];
    }

    public Set<String> getAllPlaceholderKeys() {
        return placeholders.keySet();
    }

    /**
     * Validate placeholder key
     * @param key Key to validate
     * @throws PlaceholderException if key is invalid
     */
    private void validateKey(String key) throws PlaceholderException {
        if (key == null || key.trim().isEmpty()) {
            throw new PlaceholderException("Placeholder key cannot be null or empty", PlaceholderException.ErrorCode.INVALID_KEY);
        }

        if (key.length() > MAX_KEY_LENGTH) {
            throw new PlaceholderException("Placeholder key exceeds maximum length of " + MAX_KEY_LENGTH, PlaceholderException.ErrorCode.INVALID_KEY);
        }

        if (!VALID_KEY_PATTERN.matcher(key).matches()) {
            throw new PlaceholderException("Placeholder key contains invalid characters. Only alphanumeric, underscore, and hyphen are allowed.", PlaceholderException.ErrorCode.INVALID_KEY);
        }
    }

    /**
     * Validate string placeholder value
     * @param value String value to validate
     * @throws PlaceholderException if value is invalid
     */
    private void validateStringValue(String value) throws PlaceholderException {
        if (value == null) {
            throw new PlaceholderException("Placeholder string value cannot be null", PlaceholderException.ErrorCode.INVALID_VALUE);
        }

        if (value.trim().isEmpty()) {
            throw new PlaceholderException("Placeholder string value cannot be empty", PlaceholderException.ErrorCode.INVALID_VALUE);
        }

        if (value.length() > MAX_STRING_VALUE_LENGTH) {
            throw new PlaceholderException("Placeholder string value exceeds maximum length of " + MAX_STRING_VALUE_LENGTH, PlaceholderException.ErrorCode.INVALID_VALUE);
        }
    }

    public String replacePlaceholders(String template) throws PlaceholderException {
        Pattern pattern = createPlaceholderPattern();
        Matcher matcher = pattern.matcher(template);
        StringBuilder result = new StringBuilder();

        matcher.reset();
        while (matcher.find()) {
            String key = matcher.group(1);
            PlaceholderManager.PlaceholderValue replacement = getPlaceholder(key);
            matcher.appendReplacement(result, replacement.value().toString());
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Check if a placeholder exists
     * @param key Key to check
     * @return true if placeholder exists, false otherwise
     */
    public boolean hasPlaceholder(String key) {
        return placeholders.containsKey(key);
    }

    /**
     * Get the number of placeholders
     * @return Total number of placeholders
     */
    public int size() {
        return placeholders.size();
    }

    /**
     * Clear all placeholders
     */
    public void clear() {
        placeholders.clear();
    }

    public boolean isEmpty() {
        return placeholders.isEmpty();
    }
}
