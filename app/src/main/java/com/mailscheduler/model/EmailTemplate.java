package com.mailscheduler.model;

import com.mailscheduler.exception.validation.InvalidTemplateException;
import com.mailscheduler.exception.PlaceholderException;
import com.mailscheduler.util.TemplateUtils;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailTemplate {
    private static final Logger LOGGER = Logger.getLogger(EmailTemplate.class.getName());
    private int id;
    private TemplateCategory templateCategory;
    private String subjectTemplate;
    private String bodyTemplate;
    private char[] placeholderSymbols;
    private PlaceholderManager placeholderManager;
    private int followupNumber;
    private String draftId;

    private EmailTemplate(Builder builder) {
        this.id = builder.id;
        this.templateCategory = builder.templateCategory;
        this.subjectTemplate = builder.subjectTemplate;
        this.bodyTemplate = builder.bodyTemplate;
        this.placeholderSymbols = builder.placeholderSymbols;
        this.placeholderManager = builder.placeholderManager;
        this.followupNumber = builder.followupNumber;
        this.draftId = builder.draftId;
    }

    // Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TemplateCategory getTemplateCategory() {
        return templateCategory;
    }

    public void setTemplateCategory(TemplateCategory templateCategory) {
        this.templateCategory = templateCategory;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public char[] getPlaceholderSymbols() {
        return placeholderSymbols;
    }

    public void setPlaceholderSymbols(char[] placeholderSymbols) {
        this.placeholderSymbols = placeholderSymbols;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public void setPlaceholderManager(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    public int getFollowupNumber() {
        return followupNumber;
    }

    public void setFollowupNumber(int followupNumber) {
        this.followupNumber = followupNumber;
    }

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    private void validate() throws InvalidTemplateException {
        if (!validateSubjectTemplate()) {
            throw new InvalidTemplateException("Invalid subject template");
        }
        if (!validateBodyTemplate()) {
            throw new InvalidTemplateException("Invalid body template");
        }
    }

    private boolean validateSubjectTemplate() {
        LOGGER.info("Validating subject template string");
        return TemplateUtils.validateTemplate(subjectTemplate, placeholderSymbols);
    }

    private boolean validateBodyTemplate() {
        LOGGER.info("Validating body template string");
        return TemplateUtils.validateTemplate(bodyTemplate, placeholderSymbols);
    }

    public String generateSubject() {
        LOGGER.info("Generating subject from template");
        try {
            return replacePlaceholders(subjectTemplate);
        } catch (PlaceholderException e) {
            LOGGER.severe("Failed to generate subject: " + e.getMessage());
            return "Invalid Subject Template";
        }
    }

    public String generateBody() {
        LOGGER.info("Generating body from template");
        try {
            return replacePlaceholders(bodyTemplate);
        } catch (PlaceholderException e) {
            LOGGER.severe("Failed to generate body: " + e.getMessage());
            return "Invalid Body Template";
        }
    }

    private String replacePlaceholders(String template) throws PlaceholderException {
        LOGGER.info("Replacing placeholders");
        String regex = Pattern.quote(String.valueOf(placeholderSymbols[0]))
                + "(.*?)"
                + Pattern.quote(String.valueOf(placeholderSymbols[1]));
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(template);
        StringBuilder result = new StringBuilder();

        if (matcher.find()) {
            if (placeholderManager == null || placeholderManager.isEmpty()) {
                throw new PlaceholderException("Placeholder replacement strategy has not been initialized");
            }
        }
        matcher.reset();
        while (matcher.find()) {
            String key = matcher.group(1);
            PlaceholderManager.PlaceholderValue replacement = placeholderManager.getPlaceholder(key);
            matcher.appendReplacement(result, replacement.value().toString());
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public String toString() {
        return "Subject: " + subjectTemplate + ", Body: " + bodyTemplate;
    }

    public static class Builder {
        private int id;
        private TemplateCategory templateCategory;
        private String subjectTemplate;
        private String bodyTemplate;
        private char[] placeholderSymbols;
        private PlaceholderManager placeholderManager;
        private int followupNumber;
        private String draftId;

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setTemplateCategory(TemplateCategory templateCategory) {
            this.templateCategory = templateCategory;
            return this;
        }

        public Builder setSubjectTemplate(String subjectTemplate) {
            this.subjectTemplate = subjectTemplate;
            return this;
        }

        public Builder setBodyTemplate(String bodyTemplate) {
            this.bodyTemplate = bodyTemplate;
            return this;
        }

        public Builder setPlaceholderManager(PlaceholderManager placeholderManager) {
            this.placeholderManager = placeholderManager;
            return this;
        }

        public Builder setDefaultPlaceholderSymbols() {
            this.placeholderSymbols = new char[]{'{', '}'};
            return this;
        }

        public Builder setPlaceholderSymbols(char[] placeholderSymbols) {
            this.placeholderSymbols = placeholderSymbols;
            return this;
        }

        public Builder setFollowupNumber(int followupNumber) {
            this.followupNumber = followupNumber;
            return this;
        }

        public Builder setDraftId(String draftId) {
            this.draftId = draftId;
            return this;
        }

        public EmailTemplate build() throws InvalidTemplateException {
            EmailTemplate emailTemplate = new EmailTemplate(this);
            emailTemplate.validate();
            return emailTemplate;
        }
    }

}
