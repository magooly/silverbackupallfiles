package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;

import com.projects.metalscrypto.holding.DBManager.HoldingItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HoldingSummaryHelperTest {

    @Test
    public void calculate_returnsOrderedTotalsAndTypeSums() {
        List<HoldingItem> items = new ArrayList<>();
        items.add(item("Ag", "10.5"));
        items.add(item("Gold", "3"));
        items.add(item("BTC", "2"));
        items.add(item("ETH", "4.5"));
        items.add(item("BHP", "5"));
        items.add(item("Custom", "7"));
        items.add(item("Silver", "1,000"));
        items.add(item("Silver", "bad-value"));

        HoldingSummaryHelper.SummaryTotals totals = HoldingSummaryHelper.calculate(items);

        assertEquals(1010.5F, totals.silverTotal, 0.0001F);
        assertEquals(3F, totals.goldTotal, 0.0001F);

        assertEquals("Silver", totals.orderedTotals.get(0).getKey());
        assertEquals("Gold", totals.orderedTotals.get(1).getKey());
        assertEquals("Bitcoin", totals.orderedTotals.get(2).getKey());
        assertEquals("Ethereum", totals.orderedTotals.get(3).getKey());
        assertEquals("BHP", totals.orderedTotals.get(4).getKey());
        assertEquals("Custom", totals.orderedTotals.get(5).getKey());
    }

    @Test
    public void calculate_aggregatesByNormalizedType() {
        List<HoldingItem> items = new ArrayList<>();
        items.add(item("xag", "1"));
        items.add(item("Silver", "2"));
        items.add(item("xau", "3"));

        HoldingSummaryHelper.SummaryTotals totals = HoldingSummaryHelper.calculate(items);

        assertEquals(3F, totals.silverTotal, 0.0001F);
        assertEquals(3F, totals.goldTotal, 0.0001F);

        Map.Entry<String, Float> silverEntry = totals.orderedTotals.get(0);
        assertEquals("Silver", silverEntry.getKey());
        assertEquals(3F, silverEntry.getValue(), 0.0001F);
    }

    @Test
    public void calculate_normalizesShareLabelTickerInputs() {
        List<HoldingItem> items = new ArrayList<>();
        items.add(item("anz shares", "5", "Shares"));

        HoldingSummaryHelper.SummaryTotals totals = HoldingSummaryHelper.calculate(items);

        assertEquals("ANZ", totals.orderedTotals.get(0).getKey());
        assertEquals(5F, totals.orderedTotals.get(0).getValue(), 0.0001F);
    }

    private HoldingItem item(String type, String total) {
        HoldingItem item = new HoldingItem();
        item.setItemType(type);
        item.setTotal(total);
        return item;
    }

    private HoldingItem item(String type, String total, String what) {
        HoldingItem item = item(type, total);
        item.setWhat(what);
        return item;
    }
}
