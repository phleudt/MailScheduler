package com.mailscheduler.exception;

import java.util.Map;

/**
 * Custom exception for placeholder-related errors
 */
public class PlaceholderException extends Exception {
    private final ErrorCode errorCode;
    private final Map<String, Object> context;

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
}