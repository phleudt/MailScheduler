package com.mailscheduler.google.auth.exceptions;

// Exception classes
public class MissingRefreshTokenException extends Exception {
    public MissingRefreshTokenException(String message) {
        super(message);
    }
}
