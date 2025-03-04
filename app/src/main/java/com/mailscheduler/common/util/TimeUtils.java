package com.mailscheduler.common.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

public class TimeUtils {
    public static final Logger LOGGER = Logger.getLogger(TimeUtils.class.getName());

    public static ZonedDateTime parseDateToZonedDateTime(String dateStr) {
        if (dateStr == null) return null;
        try {
            LOGGER.info("Converting " + dateStr + " to ZonedDateTime");
            // date should have format dd.mm.yyyy
            int dayOfMonth = Integer.parseInt(dateStr.substring(0, 2));
            int month = Integer.parseInt(dateStr.substring(3, 5));
            int year = Integer.parseInt(dateStr.substring(6));

            return ZonedDateTime.of(year, month, dayOfMonth, 12, 0, 0, 0, ZoneId.systemDefault());
        } catch (Exception e) {
            LOGGER.severe("Failed to parse date: " + dateStr + " to ZonedDateTime. " + e.getMessage());
            return null;
        }
    }

    public static boolean isPastDate(ZonedDateTime date) {
        if (date == null) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now();
        return date.isBefore(now);
    }
}
