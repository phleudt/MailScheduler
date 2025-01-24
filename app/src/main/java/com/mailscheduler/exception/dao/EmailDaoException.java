package com.mailscheduler.exception.dao;

public class EmailDaoException extends Exception {
    public EmailDaoException(String message) {
        super(message);
    }
    public EmailDaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
