package com.mailscheduler.infrastructure.google.auth.exceptions;

// Exception classes
public class MissingRefreshTokenException extends Exception {
    public MissingRefreshTokenException(String message) {
        super(message);
    }
}
