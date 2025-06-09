package com.mailscheduler.domain.model.template.placeholder;

/**
 * Enumerates the different types of values that can be used in placeholders.
 * <p>
 *     This enum defines the data types that can be used to replace placeholders in email templates.
 *     Each type may have different formatting or validation rules.
 * </p>
 */
public enum ValueType {
    /**
     * Plain text string value.
     */
    STRING,

    /**
     * Reference to a cell or range in a spreadsheet.
     */
    SPREADSHEET_REFERENCE
}
