package com.mailscheduler.google.auth.exceptions;

public class MalformedRefreshTokenException extends Exception {
    public MalformedRefreshTokenException(String message) {
        super(message);
    }
}
