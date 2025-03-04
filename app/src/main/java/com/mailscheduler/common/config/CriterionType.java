package com.mailscheduler.common.config;

/**
 * Enum to represent different types of sending criteria conditions
 */
public enum CriterionType {
    COLUMN_FILLED,       // Check if a specific column is non-empty
    COLUMN_VALUE_MATCH,  // Check if a column matches a specific value
    COLUMN_PATTERN_MATCH, // Check if a column matches a regex pattern
    STATUS_CHECK,        // Check a specific status column
    CUSTOM               // Placeholder for future custom condition types
}