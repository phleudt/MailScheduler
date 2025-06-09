package com.mailscheduler.domain.model.schedule;

import com.mailscheduler.domain.model.common.vo.RelativePeriod;
import com.mailscheduler.domain.model.template.Template;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Combines a FollowUpStep and its associated Template.
 * <p>
 * This class encapsulates the relationship between a follow-up step and the email template that should be used
 * for that step. It provides methods for accessing both components and calculating scheduling information.
 * </p>
 */
public record PlanStepWithTemplate(FollowUpStep step, Template template) {
    /**
     * Creates a PlanStepWithTemplate with a step and template.
     *
     * @param step     The follow-up step
     * @param template The email template
     * @throws NullPointerException if either parameter is null
     */
    public PlanStepWithTemplate(FollowUpStep step, Template template) {
        this.step = Objects.requireNonNull(step, "Step cannot be null");
        this.template = Objects.requireNonNull(template, "Template cannot be null");
    }

    /**
     * Gets the step number.
     *
     * @return The step number
     */
    public int getStepNumber() {
        return step.getStepNumber();
    }

    /**
     * Gets the wait period for this step.
     *
     * @return The wait period
     */
    public RelativePeriod getWaitPeriod() {
        return step.getWaitPeriod();
    }

    /**
     * Calculates the date when this step should be executed based on a reference date.
     *
     * @param referenceDate The reference date
     * @return The scheduled date for this step
     */
    public LocalDate calculateScheduledDate(LocalDate referenceDate) {
        return step.calculateScheduledDate(referenceDate);
    }

    /**
     * Determines if this is the initial step in a sequence.
     *
     * @return true if this is the first step
     */
    public boolean isInitialStep() {
        return step.isInitialStep();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanStepWithTemplate that)) return false;

        if (!Objects.equals(step, that.step)) return false;
        return Objects.equals(template, that.template);
    }

    @Override
    public String toString() {
        return "PlanStepWithTemplate{" +
                "stepNumber=" + step.getStepNumber() +
                ", waitPeriod=" + step.getWaitPeriod() +
                '}';
    }
}