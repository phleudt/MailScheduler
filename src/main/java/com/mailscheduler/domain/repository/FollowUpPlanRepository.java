package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpPlanType;

import java.util.Optional;

/**
 * Repository interface for managing FollowUpPlan entities.
 * <p>
 *     A FollowUpPlan defines a sequence of email follow-ups, including their timing and content.
 *     Plans can be of different types (e.g., default plans, custom plans).
 * </p>
 */
public interface FollowUpPlanRepository extends Repository<FollowUpPlan, NoMetadata> {
    /**
     * Finds a plan by its follow-up plan type.
     *
     * @param followUpPlanType The type of the follow-up plan (e.g., DEFAULT_FOLLOW_UP_PLAN vs. FOLLOW_UP_PLAN)
     * @return An Optional containing the plan if found, empty otherwise
     */
    Optional<FollowUpPlan> findByFollowUpPlanType(FollowUpPlanType followUpPlanType);

    /**
     * Returns the default follow-up plan.
     */
    FollowUpPlan findAllDefaults();

    /**
     * Saves (creates or updates) a follow-up plan.
     *
     * @param plan The follow-up plan to save
     * @return The saved follow-up plan
     */
    FollowUpPlan save(FollowUpPlan plan);
}
