package com.mailscheduler.exception.dao;

public class RecipientDaoException extends Exception {
    public RecipientDaoException(String message) {
        super(message);
    }
    public RecipientDaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
