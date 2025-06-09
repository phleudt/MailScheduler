package com.mailscheduler.util;

import java.time.LocalDate;
import java.util.logging.Logger;

public class TimeUtils {
    public static final Logger LOGGER = Logger.getLogger(TimeUtils.class.getName());

    public static LocalDate parseDateToLocalDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            LOGGER.info("Converting " + dateStr + " to ZonedDateTime");
            // date should have format dd.mm.yyyy
            int dayOfMonth = Integer.parseInt(dateStr.substring(0, 2));
            int month = Integer.parseInt(dateStr.substring(3, 5));
            int year = Integer.parseInt(dateStr.substring(6));

            return LocalDate.of(year, month, dayOfMonth);
        } catch (Exception e) {
            LOGGER.severe("Failed to parse date: " + dateStr + " to ZonedDateTime. " + e.getMessage());
            return null;
        }
    }
}

