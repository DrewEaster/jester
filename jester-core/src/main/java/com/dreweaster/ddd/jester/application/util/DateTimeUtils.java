package com.dreweaster.ddd.jester.application.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class DateTimeUtils {

    private static final ZoneId ZONE_ID_UTC = TimeZone.getTimeZone("UTC").toZoneId();

    private static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_INSTANT.withZone(ZONE_ID_UTC);

    public static String zonedDateTimeToUTCString(ZonedDateTime dateTime) {
        return dateTime.format(DATE_FORMAT);
    }

    public static ZonedDateTime zonedDateTimeFromUTCString(String dateTime) {
        return ZonedDateTime.from(DATE_FORMAT.parse(dateTime));
    }

    public static ZonedDateTime utcNow(){return ZonedDateTime.now(ZONE_ID_UTC);
    }

    public static ZonedDateTime applyTimezone(ZonedDateTime dateTime, String timezone) {
        return dateTime.toInstant().atZone(ZoneId.of(timezone));
    }
}
