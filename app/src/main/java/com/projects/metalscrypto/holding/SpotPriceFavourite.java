package com.projects.metalscrypto.holding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SpotPriceFavourite {
    static final int LIMIT = 6;
    final String kind;
    final String key;
    final String name;
    final String currency;
    final String unit;

    private SpotPriceFavourite(String kind, String key, String name, String currency, String unit) {
        this.kind = kind;
        this.key = key;
        this.name = name;
        this.currency = currency;
        this.unit = unit;
    }

    static SpotPriceFavourite metal(String name) {
        for (String supported : new String[] { "Gold", "Silver", "Platinum", "Palladium" }) {
            if (supported.equalsIgnoreCase(name)) {
                return new SpotPriceFavourite("metal", supported, supported, "USD", "oz");
            }
        }
        return null;
    }

    static SpotPriceFavourite crypto(String name, String coinId) {
        if (name == null || name.trim().isEmpty() || coinId == null
                || !coinId.matches("[a-zA-Z0-9][a-zA-Z0-9-]*")) {
            return null;
        }
        return new SpotPriceFavourite("crypto", coinId.toLowerCase(Locale.ROOT), name.trim(), "AUD", "coin");
    }

    static SpotPriceFavourite stock(String input) {
        if (input == null) {
            return null;
        }
        String ticker = input.trim().toUpperCase(Locale.ROOT).replaceFirst("\\.(AX|ASX|AU)$", "");
        if (!ticker.matches("[A-Z0-9]{2,5}")) {
            return null;
        }
        return new SpotPriceFavourite("stock", ticker, ticker, "AUD", "share");
    }

    String id() {
        return kind + ":" + key;
    }

    static String serialize(List<SpotPriceFavourite> favourites) {
        JsonArray saved = new JsonArray();
        for (SpotPriceFavourite favourite : favourites) {
            JsonObject asset = new JsonObject();
            asset.addProperty("kind", favourite.kind);
            asset.addProperty("key", favourite.key);
            asset.addProperty("name", favourite.name);
            saved.add(asset);
        }
        return saved.toString();
    }

    static List<SpotPriceFavourite> restore(String json) {
        List<SpotPriceFavourite> restored = new ArrayList<>();
        try {
            for (JsonElement entry : JsonParser.parseString(json).getAsJsonArray()) {
                JsonObject asset = entry.getAsJsonObject();
                String kind = asset.get("kind").getAsString();
                String key = asset.get("key").getAsString();
                SpotPriceFavourite favourite = null;
                if ("metal".equals(kind)) {
                    favourite = metal(key);
                } else if ("stock".equals(kind)) {
                    favourite = stock(key);
                } else if ("crypto".equals(kind)) {
                    favourite = crypto(asset.get("name").getAsString(), key);
                }
                add(restored, favourite);
            }
            return restored;
        } catch (RuntimeException exception) {
            return defaults();
        }
    }

    static boolean add(List<SpotPriceFavourite> favourites, SpotPriceFavourite candidate) {
        if (candidate == null || favourites.size() >= LIMIT) {
            return false;
        }
        for (SpotPriceFavourite favourite : favourites) {
            if (favourite.id().equals(candidate.id())) {
                return false;
            }
        }
        favourites.add(candidate);
        return true;
    }

    static List<SpotPriceFavourite> defaults() {
        List<SpotPriceFavourite> favourites = new ArrayList<>();
        favourites.add(metal("Gold"));
        favourites.add(metal("Silver"));
        return favourites;
    }
}