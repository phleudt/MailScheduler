package com.mailscheduler.domain.schedule;

public class ScheduleEntry {
    private final EntryId id;
    private final SequenceNumber number;
    private final IntervalDays intervalDays;
    private final ScheduleId scheduleId;
    private final ScheduleCategory category;

    private ScheduleEntry(Builder builder) {
        this.id = builder.id;
        this.number = builder.number;
        this.intervalDays = builder.intervalDays;
        this.scheduleId = builder.scheduleId;
        this.category = builder.category;
    }

    // Getters
    public EntryId getId() {
        return id;
    }

    public SequenceNumber getNumber() {
        return number;
    }

    public IntervalDays getIntervalDays() {
        return intervalDays;
    }

    public ScheduleId getScheduleId() {
        return scheduleId;
    }

    public ScheduleCategory getCategory() {
        return category;
    }

    public static class Builder {
        private EntryId id;
        private SequenceNumber number;
        private IntervalDays intervalDays;
        private ScheduleId scheduleId;
        private ScheduleCategory category;

        public Builder setId(int id) {
            this.id = EntryId.of(id);
            return this;
        }

        public Builder setNumber(int number) {
            this.number = SequenceNumber.of(number);
            return this;
        }

        public Builder setIntervalDays(int days) {
            this.intervalDays = IntervalDays.of(days);
            return this;
        }

        public Builder setScheduleId(int scheduleId) {
            this.scheduleId = ScheduleId.of(scheduleId);
            return this;
        }

        public Builder setCategory(ScheduleCategory category) {
            this.category = category;
            return this;
        }

        public ScheduleEntry build() {
            validate();
            return new ScheduleEntry(this);
        }

        private void validate() {
            if (number == null) {
                throw new IllegalStateException("Entry number is required");
            }
            if (intervalDays == null) {
                throw new IllegalStateException("Interval days is required");
            }
            if (scheduleId == null) {
                throw new IllegalStateException("Schedule ID is required");
            }
            if (category == null) {
                throw new IllegalStateException("Category is required");
            }
        }
    }
}
