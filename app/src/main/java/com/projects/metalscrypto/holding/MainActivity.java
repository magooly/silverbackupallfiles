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
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TAG";
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;
    private static final String TYPE_SILVER = "Silver";
    private static final String TYPE_GOLD = "Gold";
    private static final String API_ERROR_LOG_FILE = "api_errors.txt";
    private static final String PREF_DARK_MODE = "pref_dark_mode";
    private static final long BACK_EXIT_INTERVAL_MS = 2000L;
    private static final String PREF_PRICE_NOTICE_ACTIVE = "pref_price_notice_active";
    private static final String PREF_PRICE_NOTICE_DISMISSED = "pref_price_notice_dismissed";
    private static final String PREF_DISPLAY_CURRENCY = "pref_display_currency";
    private static final String PREF_WHOLE_DOLLARS_ONLY = "pref_whole_dollars_only";
    private static final String PREF_WIDE_DASHBOARD_ALL_ITEMS = "pref_wide_dashboard_all_items";
    private static final String PREF_PORTFOLIO_VALUE_DATE = "pref_portfolio_value_date";
    private static final String PREF_PORTFOLIO_VALUE_CURRENT_AUD = "pref_portfolio_value_current_aud";
    private static final String PREF_PORTFOLIO_VALUE_PREVIOUS_AUD = "pref_portfolio_value_previous_aud";
    private static final String PREF_PORTFOLIO_VALUE_HISTORY_PREFIX = "pref_portfolio_value_history_";
    private static final int PORTFOLIO_VALUE_HISTORY_DAYS = 30;
    private static final String PREF_SPOT_FAVOURITES = "pref_spot_favourites";
    private static final String PREF_SPOT_QUOTE_PREFIX = "spot_quote_";
    private static final String PREF_FX_UPDATED_AT = "pref_fx_updated_at";
    private static final String PREF_FX_RATE_PREFIX = "pref_fx_rate_usd_";
    private static final long FX_RATE_REFRESH_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final String[] SUPPORTED_CURRENCIES = { "AUD", "NZD", "USD", "GBP", "EUR", "CNY", "JPY" };
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
    private TextView tvPortfolioValueLabel, tvPortfolioValue, tvPortfolioValueCurrency, tvPortfolioValueTrend;
    private View portfolioValueSummary;
    private PortfolioValueSparklineView portfolioValueSparkline;
    private TextView tv_hld_slv_price_amount, tv_hld_gld_amount, tv_hld_ttl_amount, tvDate, tvPriceHeading,
            tvAppVersion, tvPriceNotice;
    private Button btnClearPriceNotice;
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
    private LinearLayout allItemMarketHoldingRows;
    private RadioGroup dashboardViewToggle;
    private View spotFavouritesCard, holdingValuesCard, allItemsCard;
    private LinearLayout llPriceNotice;
    private final List<HoldingSummary> dynamicHoldings = new ArrayList<>();
    private final Map<String, Float> liveStockPrices = new HashMap<>();
    private final Map<String, Float> cachedStockPrices = new HashMap<>();
    private final Set<String> requestedStockTickers = new HashSet<>();
    private final Map<String, Float> liveCryptoPrices = new HashMap<>();
    private final Map<String, Float> cachedCryptoPrices = new HashMap<>();
    private final Map<String, Double> exchangeRatesFromUsd = new HashMap<>();
    private final Set<String> promptedCryptoTypes = new HashSet<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private String displayCurrency = "AUD";
    private boolean exchangeRateRequestInProgress;
    private final List<SpotPriceFavourite> spotFavourites = new ArrayList<>();
    private final Map<String, Float> latestSpotQuotes = new HashMap<>();
    private final Map<String, PortfolioValueTrend.Direction> spotPriceMovements = new HashMap<>();
    private LinearLayout spotFavouritesContainer;
    private long lastExitBackAt = -1L;
    private Toast exitBackToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        displayCurrency = normalizeDisplayCurrency(preferences.getString(PREF_DISPLAY_CURRENCY, "AUD"));
        loadCachedExchangeRates();
        loadSpotFavourites();
        boolean darkMode = preferences.getBoolean(PREF_DARK_MODE, false);
        AppCompatDelegate
                .setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long now = SystemClock.elapsedRealtime();
                if (lastExitBackAt >= 0 && now - lastExitBackAt < BACK_EXIT_INTERVAL_MS) {
                    if (exitBackToast != null) {
                        exitBackToast.cancel();
                    }
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                lastExitBackAt = now;
                if (exitBackToast != null) {
                    exitBackToast.cancel();
                }
                exitBackToast = Toast.makeText(MainActivity.this, R.string.press_back_again_to_exit,
                        Toast.LENGTH_SHORT);
                exitBackToast.show();
            }
        });
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        editor = preferences.edit();
        shouldNavigateToResList = false;

        setupViews();
        setupDashboardViewMode();
        updateCurrencyLabels();
        VersionManager versionManager = new VersionManager(this);
        versionManager.checkAndUpdateVersion();
        migrateLegacyHoldings();
        refreshExchangeRatesIfNeeded();
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

    @Override
    protected void onResume() {
        super.onResume();
        refreshHoldingSummaries();
        ioExecutor.execute(() -> {
            BackupSyncManager.importAndMergeOnStart(this);
            BackupSyncManager.exportSnapshot(this);
            runOnUiThread(() -> {
                refreshHoldingSummaries();
                getAPIdata();
            });
        });
    }

    @Override
    protected void onPause() {
        lastExitBackAt = -1L;
        if (exitBackToast != null) {
            exitBackToast.cancel();
            exitBackToast = null;
        }
        super.onPause();
    }

    private void getLiveData() {
        if (!CustomNetwork.isNetworkAvailable(mContext)) {
            return;
        }

        refreshExchangeRatesIfNeeded();
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
                appendApiErrorLog(
                        "Metals API key missing: set METALCHARTS_API_KEY in local.properties or environment.");
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
                        recordSpotQuote(SpotPriceFavourite.metal(metalType), metalPrice);
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
                        appendApiErrorLog("Live API unsuccessful. HTTP=" + response.code()
                                + ", message=missing tracked metal prices");
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
            for (SpotPriceFavourite favourite : spotFavourites) {
                if ("crypto".equals(favourite.kind) && favourite.name.equals(cryptoType)) {
                    id = favourite.key;
                    break;
                }
            }
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
                        recordSpotQuote(SpotPriceFavourite.crypto(cryptoType, cryptoId), cryptoPrice);
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
        requestedStockTickers.addAll(stockTickers);
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
                        acceptStockQuote(ticker, stockPrice);
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
                        acceptStockQuote(ticker, stockPrice);
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

        tv_hld_slv_price_amount = findViewById(R.id.tv_hld_slv_price_amount);
        tv_hld_gld_amount = findViewById(R.id.tv_hld_gld_amount);
        tv_hld_ttl_amount = findViewById(R.id.tv_hld_ttl_amount);
        tvPortfolioValueLabel = findViewById(R.id.tv_portfolio_value_label);
        tvPortfolioValue = findViewById(R.id.tv_portfolio_value);
        tvPortfolioValueCurrency = findViewById(R.id.tv_portfolio_value_currency);
        tvPortfolioValueTrend = findViewById(R.id.tv_portfolio_value_trend);
        portfolioValueSummary = findViewById(R.id.portfolio_value_summary);
        portfolioValueSparkline = findViewById(R.id.portfolio_value_sparkline);
        portfolioValueSparkline.setVisibility(
                getResources().getConfiguration().screenWidthDp >= 600 ? View.VISIBLE : View.GONE);
        tvPriceHeading = findViewById(R.id.tv_price_heading);
        spotFavouritesContainer = findViewById(R.id.spot_price_favourites_container);
        llDynamicHoldings = findViewById(R.id.ll_dynamic_holdings);
        llPriceNotice = findViewById(R.id.ll_price_notice);
        tvPriceNotice = findViewById(R.id.tv_price_notice);
        btnClearPriceNotice = findViewById(R.id.btn_clear_price_notice);
        tvDate = findViewById(R.id.tvDate);
        tvAppVersion = findViewById(R.id.tv_app_version);

        if (tvAppVersion != null) {
            tvAppVersion.setText(getString(R.string.version_display_format, BuildConfig.VERSION_NAME));
        }
        updateSpotPricesHeading();

        updateThemeColors();
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

    private void setupDashboardViewMode() {
        dashboardViewToggle = findViewById(R.id.dashboard_view_toggle);
        spotFavouritesCard = findViewById(R.id.card_spot_favourites);
        holdingValuesCard = findViewById(R.id.card_holding_values);
        allItemsCard = findViewById(R.id.card_all_items);
        allItemMarketHoldingRows = findViewById(R.id.all_item_market_holding_rows);

        boolean largeWindow = getResources().getConfiguration().screenWidthDp >= 600;
        if (!largeWindow) {
            dashboardViewToggle.setVisibility(View.GONE);
            applyDashboardView(false);
            return;
        }

        dashboardViewToggle.setVisibility(View.VISIBLE);
        boolean showAllItems = preferences.getBoolean(PREF_WIDE_DASHBOARD_ALL_ITEMS, true);
        dashboardViewToggle.check(showAllItems
                ? R.id.dashboard_view_all_items
                : R.id.dashboard_view_favourites);
        applyDashboardView(showAllItems);
        dashboardViewToggle.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != R.id.dashboard_view_all_items
                    && checkedId != R.id.dashboard_view_favourites) {
                return;
            }
            boolean allItemsSelected = checkedId == R.id.dashboard_view_all_items;
            preferences.edit().putBoolean(PREF_WIDE_DASHBOARD_ALL_ITEMS, allItemsSelected).apply();
            applyDashboardView(allItemsSelected);
        });
    }

    private void applyDashboardView(boolean showAllItems) {
        boolean useAllItemsView = getResources().getConfiguration().screenWidthDp >= 600 && showAllItems;
        spotFavouritesCard.setVisibility(useAllItemsView ? View.GONE : View.VISIBLE);
        holdingValuesCard.setVisibility(useAllItemsView ? View.GONE : View.VISIBLE);
        allItemsCard.setVisibility(useAllItemsView ? View.VISIBLE : View.GONE);
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
        float current_silver_price = lv_slv_amt > 0 ? lv_slv_amt : d_silver_amt;
        float current_gold_price = lv_gold_amt > 0 ? lv_gold_amt : d_gold_amt;
        DecimalFormat priceFormat = new DecimalFormat(
                preferences.getBoolean(PREF_WHOLE_DOLLARS_ONLY, false) ? "#,##0" : "#,##0.00");
        priceFormat.setRoundingMode(RoundingMode.HALF_UP);
        updateCurrencyLabels();
        renderSpotFavourites(priceFormat);

        Double silverHoldingAud = CurrencyConverter.convertHoldingValue(
                silver_total, current_silver_price, "USD", "AUD", exchangeRatesFromUsd);
        Double goldHoldingAud = CurrencyConverter.convertHoldingValue(
                gold_total, current_gold_price, "USD", "AUD", exchangeRatesFromUsd);
        boolean totalAvailable = silverHoldingAud != null && goldHoldingAud != null;
        double totalHoldingAud = (silverHoldingAud == null ? 0 : silverHoldingAud)
                + (goldHoldingAud == null ? 0 : goldHoldingAud);

        for (HoldingSummary summary : dynamicHoldings) {
            if (TYPE_SILVER.equalsIgnoreCase(summary.itemType) || TYPE_GOLD.equalsIgnoreCase(summary.itemType)) {
                continue;
            }
            Double holdingAud = getHoldingValueInAud(summary);
            if (holdingAud == null) {
                totalAvailable = false;
            } else {
                totalHoldingAud += holdingAud;
            }
        }

        tv_hld_slv_price_amount.setText(formatCurrencyValue(convertAudToDisplay(silverHoldingAud), priceFormat));
        tv_hld_gld_amount.setText(formatCurrencyValue(convertAudToDisplay(goldHoldingAud), priceFormat));
        renderDynamicHoldingRows(priceFormat);
        renderAllItemMarketHoldingRows(priceFormat);
        Double totalDisplay = totalAvailable
                ? CurrencyConverter.convert(totalHoldingAud, "AUD", displayCurrency, exchangeRatesFromUsd)
                : null;
        String portfolioAmount = totalDisplay == null
                ? getString(R.string.currency_value_unavailable)
                : currencySymbol(displayCurrency) + priceFormat.format(totalDisplay);
        tvPortfolioValue.setText(portfolioAmount);
        tvPortfolioValueCurrency.setText(displayCurrency);
        tvPortfolioValueLabel.setText(getString(R.string.standard_portfolio_value,
                new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date())));
        updatePortfolioValueTrend(totalAvailable ? totalHoldingAud : null);
        tv_hld_ttl_amount.setText(formatCurrencyValue(totalDisplay, priceFormat));
        updatePriceNotice(current_silver_price, current_gold_price);

        if (lv_slv_amt > 0 && totalAvailable) {
            generateData(new DecimalFormat("#,##0.00").format(totalHoldingAud));
        }
    }

    private void updatePortfolioValueTrend(Double currentValueAud) {
        PortfolioValueTrend.Direction direction = PortfolioValueTrend.Direction.NO_PREVIOUS;
        if (currentValueAud != null && !Double.isNaN(currentValueAud) && !Double.isInfinite(currentValueAud)) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String previousValueDate = preferences.getString(PREF_PORTFOLIO_VALUE_DATE, "");
            String previousSavedValue = preferences.getString(PREF_PORTFOLIO_VALUE_CURRENT_AUD, null);
            SharedPreferences.Editor trendEditor = preferences.edit();
            if (!today.equals(preferences.getString(PREF_PORTFOLIO_VALUE_DATE, ""))) {
                String lastValue = preferences.getString(PREF_PORTFOLIO_VALUE_CURRENT_AUD, null);
                if (lastValue == null) {
                    trendEditor.remove(PREF_PORTFOLIO_VALUE_PREVIOUS_AUD);
                } else {
                    trendEditor.putString(PREF_PORTFOLIO_VALUE_PREVIOUS_AUD, lastValue);
                }
                trendEditor.putString(PREF_PORTFOLIO_VALUE_DATE, today);
            }

            Double previousValueAud = null;
            String previousText = preferences.getString(PREF_PORTFOLIO_VALUE_PREVIOUS_AUD, null);
            if (previousText != null) {
                try {
                    previousValueAud = Double.parseDouble(previousText);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Ignoring invalid saved portfolio comparison value.", e);
                }
            }
            direction = PortfolioValueTrend.compare(currentValueAud, previousValueAud);
            trendEditor.putString(PREF_PORTFOLIO_VALUE_CURRENT_AUD, Double.toString(currentValueAud)).apply();
            recordPortfolioValueHistory(today, previousValueDate, previousSavedValue, currentValueAud);
        }

        int backgroundResource;
        int indicatorResource;
        int descriptionResource;
        switch (direction) {
            case UP:
                backgroundResource = R.color.portfolio_value_background_up;
                indicatorResource = R.string.portfolio_value_trend_up;
                descriptionResource = R.string.portfolio_value_trend_up_description;
                break;
            case DOWN:
                backgroundResource = R.color.portfolio_value_background_down;
                indicatorResource = R.string.portfolio_value_trend_down;
                descriptionResource = R.string.portfolio_value_trend_down_description;
                break;
            case UNCHANGED:
                backgroundResource = R.color.portfolio_value_background_neutral;
                indicatorResource = R.string.portfolio_value_trend_unchanged;
                descriptionResource = R.string.portfolio_value_trend_unchanged_description;
                break;
            default:
                backgroundResource = R.color.portfolio_value_background_neutral;
                indicatorResource = R.string.portfolio_value_trend_no_previous;
                descriptionResource = R.string.portfolio_value_trend_no_previous_description;
                break;
        }
        portfolioValueSummary.setBackgroundColor(ContextCompat.getColor(this, backgroundResource));
        tvPortfolioValue.setTextColor(Color.BLACK);
        tvPortfolioValueLabel.setTextColor(Color.BLACK);
        tvPortfolioValueCurrency.setTextColor(Color.BLACK);
        tvPortfolioValueTrend.setText(indicatorResource);
        tvPortfolioValueTrend.setTextColor(Color.BLACK);
        tvPortfolioValueTrend.setContentDescription(getString(descriptionResource));
        updatePortfolioValueHistoryGraph();
    }

    private void recordPortfolioValueHistory(String today, String previousDate, String previousValue,
            Double currentValueAud) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar firstDay = Calendar.getInstance();
        firstDay.set(Calendar.HOUR_OF_DAY, 0);
        firstDay.set(Calendar.MINUTE, 0);
        firstDay.set(Calendar.SECOND, 0);
        firstDay.set(Calendar.MILLISECOND, 0);
        firstDay.add(Calendar.DAY_OF_YEAR, -(PORTFOLIO_VALUE_HISTORY_DAYS - 1));
        String firstDate = dateFormat.format(firstDay.getTime());

        SharedPreferences.Editor historyEditor = preferences.edit();
        if (previousDate != null && !previousDate.equals(today)
                && previousDate.compareTo(firstDate) >= 0 && isValidPortfolioHistoryValue(previousValue)) {
            historyEditor.putString(PREF_PORTFOLIO_VALUE_HISTORY_PREFIX + previousDate, previousValue);
        }
        historyEditor.putString(PREF_PORTFOLIO_VALUE_HISTORY_PREFIX + today,
                Double.toString(currentValueAud));

        for (String key : preferences.getAll().keySet()) {
            if (!key.startsWith(PREF_PORTFOLIO_VALUE_HISTORY_PREFIX)) {
                continue;
            }
            String savedDate = key.substring(PREF_PORTFOLIO_VALUE_HISTORY_PREFIX.length());
            if (savedDate.compareTo(firstDate) < 0 || savedDate.compareTo(today) > 0) {
                historyEditor.remove(key);
            }
        }
        historyEditor.apply();
    }

    private boolean isValidPortfolioHistoryValue(String value) {
        if (value == null) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(value);
            return !Double.isNaN(parsed) && !Double.isInfinite(parsed);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void updatePortfolioValueHistoryGraph() {
        if (portfolioValueSparkline == null) {
            return;
        }
        float[] values = new float[PORTFOLIO_VALUE_HISTORY_DAYS];
        boolean[] hasValue = new boolean[PORTFOLIO_VALUE_HISTORY_DAYS];
        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        day.add(Calendar.DAY_OF_YEAR, -(PORTFOLIO_VALUE_HISTORY_DAYS - 1));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int index = 0; index < PORTFOLIO_VALUE_HISTORY_DAYS; index++) {
            String key = PREF_PORTFOLIO_VALUE_HISTORY_PREFIX + dateFormat.format(day.getTime());
            String savedValue = preferences.getString(key, null);
            if (isValidPortfolioHistoryValue(savedValue)) {
                double parsed = Double.parseDouble(savedValue);
                if (Math.abs(parsed) <= Float.MAX_VALUE) {
                    values[index] = (float) parsed;
                    hasValue[index] = true;
                }
            }
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
        portfolioValueSparkline.setDailyValues(values, hasValue);
    }

    private void renderDynamicHoldingRows(DecimalFormat formatter) {
        llDynamicHoldings.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (HoldingSummary summary : dynamicHoldings) {
            View row = inflater.inflate(R.layout.view_main_dynamic_holding_row, llDynamicHoldings, false);
            TextView title = row.findViewById(R.id.tv_dynamic_type);
            TextView amount = row.findViewById(R.id.tv_dynamic_amount);
            TextView currencySymbol = row.findViewById(R.id.tv_dynamic_currency_symbol);
            TextView currencyCode = row.findViewById(R.id.tv_dynamic_currency_code);
            Double holdingAud = getHoldingValueInAud(summary);

            title.setText(summary.itemType);
            amount.setText(formatCurrencyValue(convertAudToDisplay(holdingAud), formatter));
            currencySymbol.setText(currencySymbol(displayCurrency));
            currencyCode.setText(displayCurrency);
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

    private void renderAllItemMarketHoldingRows(DecimalFormat formatter) {
        if (allItemMarketHoldingRows == null) {
            return;
        }
        allItemMarketHoldingRows.removeAllViews();
        List<HoldingSummary> summaries = new ArrayList<>();
        if (silver_total > 0) {
            summaries.add(new HoldingSummary(TYPE_SILVER, silver_total));
        }
        if (gold_total > 0) {
            summaries.add(new HoldingSummary(TYPE_GOLD, gold_total));
        }
        summaries.addAll(dynamicHoldings);

        if (summaries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setPadding(8, 12, 8, 12);
            empty.setText(R.string.all_items_empty);
            empty.setTextColor(ContextCompat.getColor(this, R.color.textview_text_color));
            allItemMarketHoldingRows.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (HoldingSummary summary : summaries) {
            View row = inflater.inflate(R.layout.view_all_item_market_holding_row,
                    allItemMarketHoldingRows, false);
            TextView item = row.findViewById(R.id.tv_all_item_name);
            TextView marketSymbol = row.findViewById(R.id.tv_all_item_market_symbol);
            TextView marketAmount = row.findViewById(R.id.tv_all_item_market_amount);
            TextView marketCurrency = row.findViewById(R.id.tv_all_item_market_currency);
            TextView quantity = row.findViewById(R.id.tv_all_item_quantity);
            TextView holdingSymbol = row.findViewById(R.id.tv_all_item_holding_symbol);
            TextView holdingAmount = row.findViewById(R.id.tv_all_item_holding_amount);
            TextView holdingCurrency = row.findViewById(R.id.tv_all_item_holding_currency);

            float marketPrice = getCurrentPriceByType(summary.itemType);
            boolean hasMarketPrice = isApiLookupType(summary.itemType) && marketPrice > 0;
            String sourceCurrency = getPriceCurrency(summary.itemType);
            Double convertedMarketPrice = hasMarketPrice
                    ? CurrencyConverter.convert(marketPrice, sourceCurrency, displayCurrency, exchangeRatesFromUsd)
                    : null;
            String marketDisplayCurrency = convertedMarketPrice == null && hasMarketPrice
                    ? sourceCurrency
                    : displayCurrency;
            Double displayedMarketPrice = convertedMarketPrice;
            if (displayedMarketPrice == null && hasMarketPrice) {
                displayedMarketPrice = (double) marketPrice;
            }
            Double holdingValue = convertAudToDisplay(getHoldingValueInAud(summary));

            item.setText(getAllItemsDisplayName(summary.itemType));
            marketAmount.setText(formatCurrencyValue(displayedMarketPrice, formatter));
            marketAmount.setTextColor(getSpotPriceMovementColor(getSpotFavouriteForType(summary.itemType)));
            marketSymbol.setText(currencySymbol(marketDisplayCurrency));
            marketCurrency.setText(marketDisplayCurrency);
            quantity.setText(formatHeldQuantity(summary));
            holdingAmount.setText(formatCurrencyValue(holdingValue, formatter));
            holdingSymbol.setText(currencySymbol(displayCurrency));
            holdingCurrency.setText(displayCurrency);
            allItemMarketHoldingRows.addView(row);
        }
    }

    private String formatHeldQuantity(HoldingSummary summary) {
        String normalizedType = HoldingsDb.normalizeItemType(summary.itemType);
        String unit;
        if (isMetalType(normalizedType)) {
            unit = getString(R.string.quantity_unit_ounces);
        } else if (isStockLookupType(normalizedType) || HoldingsDb.isStockTicker(normalizedType)) {
            unit = Math.abs(summary.totalUnits) == 1F
                    ? getString(R.string.quantity_unit_share)
                    : getString(R.string.quantity_unit_shares);
        } else if (isCryptoLookupType(normalizedType)) {
            unit = Math.abs(summary.totalUnits) == 1F
                    ? getString(R.string.quantity_unit_coin)
                    : getString(R.string.quantity_unit_coins);
        } else {
            return getString(R.string.all_items_quantity_unavailable);
        }
        DecimalFormat quantityFormat = new DecimalFormat("#,##0.####");
        return getString(R.string.all_items_quantity_format,
                quantityFormat.format(summary.totalUnits), unit);
    }

    private String getAllItemsDisplayName(String itemType) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        String unit = isMetalType(normalizedType) ? "oz"
                : isStockLookupType(normalizedType) ? "share"
                        : isCryptoLookupType(normalizedType) ? "coin" : null;
        return unit == null ? itemType : getString(R.string.spot_asset_unit, itemType, unit);
    }

    private SpotPriceFavourite getSpotFavouriteForType(String itemType) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        for (SpotPriceFavourite favourite : spotFavourites) {
            if (favourite.name.equalsIgnoreCase(normalizedType)) {
                return favourite;
            }
        }
        if (isMetalType(normalizedType)) {
            return SpotPriceFavourite.metal(normalizedType);
        }
        if (isStockLookupType(normalizedType) || HoldingsDb.isStockTicker(normalizedType)) {
            return SpotPriceFavourite.stock(normalizedType);
        }
        if (isCryptoLookupType(normalizedType)) {
            String cryptoId = resolveCryptoId(normalizedType);
            return cryptoId.isEmpty() ? null : SpotPriceFavourite.crypto(normalizedType, cryptoId);
        }
        return null;
    }

    private int getSpotPriceMovementColor(SpotPriceFavourite favourite) {
        PortfolioValueTrend.Direction direction = favourite == null
                ? PortfolioValueTrend.Direction.NO_PREVIOUS
                : spotPriceMovements.get(favourite.id());
        if (direction == PortfolioValueTrend.Direction.UP) {
            return ContextCompat.getColor(this, R.color.market_price_up);
        }
        if (direction == PortfolioValueTrend.Direction.DOWN) {
            return ContextCompat.getColor(this, R.color.red_font2);
        }
        return ContextCompat.getColor(this, R.color.textview_text_color);
    }

    private Double getHoldingValueInAud(HoldingSummary summary) {
        float currentPrice = getCurrentPriceByType(summary.itemType);
        if (isApiLookupType(summary.itemType)) {
            return CurrencyConverter.convertHoldingValue(
                    summary.totalUnits,
                    currentPrice,
                    getPriceCurrency(summary.itemType),
                    "AUD",
                    exchangeRatesFromUsd);
        }
        return (double) summary.totalUnits;
    }

    private String getPriceCurrency(String itemType) {
        return isMetalType(HoldingsDb.normalizeItemType(itemType)) ? "USD" : "AUD";
    }

    private Double convertAudToDisplay(Double amountAud) {
        return amountAud == null
                ? null
                : CurrencyConverter.convert(amountAud, "AUD", displayCurrency, exchangeRatesFromUsd);
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
                || isStockLookupType(normalizedType);
    }

    private boolean isStockLookupType(String itemType) {
        String ticker = HoldingsDb.normalizeStockTickerInput(itemType);
        return HoldingsDb.isStockTicker(itemType)
                || liveStockPrices.containsKey(ticker)
                || cachedStockPrices.containsKey(ticker);
    }

    private float getCurrentPriceByType(String itemType) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        if (TYPE_SILVER.equalsIgnoreCase(normalizedType))
            return lv_slv_amt > 0 ? lv_slv_amt : d_silver_amt;
        if (TYPE_GOLD.equalsIgnoreCase(normalizedType))
            return lv_gold_amt > 0 ? lv_gold_amt : d_gold_amt;
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
        if (isStockLookupType(normalizedType)) {
            String ticker = HoldingsDb.normalizeStockTickerInput(normalizedType);
            Float livePrice = liveStockPrices.get(ticker);
            if (livePrice != null && livePrice > 0) {
                return livePrice;
            }
            Float cachedPrice = cachedStockPrices.get(ticker);
            if (cachedPrice != null && cachedPrice > 0) {
                return cachedPrice;
            }
            SpotPriceFavourite stock = SpotPriceFavourite.stock(ticker);
            if (stock != null) {
                Float favouriteQuote = latestSpotQuotes.get(stock.id());
                return favouriteQuote != null ? favouriteQuote
                        : preferences.getFloat(PREF_SPOT_QUOTE_PREFIX + stock.id(), 0F);
            }
            return 0;
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
            if (CustomNetwork.isNetworkAvailable(mContext)) {
                hasTriggeredInitialLiveRefresh = true;
                Log.d(TAG, "Triggering initial live data refresh");
                getLiveData();
            } else if (!hasOfflineData) {
                showCachedPricesIndicator();
                Log.d(TAG, "Offline and no cached data available");
            }
        } else if (CustomNetwork.isNetworkAvailable(mContext)) {
            Set<String> newStockTickers = getTrackedStockTickers();
            newStockTickers.removeAll(requestedStockTickers);
            getStockLiveData(newStockTickers);
        }
    }

    private void showCachedPricesIndicator() {
        updateSpotPricesHeading();
    }

    private void updateSpotPricesHeading() {
        if (tvPriceHeading != null) {
            tvPriceHeading.setText(R.string.spot_prices_heading);
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
        if (id == R.id.action_update_holding) {
            startActivity(new Intent(mContext, ResListActivity.class));
            return true;
        }
        if (id == R.id.action_get_latest_prices) {
            getLiveData();
            return true;
        }
        if (id == R.id.action_display_currency) {
            showDisplayCurrencyPicker();
            return true;
        }
        if (id == R.id.action_spot_favourites) {
            showSpotFavouritesEditor();
            return true;
        }
        if (id == R.id.action_about) {
            Intent intent = new Intent(mContext, ResListActivity.class);
            intent.putExtra(ResListActivity.EXTRA_OPEN_ABOUT_ON_LAUNCH, true);
            startActivity(intent);
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

    private String normalizeDisplayCurrency(String currency) {
        if (currency != null) {
            for (String supportedCurrency : SUPPORTED_CURRENCIES) {
                if (supportedCurrency.equalsIgnoreCase(currency.trim())) {
                    return supportedCurrency;
                }
            }
        }
        return "AUD";
    }

    private void showDisplayCurrencyPicker() {
        int selectedIndex = 0;
        for (int i = 0; i < SUPPORTED_CURRENCIES.length; i++) {
            if (SUPPORTED_CURRENCIES[i].equals(displayCurrency)) {
                selectedIndex = i;
                break;
            }
        }

        AppCompatCheckBox wholeDollarsOnly = new AppCompatCheckBox(this);
        wholeDollarsOnly.setText(R.string.whole_dollars_only);
        wholeDollarsOnly.setChecked(preferences.getBoolean(PREF_WHOLE_DOLLARS_ONLY, false));
        wholeDollarsOnly.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(PREF_WHOLE_DOLLARS_ONLY, checked).apply();
            updateUI();
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_display_currency)
                .setView(wholeDollarsOnly)
                .setSingleChoiceItems(R.array.display_currency_options, selectedIndex, (dialog, which) -> {
                    if (which >= 0 && which < SUPPORTED_CURRENCIES.length) {
                        displayCurrency = SUPPORTED_CURRENCIES[which];
                        preferences.edit().putString(PREF_DISPLAY_CURRENCY, displayCurrency).apply();
                        updateUI();
                        refreshExchangeRatesIfNeeded();
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void loadCachedExchangeRates() {
        exchangeRatesFromUsd.clear();
        exchangeRatesFromUsd.put("USD", 1.0);
        for (String currency : SUPPORTED_CURRENCIES) {
            if ("USD".equals(currency)) {
                continue;
            }
            String storedRate = preferences.getString(PREF_FX_RATE_PREFIX + currency, "");
            try {
                double rate = Double.parseDouble(storedRate);
                if (rate > 0) {
                    exchangeRatesFromUsd.put(currency, rate);
                }
            } catch (NumberFormatException ignored) {
                exchangeRatesFromUsd.remove(currency);
            }
        }
    }

    private boolean hasCompleteExchangeRates() {
        for (String currency : SUPPORTED_CURRENCIES) {
            if (!exchangeRatesFromUsd.containsKey(currency)) {
                return false;
            }
        }
        return true;
    }

    private void refreshExchangeRatesIfNeeded() {
        long updatedAt = preferences.getLong(PREF_FX_UPDATED_AT, 0);
        if (exchangeRateRequestInProgress
                || (hasCompleteExchangeRates() && System.currentTimeMillis() - updatedAt < FX_RATE_REFRESH_INTERVAL_MS)
                || !CustomNetwork.isNetworkAvailable(mContext)) {
            return;
        }

        String apiKey = BuildConfig.METALCHARTS_API_KEY == null ? "" : BuildConfig.METALCHARTS_API_KEY.trim();
        if (apiKey.isEmpty()) {
            appendApiErrorLog("FX API key missing: set METALCHARTS_API_KEY in local.properties or environment.");
            return;
        }

        exchangeRateRequestInProgress = true;
        ApiWebservices apiWebservices = ApiHelperClass.getClient().create(ApiWebservices.class);
        Call<JsonObject> call = apiWebservices.getCurrencyRates("Bearer " + apiKey);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                exchangeRateRequestInProgress = false;
                if (!isUiAlive()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null
                        || !response.body().has("base")
                        || !"USD".equalsIgnoreCase(response.body().get("base").getAsString())
                        || !response.body().has("rates")
                        || !response.body().get("rates").isJsonObject()) {
                    appendApiErrorLog("FX API response unsuccessful or did not use USD as its base.");
                    return;
                }

                JsonObject responseRates = response.body().getAsJsonObject("rates");
                Map<String, Double> updatedRates = new HashMap<>();
                updatedRates.put("USD", 1.0);
                try {
                    for (String currency : SUPPORTED_CURRENCIES) {
                        if ("USD".equals(currency)) {
                            continue;
                        }
                        if (!responseRates.has(currency)) {
                            throw new IllegalStateException("Missing rate for " + currency);
                        }
                        double rate = responseRates.get(currency).getAsDouble();
                        if (rate <= 0) {
                            throw new IllegalStateException("Invalid rate for " + currency);
                        }
                        updatedRates.put(currency, rate);
                    }
                } catch (Exception e) {
                    appendApiErrorLog("FX API response missing a supported currency rate: " + e.getMessage());
                    return;
                }

                exchangeRatesFromUsd.clear();
                exchangeRatesFromUsd.putAll(updatedRates);
                SharedPreferences.Editor rateEditor = preferences.edit()
                        .putLong(PREF_FX_UPDATED_AT, System.currentTimeMillis());
                for (Map.Entry<String, Double> entry : updatedRates.entrySet()) {
                    rateEditor.putString(PREF_FX_RATE_PREFIX + entry.getKey(), String.valueOf(entry.getValue()));
                }
                rateEditor.apply();
                updateUI();
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable throwable) {
                exchangeRateRequestInProgress = false;
                if (isUiAlive()) {
                    appendApiErrorLog("FX API request failed: " + throwable.getMessage());
                }
            }
        });
    }

    private String currencySymbol(String currency) {
        switch (currency) {
            case "GBP":
                return "\u00A3";
            case "EUR":
                return "\u20AC";
            case "CNY":
                return "CN\u00A5";
            case "JPY":
                return "\u00A5";
            default:
                return "$";
        }
    }

    private void updateCurrencyLabels() {
        int[] symbolIds = {
                R.id.tv_currency_symbol_silver_holding,
                R.id.tv_currency_symbol_gold_holding,
                R.id.tv_currency_symbol_copper_holding,
                R.id.tv_currency_symbol_total
        };
        int[] codeIds = {
                R.id.tv_currency_code_silver_holding,
                R.id.tv_currency_code_gold_holding,
                R.id.tv_currency_code_copper_holding,
                R.id.tv_currency_code_total
        };
        String symbol = currencySymbol(displayCurrency);
        for (int symbolId : symbolIds) {
            TextView label = findViewById(symbolId);
            if (label != null) {
                label.setText(symbol);
            }
        }
        for (int codeId : codeIds) {
            TextView label = findViewById(codeId);
            if (label != null) {
                label.setText(displayCurrency);
            }
        }
    }

    private String formatCurrencyValue(Double amount, DecimalFormat formatter) {
        return amount == null ? getString(R.string.currency_value_unavailable) : formatter.format(amount);
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
                .setItems(
                        new CharSequence[] { getString(R.string.restore_mode_merge),
                                getString(R.string.restore_mode_replace) },
                        (dialog, which) -> restoreBackupFile(backupFileName,
                                which == 1 ? BackupSyncManager.RestoreMode.REPLACE
                                        : BackupSyncManager.RestoreMode.MERGE))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restoreBackupFile(String backupFileName, BackupSyncManager.RestoreMode mode) {
        if (!BackupSyncManager.hasBackupFile(this, backupFileName)) {
            Toast.makeText(this, R.string.restore_no_file, Toast.LENGTH_SHORT).show();
            return;
        }
        ioExecutor.execute(() -> {
            BackupSyncManager.BackupResult restoreResult = BackupSyncManager.restoreNowResult(this, backupFileName,
                    mode);
            runOnUiThread(() -> {
                if (!restoreResult.isSuccess()) {
                    Toast.makeText(this, getString(R.string.restore_failed, restoreResult.getMessage()),
                            Toast.LENGTH_LONG)
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
        updateSpotPricesHeading();

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
        metalTypes.add(TYPE_GOLD);
        metalTypes.add(TYPE_SILVER);
        for (SpotPriceFavourite favourite : spotFavourites) {
            if ("metal".equals(favourite.kind)) {
                metalTypes.add(favourite.key);
            }
        }
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
        for (SpotPriceFavourite favourite : spotFavourites) {
            if ("crypto".equals(favourite.kind)) {
                cryptoTypes.add(favourite.name);
            }
        }
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
        for (SpotPriceFavourite favourite : spotFavourites) {
            if ("stock".equals(favourite.kind)) {
                stockTickers.add(favourite.key);
            }
        }
        for (HoldingItem item : holdingsDb.getAllItems()) {
            String normalizedType = HoldingsDb.normalizeItemType(item.getItemType());
            if (isStockLookupType(normalizedType)) {
                stockTickers.add(HoldingsDb.normalizeStockTickerInput(normalizedType));
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

    private void loadSpotFavourites() {
        spotFavourites.clear();
        if (!preferences.contains(PREF_SPOT_FAVOURITES)) {
            spotFavourites.addAll(SpotPriceFavourite.defaults());
            return;
        }
        spotFavourites.addAll(SpotPriceFavourite.restore(preferences.getString(PREF_SPOT_FAVOURITES, "[]")));
    }

    private void saveSpotFavourites(List<SpotPriceFavourite> selected) {
        spotFavourites.clear();
        spotFavourites.addAll(selected);
        preferences.edit().putString(PREF_SPOT_FAVOURITES, SpotPriceFavourite.serialize(spotFavourites)).apply();
        updateUI();
        getLiveData();
    }

    private void recordSpotQuote(SpotPriceFavourite favourite, float price) {
        if (favourite != null && price > 0 && !Float.isNaN(price) && !Float.isInfinite(price)) {
            String quoteKey = PREF_SPOT_QUOTE_PREFIX + favourite.id();
            Float previousPrice = latestSpotQuotes.get(favourite.id());
            if (previousPrice == null) {
                float savedPrice = preferences.getFloat(quoteKey, 0F);
                previousPrice = savedPrice > 0 ? savedPrice : null;
            }
            spotPriceMovements.put(favourite.id(), PortfolioValueTrend.compare(
                    (double) price, previousPrice == null ? null : (double) previousPrice));
            latestSpotQuotes.put(favourite.id(), price);
            preferences.edit().putFloat(quoteKey, price).apply();
        }
    }

    private void renderSpotFavourites(DecimalFormat formatter) {
        spotFavouritesContainer.removeAllViews();
        if (spotFavourites.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.spot_empty);
            spotFavouritesContainer.addView(empty);
            return;
        }
        for (SpotPriceFavourite favourite : spotFavourites) {
            Float latest = latestSpotQuotes.get(favourite.id());
            float price = latest != null ? latest
                    : preferences.getFloat(PREF_SPOT_QUOTE_PREFIX + favourite.id(), 0F);
            if (price <= 0) {
                price = getCurrentPriceByType(favourite.name);
            }
            Double converted = price > 0
                    ? CurrencyConverter.convert(price, favourite.currency, displayCurrency, exchangeRatesFromUsd)
                    : null;
            String rowCurrency = converted == null && price > 0 ? favourite.currency : displayCurrency;
            Double displayedPrice = converted;
            if (displayedPrice == null && price > 0) {
                displayedPrice = (double) price;
            }
            View row = getLayoutInflater().inflate(R.layout.view_spot_price_row, spotFavouritesContainer, false);
            TextView label = row.findViewById(R.id.spot_asset_label);
            TextView amount = row.findViewById(R.id.spot_price_amount);
            TextView symbol = row.findViewById(R.id.spot_currency_symbol);
            TextView currency = row.findViewById(R.id.spot_currency_code);
            label.setText(getString(R.string.spot_asset_unit, favourite.name, favourite.unit));
            amount.setText(formatCurrencyValue(displayedPrice, formatter));
            amount.setTextColor(getSpotPriceMovementColor(favourite));
            symbol.setText(currencySymbol(rowCurrency));
            currency.setText(rowCurrency);
            spotFavouritesContainer.addView(row);
        }
    }

    private void showSpotFavouritesEditor() {
        List<SpotPriceFavourite> selected = new ArrayList<>(spotFavourites);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(rows);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.spot_favourites)
                .setView(scroll)
                .setPositiveButton(R.string.save, (button, which) -> saveSpotFavourites(selected))
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.spot_add_asset, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            renderSpotEditor(rows, selected, dialog);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(button -> showSpotAssetChooser(rows, selected, dialog));
        });
        dialog.show();
    }

    private void renderSpotEditor(LinearLayout rows, List<SpotPriceFavourite> selected, AlertDialog dialog) {
        rows.removeAllViews();
        dialog.setTitle(getString(R.string.spot_favourites_title, selected.size()));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(selected.size() < SpotPriceFavourite.LIMIT);
        if (selected.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.spot_empty);
            rows.addView(empty);
        }
        for (int position = 0; position < selected.size(); position++) {
            final int index = position;
            SpotPriceFavourite favourite = selected.get(index);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView label = new TextView(this);
            label.setText(getString(R.string.spot_asset_unit, favourite.name, favourite.unit));
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1F));
            ImageButton up = spotEditorButton(android.R.drawable.arrow_up_float,
                    getString(R.string.spot_move_up, favourite.name), button -> {
                        Collections.swap(selected, index, index - 1);
                        renderSpotEditor(rows, selected, dialog);
                    });
            up.setEnabled(index > 0);
            row.addView(up);
            ImageButton down = spotEditorButton(android.R.drawable.arrow_down_float,
                    getString(R.string.spot_move_down, favourite.name), button -> {
                        Collections.swap(selected, index, index + 1);
                        renderSpotEditor(rows, selected, dialog);
                    });
            down.setEnabled(index < selected.size() - 1);
            row.addView(down);
            row.addView(spotEditorButton(android.R.drawable.ic_menu_delete,
                    getString(R.string.spot_remove, favourite.name), button -> {
                        selected.remove(index);
                        renderSpotEditor(rows, selected, dialog);
                    }));
            rows.addView(row);
        }
    }

    private ImageButton spotEditorButton(int icon, String description, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);
        int size = Math.round(48 * getResources().getDisplayMetrics().density);
        button.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        button.setImageResource(icon);
        button.setContentDescription(description);
        ViewCompat.setTooltipText(button, description);
        button.setOnClickListener(listener);
        return button;
    }

    private void showSpotAssetChooser(LinearLayout rows, List<SpotPriceFavourite> selected, AlertDialog editor) {
        Map<String, SpotPriceFavourite> options = new LinkedHashMap<>();
        for (String metal : new String[] { "Gold", "Silver", "Platinum", "Palladium" }) {
            SpotPriceFavourite asset = SpotPriceFavourite.metal(metal);
            options.put(asset.id(), asset);
        }
        for (Map.Entry<String, String> coin : CRYPTO_ID_BY_TYPE.entrySet()) {
            SpotPriceFavourite asset = SpotPriceFavourite.crypto(coin.getKey(), coin.getValue());
            if (!options.containsKey(asset.id())) {
                options.put(asset.id(), asset);
            }
        }
        for (HoldingItem item : holdingsDb.getAllItems()) {
            String type = HoldingsDb.normalizeItemType(item.getItemType());
            SpotPriceFavourite asset = null;
            if (isStockLookupType(type) || "Shares".equalsIgnoreCase(item.getWhat())) {
                asset = SpotPriceFavourite.stock(type);
            } else if (isCryptoLookupType(type) || "Amount".equalsIgnoreCase(item.getWhat())) {
                asset = SpotPriceFavourite.crypto(type, resolveCryptoId(type));
            }
            if (asset != null) {
                options.put(asset.id(), asset);
            }
        }
        for (SpotPriceFavourite favourite : selected) {
            options.remove(favourite.id());
        }
        List<SpotPriceFavourite> assets = new ArrayList<>(options.values());
        String[] labels = new String[assets.size()];
        for (int index = 0; index < assets.size(); index++) {
            SpotPriceFavourite asset = assets.get(index);
            labels[index] = getString(R.string.spot_asset_kind,
                    getString(R.string.spot_asset_unit, asset.name, asset.unit),
                    "stock".equals(asset.kind) ? getString(R.string.category_stocks)
                            : "metal".equals(asset.kind) ? getString(R.string.category_metals)
                                    : getString(R.string.category_crypto));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.spot_add_asset)
                .setItems(labels, (dialog, which) -> {
                    if (!SpotPriceFavourite.add(selected, assets.get(which))) {
                        Toast.makeText(this, R.string.spot_limit_or_duplicate, Toast.LENGTH_SHORT).show();
                    }
                    renderSpotEditor(rows, selected, editor);
                })
                .setNeutralButton(R.string.spot_add_stock, (dialog, which) -> promptSpotStock(rows, selected, editor))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void promptSpotStock(LinearLayout rows, List<SpotPriceFavourite> selected, AlertDialog editor) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint(R.string.spot_stock_hint);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.spot_add_stock)
                .setView(input)
                .setPositiveButton(R.string.add, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            SpotPriceFavourite asset = SpotPriceFavourite.stock(input.getText().toString());
            if (asset == null) {
                input.setError(getString(R.string.spot_invalid_stock));
                return;
            }
            if (!SpotPriceFavourite.add(selected, asset)) {
                input.setError(getString(R.string.spot_limit_or_duplicate));
                return;
            }
            renderSpotEditor(rows, selected, editor);
            dialog.dismiss();
        }));
        dialog.show();
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

    private void acceptStockQuote(String ticker, float price) {
        recordSpotQuote(SpotPriceFavourite.stock(ticker), price);
        boolean belongsToPortfolio = cachedStockPrices.containsKey(ticker);
        for (HoldingItem item : holdingsDb.getAllItems()) {
            if (("Shares".equalsIgnoreCase(item.getWhat()) || HoldingsDb.isStockTicker(item.getItemType()))
                    && ticker.equals(HoldingsDb.normalizeStockTickerInput(item.getItemType()))) {
                belongsToPortfolio = true;
                break;
            }
        }
        if (belongsToPortfolio) {
            liveStockPrices.put(ticker, price);
            cacheStockPrice(ticker, price);
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

        Log.d(TAG, "Version history retrieved: " + versions.size() + " entries");

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
