package com.projects.metalscrypto.holding;

import com.projects.metalscrypto.holding.DBManager.HoldingsDb;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

final class BackupNonDetailedAccumulator {
    static final class Summary {
        BigDecimal total = BigDecimal.ZERO;
        String date;
        String time;
        String what;
    }

    private final Map<String, Summary> byType = new LinkedHashMap<>();

    void add(String itemType, String total, String what, String date, String time) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        Summary summary = byType.get(normalizedType);
        if (summary == null) {
            summary = new Summary();
            summary.date = date;
            summary.time = time;
            byType.put(normalizedType, summary);
        }
        summary.total = summary.total.add(parseAmount(total));
        if (what != null && !what.trim().isEmpty()) {
            summary.what = what.trim();
        }
        if (date != null && !date.trim().isEmpty()) {
            summary.date = date.trim();
        }
        if (time != null && !time.trim().isEmpty()) {
            summary.time = time.trim();
        }
    }

    Map<String, Summary> getByType() {
        return byType;
    }

    static String formatTotal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
        return normalized.toPlainString();
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
