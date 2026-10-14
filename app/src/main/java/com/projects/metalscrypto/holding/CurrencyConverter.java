package com.projects.metalscrypto.holding;

import java.util.Map;
import java.util.Locale;

public final class CurrencyConverter {
    private CurrencyConverter() {
    }

    public static Double convert(double amount, String sourceCurrency, String targetCurrency,
            Map<String, Double> ratesPerUsd) {
        String source = sourceCurrency == null ? "" : sourceCurrency.trim().toUpperCase(Locale.ROOT);
        String target = targetCurrency == null ? "" : targetCurrency.trim().toUpperCase(Locale.ROOT);
        if (source.equals(target)) {
            return amount;
        }
        if (ratesPerUsd == null) {
            return null;
        }

        Double sourceRate = ratesPerUsd.get(source);
        Double targetRate = ratesPerUsd.get(target);
        if (sourceRate == null || targetRate == null || sourceRate <= 0 || targetRate <= 0) {
            return null;
        }
        return amount / sourceRate * targetRate;
    }

    public static Double convertHoldingValue(double units, double unitPrice, String sourceCurrency,
            String targetCurrency, Map<String, Double> ratesPerUsd) {
        if (Double.isNaN(units) || Double.isInfinite(units)
                || Double.isNaN(unitPrice) || Double.isInfinite(unitPrice)) {
            return null;
        }
        if (units == 0) {
            return 0.0;
        }
        if (unitPrice <= 0) {
            return null;
        }

        double value = units * unitPrice;
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return convert(value, sourceCurrency, targetCurrency, ratesPerUsd);
    }
}