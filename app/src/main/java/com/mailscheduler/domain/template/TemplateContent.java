package com.mailscheduler.domain.template;

import com.mailscheduler.common.exception.PlaceholderException;

public record TemplateContent(String subject, String body) {
    public TemplateContent {
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject template cannot be empty");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Body template cannot be empty");
        }
    }

    public String generateSubject(PlaceholderManager placeholderManager) {
        try {
            return placeholderManager.replacePlaceholders(subject);
        } catch (PlaceholderException e) {
            throw new RuntimeException(e); // TODO
        }
    }

    public String generateBody(PlaceholderManager placeholderManager) {
        try {
            return placeholderManager.replacePlaceholders(body);
        } catch (PlaceholderException e) {
            throw new RuntimeException(e); // TODO
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TemplateContent that = (TemplateContent) object;
        return subject.equals(that.subject) && body.equals(that.body);
    }
}
