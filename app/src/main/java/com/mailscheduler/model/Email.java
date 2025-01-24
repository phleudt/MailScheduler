package com.mailscheduler.model;

import java.time.ZonedDateTime;
import java.util.List;


public class Email {
    private int id;
    private String sender;
    private String recipientEmail;
    private int recipientId;
    private String subject;
    private String body;
    private String status;
    private ZonedDateTime scheduledDate; // Time when email was send
    private EmailCategory emailCategory;
    private int followupNumber = 0; // Follow-up number of the email (0 for initial email, 1 for 1st follow-up and so on)
    private String threadId;
    private int initialEmailId = 0; // Initial email ID (0 for initial email)
    private List<String> attachments;

    // Constructor
    private Email(Builder builder) {
        if (builder.template != null) {
            this.subject = builder.template.generateSubject();
            this.body = builder.template.generateBody();
        } else {
            this.subject = builder.subject;
            this.body = builder.body;
        }

        this.id = builder.id;
        this.sender = builder.sender;
        this.recipientEmail = builder.recipientEmail;
        this.recipientId = builder.recipientId;
        this.status = builder.status;
        this.scheduledDate = builder.scheduledDate;
        this.emailCategory = builder.emailCategory;
        this.followupNumber = builder.followupNumber;
        this.threadId = builder.threadId;
        this.initialEmailId = builder.initialEmailId;
        this.attachments = builder.attachments;
    }

    public static Email generateEmailExample() {
        return new Email.Builder()
                .setSender("exampleFrom@email.com")
                .setRecipientEmail("exampleTo@email.com")
                .setSubject("Test Subject")
                .setBody("...")
                .build();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
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

    public ZonedDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(ZonedDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public EmailCategory getEmailCategory() {
        return emailCategory;
    }

    public void setEmailCategory(EmailCategory emailCategory) {
        this.emailCategory = emailCategory;
    }

    public int getFollowupNumber() {
        return followupNumber;
    }

    public void setFollowupNumber(int followupNumber) {
        this.followupNumber = followupNumber;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public int getInitialEmailId() {
        return initialEmailId;
    }

    public void setInitialEmailId(int initialEmailId) {
        this.initialEmailId = initialEmailId;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return "Email{from=" + sender + ", recipient=" + recipientEmail + ", subject=" + subject + "}";
    }

    public static class Builder {
        private int id;
        private String sender;
        private String recipientEmail;
        private int recipientId;
        private String subject;
        private String body;
        private String status;
        private ZonedDateTime scheduledDate;
        private EmailCategory emailCategory;
        private int followupNumber = 0; // Default 0, for initial email
        private String threadId;
        private int initialEmailId;
        private List<String> attachments;
        private EmailTemplate template;

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setSender(String sender) {
            this.sender = sender;
            return this;
        }

        public Builder setRecipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        public Builder setRecipientId(int recipientId) {
            this.recipientId = recipientId;
            return this ;
        }

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder setScheduledDate(ZonedDateTime scheduledDate) {
            this.scheduledDate = scheduledDate;
            return this;
        }

        public Builder setEmailCategory(EmailCategory emailCategory) {
            this.emailCategory = emailCategory;
            return this;
        }

        public Builder setFollowupNumber(int followupNumber) {
            this.followupNumber = followupNumber;
            return this;
        }

        public Builder setThreadId(String threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder setInitialEmailId(int initialEmailId) {
            this.initialEmailId = initialEmailId;
            return this;
        }

        public Builder setAttachments(List<String> attachments) {
            this.attachments = attachments;
            return this;
        }

        /**
         * When email template is set, then body and subject are ignored
         */
        public Builder setTemplate(EmailTemplate template) {
            this.template = template;
            return this;
        }

        public Email build() {
            return new Email(this);
        }
    }
}