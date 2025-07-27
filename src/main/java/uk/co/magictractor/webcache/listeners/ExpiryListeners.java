/**
 * Copyright 2025 Ken Dobson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.magictractor.webcache.listeners;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.magictractor.webcache.ExternalDataResource;

/**
 *
 */
public final class ExpiryListeners {

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    // A shared LocalDateTime to ensure that when related services use the same expiry listener either all or none get updated
    // (risk of only some if straddling the expiry time).
    private static final LocalDateTime NOW = LocalDateTime.now();

    public static ExpiryListener always() {
        return new ExpiryListener(data -> always(data));
    }

    public static ExpiryListener onHours(int... hourOfDay) {
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> nextHourFrom(lastFetched, hourOfDay)));
    }

    public static ExpiryListener daily() {
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> daily(lastFetched, 0, 0)));
    }

    public static ExpiryListener daily(int hour) {
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> daily(lastFetched, hour, 0)));
    }

    public static ExpiryListener daily(int hour, int minute) {
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> daily(lastFetched, hour, minute)));
    }

    public static ExpiryListener dayOfWeek(DayOfWeek dayOfWeek) {
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> nextDayOfWeek(lastFetched, dayOfWeek, 0, 0)));
    }

    public static ExpiryListener dayOfWeek(DayOfWeek dayOfWeek, int hour) {
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> nextDayOfWeek(lastFetched, dayOfWeek, hour, 0)));
    }

    public static ExpiryListener dayOfWeek(DayOfWeek dayOfWeek, int hour, int minute) {
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> nextDayOfWeek(lastFetched, dayOfWeek, hour, minute)));
    }

    /**
     * <p>
     * Wait a number of days after the last request.
     * </p>
     */
    public static ExpiryListener waitDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException();
        }
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> plusWait(lastFetched, days, 0, 0)));
    }

    /**
     * <p>
     * Wait a number of hours after the last request. For use with websites that
     * support 304 responses.
     * </p>
     * <p>
     * There should always be a wait to ensure tests do not ping external
     * resources.
     * </p>
     */
    public static ExpiryListener waitHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException();
        }
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> plusWait(lastFetched, 0, hours, 0)));
    }

    /**
     * <p>
     * Wait a number of minutes after the last request. For use with websites
     * that support 304 responses.
     * </p>
     * <p>
     * There should always be a wait to ensure tests do not ping external
     * resources. For this reason, a minimum of 10 minutes is imposed.
     * </p>
     */
    public static ExpiryListener waitMinutes(int minutes) {
        if (minutes <= 10) {
            throw new IllegalArgumentException("Minimum wait is 10 minutes");
        }
        return new ExpiryListener(data -> isAfterExpiryDateTime(data, lastFetched -> plusWait(lastFetched, 0, 0, minutes)));
    }

    public static LocalDateTime plusWait(LocalDateTime lastTimestamp, int days, int hours, int minutes) {

        LocalDateTime result = lastTimestamp.plusDays(days).plusHours(hours);

        if (result.getNano() > 0) {
            // Round up to a whole second.
            result = result.plusNanos(1000000 - result.getNano());
        }

        if (result.getSecond() > 0) {
            // Round up to a whole minute.
            result = result.plusSeconds(60 - result.getSecond());
        }

        int roundMinutes = 0;
        if (days > 2) {
            // Round up to whole hours.
            roundMinutes = 60;
        }
        else if (days > 0) {
            // Round up to 15 minutes.
            roundMinutes = 15;
        }
        else if (hours > 2) {
            // Round up to 5 minutes.
            roundMinutes = 15;
        }

        if (roundMinutes > 0) {
            int remainder = result.getMinute() % roundMinutes;
            if (remainder > 0) {
                result = result.plusMinutes(minutes + roundMinutes - remainder);
            }
        }
        else {
            result = result.plusMinutes(minutes);
        }

        // temporary check
        if (result.getNano() != 0 || result.getSecond() != 0) {
            throw new IllegalStateException("Expected second and none to be zero, but nano=" + result.getNano() + " and second=" + result.getSecond());
        }

        return result;
    }

    public static LocalDateTime nextHourFrom(LocalDateTime lastTimestamp, int... hoursOfDay) {

        // Fallback to first hour of next day.
        int nextHour = hoursOfDay[0];
        boolean nextDay = true;

        int lastTimestampHour = lastTimestamp.getHour();
        for (int i = 0; i < hoursOfDay.length; i++) {
            if (lastTimestampHour < hoursOfDay[i]) {
                nextHour = hoursOfDay[i];
                nextDay = false;
                break;
            }
        }

        LocalDateTime next = lastTimestamp
                .withHour(nextHour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        if (nextDay) {
            next = next.plusDays(1);
        }

        return next;
    }

    public static LocalDateTime daily(LocalDateTime lastTimestamp, int hourOfDay, int minuteOfHour) {
        LocalDateTime next = lastTimestamp.withHour(hourOfDay)
                .withMinute(minuteOfHour)
                .withSecond(0)
                .withNano(0);
        if (next.isBefore(lastTimestamp)) {
            next = next.plusDays(1);
        }

        return next;
    }

    public static LocalDateTime nextDayOfWeek(LocalDateTime lastTimestamp, DayOfWeek dayOfWeek, int hour, int minute) {
        LocalDateTime next = daily(lastTimestamp, hour, minute);

        // TODO! remove loop
        while (!dayOfWeek.equals(next.getDayOfWeek())) {
            next = next.plusDays(1);
        }

        return next;
    }

    private static Boolean always(ExternalDataResource dataResource) {
        Logger logger = LoggerFactory.getLogger(dataResource.getClass());
        logger.info("Expiry forced for {}", dataResource.name());

        return true;
    }

    private static Boolean isAfterExpiryDateTime(ExternalDataResource dataResource, Function<LocalDateTime, LocalDateTime> expiryRule) {
        LocalDateTime lastFetched = dataResource.getProperties().getTimestamp().toLocalDateTime();

        if (lastFetched == null) {
            // This shouldn't happen. For a new data resource a fetch should already have been triggered because there's no local cache.
            LoggerFactory.getLogger(dataResource.getClass()).warn("Missing timestamp for {}, so assuming expiry for {}", dataResource.name());

            // Do not use null here otherwise it could appear that there is no listener with a responsibility for checking expiry.
            return true;
        }

        LocalDateTime expiryDateTime = expiryRule.apply(lastFetched);
        boolean expired = expiryDateTime.isBefore(NOW);

        Logger logger = LoggerFactory.getLogger(dataResource.getClass());
        if (logger.isInfoEnabled()) {
            String formattedExpiryDateTime = DATE_FORMATTER.format(expiryDateTime);
            if (expired) {
                logger.info("Expiry {} has passed for {}", formattedExpiryDateTime, dataResource.name());
            }
            else {
                Duration remaining = Duration.between(NOW, expiryDateTime);
                logger.info("Expiry {} (in {}) has not passed for {}", formattedExpiryDateTime, durationDescription(remaining), dataResource.name());
            }
        }

        return expired;
    }

    public static String durationDescription(Duration duration) {
        int seconds = (int) duration.getSeconds();
        if (seconds == 1) {
            return "1 second";
        }
        if (seconds < 60) {
            return seconds + " seconds";
        }

        int minutes = (seconds + 59) / 60;
        if (minutes == 1) {
            return "1 minute";
        }
        if (minutes < 60) {
            return minutes + " minutes";
        }

        if (minutes < 12 * 60) {
            int hours = minutes / 60;
            minutes = minutes - (hours * 60);
            // return hours + " hours and " + minutes + " minutes";
            StringBuilder sb = new StringBuilder();
            sb.append(hours);
            if (hours == 1) {
                sb.append(" hour and ");
            }
            else {
                sb.append(" hours and ");
            }

            sb.append(minutes);
            if (minutes == 1) {
                sb.append(" minute");
            }
            else {
                sb.append(" minutes");
            }

            return sb.toString();
        }

        int hours = (minutes + 59) / 60;
        return hours + " hours";

        // TODO! and days...
    }

    private ExpiryListeners() {
    }

}
