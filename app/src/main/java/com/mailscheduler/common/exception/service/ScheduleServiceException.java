package com.mailscheduler.common.exception.service;


public class ScheduleServiceException extends Exception {
    public ScheduleServiceException(String message) {
        super(message);
    }

    public ScheduleServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
