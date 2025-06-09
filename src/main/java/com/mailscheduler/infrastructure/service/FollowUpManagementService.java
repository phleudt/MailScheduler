package com.mailscheduler.infrastructure.service;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpStep;
import com.mailscheduler.domain.model.schedule.FollowUpStepMetadata;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.FollowUpPlanRepository;
import com.mailscheduler.domain.repository.FollowUpStepRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service layer that provides unified management of FollowUpPlans and their associated Steps.
 * Ensures transactional integrity when performing operations that affect both entities.
 */
public class FollowUpManagementService {
    private final FollowUpPlanRepository planRepository;
    private final FollowUpStepRepository stepRepository;

    public FollowUpManagementService(
            FollowUpPlanRepository planRepository,
            FollowUpStepRepository stepRepository
    ) {
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
    }

    /**
     * Creates a new follow-up plan with its steps
     * @param plan The follow-up plan to create
     * @param stepsMetadata Metadata for each step in the plan
     * @return The created plan with its steps
     */
    public FollowUpPlan createPlanWithSteps(FollowUpPlan plan, List<FollowUpStepMetadata> stepsMetadata) {
        // Save the plan first
        FollowUpPlan savedPlan = planRepository.save(plan);

        // Save each step with its metadata
        for (int i = 0; i < plan.getSteps().size(); i++) {
            FollowUpStep step = plan.getSteps().get(i);
            FollowUpStepMetadata updatedMetadata = getFollowUpStepMetadata(stepsMetadata, i, savedPlan);

            stepRepository.saveWithMetadata(step, updatedMetadata);
        }

        return savedPlan;
    }

    private FollowUpStepMetadata getFollowUpStepMetadata(
            List<FollowUpStepMetadata> stepsMetadata,
            int i,
            FollowUpPlan savedPlan
    ) {
        FollowUpStepMetadata metadata;
        if (stepsMetadata == null || stepsMetadata.isEmpty() || stepsMetadata.size() <= i) {
            metadata = null;
        } else {
            metadata = stepsMetadata.get(i);
        }

        // Create new metadata with the saved plan's ID
        return new FollowUpStepMetadata(
                savedPlan.getId(),
                metadata != null ? metadata.templateId() : null
        );
    }

    /**
     * Retrieves a complete follow-up plan including all its steps.
     * @param planId The ID of the plan to retrieve
     * @return The plan with all its steps, or empty if not found
     */
    public Optional<FollowUpPlan> getPlanWithSteps(EntityId<FollowUpPlan> planId) {
        return planRepository.findByIdWithMetadata(planId).map(plan -> {
            List<EntityData<FollowUpStep, FollowUpStepMetadata>> steps =
                    stepRepository.findByPlanIdOrderByFollowUpNumberAsc(planId);

            FollowUpPlan.Builder builder = new FollowUpPlan.Builder()
                    .setId(plan.entity().getId())
                    .setFollowUpPlanType(plan.entity().getPlanType());

            // Add steps in order
            steps.stream()
                    .map(EntityData::entity)
                    .forEach(step -> builder.addStep(step.getWaitPeriod()));

            return builder.build();
        });
    }


    public List<EntityData<FollowUpStep, FollowUpStepMetadata>> getStepsWithMetadata(EntityId<FollowUpPlan> planId) {
        Optional<EntityData<FollowUpPlan, NoMetadata>> plan = planRepository.findByIdWithMetadata(planId);
        if (plan.isEmpty()) throw new RuntimeException("No follow up plan");

        return stepRepository.findByPlanIdOrderByFollowUpNumberAsc(planId);
    }

}
