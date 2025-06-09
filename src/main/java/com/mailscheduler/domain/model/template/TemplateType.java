package com.mailscheduler.domain.model.template;

/**
 * Enumerates the different types of email templates.
 * <p>
 *     This enum defines the categories of templates that can be used for emails, distinguishing between
 *     initial contact templates, follow-up templates, and system-provided default templates.
 * </p>
 */
public enum TemplateType {
    /**
     * Template for initial contact emails created by users.
     */
    INITIAL,

    /**
     * Template for follow-up emails created by users.
     */
    FOLLOW_UP,

    /**
     * System-provided default template for initial contact.
     */
    DEFAULT_INITIAL,

    /**
     * System-provided default template for follow-ups.
     */
    DEFAULT_FOLLOW_UP;


    /**
     * Checks if this template type is for initial emails.
     * Excludes default templates.
     *
     * @return true if this is an initial email template
     */
    public boolean isInitial() {
        return this == INITIAL;
    }

    /**
     * Checks if this template type is for follow-up emails.
     * Excludes default templates.
     *
     * @return true if this is a follow-up email template
     */
    public boolean isFollowUp() {
        return this == FOLLOW_UP;
    }

    /**
     * Checks if this template type is a default system-provided template.
     *
     * @return true if this is a default template
     */
    public boolean isDefault() {
        return this == DEFAULT_INITIAL || this == DEFAULT_FOLLOW_UP;
    }

    @Override
    public String toString() {
        return name();
    }
}
