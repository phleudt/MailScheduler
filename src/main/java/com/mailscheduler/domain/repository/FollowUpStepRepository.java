package com.mailscheduler.domain.repository;

import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpStep;
import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.schedule.FollowUpStepMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing FollowUpStep entities.
 * <p>
 *     FollowUpSteps represent individual steps in a follow-up email sequence, each with a specific
 *     wait period and potentially different content.
 * </p>
 */
public interface FollowUpStepRepository extends Repository<FollowUpStep, FollowUpStepMetadata> {
    /**
     * Lists all steps for a given plan, ordered by followupNumber asc.
     */
    List<EntityData<FollowUpStep, FollowUpStepMetadata>> findByPlanIdOrderByFollowUpNumberAsc(EntityId<FollowUpPlan> planId);

    /**
     * Finds the step for a given plan and follow-up number (1st follow-up, 2nd, etc).
     */
    Optional<EntityData<FollowUpStep, FollowUpStepMetadata>> findByPlanIdAndFollowupNumber(EntityId<FollowUpPlan> planId, int followupNumber);

    /**
     * Deletes all steps belonging to a given plan (cascade on plan deletion).
     */
    void deleteByPlanId(EntityId<FollowUpPlan> planId);
}
