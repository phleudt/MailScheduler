package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.email.EmailType;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EmailSchedulingContext {
    public enum SchedulingStatus {
        NO_EMAILS_SCHEDULED,
        PARTIAL_SEQUENCE_SCHEDULED,
        SEQUENCE_COMPLETE,
        NO_SCHEDULING_REQUIRED,
    }

    private final List<EntityData<Email, EmailMetadata>> existingEmailsWithMetadata;
    private final int currentFollowupNumber;
    private final int maxFollowupNumber;
    private final Optional<EntityId<Email>> initialEmailId;

    public EmailSchedulingContext(
            List<EntityData<Email, EmailMetadata>> existingEmailsWithMetadata,
            int maxFollowupNumber
    ) {
        this.existingEmailsWithMetadata = existingEmailsWithMetadata;
        this.currentFollowupNumber = getCurrentFollowupNumber(
                existingEmailsWithMetadata.stream()
                        .map(EntityData::metadata)
                        .toList()
        );
        this.maxFollowupNumber = maxFollowupNumber;

        this.initialEmailId = existingEmailsWithMetadata.stream()
                .filter(e -> e.entity().getType().isInitial())
                .findFirst()
                .map(entity -> entity.entity().getId());
    }

    private int getCurrentFollowupNumber(List<EmailMetadata> metadata) {
        int currentFollowupNumber = 0;
        for (EmailMetadata emailMetadata : metadata) {
            currentFollowupNumber = Math.max(currentFollowupNumber, emailMetadata.followupNumber());
        }
        return currentFollowupNumber;
    }


    public SchedulingStatus getSchedulingStatus() {
        if (existingEmailsWithMetadata.isEmpty()) {
            return SchedulingStatus.NO_EMAILS_SCHEDULED;
        } else if (isMaxFollowupsReached()) {
            return SchedulingStatus.SEQUENCE_COMPLETE;
        } else {
            return SchedulingStatus.PARTIAL_SEQUENCE_SCHEDULED;
        }
    }

    public Optional<EntityId<Email>> getInitialEmailId() {
        return initialEmailId;
    }

    public int getCurrentFollowupNumber() {
        return currentFollowupNumber;
    }

    public int getMaxFollowupNumber() {
        return maxFollowupNumber;
    }

    public LocalDate getLastScheduledDate() {
        EntityData<Email, EmailMetadata> lastEmail = getLastEmail();
        if (lastEmail == null) {
            return null;
        }
        else return lastEmail.metadata().scheduledDate();
    }

    public Optional<EntityData<Email, EmailMetadata>> getInitialEmail() {
        return existingEmailsWithMetadata.stream()
                .filter(emailData -> EmailType.INITIAL.equals(emailData.entity().getType()))
                .findFirst();
    }

    private EntityData<Email, EmailMetadata> getLastEmail() {
        EntityData<Email, EmailMetadata> lastEmail = null;
        for (EntityData<Email, EmailMetadata> entityData : existingEmailsWithMetadata) {
            if (entityData.metadata().followupNumber() == currentFollowupNumber) {
                lastEmail = entityData;
                return lastEmail;
            }
        }
        return lastEmail;
    }

    private boolean isMaxFollowupsReached() {
        return currentFollowupNumber >= maxFollowupNumber;
    }
}