package com.mailscheduler.infrastructure.persistence.entity;

public class FollowUpPlanEntity extends TableEntity {
    private String planType;

    public FollowUpPlanEntity(Long id, String followUpPlanType) {
        setId(id);
        this.planType = followUpPlanType;
    }

    public String getPlanType() {
        return planType;
    }
}
