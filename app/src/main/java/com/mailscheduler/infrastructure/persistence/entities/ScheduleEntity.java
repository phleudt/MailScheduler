package com.mailscheduler.infrastructure.persistence.entities;

public class ScheduleEntity {
    private int id;
    private int followup_number;
    private int interval_days;
    private int schedule_id;
    private String schedule_category;

    public ScheduleEntity(int id, int followup_number, int interval_days, int schedule_id, String schedule_category) {
        this.id = id;
        this.followup_number = followup_number;
        this.interval_days = interval_days;
        this.schedule_id = schedule_id;
        this.schedule_category = schedule_category;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFollowup_number() {
        return followup_number;
    }

    public void setFollowup_number(int followup_number) {
        this.followup_number = followup_number;
    }

    public int getInterval_days() {
        return interval_days;
    }

    public void setInterval_days(int interval_days) {
        this.interval_days = interval_days;
    }

    public int getSchedule_id() {
        return schedule_id;
    }

    public void setSchedule_id(int schedule_id) {
        this.schedule_id = schedule_id;
    }

    public String getSchedule_category() {
        return schedule_category;
    }

    public void setSchedule_category(String schedule_category) {
        this.schedule_category = schedule_category;
    }
}
