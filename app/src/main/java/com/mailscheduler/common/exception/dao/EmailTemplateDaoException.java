package com.mailscheduler.common.exception.dao;

public class EmailTemplateDaoException extends Exception {
  public EmailTemplateDaoException(String message) {
    super(message);
  }

  public EmailTemplateDaoException(String message, Throwable cause) {
    super(message, cause);
  }

  // Nested exceptions
  public static class NotFound extends EmailTemplateDaoException {
    public NotFound(String message) {
      super(message);
    }

    public NotFound(int id) {
      super("Email template not found with ID: " + id);
    }
  }

  public static class Validation extends EmailTemplateDaoException {
    public Validation(String message) {
      super(message);
    }
  }

  public static class Insertion extends EmailTemplateDaoException {
    public Insertion(String message, Throwable cause) {
      super(message, cause);
    }
  }
}