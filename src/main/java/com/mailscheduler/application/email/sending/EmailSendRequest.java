package com.mailscheduler.application.email.sending;

import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.common.vo.ThreadId;

/**
 * Represents a request to send an email.
 */
public record EmailSendRequest(
        Email email,
        EmailMetadata metadata,
        ThreadId threadId,
        int followUpNumber
) {

    /**
     * Checks if this request is for an initial email.
     */
    public boolean isInitialEmail() {
        return followUpNumber == 0;
    }

    /**
     * Checks if this request is for a follow-up email.
     */
    public boolean isFollowUp() {
        return followUpNumber > 0;
    }

    public static class Builder {
        private Email email;
        private EmailMetadata metadata;
        private ThreadId threadId;
        private int followUpNumber = 0;

        public Builder withEmail(Email email) {
            this.email = email;
            return this;
        }

        public Builder withMetadata(EmailMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder withThreadId(ThreadId threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder withFollowUpNumber(int followUpNumber) {
            this.followUpNumber = followUpNumber;
            return this;
        }


        public EmailSendRequest build() {
            if (email == null) {
                throw new IllegalStateException("Email cannot be null");
            }
            return new EmailSendRequest(email, metadata, threadId, followUpNumber);
        }
    }
}