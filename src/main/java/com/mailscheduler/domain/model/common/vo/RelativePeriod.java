package com.mailscheduler.domain.model.common.vo;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

/**
 * Value object representing a relative time period.
 * <p>
 *     This class wraps Java's TemporalAmount to represent periods used for scheduling follow-up emails.
 *     It provides convenience methods for creating common periods and calculating future dates based on those periods.
 * </p>
 */
public record RelativePeriod(TemporalAmount amount) {
    /**
     * Creates a validated relative period.
     *
     * @throws IllegalArgumentException if the amount is null
     */
    public RelativePeriod {
        Objects.requireNonNull(amount, "Period amount cannot be null");
    }

    /**
     * Creates a RelativePeriod representing a number of days.
     *
     * @param days The number of days
     * @return A new RelativePeriod
     * @throws IllegalArgumentException if days is negative
     */
    public static RelativePeriod ofDays(int days) {
        return new RelativePeriod(Period.ofDays(days));
    }

    /**
     * Converts this period to number of days.
     *
     * @return The number of days in this period
     * @throws UnsupportedOperationException if the period is not a Period instance
     */
    public int toDays() {
        if (amount instanceof Period period) {
            return period.getDays() + period.getMonths() * 30 + period.getYears() * 365;
        }
        throw new UnsupportedOperationException("Cannot convert non-Period TemporalAmount to days");
    }

    /**
     * Calculates a future date by adding this period to a base date.
     *
     * @param baseDate The date to add this period to
     * @return The future date
     */
    public LocalDate addTo(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "Base date cannot be null");
        return baseDate.plus(amount);
    }

    /**
     * Checks if this period is zero (no time).
     *
     * @return true if this period represents zero time
     */
    public boolean isZero() {
        if (amount instanceof Period period) {
            return period.isZero();
        }
        return false;
    }

    @Override
    public String toString() {
        if (amount instanceof Period period) {
            if (period.isZero()) {
                return "0 days";
            }

            StringBuilder result = new StringBuilder();
            if (period.getDays() > 0) {
                if (!result.isEmpty()) result.append(", ");
                result.append(period.getDays()).append(period.getDays() == 1 ? " day" : " days");
            }
            return result.toString();
        }
        return amount.toString();
    }
}