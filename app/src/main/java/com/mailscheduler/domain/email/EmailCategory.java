package com.mailscheduler.domain.email;

public enum EmailCategory {
    INITIAL,
    FOLLOW_UP,
    EXTERNALLY_INITIAL,
    EXTERNALLY_FOLLOW_UP;

    public static EmailCategory fromString(String category) {
        return switch (category) {
            case "INITIAL" -> EmailCategory.INITIAL;
            case "FOLLOW_UP" -> EmailCategory.FOLLOW_UP;
            case "EXTERNALLY_INITIAL" -> EmailCategory.EXTERNALLY_INITIAL;
            case "EXTERNALLY_FOLLOW_UP" -> EmailCategory.EXTERNALLY_FOLLOW_UP;
            default -> throw new IllegalArgumentException("EmailCategory not allowed");
        };
    }

    public static String toString(EmailCategory category) {
        return switch (category) {
            case INITIAL -> "INITIAL";
            case FOLLOW_UP -> "FOLLOW_UP";
            case EXTERNALLY_INITIAL -> "EXTERNALLY_INITIAL";
            case EXTERNALLY_FOLLOW_UP -> "EXTERNALLY_FOLLOW_UP";
        };
    }
}