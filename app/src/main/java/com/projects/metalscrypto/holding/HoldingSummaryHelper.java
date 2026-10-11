package com.projects.metalscrypto.holding;

import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HoldingSummaryHelper {
    private HoldingSummaryHelper() {
    }

    static final class SummaryTotals {
        final List<Map.Entry<String, Float>> orderedTotals;
        final float silverTotal;
        final float goldTotal;

        SummaryTotals(List<Map.Entry<String, Float>> orderedTotals, float silverTotal, float goldTotal) {
            this.orderedTotals = orderedTotals;
            this.silverTotal = silverTotal;
            this.goldTotal = goldTotal;
        }
    }

    static SummaryTotals calculate(List<HoldingItem> items) {
        Map<String, Float> totalsByType = new LinkedHashMap<>();
        for (HoldingItem item : items) {
            String normalizedType = HoldingsDb.normalizeItemType(item.getItemType());
            if ("Shares".equalsIgnoreCase(item.getWhat())) {
                String stockTicker = HoldingsDb.normalizeStockTickerInput(item.getItemType());
                if (!stockTicker.isEmpty()) {
                    normalizedType = stockTicker;
                }
            }
            float rowTotal = safeParse(item.getTotal());
            float running = totalsByType.containsKey(normalizedType) ? totalsByType.get(normalizedType) : 0F;
            totalsByType.put(normalizedType, running + rowTotal);
        }

        List<Map.Entry<String, Float>> orderedTotals = new ArrayList<>(totalsByType.entrySet());
        Collections.sort(orderedTotals, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> left, Map.Entry<String, Float> right) {
                int leftOrder = getAssetSortOrder(left.getKey());
                int rightOrder = getAssetSortOrder(right.getKey());
                if (leftOrder != rightOrder) {
                    return Integer.compare(leftOrder, rightOrder);
                }
                return left.getKey().compareToIgnoreCase(right.getKey());
            }
        });

        float silverTotal = totalsByType.containsKey("Silver") ? totalsByType.get("Silver") : 0F;
        float goldTotal = totalsByType.containsKey("Gold") ? totalsByType.get("Gold") : 0F;
        return new SummaryTotals(orderedTotals, silverTotal, goldTotal);
    }

    private static int getAssetSortOrder(String itemType) {
        if ("Silver".equalsIgnoreCase(itemType)) {
            return 0;
        }
        if ("Gold".equalsIgnoreCase(itemType)) {
            return 1;
        }
        if ("Bitcoin".equalsIgnoreCase(itemType)) {
            return 2;
        }
        if ("Ethereum".equalsIgnoreCase(itemType)) {
            return 3;
        }
        if (HoldingsDb.isStockTicker(itemType)) {
            return 4;
        }
        return 100;
    }

    private static float safeParse(String value) {
        if (value == null) {
            return 0F;
        }
        try {
            return Float.parseFloat(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0F;
        }
    }
}
