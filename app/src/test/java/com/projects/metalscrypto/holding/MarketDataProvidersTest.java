package com.projects.metalscrypto.holding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.Map;

public class MarketDataProvidersTest {
    @Test
    public void validatesUsdQuoteIdentityAndPrice() {
        JsonObject quote = JsonParser.parseString("{\"symbol\":\"XAG\",\"currency\":\"USD\",\"price\":30.25}")
                .getAsJsonObject();
        assertEquals(30.25F, MarketDataProviders.parseMetalPrice(quote, "XAG"), 0F);
        assertEquals(0F, MarketDataProviders.parseMetalPrice(quote, "XAU"), 0F);
        quote.addProperty("currency", "AUD");
        assertEquals(0F, MarketDataProviders.parseMetalPrice(quote, "XAG"), 0F);
        quote.addProperty("currency", "USD");
        quote.addProperty("price", -1);
        assertEquals(0F, MarketDataProviders.parseMetalPrice(quote, "XAG"), 0F);
        quote.addProperty("price", Double.POSITIVE_INFINITY);
        assertEquals(0F, MarketDataProviders.parseMetalPrice(quote, "XAG"), 0F);
        assertEquals(0F, MarketDataProviders.parseMetalPrice(new JsonObject(), "XAG"), 0F);
    }

    @Test
    public void doesNotTreatCopperFuturesAsPreciousMetalOunces() {
        assertEquals("XAU", MarketDataProviders.metalSymbol("Gold"));
        assertEquals("XAG", MarketDataProviders.metalSymbol("Silver"));
        assertEquals("XPT", MarketDataProviders.metalSymbol("Platinum"));
        assertEquals("XPD", MarketDataProviders.metalSymbol("Palladium"));
        assertNull(MarketDataProviders.metalSymbol("Copper"));
    }

    @Test
    public void parsesUsdBaseRatesWithoutInvertingThem() {
        Map<String, Double> rates = MarketDataProviders.parseExchangeRates(JsonParser.parseString(
                "[{\"base\":\"USD\",\"quote\":\"AUD\",\"rate\":1.5},"
                        + "{\"base\":\"USD\",\"quote\":\"JPY\",\"rate\":150}]"),
                new String[] { "USD", "AUD", "JPY" });
        assertEquals(1.0, rates.get("USD"), 0F);
        assertEquals(1.5, rates.get("AUD"), 0F);
        assertEquals(150.0, rates.get("JPY"), 0F);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIncompleteRates() {
        MarketDataProviders.parseExchangeRates(JsonParser.parseString("[]"), new String[] { "USD", "AUD" });
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidRates() {
        MarketDataProviders.parseExchangeRates(JsonParser.parseString(
                "[{\"base\":\"USD\",\"quote\":\"AUD\",\"rate\":0}]"), new String[] { "USD", "AUD" });
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsConflictingDuplicateRates() {
        MarketDataProviders.parseExchangeRates(JsonParser.parseString(
                "[{\"base\":\"USD\",\"quote\":\"AUD\",\"rate\":1.5},"
                        + "{\"base\":\"USD\",\"quote\":\"AUD\",\"rate\":2}]"),
                new String[] { "USD", "AUD" });
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIncorrectBase() {
        MarketDataProviders.parseExchangeRates(JsonParser.parseString(
                "[{\"base\":\"EUR\",\"quote\":\"AUD\",\"rate\":1.5}]"), new String[] { "USD", "AUD" });
    }
}