package com.mailscheduler.dto;

import com.mailscheduler.model.EmailCategory;

import java.time.ZonedDateTime;

public final class EmailDto {
    private final int id;
    private final String subject;
    private final String body;
    private final String status;
    private final ZonedDateTime scheduledDate;
    private final EmailCategory emailCategory;
    private final int followupNumber;
    private final String threadId;
    private final int scheduleId;
    private final Integer initialEmailId; // Nullable field
    private final int recipientId;

    public EmailDto(int id, String subject, String body, String status, ZonedDateTime scheduledDate, EmailCategory emailCategory, int followupNumber, String threadId, int scheduleId, Integer initialEmailId, int recipientId) {
        this.id = id;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.scheduledDate = scheduledDate;
        this.emailCategory = emailCategory;
        this.followupNumber = followupNumber;
        this.threadId = threadId;
        this.scheduleId = scheduleId;
        this.initialEmailId = initialEmailId;
        this.recipientId = recipientId;
    }

    // Getters only (no setters for immutability)
    public int getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public ZonedDateTime getScheduledDate() {
        return scheduledDate;
    }

    public EmailCategory getEmailCategory() {
        return emailCategory;
    }

    public int getFollowupNumber() {
        return followupNumber;
    }

    public String getThreadId() {
        return threadId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public Integer getInitialEmailId() {
        return initialEmailId;
    }

    public int getRecipientId() {
        return recipientId;
    }
}

