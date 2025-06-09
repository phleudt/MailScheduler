package com.mailscheduler.domain.model.schedule;

/**
 * Exception thrown when creating a communication plan fails.
 * <p>
 *     This exception provides specific error information about issues that occur during plan creation,
 *     such as missing templates or invalid step configurations.
 * </p>
 */
public class PlanCreationException extends Exception {

    public PlanCreationException(String message) {
        super(message);
    }

    public PlanCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}