package com.projects.metalscrypto.holding;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

final class MarketDataProviders {
    // Set either flag to false to use the retained MetalCharts implementation and
    // API key for that feed.
    static final boolean USE_KEYLESS_METALS = true;
    static final boolean USE_KEYLESS_FX = true;
    static final String METALS_URL = "https://api.gold-api.com/";
    static final String FX_URL = "https://api.frankfurter.dev/";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private static final KeylessApi METALS = service(METALS_URL);
    private static final KeylessApi FX = service(FX_URL);

    private MarketDataProviders() {
    }

    interface KeylessApi {
        @GET("price/{symbol}/USD")
        Call<JsonObject> metalPrice(@Path("symbol") String symbol);

        @GET("v2/rates")
        Call<JsonElement> exchangeRates(@Query("base") String base, @Query("quotes") String quotes);
    }

    private static KeylessApi service(String baseUrl) {
        return new Retrofit.Builder().baseUrl(baseUrl).client(CLIENT)
                .addConverterFactory(GsonConverterFactory.create()).build().create(KeylessApi.class);
    }

    static Call<JsonObject> metalPrice(String symbol) {
        return METALS.metalPrice(symbol);
    }

    static Call<JsonElement> exchangeRates() {
        return FX.exchangeRates("USD", "AUD,NZD,GBP,EUR,CNY,JPY");
    }

    static String metalSymbol(String type) {
        if ("Gold".equalsIgnoreCase(type)) {
            return "XAU";
        }
        if ("Silver".equalsIgnoreCase(type)) {
            return "XAG";
        }
        if ("Platinum".equalsIgnoreCase(type)) {
            return "XPT";
        }
        if ("Palladium".equalsIgnoreCase(type)) {
            return "XPD";
        }
        return null;
    }

    static float parseMetalPrice(JsonObject body, String expectedSymbol) {
        try {
            if (body == null || !expectedSymbol.equals(body.get("symbol").getAsString())
                    || !"USD".equalsIgnoreCase(body.get("currency").getAsString())) {
                return 0F;
            }
            float price = body.get("price").getAsFloat();
            return price > 0 && !Float.isNaN(price) && !Float.isInfinite(price) ? price : 0F;
        } catch (RuntimeException exception) {
            return 0F;
        }
    }

    static Map<String, Double> parseExchangeRates(JsonElement body, String[] requiredCurrencies) {
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("USD", 1.0);
        if (body == null || !body.isJsonArray()) {
            throw new IllegalArgumentException("Frankfurter response must be a rates array.");
        }
        for (JsonElement row : body.getAsJsonArray()) {
            JsonObject quote = row.getAsJsonObject();
            if (!"USD".equalsIgnoreCase(quote.get("base").getAsString())) {
                throw new IllegalArgumentException("Frankfurter rate base is not USD.");
            }
            String currency = quote.get("quote").getAsString().toUpperCase(Locale.ROOT);
            double rate = quote.get("rate").getAsDouble();
            if (rate <= 0 || Double.isNaN(rate) || Double.isInfinite(rate)) {
                throw new IllegalArgumentException("Invalid Frankfurter rate for " + currency);
            }
            if ("USD".equals(currency) && rate != 1.0) {
                throw new IllegalArgumentException("Invalid USD base rate.");
            }
            if (rates.containsKey(currency) && rates.get(currency) != rate) {
                throw new IllegalArgumentException("Conflicting Frankfurter rates for " + currency);
            }
            rates.put(currency, rate);
        }
        for (String currency : requiredCurrencies) {
            if (!rates.containsKey(currency)) {
                throw new IllegalArgumentException("Missing Frankfurter rate for " + currency);
            }
        }
        return rates;
    }
}