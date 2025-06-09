package com.mailscheduler.infrastructure.persistence.repository.exception;

public class DataAccessException extends RepositoryException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
