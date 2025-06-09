package com.mailscheduler.infrastructure.persistence.entity;

import java.sql.Timestamp;

/**
 * Entity representing the recipients table
 */
public class RecipientEntity extends TableEntity {
    private Long contactId;
    private String emailAddress;
    private Long followupPlanId;
    private String salutation;
    private Timestamp initialContactDate;
    private boolean hasReplied;
    private String threadId;

    public RecipientEntity(
            Long id,
            Long contactId,
            String emailAddress,
            Long followupPlanId,
            String salutation,
            Timestamp initialContactDate,
            boolean hasReplied,
            String threadId
    ) {
        this.setId(id);
        this.contactId = contactId;
        this.emailAddress = emailAddress;
        this.followupPlanId = followupPlanId;
        this.salutation = salutation;
        this.initialContactDate = initialContactDate;
        this.hasReplied = hasReplied;
        this.threadId = threadId;
    }

    // Getters and Setters
    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Long getFollowupPlanId() {
        return followupPlanId;
    }

    public void setFollowupPlanId(Long followupPlanId) {
        this.followupPlanId = followupPlanId;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public Timestamp getInitialContactDate() {
        return initialContactDate;
    }

    public void setInitialContactDate(Timestamp initialContactDate) {
        this.initialContactDate = initialContactDate;
    }

    public boolean hasReplied() {
        return hasReplied;
    }

    public void setHasReplied(boolean hasReplied) {
        this.hasReplied = hasReplied;
    }

    public String getThreadId() {
        return threadId;
    }
}