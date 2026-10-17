package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.util.Calendar;

public class PriceRetryPolicyTest {
    @Test
    public void dailyGateBlocksSameDayAndAllowsNextCalendarDay() {
        Calendar day = Calendar.getInstance();
        day.clear();
        day.set(2026, Calendar.OCTOBER, 17, 12, 0, 0);
        long success = day.getTimeInMillis();
        assertFalse(PriceRetryPolicy.canRequestDaily(false, success + 120_000L, 0L, 0L, success));
        day.add(Calendar.DAY_OF_YEAR, 1);
        assertTrue(PriceRetryPolicy.canRequestDaily(false, day.getTimeInMillis(), 0L, 0L, success));
        assertFalse(PriceRetryPolicy.canRequestDaily(false, day.getTimeInMillis(), 0L,
                day.getTimeInMillis() + 60_000L, success));
        assertTrue(PriceRetryPolicy.canRequestDaily(false, success, 0L, 0L, 0L));
    }

    @Test
    public void dailyGateResetsAtNewYearAndFailedAttemptsStillRespectInterval() {
        Calendar day = Calendar.getInstance();
        day.clear();
        day.set(2026, Calendar.DECEMBER, 31, 23, 58, 0);
        long previousSuccess = day.getTimeInMillis();
        day.add(Calendar.MINUTE, 2);
        long newYear = day.getTimeInMillis();
        assertTrue(PriceRetryPolicy.canRequestDaily(false, newYear, previousSuccess, 0L, previousSuccess));
        assertFalse(PriceRetryPolicy.canRequestDaily(false, newYear, newYear - 1000L, 0L, previousSuccess));
        assertFalse(PriceRetryPolicy.canRequestDaily(true, newYear, 0L, 0L, previousSuccess));
        assertTrue(PriceRetryPolicy.canRequestDaily(false, newYear, 0L, 0L, 0L));
        assertFalse(PriceRetryPolicy.canRequestDaily(false, newYear + 120_000L, 0L, 0L, newYear));
    }

    @Test
    public void blocksOverlappingRapidAndRateLimitedRequests() {
        assertFalse(PriceRetryPolicy.canRequest(true, 120_000L, 0L, 0L));
        assertFalse(PriceRetryPolicy.canRequest(false, 120_000L, 119_000L, 0L));
        assertFalse(PriceRetryPolicy.canRequest(false, 120_000L, 0L, 121_000L));
        assertTrue(PriceRetryPolicy.canRequest(false, 120_000L, 60_000L, 120_000L));
    }

    @Test
    public void respectsRetryAfterSeconds() {
        assertEquals(121_000L, PriceRetryPolicy.retryAt("120", 1000L));
    }

    @Test
    public void enforcesMinimumDelay() {
        assertEquals(61_000L, PriceRetryPolicy.retryAt("0", 1000L));
    }

    @Test
    public void defaultsWhenHeaderIsMissingInvalidOrOverflows() {
        long expected = 1000L + PriceRetryPolicy.DEFAULT_RETRY_MS;
        assertEquals(expected, PriceRetryPolicy.retryAt(null, 1000L));
        assertEquals(expected, PriceRetryPolicy.retryAt("invalid", 1000L));
        assertEquals(expected, PriceRetryPolicy.retryAt("-5", 1000L));
        assertEquals(expected, PriceRetryPolicy.retryAt("9223372036854775807", 1000L));
    }

    @Test
    public void respectsHttpDateAndDoesNotImmediatelyRetryPastDates() {
        assertEquals(1_445_412_480_000L,
                PriceRetryPolicy.retryAt("Wed, 21 Oct 2015 07:28:00 GMT", 1000L));
        assertEquals(1_445_412_600_000L,
                PriceRetryPolicy.retryAt("Wed, 21 Oct 2015 07:28:00 GMT", 1_445_412_540_000L));
    }
}