package com.mailscheduler.infrastructure.persistence.entity;

public class TemplateEntity extends TableEntity {
    private String templateType;
    private String subjectTemplate;
    private String bodyTemplate;
    private String delimiters;
    private String placeholders;
    private String draftId;

    public TemplateEntity(
            Long id,
            String templateType,
            String subjectTemplate,
            String bodyTemplate,
            String delimiters,
            String placeholders,
            String draftId
    ) {
        setId(id);
        this.templateType = templateType;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.delimiters = delimiters;
        this.placeholders = placeholders;
        this.draftId = draftId;
    }

    // Getters and Setters
    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
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

    public String getDelimiters() {
        return delimiters;
    }

    public void setDelimiters(String delimiters) {
        this.delimiters = delimiters;
    }

    public String getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(String placeholders) {
        this.placeholders = placeholders;
    }

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }
}
