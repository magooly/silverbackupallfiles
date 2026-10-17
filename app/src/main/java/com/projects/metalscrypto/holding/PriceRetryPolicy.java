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

    private PriceRetryPolicy() {
    }

    static boolean canRequest(boolean inProgress, long now, long lastAttempt, long retryAt) {
        return !inProgress && now >= retryAt && now - lastAttempt >= MIN_INTERVAL_MS;
    }

    static boolean canRequestDaily(boolean inProgress, long now, long lastAttempt, long retryAt, long lastSuccess) {
        if (!canRequest(inProgress, now, lastAttempt, retryAt)) {
            return false;
        }
        if (lastSuccess <= 0) {
            return true;
        }
        Calendar currentDay = Calendar.getInstance();
        currentDay.setTimeInMillis(now);
        Calendar savedDay = Calendar.getInstance();
        savedDay.setTimeInMillis(lastSuccess);
        return currentDay.get(Calendar.ERA) != savedDay.get(Calendar.ERA)
                || currentDay.get(Calendar.YEAR) != savedDay.get(Calendar.YEAR)
                || currentDay.get(Calendar.DAY_OF_YEAR) != savedDay.get(Calendar.DAY_OF_YEAR);
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