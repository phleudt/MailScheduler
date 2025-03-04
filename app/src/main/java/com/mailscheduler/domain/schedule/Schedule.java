package com.mailscheduler.domain.schedule;

import java.util.*;

public class Schedule implements Iterable<ScheduleEntry> {
    private final ScheduleId id;
    private final List<ScheduleEntry> entries;

    private Schedule(Builder builder) {
        this.id = builder.id;
        this.entries = new ArrayList<>(builder.entries);
        sortEntries();
    }

    private void sortEntries() {
        entries.sort(Comparator.comparing(entry -> entry.getNumber().value()));
    }

    public void validateSequence() {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getNumber().value() != i + 1) {
                throw new IllegalStateException("Schedule entries must be sequential");
            }
        }
    }

    public Optional<ScheduleEntry> getNextEntry(SequenceNumber currentNumber) {
        return entries.stream()
                .filter(entry -> entry.getNumber().value() > currentNumber.value())
                .findFirst();
    }

    // Getters
    public ScheduleId getId() {
        return id;
    }

    public List<ScheduleEntry> getEntries() {
        return entries;
    }

    @Override
    public Iterator<ScheduleEntry> iterator() {
        return entries.iterator();
    }

    public static class Builder {
        private ScheduleId id;
        private final List<ScheduleEntry> entries = new ArrayList<>();

        public Builder setScheduleId(int id) {
            this.id = ScheduleId.of(id);
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
            if (id == null) {
                throw new IllegalStateException("Schedule ID is required");
            }
            return new Schedule(this);
        }
    }
}
