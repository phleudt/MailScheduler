package com.mailscheduler.infrastructure.google.auth.exceptions;

public class NoInternetConnectionException extends Exception {
    public NoInternetConnectionException(String message) {
        super(message);
    }
}
