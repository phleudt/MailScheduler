package com.mailscheduler.domain.model.template.placeholder;

import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages template placeholders and their replacements.
 * <p>
 *     This class handles the registration, validation, and replacement of placeholders in template strings.
 *     It supports defining custom delimiters for placeholders and maintains a mapping of
 *     placeholder keys to their reference values.
 * </p>
 */
public class PlaceholderManager {
    private final Map<String, SpreadsheetReference> placeholders;
    private final PlaceholderValidator validator;
    private final char[] delimiters;

    /**
     * Creates a new placeholder manager with default delimiters '{' and '}'.
     */
    public PlaceholderManager() {
        this.placeholders = new HashMap<>();
        this.validator = new PlaceholderValidator();
        this.delimiters = new char[]{'{', '}'};
    }

    public PlaceholderManager(char[] delimiters, Map<String, SpreadsheetReference> placeholders) {
        Objects.requireNonNull(delimiters, "Delimiters cannot be null");
        Objects.requireNonNull(placeholders, "Placeholders map cannot be null");

        if (delimiters.length != 2) {
            throw new IllegalArgumentException("Delimiters array must contain exactly 2 elements (opening and closing)");
        }

        this.delimiters = delimiters.clone();
        this.placeholders = new HashMap<>(placeholders);
        this.validator = new PlaceholderValidator();
    }

    /**
     * Adds a new placeholder with validation.
     *
     * @param key The placeholder key
     * @param value The spreadsheet reference value
     * @throws PlaceholderException if key is invalid or already exists
     * @throws NullPointerException if either parameter is null
     */
    public void addPlaceholder(String key, SpreadsheetReference value) throws PlaceholderException {
        Objects.requireNonNull(key, "Placeholder key cannot be null");
        Objects.requireNonNull(value, "Placeholder value cannot be null");

        validator.validateKey(key);

        if (placeholders.containsKey(key)) {
            throw PlaceholderException.duplicateKey(key);
        }

        placeholders.put(key, value);
    }

    /**
     * Removes a placeholder.
     *
     * @param key The placeholder key to remove
     * @throws PlaceholderException if the key doesn't exist
     * @throws NullPointerException if key is null
     */
    public void removePlaceholder(String key) throws PlaceholderException {
        Objects.requireNonNull(key, "Placeholder key cannot be null");

        if (!placeholders.containsKey(key)) {
            throw PlaceholderException.keyNotFound(key);
        }
        placeholders.remove(key);
    }

    private SpreadsheetReference getPlaceholder(String key) throws PlaceholderException {
        SpreadsheetReference value = placeholders.get(key);
        if (value == null) {
            throw PlaceholderException.keyNotFound(key);
        }
        return value;
    }

    /**
     * Gets the delimiters used for placeholders.
     *
     * @return The opening and closing delimiter characters
     */
    public char[] getDelimiters() {
        return delimiters.clone();
    }

    /**
     * Gets all placeholder mappings.
     *
     * @return An unmodifiable view of the placeholders map
     */
    public Map<String, SpreadsheetReference> getPlaceholders() {
        return Collections.unmodifiableMap(placeholders);
    }

    /**
     * Gets all placeholder keys.
     *
     * @return A set of all defined placeholder keys
     */
    public Set<String> getPlaceholderKeys() {
        return Collections.unmodifiableSet(placeholders.keySet());
    }

    /**
     * Checks if a placeholder with the given key exists.
     *
     * @param key The placeholder key to check
     * @return true if the placeholder exists
     */
    public boolean containsPlaceholder(String key) {
        return key != null && placeholders.containsKey(key);
    }

    /**
     * Extracts all placeholders from a template string.
     *
     * @param template The template string to scan
     * @return A list of placeholder keys found in the template
     * @throws NullPointerException if template is null
     */
    public Set<String> extractPlaceholders(String template) {
        Objects.requireNonNull(template, "Template cannot be null");

        Pattern pattern = Pattern.compile(
                Pattern.quote(String.valueOf(delimiters[0])) +
                        "(.*?)" +
                        Pattern.quote(String.valueOf(delimiters[1]))
        );

        Matcher matcher = pattern.matcher(template);
        Set<String> keys = new java.util.HashSet<>();

        while (matcher.find()) {
            keys.add(matcher.group(1));
        }

        return Collections.unmodifiableSet(keys);
    }
}
