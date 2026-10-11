package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

public class BackupNonDetailedAccumulatorTest {

    @Test
    public void add_aggregatesTotalsByNormalizedType() {
        BackupNonDetailedAccumulator accumulator = new BackupNonDetailedAccumulator();

        accumulator.add("btc", "1.25", "Amount", "01-01-2026", "01:00:00 AM");
        accumulator.add("Bitcoin", "2", "Amount", "02-01-2026", "02:00:00 AM");
        accumulator.add("ETH", "3", "Amount", "03-01-2026", "03:00:00 AM");

        Map<String, BackupNonDetailedAccumulator.Summary> byType = accumulator.getByType();
        assertEquals(2, byType.size());
        assertEquals(0, byType.get("Bitcoin").total.compareTo(new BigDecimal("3.25")));
        assertEquals(0, byType.get("Ethereum").total.compareTo(new BigDecimal("3")));
    }

    @Test
    public void add_ignoresInvalidTotals() {
        BackupNonDetailedAccumulator accumulator = new BackupNonDetailedAccumulator();
        accumulator.add("Bitcoin", "abc", "Amount", "01-01-2026", "01:00:00 AM");
        accumulator.add("Bitcoin", "1,000", "Amount", "01-01-2026", "01:00:00 AM");

        assertEquals(0, accumulator.getByType().get("Bitcoin").total.compareTo(new BigDecimal("1000")));
    }
}
