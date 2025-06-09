package com.mailscheduler.infrastructure.google.auth.exceptions;

/**
 * Exception thrown when internet connectivity is required but not available.
 */
public class NoInternetConnectionException extends GoogleAuthException {
    public NoInternetConnectionException(String message) {
        super(message);
    }
}
