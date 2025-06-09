package com.mailscheduler.infrastructure.persistence.entity;

import java.sql.Timestamp;

public class EmailEntity extends TableEntity {
    private Long initialEmailId;
    private Long recipientId;
    private String subject;
    private String body;
    private String emailType;
    private Integer followupNumber;
    private String status;
    private String failureReason;
    private Timestamp scheduledDate;
    private Timestamp sentDate;

    public EmailEntity(
            Long id,
            Long initialEmailId,
            Long recipientId,
            String subject,
            String body,
            String emailType,
            Integer followupNumber,
            String status,
            String failureReason,
            Timestamp scheduledDate,
            Timestamp sentDate
    ) {
        setId(id);
        this.initialEmailId = initialEmailId;
        this.recipientId = recipientId;
        this.subject = subject;
        this.body = body;
        this.emailType = emailType;
        this.followupNumber = followupNumber;
        this.status = status;
        this.failureReason = failureReason;
        this.scheduledDate = scheduledDate;
        this.sentDate = sentDate;
    }

    public Long getInitialEmailId() {
        return initialEmailId;
    }

    public void setInitialEmailId(Long initialEmailId) {
        this.initialEmailId = initialEmailId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public Integer getFollowupNumber() {
        return followupNumber;
    }

    public void setFollowupNumber(int followupNumber) {
        this.followupNumber = followupNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Timestamp getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Timestamp scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Timestamp getSentDate() {
        return sentDate;
    }

    public void setSentDate(Timestamp sentDate) {
        this.sentDate = sentDate;
    }
}
