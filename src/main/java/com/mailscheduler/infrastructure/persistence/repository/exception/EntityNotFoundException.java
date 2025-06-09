package com.mailscheduler.infrastructure.persistence.repository.exception;

public class EntityNotFoundException extends RepositoryException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
