package com.mailscheduler.model;

public class ScheduleEntry {
    private int id;
    private int number;
    private int intervalDays;
    private int scheduleId;
    private ScheduleCategory scheduleCategory;

    public ScheduleEntry(int id, int number, int intervalDays, int scheduleId, ScheduleCategory scheduleCategory) {
        this.id = id;
        this.number = number;
        this.intervalDays = intervalDays;
        this.scheduleId = scheduleId;
        this.scheduleCategory = scheduleCategory;
    }

    public ScheduleEntry(int number, int intervalDays, int scheduleId, ScheduleCategory scheduleCategory) {
        // Constructor for when the entries is not from the database
        this.id = -1;
        this.number = number;
        this.intervalDays = intervalDays;
        this.scheduleId = scheduleId;
        this.scheduleCategory = scheduleCategory;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(int intervalDays) {
        this.intervalDays = intervalDays;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public ScheduleCategory getScheduleCategory() {
        return scheduleCategory;
    }

    public void setScheduleCategory(ScheduleCategory scheduleCategory) {
        this.scheduleCategory = scheduleCategory;
    }
}
