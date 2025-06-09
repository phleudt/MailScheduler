package com.mailscheduler.domain.model.recipient;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.EntityMetadata;
import com.mailscheduler.domain.model.common.vo.ThreadId;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;

import java.util.Objects;
import java.util.Optional;

/**
 * Metadata associated with a recipient for tracking and communication threads.
 * <p>
 *     This record contains persistence-specific data that links a recipient to its contact, follow-up plan,
 *     and email thread. It implements the EntityMetadata interface to support the repository pattern.
 * </p>
 */
public record RecipientMetadata(
        EntityId<Contact> contactId,
        EntityId<FollowUpPlan> followupPlanId,
        ThreadId threadId
) implements EntityMetadata {

    /**
     * Creates validated recipient metadata.
     *
     * @throws NullPointerException if contactId is null
     */
    public RecipientMetadata {
        Objects.requireNonNull(contactId, "Contact ID cannot be null");
        // Other fields can be null depending on the recipient's state
    }

    /**
     * Gets the optional follow-up plan ID.
     *
     * @return an Optional containing the follow-up plan ID, or empty if none exists
     */
    public Optional<EntityId<FollowUpPlan>> getFollowupPlanId() {
        return Optional.ofNullable(followupPlanId);
    }

    /**
     * Gets the optional thread ID.
     *
     * @return an Optional containing the thread ID, or empty if none exists
     */
    public Optional<ThreadId> getThreadId() {
        return Optional.ofNullable(threadId);
    }

    /**
     * Creates a copy of this metadata with a new thread ID.
     *
     * @param threadId The new thread ID
     * @return A new RecipientMetadata with the updated thread ID
     */
    public RecipientMetadata withThreadId(ThreadId threadId) {
        return new RecipientMetadata(contactId, followupPlanId, threadId);
    }

    @Override
    public String toString() {
        return "RecipientMetadata{" +
                "contactId=" + contactId +
                ", followupPlanId=" + followupPlanId +
                ", threadId=" + threadId +
                '}';
    }

    /**
     * Builder for creating RecipientMetadata instances.
     */
    public static class Builder {
        private EntityId<Contact> contactId;
        private EntityId<FollowUpPlan> followupPlanId;
        private ThreadId threadId;

        public Builder contactId(EntityId<Contact> contactId) {
            this.contactId = contactId;
            return this;
        }

        public Builder followupPlanId(EntityId<FollowUpPlan> followupPlanId) {
            this.followupPlanId = followupPlanId;
            return this;
        }

        public Builder threadId(ThreadId threadId) {
            this.threadId = threadId;
            return this;
        }

        /**
         * Initializes the builder with values from existing metadata.
         *
         * @param metadata The metadata to copy values from
         * @return A new builder with copied values
         */
        public static Builder from(RecipientMetadata metadata) {
            return new Builder()
                    .contactId(metadata.contactId)
                    .followupPlanId(metadata.followupPlanId)
                    .threadId(metadata.threadId);
        }

        public RecipientMetadata build() {
            return new RecipientMetadata(contactId, followupPlanId, threadId);
        }
    }
}
