package com.mailscheduler.domain.model.email;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.EntityMetadata;
import com.mailscheduler.domain.model.recipient.Recipient;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Metadata associated with an email for tracking its status and scheduling.
 * <p>
 *     This record contains persistence-specific data related to email delivery, scheduling, and relationships
 *     to other entities. It implements the EntityMetadata interface to support the repository pattern.
 * </p>
 */
public record EmailMetadata(
        EntityId<Email> initialEmailId,
        EntityId<Recipient> recipientId,
        int followupNumber,
        EmailStatus status,
        String failureReason,
        LocalDate scheduledDate,
        LocalDate sentDate
) implements EntityMetadata {

    /**
     * Creates validated email metadata.
     *
     * @throws IllegalArgumentException if status is null
     */
    public EmailMetadata {
        Objects.requireNonNull(status, "Email status cannot be null");

        // Validate that failure reason is present if status is FAILED
        if (status == EmailStatus.FAILED && (failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException("Failure reason must be provided for failed emails");
        }

        // Validate that sent date is present if status is SENT
        if (status == EmailStatus.SENT && sentDate == null) {
            throw new IllegalArgumentException("Sent date must be provided for sent emails");
        }

        // Normalize empty reason to null
        if (failureReason != null && failureReason.isBlank()) {
            failureReason = null;
        }
    }

    /**
     * Checks whether this email has failed.
     *
     * @return true if the email status is FAILED
     */
    public boolean hasFailed() {
        return status == EmailStatus.FAILED;
    }

    /**
     * Checks whether this email has been sent.
     *
     * @return true if the email status is SENT
     */
    public boolean hasBeenSent() {
        return status == EmailStatus.SENT;
    }

    /**
     * Checks whether this email is pending to be sent.
     *
     * @return true if the email status is PENDING
     */
    public boolean isPending() {
        return status == EmailStatus.PENDING;
    }

    /**
     * Gets the failure reason if the email has failed.
     *
     * @return an Optional containing the failure reason, or empty if not failed
     */
    public Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    /**
     * Checks if this email is part of a follow-up sequence.
     *
     * @return true if the followupNumber is greater than 0
     */
    public boolean isFollowUp() {
        return followupNumber > 0;
    }

    /**
     * Returns an updated copy of this metadata with the status set to SENT
     * and the sent date set to today.
     *
     * @return A new EmailMetadata with updated sent information
     */
    public EmailMetadata markAsSent() {
        return new EmailMetadata(
                initialEmailId,
                recipientId,
                followupNumber,
                EmailStatus.SENT,
                null,
                scheduledDate,
                LocalDate.now()
        );
    }


    /**
     * Returns an updated copy of this metadata with the status set to FAILED
     * and the provided failure reason.
     *
     * @param reason The reason for the failure
     * @return A new EmailMetadata with updated failure information
     */
    public EmailMetadata markAsFailed(String reason) {
        return new EmailMetadata(
                initialEmailId,
                recipientId,
                followupNumber,
                EmailStatus.FAILED,
                reason,
                scheduledDate,
                null
        );
    }

    /**
     * Returns an updated copy of this metadata with the status set to CANCELLED.
     *
     * @return A new EmailMetadata with updated status
     */
    public EmailMetadata cancel() {
        return new EmailMetadata(
                initialEmailId,
                recipientId,
                followupNumber,
                EmailStatus.CANCELLED,
                null,
                scheduledDate,
                null
        );
    }

    /**
     * Returns an updated copy of this metadata with a new scheduled date.
     *
     * @param newScheduledDate The new date to schedule the email
     * @return A new EmailMetadata with updated scheduling
     */
    public EmailMetadata reschedule(LocalDate newScheduledDate) {
        Objects.requireNonNull(newScheduledDate, "Scheduled date cannot be null");

        if (status != EmailStatus.PENDING) {
            throw new IllegalStateException("Cannot reschedule an email that is not pending");
        }

        return new EmailMetadata(
                initialEmailId,
                recipientId,
                followupNumber,
                status,
                failureReason,
                newScheduledDate,
                sentDate
        );
    }

    /**
     * Builder for creating EmailMetadata instances.
     */
    public static class Builder {
        private EntityId<Email> initialEmailId;
        private EntityId<Recipient> recipientId;
        private int followupNumber;
        private EmailStatus status = EmailStatus.PENDING;
        private String failureReason;
        private LocalDate scheduledDate;
        private LocalDate sentDate;

        /**
         * Sets the ID of the initial email in a sequence.
         *
         * @param initialEmailId The initial email ID
         * @return This builder instance
         */
        public Builder initialEmailId(EntityId<Email> initialEmailId) {
            this.initialEmailId = initialEmailId;
            return this;
        }

        public Builder recipientId(EntityId<Recipient> recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder followupNumber(int followupNumber) {
            this.followupNumber = followupNumber;
            return this;
        }

        public Builder status(EmailStatus status) {
            if (status == null) {
                throw new IllegalArgumentException("status must not be null");
            }

            this.status = Objects.requireNonNull(status, "status must not be null");
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder scheduledDate(LocalDate scheduledDate) {
            this.scheduledDate = scheduledDate;
            return this;
        }

        public Builder sentDate(LocalDate sentDate) {
            this.sentDate = sentDate;
            return this;
        }

        /**
         * Initializes the builder with values from an existing EmailMetadata
         *
         * @param original The original metadata to copy values from
         * @return The builder instance with copied values
         */
        public Builder from(EmailMetadata original) {
            this.initialEmailId = original.initialEmailId;
            this.recipientId = original.recipientId;
            this.followupNumber = original.followupNumber;
            this.status = original.status;
            this.failureReason = original.failureReason;
            this.scheduledDate = original.scheduledDate;
            this.sentDate = original.sentDate;
            return this;
        }

        public EmailMetadata build() {
            return new EmailMetadata(
                    initialEmailId,
                    recipientId,
                    followupNumber,
                    status,
                    failureReason,
                    scheduledDate,
                    sentDate
            );
        }

        /**
         * Creates an initial pending email metadata.
         *
         * @param recipientId The ID of the recipient
         * @param scheduledDate The date when the email is scheduled to be sent
         * @return A new EmailMetadata for an initial email
         */
        public static EmailMetadata createInitial(EntityId<Recipient> recipientId, LocalDate scheduledDate) {
            return new Builder()
                    .recipientId(recipientId)
                    .followupNumber(0)
                    .status(EmailStatus.PENDING)
                    .scheduledDate(scheduledDate)
                    .build();
        }

        /**
         * Creates a follow-up email metadata.
         *
         * @param initialEmailId The ID of the initial email in the sequence
         * @param recipientId The ID of the recipient
         * @param followupNumber The follow-up sequence number
         * @param scheduledDate The date when the follow-up is scheduled to be sent
         * @return A new EmailMetadata for a follow-up email
         */
        public static EmailMetadata createFollowUp(
                EntityId<Email> initialEmailId,
                EntityId<Recipient> recipientId,
                int followupNumber,
                LocalDate scheduledDate) {

            return new Builder()
                    .initialEmailId(initialEmailId)
                    .recipientId(recipientId)
                    .followupNumber(followupNumber)
                    .status(EmailStatus.PENDING)
                    .scheduledDate(scheduledDate)
                    .build();
        }
    }
}
