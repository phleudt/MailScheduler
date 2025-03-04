package com.mailscheduler.domain.template;

import com.mailscheduler.common.exception.validation.InvalidTemplateException;

public class Template {
    private final TemplateId id;
    private final TemplateCategory category;
    private TemplateContent content;
    private PlaceholderManager placeholderManager;
    private final int followUpNumber;
    private String draftId;

    private Template(Builder builder) {
        this.id = builder.id;
        this.category = builder.category;
        this.content = builder.content;
        this.placeholderManager = builder.placeholderManager;
        this.followUpNumber = builder.followUpNumber;
        this.draftId = builder.draftId;
    }

    public String generateSubject() {
        return content.generateSubject(placeholderManager);
    }

    public String generateBody() {
        return content.generateBody(placeholderManager);
    }

    // Getters
    public TemplateId getId() {
        return id;
    }

    public TemplateCategory getCategory() {
        return category;
    }

    public TemplateContent getContent() {
        return content;
    }

    public String getSubject() {
        return content.subject();
    }

    public String getBody() {
        return content.body();
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public int getFollowUpNumber() {
        return followUpNumber;
    }

    public String getDraftId() {
        return draftId;
    }


    // Setters
    public void setContent(TemplateContent content) {
        this.content = content;
    }

    public void setPlaceholderManager(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public static class Builder {
        private TemplateId id;
        private TemplateCategory category;
        private String subject;
        private String body;
        private PlaceholderManager placeholderManager;  // TODO: Wie den placeholder manager handhaben
        private int followUpNumber;
        private String draftId;
        private TemplateContent content;

        public Builder setId(int id) {
            this.id = TemplateId.of(id);
            return this;
        }

        public Builder setCategory(TemplateCategory category) {
            this.category = category;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setPlaceholderManager(PlaceholderManager placeholderManager) {
            this.placeholderManager = placeholderManager;
            return this;
        }

        public Builder setFollowUpNumber(int followUpNumber) {
            this.followUpNumber = followUpNumber;
            return this;
        }

        public Builder setDraftId(String draftId) {
            this.draftId = draftId;
            return this;
        }

        public Template build() {
            content = new TemplateContent(subject, body);
            validate();
            return new Template(this);
        }

        private void validate() {
            if (category == null) {
                throw new IllegalStateException("Template category is required");
            }
            if (content == null) {
                throw new IllegalStateException("Template content is required");
            }
            if (placeholderManager == null) {
                throw new IllegalStateException("Placeholder configuration is required");
            }
        }
    }

}
