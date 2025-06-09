package com.mailscheduler.domain.factory;

import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpPlanType;
import com.mailscheduler.domain.model.common.vo.RelativePeriod;

import java.util.List;
import java.util.Objects;

/**
 * Factory for creating FollowUpPlan entities.
 * <p>
 *     This factory provides methods for creating various types of follow-up plans with different
 *     waiting period patterns for email sequences.
 * </p>
 */
public class FollowUpPlanFactory {

    /**
     * Creates a default follow-up plan with the specified waiting periods in days.
     *
     * @param waitingPeriods List of waiting periods in days between follow-up emails
     * @return A new FollowUpPlan instance with the specified waiting periods
     * @throws IllegalArgumentException If the waiting periods are invalid
     */
    public static FollowUpPlan createDefaultPlan(List<Integer> waitingPeriods) {
        validateInput(waitingPeriods);

        FollowUpPlan.Builder builder = new FollowUpPlan.Builder();
        builder.setFollowUpPlanType(FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN);
        for (Integer waitingPeriod : waitingPeriods) {
            builder.addStep(RelativePeriod.ofDays(waitingPeriod));
        }
        return builder.build();
    }

    /**
     * Creates a custom follow-up plan with the specified waiting periods in days.
     *
     * @param waitingPeriods List of waiting periods in days between follow-up emails
     * @return A new FollowUpPlan instance with the specified waiting periods
     * @throws IllegalArgumentException If the waiting periods are invalid
     */
    public static FollowUpPlan createCustomPlan(List<Integer> waitingPeriods) {
        validateInput(waitingPeriods);

        FollowUpPlan.Builder builder = new FollowUpPlan.Builder();
        builder.setFollowUpPlanType(FollowUpPlanType.FOLLOW_UP_PLAN);
        for (Integer waitingPeriod : waitingPeriods) {
            builder.addStep(RelativePeriod.ofDays(waitingPeriod));
        }
        return builder.build();
    }

    private static void validateInput(List<Integer> waitingPeriods) {
        if (waitingPeriods == null || waitingPeriods.isEmpty()) {
            throw new IllegalArgumentException("Waiting periods list cannot be null or empty");
        }
        if (waitingPeriods.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Waiting periods cannot contain null values");
        }
        if (waitingPeriods.stream().anyMatch(days -> days < 0)) {
            throw new IllegalArgumentException("Waiting periods cannot be negative");
        }
    }
}