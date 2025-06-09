package com.mailscheduler.infrastructure.spreadsheet.exception;

/**
 * Exception thrown when an operation on a spreadsheet fails.
 */
public class SpreadsheetOperationException extends Exception {

    public SpreadsheetOperationException(String message) {
        super(message);
    }

    public SpreadsheetOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}