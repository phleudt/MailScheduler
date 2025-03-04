package com.mailscheduler.common.exception;

/**
 * Represents an exception specific to EmailTemplateManager operations.
 * This exception provides a more specific and contextual error handling
 * mechanism for email template management processes.
 */
public class EmailTemplateManagerException extends Exception {
    /**
     * Constructs a new EmailTemplateManagerException with a detail message.
     *
     * @param message A descriptive message about the exception
     */
    public EmailTemplateManagerException(String message) {
        super(message);
    }

    /**
     * Constructs a new EmailTemplateManagerException with a detail message and cause.
     *
     * @param message A descriptive message about the exception
     * @param cause The underlying cause of the exception
     */
    public EmailTemplateManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}