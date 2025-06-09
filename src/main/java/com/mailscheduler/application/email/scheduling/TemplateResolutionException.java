package com.mailscheduler.application.email.scheduling;

/**
 * Exception thrown when there is an error resolving placeholders in a template.
 */
public class TemplateResolutionException extends Exception {
    public TemplateResolutionException(String message) {
        super(message);
    }

    public TemplateResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
