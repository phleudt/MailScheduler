package com.mailscheduler.infrastructure.persistence.entities;

public class TemplateEntity {
    private int id;
    private String draft_id;
    private String template_category;
    private String subject_template;
    private String body_template;
    private String placeholder_symbols;
    private String placeholders;
    private int followup_number;

    public TemplateEntity(
            int id,
            String draft_id,
            String template_category,
            String subject_template,
            String body_template,
            String placeholder_symbols,
            String placeholders,
            int followup_number
    ) {
        this.id = id;
        this.draft_id = draft_id;
        this.template_category = template_category;
        this.subject_template = subject_template;
        this.body_template = body_template;
        this.placeholder_symbols = placeholder_symbols;
        this.placeholders = placeholders;
        this.followup_number = followup_number;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDraft_id() {
        return draft_id;
    }

    public void setDraft_id(String draft_id) {
        this.draft_id = draft_id;
    }

    public String getTemplate_category() {
        return template_category;
    }

    public void setTemplate_category(String template_category) {
        this.template_category = template_category;
    }

    public String getSubject_template() {
        return subject_template;
    }

    public void setSubject_template(String subject_template) {
        this.subject_template = subject_template;
    }

    public String getBody_template() {
        return body_template;
    }

    public void setBody_template(String body_template) {
        this.body_template = body_template;
    }

    public String getPlaceholder_symbols() {
        return placeholder_symbols;
    }

    public void setPlaceholder_symbols(String placeholder_symbols) {
        this.placeholder_symbols = placeholder_symbols;
    }

    public String getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(String placeholders) {
        this.placeholders = placeholders;
    }

    public int getFollowup_number() {
        return followup_number;
    }

    public void setFollowup_number(int followup_number) {
        this.followup_number = followup_number;
    }
}
