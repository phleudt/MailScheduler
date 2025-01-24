package com.mailscheduler.exception.service;


public class FollowUpScheduleServiceException extends Exception {
    public FollowUpScheduleServiceException(String message) {
        super(message);
    }

    public FollowUpScheduleServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
