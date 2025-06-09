package com.mailscheduler.domain.model.schedule;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import com.mailscheduler.domain.model.common.vo.RelativePeriod;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing a single step in a follow-up email sequence.
 * <p>
 *     Each step defines when a follow-up email should be sent relative to the previous email or initial contact.
 *     Steps are ordered by number within a FollowUpPlan.
 * </p>
 */
public class FollowUpStep extends IdentifiableEntity<FollowUpStep> {
    private final int stepNumber;
    private final RelativePeriod waitPeriod;

    /**
     * Creates a follow-up step with a step number and wait period.
     *
     * @param stepNumber The sequence number of this step in a plan
     * @param waitPeriod The period to wait after the previous step
     * @throws IllegalArgumentException if stepNumber is negative or waitPeriod is null
     */
    public FollowUpStep(int stepNumber, RelativePeriod waitPeriod) {
        if (stepNumber < 0) {
            throw new IllegalArgumentException("Step number cannot be negative");
        }
        Objects.requireNonNull(waitPeriod, "Wait period cannot be null");

        this.stepNumber = stepNumber;
        this.waitPeriod = waitPeriod;
    }

    /**
     * Creates a follow-up step with an ID, step number and wait period.
     *
     * @param id The entity ID
     * @param stepNumber The sequence number of this step in a plan
     * @param waitPeriod The period to wait after the previous step
     * @throws IllegalArgumentException if stepNumber is negative or waitPeriod is null
     */
    public FollowUpStep(EntityId<FollowUpStep> id, int stepNumber, RelativePeriod waitPeriod) {
        this(stepNumber, waitPeriod);
        setId(id);
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public RelativePeriod getWaitPeriod() {
        return waitPeriod;
    }

    public int getWaitPeriodInDays() {
        return waitPeriod.toDays();
    }


    /**
     * Calculates the date when this step should be executed based on a reference date.
     *
     * @param referenceDate The reference date (e.g., the date of the previous step)
     * @return The scheduled date for this step
     */
    public LocalDate calculateScheduledDate(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "Reference date cannot be null");
        return waitPeriod.addTo(referenceDate);
    }

    /**
     * Determines if this is the initial step (step number 0).
     *
     * @return true if this is the first step
     */
    public boolean isInitialStep() {
        return stepNumber == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowUpStep step)) return false;
        if (!super.equals(o)) return false;

        if (stepNumber != step.stepNumber) return false;
        return Objects.equals(waitPeriod, step.waitPeriod);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + stepNumber;
        result = 31 * result + (waitPeriod != null ? waitPeriod.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FollowUpStep{" +
                "id=" + getId() +
                ", stepNumber=" + stepNumber +
                ", waitPeriod=" + waitPeriod +
                '}';
    }

    /**
     * Builder for creating FollowUpStep instances.
     */
    public static class Builder {
        private EntityId<FollowUpStep> id;
        private int stepNumber;
        private RelativePeriod waitPeriod;

        /**
         * Sets the entity ID.
         *
         * @param id The entity ID
         * @return This builder instance
         */
        public Builder id(EntityId<FollowUpStep> id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the step number.
         *
         * @param stepNumber The step number
         * @return This builder instance
         */
        public Builder stepNumber(int stepNumber) {
            this.stepNumber = stepNumber;
            return this;
        }

        /**
         * Sets the wait period.
         *
         * @param waitPeriod The wait period
         * @return This builder instance
         */
        public Builder waitPeriod(RelativePeriod waitPeriod) {
            this.waitPeriod = waitPeriod;
            return this;
        }

        /**
         * Builds a new FollowUpStep with the current builder values.
         *
         * @return A new FollowUpStep instance
         * @throws IllegalArgumentException if validation fails
         */
        public FollowUpStep build() {
            FollowUpStep step = new FollowUpStep(stepNumber, waitPeriod);
            if (id != null) {
                step.setId(id);
            }
            return step;
        }
    }
}