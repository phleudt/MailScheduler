package com.mailscheduler.domain.email;

import com.mailscheduler.application.email.factory.EmailFactory;
import com.mailscheduler.domain.common.EmailAddress;
import com.mailscheduler.domain.recipient.Recipient;
import com.mailscheduler.domain.recipient.RecipientId;
import com.mailscheduler.domain.template.Template;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class Email {
    private final EmailId id;
    private final EmailAddress sender;
    private EmailAddress recipient;
    private RecipientId recipientId;
    private final Subject subject;
    private final Body body;
    private EmailStatus status;
    private final ZonedDateTime scheduledDate;  // TODO: Maybe ScheduledDate class -> validates
    private final EmailCategory emailCategory;
    private final int followupNumber;
    private String threadId;
    private final EmailId initialEmailId;
    private final List<String> attachments;

    // Constructor
    private Email(Builder builder) {
        this.id = builder.id;
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.recipientId = builder.recipientId;
        this.subject = builder.template != null ?
                Subject.of(builder.template.generateSubject()) :
                builder.subject;
        this.body = builder.template != null ?
                Body.of(builder.template.generateBody()) :
                builder.body;
        this.status = builder.status;
        this.scheduledDate = builder.scheduledDate;
        this.emailCategory = builder.category;
        this.followupNumber = builder.followupNumber;
        this.threadId = builder.threadId;
        this.initialEmailId = builder.initialEmailId;
        this.attachments = new ArrayList<>(builder.attachments);
    }

    // Domain behavior methods
    public void schedule() {
        validateForScheduling();
        this.status = EmailStatus.PENDING;
    }

    public void markAsSent(String threadId) {
        if (status != EmailStatus.PENDING) {
            throw new IllegalStateException("Email must be pending to be marked as sent");
        }
        this.status = EmailStatus.SENT;
        this.threadId = threadId;
    }

    public void markAsFailed() {
        this.status = EmailStatus.FAILED;
    }

    public boolean canBeSent() {
        return status.isPending() &&
                scheduledDate.isBefore(ZonedDateTime.now());
    }

    public boolean isFollowUp() {
        return emailCategory == EmailCategory.FOLLOW_UP;
    }

    public boolean isSent() {
        return EmailStatus.SENT.equals(status);
    }

    private void validateForScheduling() {
        if (sender == null) {
            throw new IllegalStateException("Sender must be specified");
        }
        if (recipient == null) {
            throw new IllegalStateException("Recipient must be specified");
        }
        if (scheduledDate == null) {
            throw new IllegalStateException("Scheduled date must be specified");
        }
        if (subject == null || subject.value().isBlank()) {
            throw new IllegalStateException("Subject must not be empty");
        }
        if (body == null || body.value().isBlank()) {
            throw new IllegalStateException("Body must not be empty");
        }
    }

    // Getters
    public EmailId getId() {
        return id;
    }

    public EmailAddress getSender() {
        return sender;
    }

    public EmailAddress getRecipient() {
        return recipient;
    }

    public RecipientId getRecipientId() {
        return recipientId;
    }

    public Subject getSubject() {
        return subject;
    }

    public Body getBody() {
        return body;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public ZonedDateTime getScheduledDate() {
        return scheduledDate;
    }

    public EmailCategory getCategory() {
        return emailCategory;
    }

    public int getFollowupNumber() {
        return followupNumber;
    }

    public Optional<String> getThreadId() {
        if (threadId == null || threadId.isEmpty()) return Optional.empty();
        return Optional.of(threadId);
    }

    public Optional<EmailId> getInitialEmailId() {
        return Optional.ofNullable(initialEmailId);
    }

    public List<String> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    // Setters
    public void setRecipient(EmailAddress recipient) {
        this.recipient = recipient;
    }

    public void setRecipientId(RecipientId recipientId) {
        this.recipientId = recipientId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Email{sender=" + sender + ", recipient=" + recipient + ", subject=" + subject + "}";
    }

    public static class Builder {
        private EmailId id;
        private EmailAddress sender;
        private EmailAddress recipient;
        private RecipientId recipientId;
        private Subject subject;
        private Body body;
        private EmailStatus status = EmailStatus.PENDING;
        private ZonedDateTime scheduledDate;
        private EmailCategory category;
        private int followupNumber = 0;
        private String threadId;
        private EmailId initialEmailId;
        private List<String> attachments = new ArrayList<>();
        private Template template;

        public Builder setId(int id) {
            this.id = EmailId.of(id);
            return this;
        }

        public Builder setSender(EmailAddress sender) {
            this.sender = sender;
            return this;
        }

        public Builder setSender(String sender) {
            this.sender = EmailAddress.of(sender);
            return this;
        }

        public Builder setRecipient(Recipient recipient) {
            this.recipient = recipient.getEmailAddress();
            this.recipientId = recipient.getId();
            return this;
        }

        public Builder setRecipient(String recipient) {
            this.recipient = EmailAddress.of(recipient);
            return this;
        }

        public Builder setRecipient(EmailAddress recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder setRecipientId(int recipientId) {
            this.recipientId = RecipientId.of(recipientId);
            return this;
        }

        public Builder setRecipientId(RecipientId recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = Subject.of(subject);
            return this;
        }

        public Builder setBody(String body) {
            this.body = Body.of(body);
            return this;
        }

        public Builder setTemplate(Template template) {
            this.template = template;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = EmailStatus.valueOf(status);
            return this;
        }

        public Builder setStatus(EmailStatus status) {
            this.status = status;
            return this;
        }

        public Builder setScheduledDate(ZonedDateTime scheduledDate) {
            this.scheduledDate = scheduledDate;
            return this;
        }

        public Builder setCategory(EmailCategory category) {
            this.category = category;
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
            this.initialEmailId = EmailId.of(initialEmailId);
            return this;
        }

        public Builder setInitialEmailId(EmailId initialEmailId) {
            this.initialEmailId = initialEmailId;
            return this;
        }

        public Builder setAttachments(List<String> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Email build() {
            return new Email(this);
        }
    }
}