package com.mailscheduler.domain.schedule;

/**
 * Represents the position of an entry within a schedule sequence.
 * The sequence starts at 1 for the first entry and increases sequentially.
 */
public record SequenceNumber(int value) {
    public SequenceNumber {
        if (value < 1) {
            throw new IllegalArgumentException("Sequence number must be at least 1");
        }
    }

    public static SequenceNumber first() {
        return new SequenceNumber(1);
    }

    public static SequenceNumber of(int value) {
        return new SequenceNumber(value);
    }

    public SequenceNumber next() {
        return new SequenceNumber(value + 1);
    }

    public boolean isFirst() {
        return value == 1;
    }

    public boolean comesAfter(SequenceNumber other) {
        return this.value > other.value;
    }

    public boolean comesBefore(SequenceNumber other) {
        return this.value < other.value;
    }
}
