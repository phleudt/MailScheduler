package com.mailscheduler.infrastructure.persistence.entities;

import java.sql.Timestamp;

public class EmailEntity {
    private int id;
    private String subject;
    private String body;
    private String status;
    private Timestamp scheduled_date;
    private String email_category;
    private int followup_number;
    private String thread_id;
    private int schedule_id;
    private int initial_email_id;
    private int recipient_id;

    public EmailEntity(
            int id,
            String subject,
            String body,
            String status,
            Timestamp scheduled_date,
            String email_category,
            int followup_number,
            String thread_id,
            int schedule_id,
            int initial_email_id,
            int recipient_id) {
        this.id = id;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.scheduled_date = scheduled_date;
        this.email_category = email_category;
        this.followup_number = followup_number;
        this.thread_id = thread_id;
        this.schedule_id = schedule_id;
        this.initial_email_id = initial_email_id;
        this.recipient_id = recipient_id;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getScheduled_date() {
        return scheduled_date;
    }

    public void setScheduled_date(Timestamp scheduled_date) {
        this.scheduled_date = scheduled_date;
    }

    public String getEmail_category() {
        return email_category;
    }

    public void setEmail_category(String email_category) {
        this.email_category = email_category;
    }

    public int getFollowup_number() {
        return followup_number;
    }

    public void setFollowup_number(int followup_number) {
        this.followup_number = followup_number;
    }

    public String getThread_id() {
        return thread_id;
    }

    public void setThread_id(String thread_id) {
        this.thread_id = thread_id;
    }

    public int getSchedule_id() {
        return schedule_id;
    }

    public void setSchedule_id(int schedule_id) {
        this.schedule_id = schedule_id;
    }

    public int getInitial_email_id() {
        return initial_email_id;
    }

    public void setInitial_email_id(int initial_email_id) {
        this.initial_email_id = initial_email_id;
    }

    public int getRecipient_id() {
        return recipient_id;
    }

    public void setRecipient_id(int recipient_id) {
        this.recipient_id = recipient_id;
    }

    @Override
    public String toString() {
        int bodySnippetLength = Math.min(20, body.length());
        return "Id: " + id + ", subject: " + subject + ", body snippet: " + body.substring(0, bodySnippetLength) + "...";
    }
}
