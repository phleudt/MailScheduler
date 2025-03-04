package com.mailscheduler.domain.template;

public record TemplateId(int value) {
    public static TemplateId of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Template ID cannot be null");
        }
        return new TemplateId(value);
    }
}
