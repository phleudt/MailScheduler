package com.mailscheduler.model;

public enum TemplateCategory {
    INITIAL,
    FOLLOW_UP,
    DEFAULT_INITIAL,
    DEFAULT_FOLLOW_UP;

    public static TemplateCategory fromString(String category) {
        return switch (category) {
            case "INITIAL" -> TemplateCategory.INITIAL;
            case "FOLLOW_UP" -> TemplateCategory.FOLLOW_UP;
            case "DEFAULT_INITIAL" -> TemplateCategory.DEFAULT_INITIAL;
            case "DEFAULT_FOLLOW_UP" -> TemplateCategory.DEFAULT_FOLLOW_UP;
            default -> throw new IllegalArgumentException("TemplateCategory not allowed");
        };
    }

    public static String toString(TemplateCategory category) {
        return switch (category) {
            case INITIAL -> "INITIAL";
            case FOLLOW_UP -> "FOLLOW_UP";
            case DEFAULT_INITIAL -> "DEFAULT_INITIAL";
            case DEFAULT_FOLLOW_UP -> "DEFAULT_FOLLOW_UP";
        };
    }
}