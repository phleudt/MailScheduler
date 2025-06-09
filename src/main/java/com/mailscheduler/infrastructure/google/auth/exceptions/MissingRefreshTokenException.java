package com.mailscheduler.infrastructure.google.auth.exceptions;

/**
 * Exception thrown when a required refresh token is missing.
 */
public class MissingRefreshTokenException extends GoogleAuthException {
    public MissingRefreshTokenException(String message) {
        super(message);
    }

    public MissingRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
