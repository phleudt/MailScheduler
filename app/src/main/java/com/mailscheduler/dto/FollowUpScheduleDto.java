package com.mailscheduler.dto;

import com.mailscheduler.model.ScheduleCategory;

public final class FollowUpScheduleDto {
    private final int id;
    private final int followupNumber;
    private final int intervalDays;
    private final int scheduleId;
    private final ScheduleCategory scheduleCategory;

    public FollowUpScheduleDto(int id, int followupNumber, int intervalDays, int scheduleId, ScheduleCategory scheduleCategory) {
        this.id = id;
        this.followupNumber = followupNumber;
        this.intervalDays = intervalDays;
        this.scheduleId = scheduleId;
        this.scheduleCategory = scheduleCategory;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public int getFollowupNumber() {
        return followupNumber;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public ScheduleCategory getScheduleCategory() {
        return scheduleCategory;
    }
}
