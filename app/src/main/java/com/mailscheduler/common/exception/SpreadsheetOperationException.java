package com.mailscheduler.common.exception;

/**
 * Exception thrown when a spreadsheet operation fails.
 * Provides more specific error handling for spreadsheet-related issues.
 */
public class SpreadsheetOperationException extends Exception {
    /**
     * Constructs a new SpreadsheetOperationException with the specified detail message.
     *
     * @param message the detail message
     */
    public SpreadsheetOperationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SpreadsheetOperationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public SpreadsheetOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
