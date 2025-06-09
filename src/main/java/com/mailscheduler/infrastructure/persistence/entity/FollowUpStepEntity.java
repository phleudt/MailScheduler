package com.mailscheduler.infrastructure.persistence.entity;

public class FollowUpStepEntity extends TableEntity {
    private Long planId;
    private int stepNumber;
    private int waitingPeriod;
    private Long templateId;

    public FollowUpStepEntity(Long id, Long planId, int stepNumber, int waitingPeriod, Long templateId) {
        setId(id);
        this.planId = planId;
        this.stepNumber = stepNumber;
        this.waitingPeriod = waitingPeriod;
        this.templateId = templateId;
    }

    // Getters and Setters
    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public int getWaitingPeriod() {
        return waitingPeriod;
    }

    public void setWaitingPeriod(int waitingPeriod) {
        this.waitingPeriod = waitingPeriod;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }
}
