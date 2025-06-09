package com.mailscheduler.domain.model.template.placeholder;

import java.util.Map;

/**
 * Exception thrown when operations on placeholders fail.
 * <p>
 *     This exception provides specific error information with error codes
 *     and contextual details about the failure to help with debugging
 *     and proper error handling.
 * </p>
 */
public class PlaceholderException extends Exception {
    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    /**
     * Categorizes the different types of placeholder errors.
     */
    public enum ErrorCode {
        INVALID_KEY,
        INVALID_VALUE,
        KEY_NOT_FOUND,
        DUPLICATE_KEY,
        TYPE_MISMATCH
    }

    public PlaceholderException(String message) {
        super(message);
        this.errorCode = null;
        this.context = null;
    }

    public PlaceholderException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }

    public PlaceholderException(String message, ErrorCode errorCode, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return context;
    }


    /**
     * Creates a new exception for an invalid key.
     *
     * @param key The invalid key
     * @return A new PlaceholderException
     */
    public static PlaceholderException invalidKey(String key) {
        return new PlaceholderException(
                "Invalid placeholder key: " + key,
                ErrorCode.INVALID_KEY,
                Map.of("key", key != null ? key : "null")
        );
    }

    /**
     * Creates a new exception for a duplicate key.
     *
     * @param key The duplicate key
     * @return A new PlaceholderException
     */
    public static PlaceholderException duplicateKey(String key) {
        return new PlaceholderException(
                "Placeholder key already exists: " + key,
                ErrorCode.DUPLICATE_KEY,
                Map.of("key", key)
        );
    }

    /**
     * Creates a new exception for a key not found.
     *
     * @param key The key that wasn't found
     * @return A new PlaceholderException
     */
    public static PlaceholderException keyNotFound(String key) {
        return new PlaceholderException(
                "Placeholder key not found: " + key,
                ErrorCode.KEY_NOT_FOUND,
                Map.of("key", key)
        );
    }
}
