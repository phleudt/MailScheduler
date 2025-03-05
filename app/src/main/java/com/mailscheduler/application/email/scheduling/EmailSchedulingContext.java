package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.domain.email.Email;
import com.mailscheduler.domain.email.EmailCategory;
import com.mailscheduler.domain.email.EmailId;

import java.util.List;
import java.util.Optional;

public class EmailSchedulingContext {
    public enum SchedulingStatus {
        NO_EMAILS_SCHEDULED,
        PARTIAL_SEQUENCE_SCHEDULED,
        SEQUENCE_COMPLETE,
        NO_SCHEDULING_REQUIRED,
    }

    private final List<Email> existingEmails;
    private final int currentFollowupNumber;
    private final int maxFollowupNumber;
    private final Optional<EmailId> initialEmailId;
    private final Optional<String> threadId;

    public EmailSchedulingContext(
            List<Email> existingEmails,
            int currentFollowupNumber,
            int maxFollowupNumber
    ) {
        this.existingEmails = existingEmails;
        this.currentFollowupNumber = currentFollowupNumber;
        this.maxFollowupNumber = maxFollowupNumber;

        // Find initial email and thread ID from existing emails
        this.initialEmailId = existingEmails.stream()
                .filter(e -> e.getCategory() == EmailCategory.INITIAL)
                .findFirst()
                .map(Email::getId);

        this.threadId = existingEmails.stream()
                .filter(e -> e.getThreadId() != null && e.getThreadId().isPresent())
                .findFirst()
                .map(Email::getThreadId).get();
    }

    public EmailSchedulingContext.SchedulingStatus getSchedulingStatus() {
        if (existingEmails.isEmpty()) {
            return SchedulingStatus.NO_EMAILS_SCHEDULED;
        } else if (isMaxFollowupsReached()) {
            return SchedulingStatus.SEQUENCE_COMPLETE;
        } else {
            return SchedulingStatus.PARTIAL_SEQUENCE_SCHEDULED;
        }
    }

    public Optional<EmailId> getInitialEmailId() {
        return initialEmailId;
    }

    public Optional<String> getThreadId() {
        return threadId;
    }

    public int getCurrentFollowupNumber() {
        return currentFollowupNumber;
    }

    private boolean isMaxFollowupsReached() {
        return currentFollowupNumber >= maxFollowupNumber;
    }
}