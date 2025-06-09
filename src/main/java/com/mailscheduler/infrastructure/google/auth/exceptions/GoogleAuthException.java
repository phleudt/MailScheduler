package com.mailscheduler.infrastructure.google.auth.exceptions;

/**
 * Base exception class for all Google authentication related exceptions.
 */
public class GoogleAuthException extends Exception {
    public GoogleAuthException(String message) {
        super(message);
    }

    public GoogleAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
