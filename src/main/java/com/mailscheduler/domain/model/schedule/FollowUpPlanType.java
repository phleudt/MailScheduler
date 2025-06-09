package com.mailscheduler.domain.model.schedule;

/**
 * Enumerates the different types of follow-up plans.
 * <p>
 *     This enum defines the categories of follow-up plans that can be created,
 *     distinguishing between default system plans and user-created plans.
 * </p>
 */
public enum FollowUpPlanType {
    /**
     * The default system-provided follow-up plan.
     */
    DEFAULT_FOLLOW_UP_PLAN,

    /**
     * A custom user-created follow-up plan.
     */
    FOLLOW_UP_PLAN;

    /**
     * Checks if this plan type represents a default system plan.
     *
     * @return true if this is a default system plan
     */
    public boolean isDefault() {
        return this == DEFAULT_FOLLOW_UP_PLAN;
    }

    /**
     * Checks if this plan type represents a custom user plan.
     *
     * @return true if this is a custom user plan
     */
    public boolean isCustom() {
        return this == FOLLOW_UP_PLAN;
    }
}
