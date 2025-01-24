package com.mailscheduler.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Schedule {
    private int scheduleId;
    private List<ScheduleEntry> entries;

    private Schedule(Builder builder) {
        this.scheduleId = builder.scheduleId;
        this.entries = builder.entries;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public List<ScheduleEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ScheduleEntry> entries) {
        this.entries = entries;
    }

    public static class Builder {
        private int scheduleId;
        private List<ScheduleEntry> entries = new ArrayList<>();

        public Builder setScheduleId(int scheduleId) {
            this.scheduleId = scheduleId;
            return this;
        }

        public Builder addEntry(ScheduleEntry entry) {
            this.entries.add(entry);
            return this;
        }

        public Builder addEntries(List<ScheduleEntry> entries) {
            this.entries.addAll(entries);
            return this;
        }

        public Schedule build() {
            sortScheduleEntries();
            return new Schedule(this);
        }

        private void sortScheduleEntries() {
            entries.sort(Comparator.comparingInt(ScheduleEntry::getNumber));
        }
    }
}
