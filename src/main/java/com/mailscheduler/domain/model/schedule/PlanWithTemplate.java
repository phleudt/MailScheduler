package com.mailscheduler.domain.model.schedule;

import java.util.*;

/**
 * Represents a complete follow-up plan with associated email templates.
 * <p>
 *     This class combines a FollowUpPlan with its associated email templates,
 *     providing a complete definition of an email communication sequence. It
 *     contains methods for accessing the steps and calculating scheduling.
 * </p>
 */
public class PlanWithTemplate {
    private final List<PlanStepWithTemplate> stepsWithTemplates;

    public PlanWithTemplate() {
        this.stepsWithTemplates = new ArrayList<>();
    }

    public PlanWithTemplate(List<PlanStepWithTemplate> stepsWithTemplates) {
        this.stepsWithTemplates = stepsWithTemplates;
    }

    public void addStep(PlanStepWithTemplate step) {
        stepsWithTemplates.add(step);
    }

    /**
     * Gets all steps in this plan.
     *
     * @return An unmodifiable list of steps with templates
     */
    public List<PlanStepWithTemplate> getStepsWithTemplates() {
        return Collections.unmodifiableList(stepsWithTemplates);
    }

    /**
     * Gets the next step after the specified current step.
     *
     * @param currentStep The index of the current step
     * @return Optional containing the next step, or empty if no more steps exist
     */
    public Optional<PlanStepWithTemplate> getNextStep(int currentStep) {
        int nextStep = currentStep + 1;
        return nextStep < stepsWithTemplates.size() ? Optional.of(stepsWithTemplates.get(nextStep)) : Optional.empty();
    }

    /**
     * Gets the number of follow-up steps (excluding the initial step).
     *
     * @return The number of follow-up steps
     */
    public int getFollowUpCount() {
        return Math.max(0, stepsWithTemplates.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanWithTemplate that)) return false;

        return Objects.equals(stepsWithTemplates, that.stepsWithTemplates);
    }

    @Override
    public int hashCode() {
        return stepsWithTemplates != null ? stepsWithTemplates.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PlanWithTemplate{" +
                "steps=" + stepsWithTemplates.size() +
                '}';
    }
}

