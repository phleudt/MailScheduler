package com.mailscheduler.domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.vo.RelativePeriod;

import java.util.*;

/**
 * Entity representing a plan for following up on emails with multiple steps.
 * <p>
 *     A follow-up plan consists of a sequence of steps, each with its own wait period.
 *     This entity provides methods for creating and managing email follow-up sequences.
 * </p>
 */
public class FollowUpPlan extends IdentifiableEntity<FollowUpPlan> {
    private final List<FollowUpStep> steps;
    private final FollowUpPlanType planType;

    /**
     * Private constructor used by the Builder.
     *
     * @param builder The builder containing plan values
     */
    private FollowUpPlan(Builder builder) {
        this.setId(builder.id);
        this.steps = new ArrayList<>(builder.steps);
        this.planType = builder.planType;
    }

    /**
     * Returns an unmodifiable view of the follow-up steps.
     */
    public List<FollowUpStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Gets the next step in the follow-up sequence if available.
     *
     * @param currentStep The index of the current step
     * @return Optional containing the next step, or empty if no more steps exist
     */
    public Optional<FollowUpStep> getNextStep(int currentStep) {
        int nextStep = currentStep + 1;
        return nextStep < steps.size() ? Optional.of(steps.get(nextStep)) : Optional.empty();
    }

    public FollowUpPlanType getPlanType() {
        return planType;
    }

    /**
     * Gets a specific step by its number.
     *
     * @param stepNumber The step number to retrieve
     * @return Optional containing the step, or empty if not found
     */
    public Optional<FollowUpStep> getStepByNumber(int stepNumber) {
        return steps.stream()
                .filter(step -> step.getStepNumber() == stepNumber)
                .findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowUpPlan that)) return false;
        if (!super.equals(o)) return false;

        if (!Objects.equals(steps, that.steps)) return false;
        return planType == that.planType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (steps != null ? steps.hashCode() : 0);
        result = 31 * result + (planType != null ? planType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FollowUpPlan{" +
                "id=" + getId() +
                ", type=" + planType +
                ", steps=" + steps.size() +
                '}';
    }


    /**
     * Builder for creating FollowUpPlan instances.
     */
    public static final class Builder {
        private EntityId<FollowUpPlan> id;
        private final List<FollowUpStep> steps = new ArrayList<>();
        private FollowUpPlanType planType = FollowUpPlanType.DEFAULT_FOLLOW_UP_PLAN;

        public Builder setId(EntityId<FollowUpPlan> id) {
            this.id = id;
            return this;
        }

        public Builder setFollowUpPlanType(FollowUpPlanType planType) {
            this.planType = planType;
            return this;
        }

        public Builder addStep(RelativePeriod waitPeriod) {
            steps.add(new FollowUpStep(steps.size(), waitPeriod));
            return this;
        }

        public FollowUpPlan build() {
            return new FollowUpPlan(this);
        }
    }
}
