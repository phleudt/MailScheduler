package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.domain.email.Email;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects and aggregates email scheduling results.
 * Responsible for collecting initial and follow-up emails during the scheduling process.
 */
public class EmailSchedulingResultCollector {
    private final List<Email> initialEmails;
    private final List<Email> followupEmails;

    /**
     * Creates a new EmailSchedulingResultCollector with empty result lists.
     */
    public EmailSchedulingResultCollector() {
        this.initialEmails = new ArrayList<>();
        this.followupEmails = new ArrayList<>();
    }

    /**
     * Adds the results from a single recipient's email scheduling operation.
     *
     * @param result The scheduling result for a single recipient
     * @throws NullPointerException if result is null
     */
    public void addResult(EmailSchedulingService.ScheduledRecipientResult result) {
        Objects.requireNonNull(result, "ScheduledRecipientResult cannot be null");

        if (result.initialEmails() != null) {
            initialEmails.addAll(result.initialEmails());
        }
        if (result.followupEmails() != null) {
            followupEmails.addAll(result.followupEmails());
        }
    }

    /**
     * Adds multiple scheduling results at once.
     *
     * @param results List of scheduling results to add
     * @throws NullPointerException if results list is null
     */
    public void addResults(List<EmailSchedulingService.ScheduledRecipientResult> results) {
        Objects.requireNonNull(results, "Results list cannot be null");
        results.forEach(this::addResult);
    }

    /**
     * Creates and returns the final ScheduledEmailsResult.
     *
     * @return A new ScheduledEmailsResult containing all collected emails
     */
    public EmailSchedulingService.ScheduledEmailsResult getResult() {
        return new EmailSchedulingService.ScheduledEmailsResult(
                new ArrayList<>(initialEmails),
                new ArrayList<>(followupEmails)
        );
    }

    /**
     * Returns the current count of initial emails.
     *
     * @return The number of initial emails collected
     */
    public int getInitialEmailCount() {
        return initialEmails.size();
    }

    /**
     * Returns the current count of follow-up emails.
     *
     * @return The number of follow-up emails collected
     */
    public int getFollowupEmailCount() {
        return followupEmails.size();
    }

    /**
     * Checks if no emails have been collected.
     *
     * @return true if both initial and follow-up email lists are empty
     */
    public boolean isEmpty() {
        return initialEmails.isEmpty() && followupEmails.isEmpty();
    }

    /**
     * Clears all collected results.
     */
    public void clear() {
        initialEmails.clear();
        followupEmails.clear();
    }

    /**
     * Returns the total number of emails collected.
     *
     * @return The sum of initial and follow-up emails
     */
    public int getTotalEmailCount() {
        return initialEmails.size() + followupEmails.size();
    }

    @Override
    public String toString() {
        return String.format("EmailSchedulingResultCollector{initialEmails=%d, followupEmails=%d}",
                getInitialEmailCount(), getFollowupEmailCount());
    }
}