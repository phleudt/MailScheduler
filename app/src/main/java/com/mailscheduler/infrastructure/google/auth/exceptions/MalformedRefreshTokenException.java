package com.mailscheduler.infrastructure.google.auth.exceptions;

public class MalformedRefreshTokenException extends Exception {
    public MalformedRefreshTokenException(String message) {
        super(message);
    }
}
