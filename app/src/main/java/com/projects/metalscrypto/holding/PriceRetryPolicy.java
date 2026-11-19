package com.projects.metalscrypto.holding;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

final class PriceRetryPolicy {
    static final long MIN_INTERVAL_MS = 60_000L;
    static final long DEFAULT_RETRY_MS = 15 * 60_000L;
    static final int DAILY_SUCCESS_LIMIT = 4;

    private PriceRetryPolicy() {
    }

    static boolean canRequest(boolean inProgress, long now, long lastAttempt, long retryAt) {
        return !inProgress && now >= retryAt && now - lastAttempt >= MIN_INTERVAL_MS;
    }

    static boolean canRequestDaily(boolean inProgress, long now, long lastAttempt, long retryAt, long lastSuccess) {
        return canRequestDaily(inProgress, now, lastAttempt, retryAt, lastSuccess,
                successfulUpdatesToday(now, lastSuccess, 0));
    }

    static boolean canRequestDaily(boolean inProgress, long now, long lastAttempt, long retryAt, long lastSuccess,
            int savedSuccessCount) {
        if (!canRequest(inProgress, now, lastAttempt, retryAt)) {
            return false;
        }
        return successfulUpdatesToday(now, lastSuccess, savedSuccessCount) < DAILY_SUCCESS_LIMIT;
    }

    static int successfulUpdatesToday(long now, long lastSuccess, int savedSuccessCount) {
        if (lastSuccess <= 0) {
            return 0;
        }
        if (!isSameCalendarDay(now, lastSuccess)) {
            return 0;
        }
        return Math.max(savedSuccessCount, 1);
    }

    private static boolean isSameCalendarDay(long first, long second) {
        Calendar firstDay = Calendar.getInstance();
        firstDay.setTimeInMillis(first);
        Calendar secondDay = Calendar.getInstance();
        secondDay.setTimeInMillis(second);
        return firstDay.get(Calendar.ERA) == secondDay.get(Calendar.ERA)
                && firstDay.get(Calendar.YEAR) == secondDay.get(Calendar.YEAR)
                && firstDay.get(Calendar.DAY_OF_YEAR) == secondDay.get(Calendar.DAY_OF_YEAR);
    }

    static long retryAt(String retryAfter, long now) {
        if (retryAfter != null) {
            String value = retryAfter.trim();
            if (value.matches("[0-9]+")) {
                try {
                    long seconds = Long.parseLong(value);
                    if (seconds > (Long.MAX_VALUE - now) / 1000L) {
                        return now + DEFAULT_RETRY_MS;
                    }
                    long delay = Math.max(MIN_INTERVAL_MS, seconds * 1000L);
                    return now + delay;
                } catch (NumberFormatException exception) {
                    return now + DEFAULT_RETRY_MS;
                }
            }
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date parsed = format.parse(value, position);
            if (parsed != null && position.getIndex() == value.length()) {
                return Math.max(now + MIN_INTERVAL_MS, parsed.getTime());
            }
        }
        return now + DEFAULT_RETRY_MS;
    }
}