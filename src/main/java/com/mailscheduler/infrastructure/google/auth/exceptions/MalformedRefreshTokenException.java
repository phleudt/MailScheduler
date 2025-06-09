package com.mailscheduler.infrastructure.google.auth.exceptions;

/**
 * Exception thrown when a refresh token is malformed or invalid.
 */
public class MalformedRefreshTokenException extends GoogleAuthException {
    public MalformedRefreshTokenException(String message) {
        super(message);
    }

    public MalformedRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
