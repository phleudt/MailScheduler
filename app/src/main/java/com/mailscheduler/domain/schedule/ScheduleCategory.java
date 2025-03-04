package com.mailscheduler.domain.schedule;

public enum ScheduleCategory {
    DEFAULT_SCHEDULE,
    SCHEDULE;

    public static ScheduleCategory fromString(String category) {
        return switch (category) {
            case "DEFAULT_SCHEDULE" -> ScheduleCategory.DEFAULT_SCHEDULE;
            case "SCHEDULE" -> ScheduleCategory.SCHEDULE;
            default -> throw new IllegalArgumentException("ScheduleCategory not allowed");
        };
    }

    public static String toString(ScheduleCategory category) {
        return switch (category) {
            case DEFAULT_SCHEDULE -> "DEFAULT_SCHEDULE";
            case SCHEDULE -> "SCHEDULE";
        };
    }
}