package com.mailscheduler.domain.model.template.placeholder;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates placeholder keys according to system requirements.
 * <p>
 *     This class ensures placeholder keys meet length and character restrictions to prevent injection attacks
 *     and ensure compatibility with the template system.
 * </p>
 */
public class PlaceholderValidator {
    /**
     * Maximum length allowed for placeholder keys.
     */
    private static final int MAX_KEY_LENGTH = 50;

    /**
     * Pattern defining valid characters for placeholder keys.
     * Only alphanumeric characters, underscores, and hyphens are allowed.
     */
    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * Validates a placeholder key against system requirements.
     *
     * @param key The placeholder key to validate
     * @throws PlaceholderException if the key is invalid
     */
    public void validateKey(String key) throws PlaceholderException {
        if (key == null || key.trim().isEmpty()) {
            throw new PlaceholderException("Placeholder key cannot be null or empty",
                    PlaceholderException.ErrorCode.INVALID_KEY
            );
        }

        String trimmedKey = key.trim();

        if (!key.equals(trimmedKey)) {
            throw new PlaceholderException(
                    "Placeholder key cannot contain leading or trailing whitespace",
                    PlaceholderException.ErrorCode.INVALID_KEY,
                    Map.of("key", key)
            );
        }

        if (key.length() > MAX_KEY_LENGTH) {
            throw new PlaceholderException(
                    "Placeholder key exceeds maximum length of " + MAX_KEY_LENGTH + " characters",
                    PlaceholderException.ErrorCode.INVALID_KEY,
                    Map.of("key", key, "length", key.length(), "maxLength", MAX_KEY_LENGTH)
            );
        }

        if (!VALID_KEY_PATTERN.matcher(key).matches()) {
            throw new PlaceholderException(
                    "Placeholder key contains invalid characters. Only alphanumeric, underscore, and hyphen are allowed.",
                    PlaceholderException.ErrorCode.INVALID_KEY,
                    Map.of("key", key, "pattern", VALID_KEY_PATTERN.pattern())
            );
        }
    }

    /**
     * Validates a placeholder value for the given type.
     *
     * @param value The value to validate
     * @param type The expected type of the value
     * @throws PlaceholderException if the value is invalid for the specified type
     */
    public void validateValue(Object value, ValueType type) throws PlaceholderException {
        if (value == null) {
            throw new PlaceholderException(
                    "Placeholder value cannot be null",
                    PlaceholderException.ErrorCode.INVALID_VALUE,
                    Map.of("type", type)
            );
        }

        switch (type) {
            case STRING -> {
                if (!(value instanceof String)) {
                    throw new PlaceholderException(
                            "Placeholder value must be a String for type STRING",
                            PlaceholderException.ErrorCode.TYPE_MISMATCH,
                            Map.of("type", type, "actualType", value.getClass().getName())
                    );
                }
            }
            case SPREADSHEET_REFERENCE -> {
                // Validation for spreadsheet references would go here
                // For now, we're just checking it's not a primitive type
                if (value.getClass().isPrimitive()) {
                    throw new PlaceholderException(
                            "Placeholder value cannot be a primitive type for SPREADSHEET_REFERENCE",
                            PlaceholderException.ErrorCode.TYPE_MISMATCH,
                            Map.of("type", type, "actualType", value.getClass().getName())
                    );
                }
            }
        }
    }
}