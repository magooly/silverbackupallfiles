package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class CurrencyConverterTest {
    private final Map<String, Double> rates = new HashMap<>();

    public CurrencyConverterTest() {
        rates.put("USD", 1.0);
        rates.put("AUD", 1.5);
        rates.put("NZD", 1.8);
    }

    @Test
    public void convertsUsdToAud() {
        assertEquals(Double.valueOf(150.0), CurrencyConverter.convert(100.0, "USD", "AUD", rates));
    }

    @Test
    public void convertsAudToNzdThroughUsdBase() {
        assertEquals(120.0, CurrencyConverter.convert(100.0, "AUD", "NZD", rates), 0.000000001);
    }

    @Test
    public void sameCurrencyDoesNotRequireRates() {
        assertEquals(Double.valueOf(25.0), CurrencyConverter.convert(25.0, "AUD", "AUD", null));
    }

    @Test
    public void usdMetalHoldingDisplaysWithoutAudExchangeRate() {
        assertEquals(Double.valueOf(300.0),
                CurrencyConverter.convertHoldingValue(10.0, 30.0, "USD", "USD", null));
        assertNull(CurrencyConverter.convertHoldingValue(10.0, 30.0, "USD", "AUD", null));
    }

    @Test
    public void displayConversionDoesNotRequireAnUnrelatedAudRate() {
        Map<String, Double> displayRates = new HashMap<>();
        displayRates.put("USD", 1.0);
        displayRates.put("EUR", 0.9);
        assertEquals(270.0,
                CurrencyConverter.convertHoldingValue(10.0, 30.0, "USD", "EUR", displayRates), 0.000001);
        assertNull(CurrencyConverter.convertHoldingValue(10.0, 30.0, "USD", "AUD", displayRates));
    }

    @Test
    public void audStockHoldingDisplaysWithoutExchangeRates() {
        assertEquals(Double.valueOf(50.0),
                CurrencyConverter.convertHoldingValue(10.0, 5.0, "AUD", "AUD", null));
        assertNull(CurrencyConverter.convertHoldingValue(10.0, 5.0, "AUD", "USD", null));
    }

    @Test
    public void missingRateReturnsNullInsteadOfAnUnconvertedAmount() {
        assertNull(CurrencyConverter.convert(100.0, "USD", "GBP", rates));
    }

    @Test
    public void missingUnitPriceReturnsNullForAnExistingHolding() {
        assertNull(CurrencyConverter.convertHoldingValue(2.0, 0.0, "USD", "AUD", rates));
    }

    @Test
    public void zeroUnitsHaveZeroValueWithoutAQuote() {
        assertEquals(Double.valueOf(0.0),
                CurrencyConverter.convertHoldingValue(0.0, 0.0, "USD", "AUD", null));
    }
}