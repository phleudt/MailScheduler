package com.mailscheduler.domain.model.email;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.vo.email.Subject;

import java.util.Objects;

/**
 * Represents an email in the system with all its required components.
 * <p>
 *     This entity contains the core attributes of an email message: sender, recipient, subject, body, and type.
 *     It extends IdentifiableEntity to support persistence and tracking in the system.
 * </p>
 */
public class Email extends IdentifiableEntity<Email> {
    private final EmailAddress sender;
    private final EmailAddress recipient;
    private final Subject subject;
    private final Body body;
    private final EmailType type;

    /**
     * Creates an email with the specified ID and content.
     *
     * @param id The email entity ID
     * @param sender The sender's email address
     * @param recipient The recipient's email address
     * @param subject The email subject
     * @param body The email body content
     * @param type The type of email
     * @throws NullPointerException if any required parameter is null
     */
    public Email(EntityId<Email> id, EmailAddress sender, EmailAddress recipient, Subject subject, Body body, EmailType type) {
        this.setId(id);
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.type = type;
    }

    /**
     * Private constructor used by the Builder.
     *
     * @param builder The builder containing email values
     */
    private Email(Builder builder) {
        this.setId(builder.id);
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.subject = builder.subject;
        this.body = builder.body;
        this.type = builder.type;
    }

    /**
     * Creates an email without an ID (for new emails not yet persisted).
     *
     * @param sender The sender's email address
     * @param recipient The recipient's email address
     * @param subject The email subject
     * @param body The email body content
     * @param type The type of email
     * @throws NullPointerException if any required parameter is null
     */
    public Email(EmailAddress sender, EmailAddress recipient, Subject subject, Body body, EmailType type) {
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.type = type;
    }

    /**
     * Gets the sender's email address.
     *
     * @return The sender email address
     */
    public EmailAddress getSender() {
        return sender;
    }

    /**
     * Gets the recipient's email address.
     *
     * @return The recipient email address
     */
    public EmailAddress getRecipient() {
        return recipient;
    }

    public Subject getSubject() {
        return subject;
    }

    public Body getBody() {
        return body;
    }

    public EmailType getType() {
        return type;
    }

    /**
     * Checks if this is an initial email.
     *
     * @return true if this is an initial email, false if it's a follow-up
     */
    public boolean isInitialEmail() {
        return type.isInitial();
    }

    /**
     * Checks if this is a follow-up email.
     *
     * @return true if this is a follow-up email
     */
    public boolean isFollowUp() {
        return type.isFollowUp();
    }

    /**
     * Creates a copy of this email with a new recipient.
     *
     * @param newRecipient The new recipient email address
     * @return A new Email with the updated recipient
     */
    public Email withRecipient(EmailAddress newRecipient) {
        return new Email(getId(), sender, newRecipient, subject, body, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        if (!super.equals(o)) return false;

        if (!Objects.equals(sender, email.sender)) return false;
        if (!Objects.equals(recipient, email.recipient)) return false;
        if (!Objects.equals(subject, email.subject)) return false;
        if (!Objects.equals(body, email.body)) return false;
        return type == email.type;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sender != null ? sender.hashCode() : 0);
        result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Email{" +
                "id=" + getId() +
                ", sender=" + sender +
                ", recipient=" + recipient +
                ", subject=" + subject +
                ", type=" + type +
                '}';
    }

    /**
     * Builder for creating Email instances.
     */
    public static class Builder {
        private EntityId<Email> id;
        private EmailAddress sender;
        private EmailAddress recipient;
        private Subject subject;
        private Body body;
        private EmailType type;

        public Builder setId(EntityId<Email> id) {
            this.id = id;
            return this;
        }

        public Builder setSenderEmail(EmailAddress sender) {
            this.sender = sender;
            return this;
        }

        public Builder setRecipientEmail(EmailAddress recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = new Subject(subject);
            return this;
        }

        public Builder setSubject(Subject subject) {
            this.subject = subject;
            return this;
        }

        public Builder setBody(String body) {
            this.body = new Body(body);
            return this;
        }

        public Builder setBody(Body body) {
            this.body = body;
            return this;
        }

        public Builder setType(EmailType type) {
            this.type = type;
            return this;
        }

        /**
         * Initializes the builder with values from an existing Email.
         *
         * @param email The email to copy values from
         * @return This builder instance
         */
        public Builder from(Email email) {
            setId(email.getId());
            this.sender = email.sender;
            this.recipient = email.recipient;
            this.subject = email.subject;
            this.body = email.body;
            this.type = email.type;
            return this;
        }

        public Email build() {
            return new Email(this);
        }
    }
}
