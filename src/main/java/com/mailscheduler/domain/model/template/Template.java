package com.mailscheduler.domain.model.template;

import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import com.mailscheduler.domain.model.template.placeholder.PlaceholderManager;

import java.util.*;

/**
 * Represents an email template with placeholder support.
 * <p>
 *     This entity provides functionality to generate personalized email content by replacing placeholders
 *     in the template with actual values. Templates can be of different types (initial, follow-up, etc.)
 *     and contain customizable subject and body content.
 * </p>
 */
public class Template extends IdentifiableEntity<Template> {
    private final TemplateType type;
    private Subject subject;
    private final Body body;
    private final PlaceholderManager placeholderManager;

    private Template(Builder builder) {
        this.setId(builder.id);
        this.type = builder.type;
        this.subject = builder.subject;
        this.body = builder.body;
        this.placeholderManager = builder.placeholderManager != null ?
                builder.placeholderManager : new PlaceholderManager();
    }

    public TemplateType getType() {
        return type;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Body getBody() {
        return body;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public boolean isEmpty() {
        return subject == null && body == null && placeholderManager == null;
    }

    /**
     * Extracts all placeholders used in the template body.
     *
     * @return A set of placeholder keys found in the body
     */
    public Set<String> getPlaceholdersInBody() {
        if (body == null || body.isEmpty()) {
            return Collections.emptySet();
        }

        if (placeholderManager == null) {
            return Collections.emptySet();
        }

        return placeholderManager.extractPlaceholders(body.value());
    }

    /**
     * Extracts all placeholders used in the template subject.
     *
     * @return A set of placeholder keys found in the subject
     */
    public Set<String> getPlaceholdersInSubject() {
        if (subject == null) {
            return Collections.emptySet();
        }

        if (placeholderManager == null) {
            return Collections.emptySet();
        }

        return placeholderManager.extractPlaceholders(subject.value());
    }

    /**
     * Gets all unique placeholders used in this template (both subject and body).
     *
     * @return A set of all placeholder keys used in the template
     */
    public Set<String> getAllPlaceholders() {
        Set<String> bodyPlaceholders = getPlaceholdersInBody();
        Set<String> subjectPlaceholders = getPlaceholdersInSubject();

        if (bodyPlaceholders.isEmpty()) {
            return subjectPlaceholders;
        }

        if (subjectPlaceholders.isEmpty()) {
            return bodyPlaceholders;
        }

        // Combine both sets
        Set<String> allPlaceholders = new java.util.HashSet<>(bodyPlaceholders);
        allPlaceholders.addAll(subjectPlaceholders);
        return Collections.unmodifiableSet(allPlaceholders);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Template that)) return false;
        if (!super.equals(o)) return false;

        if (type != that.type) return false;
        if (!Objects.equals(subject, that.subject)) return false;
        return Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Template{" +
                "id=" + getId() +
                ", type=" + type +
                ", subject='" + (subject != null ? subject.truncated(20) : "null") + '\'' +
                '}';
    }

    /**
     * Builder for creating Template instances.
     */
    public static class Builder {
        private EntityId<Template> id;
        private TemplateType type;
        private Subject subject;
        private Body body;
        private PlaceholderManager placeholderManager;

        public Builder setId(EntityId<Template> id) {
            this.id = id;
            return this;
        }

        public Builder setType(TemplateType type) {
            this.type = type;
            return this;
        }

        public Builder setSubject(Subject subject) {
            this.subject = subject;
            return this;
        }

        public Builder setBody(Body body) {
            this.body = body;
            return this;
        }

        public Builder setPlaceholderManager(PlaceholderManager placeholderManager) {
            this.placeholderManager = placeholderManager;
            return this;
        }

        public Builder from(Template template) {
            this.id = template.getId();
            this.type = template.getType();
            this.subject = template.getSubject();
            this.body = template.getBody();
            this.placeholderManager = template.getPlaceholderManager();
            return this;
        }

        public Template build() {
            return new Template(this);
        }
    }
}
