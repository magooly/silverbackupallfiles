package com.projects.metalscrypto.holding;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.projects.metalscrypto.holding.DBManager.GoldT_DB;
import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;
import com.projects.metalscrypto.holding.DBManager.SilverDB;
import com.projects.metalscrypto.holding.DBManager.SilverDBModel;
import com.projects.metalscrypto.holding.DBManager.SilverT_DB;
import com.projects.metalscrypto.holding.DBManager.VersionManager;
import com.projects.metalscrypto.holding.Retrofit.CryptoApiHelperClass;
import com.projects.metalscrypto.holding.Retrofit.CryptoApiWebservices;
import com.projects.metalscrypto.holding.Retrofit.ApiHelperClass;
import com.projects.metalscrypto.holding.Retrofit.ApiWebservices;
import com.projects.metalscrypto.holding.Retrofit.StockApiHelperClass;
import com.projects.metalscrypto.holding.Retrofit.StockApiWebservices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TAG";
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;
    private static final String TYPE_SILVER = "Silver";
    private static final String TYPE_GOLD = "Gold";
    private static final String API_ERROR_LOG_FILE = "api_errors.txt";
    private static final String PREF_DARK_MODE = "pref_dark_mode";
    private static final String PREF_PRICE_NOTICE_ACTIVE = "pref_price_notice_active";
    private static final String PREF_PRICE_NOTICE_DISMISSED = "pref_price_notice_dismissed";
    private static final String PREF_STOCK_PRICE_PREFIX = "stock_price_";
    private static final String PREF_CRYPTO_PRICE_PREFIX = "crypto_price_";
    private static final String PREF_CRYPTO_ID_PREFIX = "crypto_id_";
    private static final String EODHD_FORMAT_JSON = "json";
    private static final String EODHD_ORDER_DESC = "d";
    private static final Map<String, String> METAL_SYMBOL_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> CRYPTO_ID_BY_TYPE = new LinkedHashMap<>();

    static {
        METAL_SYMBOL_BY_TYPE.put("Gold", "XAU");
        METAL_SYMBOL_BY_TYPE.put("Silver", "XAG");
        METAL_SYMBOL_BY_TYPE.put("Copper", "XCU");
        METAL_SYMBOL_BY_TYPE.put("Platinum", "XPT");
        METAL_SYMBOL_BY_TYPE.put("Palladium", "XPD");
        METAL_SYMBOL_BY_TYPE.put("Lead", "XLD");
        METAL_SYMBOL_BY_TYPE.put("Aluminum", "XAL");
        METAL_SYMBOL_BY_TYPE.put("Nickel", "XNI");
        METAL_SYMBOL_BY_TYPE.put("Zinc", "XZN");

        CRYPTO_ID_BY_TYPE.put("Bitcoin", "bitcoin");
        CRYPTO_ID_BY_TYPE.put("Ethereum", "ethereum");
        CRYPTO_ID_BY_TYPE.put("Solana", "solana");
        CRYPTO_ID_BY_TYPE.put("Cardano", "cardano");
        CRYPTO_ID_BY_TYPE.put("Ripple", "ripple");
        CRYPTO_ID_BY_TYPE.put("Xrp", "ripple");
        CRYPTO_ID_BY_TYPE.put("Dogecoin", "dogecoin");
        CRYPTO_ID_BY_TYPE.put("Litecoin", "litecoin");
        CRYPTO_ID_BY_TYPE.put("Polkadot", "polkadot");
        CRYPTO_ID_BY_TYPE.put("Chainlink", "chainlink");
        CRYPTO_ID_BY_TYPE.put("Avalanche", "avalanche-2");
    }

    private Context mContext;
    private EditText et_slv_slv_price_amount, et_slv_gld_amount;
    private TextView tv_hld_slv_price_amount, tv_hld_gld_amount, tv_hld_ttl_amount, tvDate, tvPriceHeading,
            tvAppVersion, tvPriceNotice;
    private Button btn_holding_update, btn_silver_update, btnClearPriceNotice;
    private String slv_gld_price, slv_slv_price, slv_cpr_price, time, date;

    private SilverDB silverDB;
    private SilverT_DB silverT_db;
    private GoldT_DB goldT_db;
    private HoldingsDb holdingsDb;

    private float silver_total = 0, gold_total = 0;
    private float d_gold_amt, d_silver_amt, d_copper_amt, d_platinum_amt, d_palladium_amt, d_lead_amt,
            d_aluminum_amt, d_nickel_amt, d_zinc_amt, d_btc_amt, d_eth_amt,
            lv_gold_amt, lv_slv_amt, lv_cpr_amt, lv_platinum_amt, lv_palladium_amt, lv_lead_amt,
            lv_aluminum_amt, lv_nickel_amt, lv_zinc_amt, lv_btc_amt, lv_eth_amt;

    private List<SilverDBModel> alSilver;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private String pendingDataToWrite = null;
    private boolean shouldNavigateToResList = false;
    private boolean hasTriggeredInitialLiveRefresh = false;
    private boolean hasLoggedMissingMetalsApiKey = false;
    private boolean hasLoggedMissingStockApiKey = false;
    private LinearLayout llDynamicHoldings;
    private LinearLayout llPriceNotice;
    private final List<HoldingSummary> dynamicHoldings = new ArrayList<>();
    private final Map<String, Float> liveStockPrices = new HashMap<>();
    private final Map<String, Float> cachedStockPrices = new HashMap<>();
    private final Map<String, Float> liveCryptoPrices = new HashMap<>();
    private final Map<String, Float> cachedCryptoPrices = new HashMap<>();
    private final Set<String> promptedCryptoTypes = new HashSet<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        boolean darkMode = preferences.getBoolean(PREF_DARK_MODE, false);
        AppCompatDelegate
                .setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        editor = preferences.edit();
        shouldNavigateToResList = false;

        setupViews();
        VersionManager versionManager = new VersionManager(this);
        versionManager.checkAndUpdateVersion();
        migrateLegacyHoldings();
        ioExecutor.execute(() -> {
            BackupSyncManager.importAndMergeOnStart(this);
            BackupSyncManager.exportSnapshot(this);
            runOnUiThread(() -> {
                refreshHoldingSummaries();
                getAPIdata();
            });
        });
        checkAndRequestPermissions();
    }

    private void getLiveData() {
        if (!CustomNetwork.isNetworkAvailable(mContext)) {
            return;
        }

        refreshHoldingSummaries();
        Set<String> trackedMetalTypes = getTrackedMetalTypes();
        Set<String> trackedCryptoTypes = getTrackedCryptoTypes();
        Set<String> trackedStockTickers = getTrackedStockTickers();

        getMetalLiveData(trackedMetalTypes);
        getCryptoLiveData(trackedCryptoTypes);
        getStockLiveData(trackedStockTickers);
    }

    private void getMetalLiveData(Set<String> trackedMetalTypes) {
        if (trackedMetalTypes.isEmpty()) {
            return;
        }
        String apiKey = BuildConfig.METALCHARTS_API_KEY == null ? "" : BuildConfig.METALCHARTS_API_KEY.trim();
        if (apiKey.isEmpty()) {
            if (!hasLoggedMissingMetalsApiKey) {
                appendApiErrorLog("Metals API key missing: set METALCHARTS_API_KEY in local.properties or environment.");
                hasLoggedMissingMetalsApiKey = true;
            }
            showCachedPricesIndicator();
            return;
        }

        ApiWebservices apiWebservices = ApiHelperClass.getClient().create(ApiWebservices.class);
        Call<JsonObject> call = apiWebservices.getPriceRes("Bearer " + apiKey);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isUiAlive()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    boolean hasUpdatedPrice = false;
                    for (String metalType : trackedMetalTypes) {
                        String symbol = METAL_SYMBOL_BY_TYPE.get(metalType);
                        if (symbol == null) {
                            continue;
                        }
                        float metalPrice = parseMetalChartsPrice(body, symbol);
                        if (metalPrice <= 0) {
                            continue;
                        }
                        if ("Gold".equalsIgnoreCase(metalType)) {
                            lv_gold_amt = metalPrice;
                            slv_gld_price = String.valueOf(metalPrice);
                        } else if ("Silver".equalsIgnoreCase(metalType)) {
                            lv_slv_amt = metalPrice;
                            slv_slv_price = String.valueOf(metalPrice);
                        } else if ("Copper".equalsIgnoreCase(metalType)) {
                            lv_cpr_amt = metalPrice;
                        } else if ("Platinum".equalsIgnoreCase(metalType)) {
                            lv_platinum_amt = metalPrice;
                        } else if ("Palladium".equalsIgnoreCase(metalType)) {
                            lv_palladium_amt = metalPrice;
                        } else if ("Lead".equalsIgnoreCase(metalType)) {
                            lv_lead_amt = metalPrice;
                        } else if ("Aluminum".equalsIgnoreCase(metalType)) {
                            lv_aluminum_amt = metalPrice;
                        } else if ("Nickel".equalsIgnoreCase(metalType)) {
                            lv_nickel_amt = metalPrice;
                        } else if ("Zinc".equalsIgnoreCase(metalType)) {
                            lv_zinc_amt = metalPrice;
                        }
                        hasUpdatedPrice = true;
                    }

                    if (hasUpdatedPrice) {
                        updateUI();
                        if (lv_gold_amt > 0 && lv_slv_amt > 0) {
                            insertdatainDB();
                        }
                    } else {
                        appendApiErrorLog("Live API unsuccessful. HTTP=" + response.code() + ", message=missing tracked metal prices");
                        Log.w(TAG, "Live response missing tracked metal prices, using cached prices");
                        showCachedPricesIndicator();
                    }
                } else {
                    appendApiErrorLog("Live API unsuccessful. HTTP=" + response.code());
                    Log.w(TAG, "Live response unsuccessful, using cached prices");
                    showCachedPricesIndicator();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isUiAlive()) {
                    return;
                }
                appendApiErrorLog("Live API failure: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                Log.e(TAG, "Failed to fetch live data", t);
                showCachedPricesIndicator();
            }
        });
    }

    private float parseMetalChartsPrice(JsonObject body, String symbol) {
        try {
            if (body != null
                    && body.has("data")
                    && body.get("data").isJsonObject()) {
                JsonObject data = body.getAsJsonObject("data");
                if (data.has(symbol)
                        && data.get(symbol).isJsonObject()) {
                    JsonObject quote = data.getAsJsonObject(symbol);
                    if (quote.has("price")) {
                        return quote.get("price").getAsFloat();
                    }
                }
            }
        } catch (Exception e) {
            appendApiErrorLog("Metals parse failure for " + symbol + ": " + e.getMessage());
        }
        appendApiErrorLog("Metals parse failure for " + symbol + ": price not found.");
        return 0;
    }

    private void getCryptoLiveData(Set<String> trackedCryptoTypes) {
        if (trackedCryptoTypes.isEmpty()) {
            return;
        }

        Map<String, String> cryptoIdToType = new LinkedHashMap<>();
        Set<String> unresolvedTypes = new LinkedHashSet<>();
        for (String cryptoType : trackedCryptoTypes) {
            String id = resolveCryptoId(cryptoType);
            if (id == null || id.trim().isEmpty()) {
                unresolvedTypes.add(cryptoType);
                continue;
            }
            cryptoIdToType.put(id, cryptoType);
        }

        if (!cryptoIdToType.isEmpty()) {
            fetchCryptoPrices(cryptoIdToType);
        }

        for (String unresolvedType : unresolvedTypes) {
            resolveCryptoIdAndFetch(unresolvedType);
        }
    }

    private void fetchCryptoPrices(Map<String, String> cryptoIdToType) {
        CryptoApiWebservices cryptoApiWebservices = CryptoApiHelperClass.getClient().create(CryptoApiWebservices.class);
        String ids = String.join(",", cryptoIdToType.keySet());
        Call<JsonObject> call = cryptoApiWebservices.getCryptoPrices(ids, "aud");

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isUiAlive()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    boolean hasUpdatedPrice = false;
                    for (Map.Entry<String, String> entry : cryptoIdToType.entrySet()) {
                        String cryptoId = entry.getKey();
                        String cryptoType = entry.getValue();
                        float cryptoPrice = parseCryptoAud(body, cryptoId);
                        if (cryptoPrice <= 0) {
                            promptForCryptoIdOverride(cryptoType, cryptoId);
                            continue;
                        }
                        if ("Bitcoin".equalsIgnoreCase(cryptoType)) {
                            lv_btc_amt = cryptoPrice;
                        } else if ("Ethereum".equalsIgnoreCase(cryptoType)) {
                            lv_eth_amt = cryptoPrice;
                        } else {
                            liveCryptoPrices.put(cryptoType, cryptoPrice);
                            cacheCryptoPrice(cryptoType, cryptoPrice);
                        }
                        hasUpdatedPrice = true;
                    }
                    if (hasUpdatedPrice) {
                        updateUI();
                    }
                } else {
                    appendApiErrorLog("Crypto API unsuccessful. HTTP=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isUiAlive()) {
                    return;
                }
                appendApiErrorLog("Crypto API failure: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    private void resolveCryptoIdAndFetch(String cryptoType) {
        CryptoApiWebservices cryptoApiWebservices = CryptoApiHelperClass.getClient().create(CryptoApiWebservices.class);
        Call<JsonObject> call = cryptoApiWebservices.searchCrypto(cryptoType);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isUiAlive()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    appendApiErrorLog("Crypto search unsuccessful for " + cryptoType + ". HTTP=" + response.code());
                    promptForCryptoIdOverride(cryptoType, "");
                    return;
                }
                String cryptoId = parseCryptoIdFromSearch(response.body(), cryptoType);
                if (cryptoId == null || cryptoId.trim().isEmpty()) {
                    appendApiErrorLog("Crypto id not found for " + cryptoType);
                    promptForCryptoIdOverride(cryptoType, "");
                    return;
                }
                cacheCryptoId(cryptoType, cryptoId);
                Map<String, String> oneCrypto = new LinkedHashMap<>();
                oneCrypto.put(cryptoId, cryptoType);
                fetchCryptoPrices(oneCrypto);
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isUiAlive()) {
                    return;
                }
                appendApiErrorLog("Crypto search failure for " + cryptoType + ": "
                        + t.getClass().getSimpleName() + " - " + t.getMessage());
                promptForCryptoIdOverride(cryptoType, "");
            }
        });
    }

    private void getStockLiveData(Set<String> stockTickers) {
        if (stockTickers.isEmpty()) {
            return;
        }

        if (BuildConfig.EODHD_API_KEY == null || BuildConfig.EODHD_API_KEY.trim().isEmpty()) {
            if (!hasLoggedMissingStockApiKey) {
                appendApiErrorLog(
                        "Stock API key missing: set EODHD_API_KEY in local.properties or environment.");
                hasLoggedMissingStockApiKey = true;
            }
            return;
        }

        StockApiWebservices stockApiWebservices = StockApiHelperClass.getClient().create(StockApiWebservices.class);
        String stockApiKey = BuildConfig.EODHD_API_KEY.trim();
        for (String ticker : stockTickers) {
            String asxSymbol = HoldingsDb.toAsxQuoteSymbol(ticker);
            fetchStockPrice(stockApiWebservices, stockApiKey, ticker, asxSymbol, true);
        }
    }

    private void fetchStockPrice(StockApiWebservices stockApiWebservices, String apiKey, String ticker,
            String queriedSymbol, boolean allowFallback) {
        Call<JsonElement> call = stockApiWebservices.getLatestPrice(
                queriedSymbol,
                apiKey,
                EODHD_FORMAT_JSON);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call, @NonNull Response<JsonElement> response) {
                if (!isUiAlive()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    float stockPrice = parseStockPrice(response.body(), queriedSymbol);
                    if (stockPrice > 0) {
                        liveStockPrices.put(ticker, stockPrice);
                        cacheStockPrice(ticker, stockPrice);
                        updateUI();
                        return;
                    }
                }

                fetchStockEodPrice(stockApiWebservices, apiKey, ticker, queriedSymbol, allowFallback);
            }

            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
                if (!isUiAlive()) {
                    return;
                }
                fetchStockEodPrice(stockApiWebservices, apiKey, ticker, queriedSymbol, allowFallback);
            }
        });
    }

    private void fetchStockEodPrice(StockApiWebservices stockApiWebservices, String apiKey, String ticker,
            String queriedSymbol, boolean allowFallback) {
        Call<JsonElement> call = stockApiWebservices.getEndOfDayPrice(
                queriedSymbol,
                apiKey,
                EODHD_FORMAT_JSON,
                EODHD_ORDER_DESC);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call, @NonNull Response<JsonElement> response) {
                if (!isUiAlive()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    float stockPrice = parseStockPrice(response.body(), queriedSymbol);
                    if (stockPrice > 0) {
                        liveStockPrices.put(ticker, stockPrice);
                        cacheStockPrice(ticker, stockPrice);
                        updateUI();
                        return;
                    }
                }

                if (allowFallback && !ticker.equalsIgnoreCase(queriedSymbol)) {
                    fetchStockPrice(stockApiWebservices, apiKey, ticker, ticker, false);
                    return;
                }

                String status = extractStockApiStatus(response.body());
                appendApiErrorLog("Stock API unsuccessful for " + queriedSymbol
                        + ". HTTP=" + response.code() + ", status=" + status);
            }

            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
                if (!isUiAlive()) {
                    return;
                }
                if (allowFallback && !ticker.equalsIgnoreCase(queriedSymbol)) {
                    fetchStockPrice(stockApiWebservices, apiKey, ticker, ticker, false);
                    return;
                }
                appendApiErrorLog("Stock API failure for " + queriedSymbol
                        + ": " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        });
    }

    private float parseCryptoAud(JsonObject body, String symbol) {
        try {
            if (body != null
                    && body.has(symbol)
                    && body.getAsJsonObject(symbol).has("aud")) {
                return body.getAsJsonObject(symbol).get("aud").getAsFloat();
            }
        } catch (Exception e) {
            appendApiErrorLog("Crypto parse failure for " + symbol + ": " + e.getMessage());
        }
        return 0;
    }

    private String resolveCryptoId(String cryptoType) {
        if (cryptoType == null || cryptoType.trim().isEmpty()) {
            return "";
        }
        String normalizedType = HoldingsDb.normalizeItemType(cryptoType);
        String knownId = CRYPTO_ID_BY_TYPE.get(normalizedType);
        if (knownId != null && !knownId.trim().isEmpty()) {
            return knownId;
        }

        String cachedId = preferences.getString(PREF_CRYPTO_ID_PREFIX + normalizedType, "");
        if (cachedId != null && !cachedId.trim().isEmpty()) {
            return cachedId.trim();
        }

        return "";
    }

    private void cacheCryptoId(String cryptoType, String cryptoId) {
        if (cryptoType == null || cryptoType.trim().isEmpty() || cryptoId == null || cryptoId.trim().isEmpty()) {
            return;
        }
        String normalizedType = HoldingsDb.normalizeItemType(cryptoType);
        preferences.edit().putString(PREF_CRYPTO_ID_PREFIX + normalizedType, cryptoId.trim()).apply();
    }

    private String parseCryptoIdFromSearch(JsonObject body, String cryptoType) {
        try {
            if (body == null || !body.has("coins") || !body.get("coins").isJsonArray()) {
                return "";
            }
            String normalizedType = HoldingsDb.normalizeItemType(cryptoType);
            String normalizedLower = normalizedType.toLowerCase(Locale.US);
            JsonArray coins = body.getAsJsonArray("coins");
            for (JsonElement coinElement : coins) {
                if (!coinElement.isJsonObject()) {
                    continue;
                }
                JsonObject coin = coinElement.getAsJsonObject();
                String id = coin.has("id") ? coin.get("id").getAsString() : "";
                String symbol = coin.has("symbol") ? coin.get("symbol").getAsString() : "";
                String name = coin.has("name") ? coin.get("name").getAsString() : "";
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }
                if (normalizedLower.equalsIgnoreCase(symbol)
                        || normalizedLower.equalsIgnoreCase(name.toLowerCase(Locale.US))) {
                    return id;
                }
            }
            if (coins.size() > 0 && coins.get(0).isJsonObject()) {
                JsonObject firstCoin = coins.get(0).getAsJsonObject();
                if (firstCoin.has("id")) {
                    return firstCoin.get("id").getAsString();
                }
            }
        } catch (Exception e) {
            appendApiErrorLog("Crypto search parse failure for " + cryptoType + ": " + e.getMessage());
        }
        return "";
    }

    private void promptForCryptoIdOverride(String cryptoType, String suggestedId) {
        String normalizedType = HoldingsDb.normalizeItemType(cryptoType);
        if (normalizedType.trim().isEmpty() || promptedCryptoTypes.contains(normalizedType)) {
            return;
        }
        if (!isUiAlive()) {
            return;
        }
        promptedCryptoTypes.add(normalizedType);

        runOnUiThread(() -> {
            if (!isUiAlive()) {
                return;
            }
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setSingleLine(true);
            String existing = preferences.getString(PREF_CRYPTO_ID_PREFIX + normalizedType, "");
            String defaultValue = (existing != null && !existing.trim().isEmpty())
                    ? existing.trim()
                    : (suggestedId == null ? "" : suggestedId.trim());
            input.setText(defaultValue);
            input.setSelection(input.getText().length());
            input.setHint("coingecko-id");

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.crypto_id_prompt_title, normalizedType))
                    .setMessage(R.string.crypto_id_prompt_message)
                    .setView(input)
                    .setPositiveButton(R.string.save, null)
                    .setNegativeButton(R.string.cancel, null)
                    .create();

            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String enteredId = input.getText().toString().trim().toLowerCase(Locale.US);
                if (enteredId.isEmpty()) {
                    Toast.makeText(this, R.string.crypto_id_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                cacheCryptoId(normalizedType, enteredId);
                Map<String, String> oneCrypto = new LinkedHashMap<>();
                oneCrypto.put(enteredId, normalizedType);
                fetchCryptoPrices(oneCrypto);
                dialog.dismiss();
            }));

            dialog.show();
        });
    }

    private float parseStockPrice(JsonElement body, String queriedSymbol) {
        try {
            if (body == null || body.isJsonNull()) {
                return 0;
            }
            if (body.isJsonObject()) {
                JsonObject quote = body.getAsJsonObject();
                if (quote.has("close")) {
                    return Float.parseFloat(quote.get("close").getAsString().trim());
                }
            }
            if (body.isJsonArray() && body.getAsJsonArray().size() > 0 && body.getAsJsonArray().get(0).isJsonObject()) {
                JsonObject latest = body.getAsJsonArray().get(0).getAsJsonObject();
                if (latest.has("close")) {
                    return Float.parseFloat(latest.get("close").getAsString().trim());
                }
            }

            String message = extractStockApiStatus(body);
            if (!"unknown".equalsIgnoreCase(message)) {
                appendApiErrorLog("Stock API parse failure for " + queriedSymbol + ": " + message);
            }
        } catch (Exception e) {
            appendApiErrorLog("Stock parse failure for " + queriedSymbol + ": " + e.getMessage());
        }
        return 0;
    }

    private String extractStockApiStatus(JsonElement body) {
        if (body == null || body.isJsonNull() || !body.isJsonObject()) {
            return "unknown";
        }
        JsonObject object = body.getAsJsonObject();
        if (object.has("status")) {
            return object.get("status").getAsString();
        }
        if (object.has("message")) {
            return object.get("message").getAsString();
        }
        if (object.has("error")) {
            return object.get("error").getAsString();
        }
        if (object.has("errmsg")) {
            return object.get("errmsg").getAsString();
        }
        return "unknown";
    }

    private void appendApiErrorLog(String message) {
        String now = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = now + " - " + message + "\n";
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(API_ERROR_LOG_FILE, Context.MODE_APPEND);
            fos.write(entry.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write API error log", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close API error log output stream", e);
                }
            }
        }
    }

    private void setupViews() {
        mContext = MainActivity.this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        et_slv_slv_price_amount = findViewById(R.id.et_slv_slv_price_amount);
        et_slv_gld_amount = findViewById(R.id.et_slv_gld_amount);
        tv_hld_slv_price_amount = findViewById(R.id.tv_hld_slv_price_amount);
        tv_hld_gld_amount = findViewById(R.id.tv_hld_gld_amount);
        tv_hld_ttl_amount = findViewById(R.id.tv_hld_ttl_amount);
        tvPriceHeading = findViewById(R.id.tv_price_heading);
        llDynamicHoldings = findViewById(R.id.ll_dynamic_holdings);
        llPriceNotice = findViewById(R.id.ll_price_notice);
        tvPriceNotice = findViewById(R.id.tv_price_notice);
        btnClearPriceNotice = findViewById(R.id.btn_clear_price_notice);
        btn_holding_update = findViewById(R.id.btn_holding_update);
        btn_silver_update = findViewById(R.id.btn_silver_update);
        tvDate = findViewById(R.id.tvDate);
        tvAppVersion = findViewById(R.id.tv_app_version);

        if (tvAppVersion != null) {
            tvAppVersion.setText(getString(R.string.version_display_format, BuildConfig.VERSION_NAME));
        }

        updateThemeColors();
        btn_holding_update.setOnClickListener(this);
        btn_silver_update.setOnClickListener(this);
        if (btnClearPriceNotice != null) {
            btnClearPriceNotice.setOnClickListener(v -> dismissPriceNotice());
        }

        silverDB = new SilverDB(mContext);
        silverT_db = new SilverT_DB(mContext);
        goldT_db = new GoldT_DB(mContext);
        holdingsDb = new HoldingsDb(mContext);
        loadCachedStockPrices();
        loadCachedCryptoPrices();
    }

    private void refreshHoldingSummaries() {
        dynamicHoldings.clear();
        HoldingSummaryHelper.SummaryTotals summaryTotals = HoldingSummaryHelper.calculate(holdingsDb.getAllItems());
        silver_total = summaryTotals.silverTotal;
        gold_total = summaryTotals.goldTotal;

        for (Map.Entry<String, Float> entry : summaryTotals.orderedTotals) {
            if (TYPE_SILVER.equalsIgnoreCase(entry.getKey()) || TYPE_GOLD.equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            dynamicHoldings.add(new HoldingSummary(entry.getKey(), entry.getValue()));
        }
        updateUI();
    }

    private void updateThemeColors() {
        boolean darkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        View root = findViewById(R.id.main_root);
        if (root != null) {
            root.setBackgroundColor(getResources().getColor(darkMode ? R.color.dark_surface : R.color.light_green));
        }

        int primaryText = darkMode ? Color.parseColor("#F1F3F5") : Color.BLACK;
        int secondaryText = darkMode ? Color.parseColor("#C3C8CD") : Color.parseColor("#666666");

        if (tvDate != null)
            tvDate.setTextColor(primaryText);
        if (tvPriceHeading != null)
            tvPriceHeading.setTextColor(primaryText);
        if (tvAppVersion != null)
            tvAppVersion.setTextColor(secondaryText);
    }

    private void updateUI() {
        float current_silver_price = d_silver_amt > 0 ? d_silver_amt : lv_slv_amt;
        float current_gold_price = d_gold_amt > 0 ? d_gold_amt : lv_gold_amt;
        DecimalFormat priceFormat = new DecimalFormat("#,##0");

        // Silver and Gold remain fixed rows on the first screen.
        et_slv_slv_price_amount.setText(priceFormat.format(current_silver_price));
        et_slv_gld_amount.setText(priceFormat.format(current_gold_price));

        float silverHolding = silver_total * current_silver_price;
        float goldHolding = gold_total * current_gold_price;
        float totalHolding = silverHolding + goldHolding;

        for (HoldingSummary summary : dynamicHoldings) {
            if (TYPE_SILVER.equalsIgnoreCase(summary.itemType) || TYPE_GOLD.equalsIgnoreCase(summary.itemType)) {
                continue;
            }
            totalHolding += getHoldingValue(summary);
        }

        tv_hld_slv_price_amount.setText(priceFormat.format(silverHolding));
        tv_hld_gld_amount.setText(priceFormat.format(goldHolding));
        renderDynamicHoldingRows(priceFormat);
        tv_hld_ttl_amount.setText(priceFormat.format(totalHolding));
        updatePriceNotice(current_silver_price, current_gold_price);

        if (lv_slv_amt > 0) {
            generateData(priceFormat.format(totalHolding));
        }
    }

    private void renderDynamicHoldingRows(DecimalFormat formatter) {
        llDynamicHoldings.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (HoldingSummary summary : dynamicHoldings) {
            float holdingValue = getHoldingValue(summary);
            View row = inflater.inflate(R.layout.view_main_dynamic_holding_row, llDynamicHoldings, false);
            TextView title = row.findViewById(R.id.tv_dynamic_type);
            TextView amount = row.findViewById(R.id.tv_dynamic_amount);

            title.setText(summary.itemType);
            amount.setText(formatter.format(holdingValue));
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                title.setTextColor(Color.parseColor("#EFF2F5"));
                amount.setTextColor(Color.parseColor("#EFF2F5"));
            } else {
                title.setTextColor(getResources().getColor(R.color.textview_text_color));
                amount.setTextColor(getResources().getColor(R.color.textview_text_color));
            }

            llDynamicHoldings.addView(row);
        }
    }

    private float getHoldingValue(HoldingSummary summary) {
        float currentPrice = getCurrentPriceByType(summary.itemType);
        if (isApiLookupType(summary.itemType) && currentPrice > 0) {
            return summary.totalUnits * currentPrice;
        }
        if (isApiLookupType(summary.itemType)) {
            return 0;
        }
        // For custom/non-API items, screen 2 already stores calculated total value.
        return summary.totalUnits;
    }

    private void updatePriceNotice(float currentSilverPrice, float currentGoldPrice) {
        List<String> missingTypes = new ArrayList<>();
        if (silver_total > 0 && currentSilverPrice <= 0) {
            missingTypes.add(TYPE_SILVER);
        }
        if (gold_total > 0 && currentGoldPrice <= 0) {
            missingTypes.add(TYPE_GOLD);
        }
        for (HoldingSummary summary : dynamicHoldings) {
            if (summary.totalUnits <= 0 || !isApiLookupType(summary.itemType)) {
                continue;
            }
            if (getCurrentPriceByType(summary.itemType) <= 0) {
                missingTypes.add(summary.itemType);
            }
        }

        if (missingTypes.isEmpty()) {
            preferences.edit()
                    .putBoolean(PREF_PRICE_NOTICE_ACTIVE, false)
                    .putBoolean(PREF_PRICE_NOTICE_DISMISSED, false)
                    .apply();
            hidePriceNotice();
            return;
        }

        boolean isActive = preferences.getBoolean(PREF_PRICE_NOTICE_ACTIVE, false);
        boolean dismissed = preferences.getBoolean(PREF_PRICE_NOTICE_DISMISSED, false);
        if (!isActive) {
            preferences.edit()
                    .putBoolean(PREF_PRICE_NOTICE_ACTIVE, true)
                    .putBoolean(PREF_PRICE_NOTICE_DISMISSED, false)
                    .apply();
            dismissed = false;
        }

        if (dismissed) {
            hidePriceNotice();
            return;
        }

        if (tvPriceNotice != null) {
            tvPriceNotice.setText(getString(R.string.price_notice_missing_values, joinItemTypes(missingTypes)));
        }
        if (llPriceNotice != null) {
            llPriceNotice.setVisibility(View.VISIBLE);
        }
    }

    private void dismissPriceNotice() {
        preferences.edit().putBoolean(PREF_PRICE_NOTICE_DISMISSED, true).apply();
        hidePriceNotice();
    }

    private void hidePriceNotice() {
        if (llPriceNotice != null) {
            llPriceNotice.setVisibility(View.GONE);
        }
    }

    private String joinItemTypes(List<String> itemTypes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < itemTypes.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(itemTypes.get(i));
        }
        return builder.toString();
    }

    private boolean isApiLookupType(String itemType) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        return isMetalType(normalizedType)
                || isCryptoLookupType(normalizedType)
                || HoldingsDb.isStockTicker(normalizedType);
    }

    private float getCurrentPriceByType(String itemType) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        if (TYPE_SILVER.equalsIgnoreCase(normalizedType))
            return d_silver_amt > 0 ? d_silver_amt : lv_slv_amt;
        if (TYPE_GOLD.equalsIgnoreCase(normalizedType))
            return d_gold_amt > 0 ? d_gold_amt : lv_gold_amt;
        if ("Copper".equalsIgnoreCase(normalizedType))
            return d_copper_amt > 0 ? d_copper_amt : lv_cpr_amt;
        if ("Platinum".equalsIgnoreCase(normalizedType))
            return d_platinum_amt > 0 ? d_platinum_amt : lv_platinum_amt;
        if ("Palladium".equalsIgnoreCase(normalizedType))
            return d_palladium_amt > 0 ? d_palladium_amt : lv_palladium_amt;
        if ("Lead".equalsIgnoreCase(normalizedType))
            return d_lead_amt > 0 ? d_lead_amt : lv_lead_amt;
        if ("Aluminum".equalsIgnoreCase(normalizedType))
            return d_aluminum_amt > 0 ? d_aluminum_amt : lv_aluminum_amt;
        if ("Nickel".equalsIgnoreCase(normalizedType))
            return d_nickel_amt > 0 ? d_nickel_amt : lv_nickel_amt;
        if ("Zinc".equalsIgnoreCase(normalizedType))
            return d_zinc_amt > 0 ? d_zinc_amt : lv_zinc_amt;
        if ("Bitcoin".equalsIgnoreCase(normalizedType))
            return d_btc_amt > 0 ? d_btc_amt : lv_btc_amt;
        if ("Ethereum".equalsIgnoreCase(normalizedType))
            return d_eth_amt > 0 ? d_eth_amt : lv_eth_amt;
        if (isCryptoLookupType(normalizedType)) {
            Float livePrice = liveCryptoPrices.get(normalizedType);
            if (livePrice != null && livePrice > 0) {
                return livePrice;
            }
            Float cachedPrice = cachedCryptoPrices.get(normalizedType);
            return cachedPrice != null ? cachedPrice : 0;
        }
        if (HoldingsDb.isStockTicker(normalizedType)) {
            Float livePrice = liveStockPrices.get(normalizedType);
            if (livePrice != null && livePrice > 0) {
                return livePrice;
            }
            Float cachedPrice = cachedStockPrices.get(normalizedType);
            return cachedPrice != null ? cachedPrice : 0;
        }
        return 0;
    }

    private boolean isMetalType(String normalizedType) {
        return METAL_SYMBOL_BY_TYPE.containsKey(normalizedType);
    }

    private boolean isCryptoLookupType(String normalizedType) {
        return CRYPTO_ID_BY_TYPE.containsKey(normalizedType)
                || liveCryptoPrices.containsKey(normalizedType)
                || cachedCryptoPrices.containsKey(normalizedType);
    }

    private float safeParse(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Float.parseFloat(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing numeric value: " + value, e);
            return 0;
        }
    }

    private void migrateLegacyHoldings() {
        holdingsDb.importLegacyData(silverT_db.getSilverList(), "Silver", "silver");
        holdingsDb.importLegacyData(goldT_db.getGoldList(), "Gold", "gold");
    }

    private void getAPIdata() {
        d_gold_amt = 0;
        d_silver_amt = 0;
        d_copper_amt = 0;
        d_platinum_amt = 0;
        d_palladium_amt = 0;
        d_lead_amt = 0;
        d_aluminum_amt = 0;
        d_nickel_amt = 0;
        d_zinc_amt = 0;
        d_btc_amt = 0;
        d_eth_amt = 0;

        alSilver = silverDB.getSilverData(date);
        boolean hasOfflineData = alSilver != null && !alSilver.isEmpty();
        if (hasOfflineData) {
            Log.d(TAG, "Offline data fetched");
            try {
                d_gold_amt = Float.parseFloat(alSilver.get(0).getGold());
                d_silver_amt = Float.parseFloat(alSilver.get(0).getSilver());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing offline data", e);
            }
            updateUI();
            showCachedPricesIndicator();
        } else {
            Log.d(TAG, "No offline data for today");
        }

        // Auto-refresh only once per app opening; manual refresh button remains
        // available.
        if (!hasTriggeredInitialLiveRefresh) {
            hasTriggeredInitialLiveRefresh = true;
            if (CustomNetwork.isNetworkAvailable(mContext)) {
                Log.d(TAG, "Triggering initial live data refresh");
                getLiveData();
            } else if (!hasOfflineData) {
                showCachedPricesIndicator();
                Log.d(TAG, "Offline and no cached data available");
            }
        }
    }

    private void showCachedPricesIndicator() {
        if (alSilver != null && !alSilver.isEmpty()) {
            SilverDBModel cached = alSilver.get(0);
            if (tvPriceHeading != null) {
                tvPriceHeading.setText(getString(
                        R.string.price_updated_cached_at,
                        cached.getDate(),
                        cached.getTime()));
            }
        } else {
            if (tvPriceHeading != null) {
                tvPriceHeading.setText(R.string.price_update_unavailable);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_silver_update) {
            getLiveData();
        } else if (id == R.id.btn_holding_update) {
            startActivity(new Intent(mContext, ResListActivity.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation, menu);
        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        if (darkModeItem != null) {
            darkModeItem.setChecked(preferences.getBoolean(PREF_DARK_MODE, false));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            startActivity(new Intent(mContext, ResListActivity.class));
            return true;
        }
        if (id == R.id.action_backup) {
            promptForBackupFileName(false);
            return true;
        }
        if (id == R.id.action_restore) {
            showAvailableBackupsForRestore();
            return true;
        }
        if (id == R.id.action_version_history) {
            showVersionHistory();
            return true;
        }
        if (id == R.id.action_dark_mode) {
            boolean isChecked = !item.isChecked();
            item.setChecked(isChecked);
            preferences.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void promptForBackupFileName(boolean restore) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setHint(R.string.backup_filename_hint);
        input.setText(BackupSyncManager.normalizeBackupFileName("holdings_backup"));
        input.setSelection(input.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(restore ? R.string.action_restore : R.string.action_backup)
                .setMessage(R.string.backup_filename_prompt)
                .setView(input)
                .setPositiveButton(restore ? R.string.action_restore : R.string.action_backup, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String fileName = input.getText().toString().trim();
            if (fileName.isEmpty()) {
                Toast.makeText(this, R.string.backup_filename_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String normalizedFileName = BackupSyncManager.normalizeBackupFileName(fileName);
            if (restore) {
                showRestoreModeDialog(normalizedFileName);
            } else {
                ioExecutor.execute(() -> {
                    BackupSyncManager.BackupResult exportResult = BackupSyncManager.exportSnapshotResult(this,
                            normalizedFileName, true);
                    runOnUiThread(() -> {
                        if (!exportResult.isSuccess()) {
                            Toast.makeText(this, getString(R.string.backup_failed, exportResult.getMessage()),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(this, exportResult.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
            }
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showAvailableBackupsForRestore() {
        List<String> availableBackups = BackupSyncManager.listAvailableBackupFiles(this);
        if (availableBackups.isEmpty()) {
            Toast.makeText(this, R.string.restore_no_file, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] backupNames = availableBackups.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_select_backup)
                .setItems(backupNames, (dialog, which) -> showRestoreModeDialog(backupNames[which]))
                .setPositiveButton(R.string.restore_enter_filename, (dialog, which) -> promptForBackupFileName(true))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRestoreModeDialog(String backupFileName) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_mode_title)
                .setItems(new CharSequence[] { getString(R.string.restore_mode_merge), getString(R.string.restore_mode_replace) },
                        (dialog, which) -> restoreBackupFile(backupFileName,
                                which == 1 ? BackupSyncManager.RestoreMode.REPLACE : BackupSyncManager.RestoreMode.MERGE))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restoreBackupFile(String backupFileName, BackupSyncManager.RestoreMode mode) {
        if (!BackupSyncManager.hasBackupFile(this, backupFileName)) {
            Toast.makeText(this, R.string.restore_no_file, Toast.LENGTH_SHORT).show();
            return;
        }
        ioExecutor.execute(() -> {
            BackupSyncManager.BackupResult restoreResult = BackupSyncManager.restoreNowResult(this, backupFileName, mode);
            runOnUiThread(() -> {
                if (!restoreResult.isSuccess()) {
                    Toast.makeText(this, getString(R.string.restore_failed, restoreResult.getMessage()), Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                migrateLegacyHoldings();
                refreshHoldingSummaries();
                Toast.makeText(this, restoreResult.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void insertdatainDB() {
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss a", Locale.US);
        time = mdformat.format(calendar.getTime());
        if (tvPriceHeading != null) {
            tvPriceHeading.setText(getString(R.string.price_updated_at, date, time));
        }

        if (slv_slv_price != null && !slv_slv_price.isEmpty()) {
            if (silverDB.getSilverData(date).isEmpty()) {
                silverDB.insertData("", slv_slv_price, slv_gld_price, date, time);
            } else {
                silverDB.updateData("1", slv_slv_price, slv_gld_price, date, time);
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getAPIdata();
        migrateLegacyHoldings();
        refreshHoldingSummaries();
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    public void generateData(String total) {
        if (total == null || total.equals("0") || total.equals(".00")) {
            return;
        }
        pendingDataToWrite = date + "," + total;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        executeFileWriting();
    }

    private void executeFileWriting() {
        if (pendingDataToWrite == null)
            return;
        String getPreferenceDate = preferences.getString("Date", "");
        if (getPreferenceDate.isEmpty() || !getPreferenceDate.equals(date)) {
            writeTextFile(pendingDataToWrite);
        }
        pendingDataToWrite = null;
    }

    @SuppressLint("NewApi")
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE },
                        REQUEST_ID_MULTIPLE_PERMISSIONS);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mContext, "Storage permission is required to add items.", Toast.LENGTH_LONG).show();
                shouldNavigateToResList = false;
            } else {
                BackupSyncManager.exportSnapshot(this);
            }
        }
    }

    private void writeTextFile(String sBody) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File root = new File(dir, mContext.getString(R.string.app_name));
            if (!root.exists() && !root.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return;
            }
            editor.putString("Date", date).apply();
            File gpxfile = new File(root, mContext.getString(R.string.app_name) + ".txt");
            FileWriter writer = new FileWriter(gpxfile, true);
            writer.append(sBody).append("\n");
            writer.flush();
            writer.close();
            Toast.makeText(mContext, "File data saved in Documents folder", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "generateData: failed", e);
        }
    }

    private static class HoldingSummary {
        final String itemType;
        final float totalUnits;

        HoldingSummary(String itemType, float totalUnits) {
            this.itemType = itemType;
            this.totalUnits = totalUnits;
        }
    }

    private Set<String> getTrackedMetalTypes() {
        Set<String> metalTypes = new LinkedHashSet<>();
        for (HoldingItem item : holdingsDb.getAllItems()) {
            String normalizedType = HoldingsDb.normalizeItemType(item.getItemType());
            if (isMetalType(normalizedType)) {
                metalTypes.add(normalizedType);
            }
        }
        return metalTypes;
    }

    private Set<String> getTrackedCryptoTypes() {
        Set<String> cryptoTypes = new LinkedHashSet<>();
        for (HoldingItem item : holdingsDb.getAllItems()) {
            String normalizedType = HoldingsDb.normalizeItemType(item.getItemType());
            if (isMetalType(normalizedType) || HoldingsDb.isStockTicker(normalizedType)) {
                continue;
            }
            if (CRYPTO_ID_BY_TYPE.containsKey(normalizedType)
                    || "Amount".equalsIgnoreCase(item.getWhat())
                    || cachedCryptoPrices.containsKey(normalizedType)) {
                cryptoTypes.add(normalizedType);
            }
        }
        return cryptoTypes;
    }

    private Set<String> getTrackedStockTickers() {
        Set<String> stockTickers = new LinkedHashSet<>();
        for (HoldingItem item : holdingsDb.getAllItems()) {
            String normalizedType = HoldingsDb.normalizeItemType(item.getItemType());
            if (HoldingsDb.isStockTicker(normalizedType)) {
                stockTickers.add(normalizedType);
                continue;
            }
            if ("Shares".equalsIgnoreCase(item.getWhat())) {
                String stockTicker = HoldingsDb.normalizeStockTickerInput(item.getItemType());
                if (!stockTicker.isEmpty()) {
                    stockTickers.add(stockTicker);
                }
            }
        }
        return stockTickers;
    }

    private void loadCachedCryptoPrices() {
        cachedCryptoPrices.clear();
        Map<String, ?> allPreferences = preferences.getAll();
        for (Map.Entry<String, ?> entry : allPreferences.entrySet()) {
            if (!entry.getKey().startsWith(PREF_CRYPTO_PRICE_PREFIX)) {
                continue;
            }
            String cryptoType = entry.getKey().substring(PREF_CRYPTO_PRICE_PREFIX.length());
            float value = 0;
            Object rawValue = entry.getValue();
            if (rawValue instanceof Float) {
                value = (Float) rawValue;
            } else if (rawValue instanceof Number) {
                value = ((Number) rawValue).floatValue();
            } else if (rawValue instanceof String) {
                value = safeParse((String) rawValue);
            }

            if (value > 0) {
                cachedCryptoPrices.put(cryptoType, value);
            }
        }
    }

    private void cacheCryptoPrice(String cryptoType, float price) {
        if (cryptoType == null || cryptoType.trim().isEmpty() || price <= 0) {
            return;
        }
        String normalizedType = HoldingsDb.normalizeItemType(cryptoType);
        liveCryptoPrices.put(normalizedType, price);
        cachedCryptoPrices.put(normalizedType, price);
        preferences.edit().putFloat(PREF_CRYPTO_PRICE_PREFIX + normalizedType, price).apply();
    }

    private void loadCachedStockPrices() {
        cachedStockPrices.clear();
        Map<String, ?> allPreferences = preferences.getAll();
        for (Map.Entry<String, ?> entry : allPreferences.entrySet()) {
            if (!entry.getKey().startsWith(PREF_STOCK_PRICE_PREFIX)) {
                continue;
            }

            String ticker = entry.getKey().substring(PREF_STOCK_PRICE_PREFIX.length());
            float value = 0;
            Object rawValue = entry.getValue();
            if (rawValue instanceof Float) {
                value = (Float) rawValue;
            } else if (rawValue instanceof Number) {
                value = ((Number) rawValue).floatValue();
            } else if (rawValue instanceof String) {
                value = safeParse((String) rawValue);
            }

            if (value > 0) {
                cachedStockPrices.put(ticker, value);
            }
        }
    }

    private void cacheStockPrice(String ticker, float price) {
        if (ticker == null || ticker.trim().isEmpty() || price <= 0) {
            return;
        }

        cachedStockPrices.put(ticker, price);
        preferences.edit().putFloat(PREF_STOCK_PRICE_PREFIX + ticker, price).apply();
    }

    private boolean isUiAlive() {
        if (isFinishing()) {
            return false;
        }
        return !isDestroyed();
    }

    private void showVersionHistory() {
        VersionManager versionManager = new VersionManager(this);
        java.util.List<VersionManager.VersionEntry> versions = versionManager.getAllVersions();

        if (versions.isEmpty()) {
            Toast.makeText(this, "No version history available", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder historyText = new StringBuilder();
        for (VersionManager.VersionEntry entry : versions) {
            historyText.append(entry.toString()).append("\n");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Version History")
                .setMessage(historyText.toString().trim())
                .setPositiveButton("OK", null)
                .create();
        dialog.show();
    }
}
