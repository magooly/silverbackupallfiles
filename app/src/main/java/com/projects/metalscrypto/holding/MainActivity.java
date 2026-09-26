package com.projects.metalscrypto.holding;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;
import com.projects.metalscrypto.holding.DBManager.GoldT_DB;
import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;
import com.projects.metalscrypto.holding.DBManager.SilverDB;
import com.projects.metalscrypto.holding.DBManager.SilverDBModel;
import com.projects.metalscrypto.holding.DBManager.SilverT_DB;
import com.projects.metalscrypto.holding.Retrofit.CryptoApiHelperClass;
import com.projects.metalscrypto.holding.Retrofit.CryptoApiWebservices;
import com.projects.metalscrypto.holding.Retrofit.ApiHelperClass;
import com.projects.metalscrypto.holding.Retrofit.ApiWebservices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private Context mContext;
    private EditText et_slv_slv_price_amount, et_slv_gld_amount;
    private TextView tv_hld_slv_price_amount, tv_hld_gld_amount, tv_hld_ttl_amount, tvDate, tvPriceHeading, tvAppVersion;
    private SwitchCompat darkModeSwitch;
    private Button btn_holding_update, btn_silver_update;
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
    private LinearLayout llDynamicHoldings;
    private final List<HoldingSummary> dynamicHoldings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        boolean darkMode = preferences.getBoolean(PREF_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        editor = preferences.edit();
        editor.clear();
        editor.apply();
        shouldNavigateToResList = false;

        setupViews();
        getAPIdata();
        migrateLegacyHoldings();
        BackupSyncManager.importAndMergeOnStart(this);
        refreshHoldingSummaries();
        BackupSyncManager.exportSnapshot(this);
        checkAndRequestPermissions();
    }

    private void getLiveData() {
        if (!CustomNetwork.isNetworkAvailable(mContext)) {
            return;
        }

        // Refresh crypto prices from a dedicated endpoint at app open.
        getCryptoLiveData();

        ApiWebservices apiWebservices = ApiHelperClass.getClient().create(ApiWebservices.class);
        Call<PriceModel> call = apiWebservices.getPriceRes(
                "885450b505ea1cde039b28b14e1935c7",
                "AUD",
                "XAU,XAG"
        );

        call.enqueue(new Callback<PriceModel>() {
            @Override
            public void onResponse(@NonNull Call<PriceModel> call, @NonNull Response<PriceModel> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    PriceModel priceModelRes = response.body();
                    lv_gold_amt = (float) priceModelRes.getRates().getAudxau();
                    lv_slv_amt = (float) priceModelRes.getRates().getAudxag();
                    lv_cpr_amt = (float) priceModelRes.getRates().getAudxcu();
                    lv_platinum_amt = (float) priceModelRes.getRates().getAudxpt();
                    lv_palladium_amt = (float) priceModelRes.getRates().getAudxpd();
                    lv_lead_amt = (float) priceModelRes.getRates().getAudxld();
                    lv_aluminum_amt = (float) priceModelRes.getRates().getAudxal();
                    lv_nickel_amt = (float) priceModelRes.getRates().getAudxni();
                    lv_zinc_amt = (float) priceModelRes.getRates().getAudxzn();
                    // BTC/ETH are loaded by getCryptoLiveData(); do not overwrite them here.

                    slv_gld_price = String.valueOf(lv_gold_amt);
                    slv_slv_price = String.valueOf(lv_slv_amt);
                    slv_cpr_price = String.valueOf(lv_cpr_amt);

                    updateUI();
                    insertdatainDB();
                } else {
                    String apiMessage = "unknown";
                    if (response.body() != null && response.body().getError() != null) {
                        apiMessage = response.body().getError().getMessage();
                    }
                    String errorEntry = "Live API unsuccessful. HTTP=" + response.code() + ", message=" + apiMessage;
                    appendApiErrorLog(errorEntry);
                    Log.w(TAG, "Live response unsuccessful, using cached prices");
                    showCachedPricesIndicator();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PriceModel> call, @NonNull Throwable t) {
                appendApiErrorLog("Live API failure: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                Log.e(TAG, "Failed to fetch live data", t);
                showCachedPricesIndicator();
            }
        });
    }

    private void getCryptoLiveData() {
        CryptoApiWebservices cryptoApiWebservices = CryptoApiHelperClass.getClient().create(CryptoApiWebservices.class);
        Call<JsonObject> call = cryptoApiWebservices.getCryptoPrices("bitcoin,ethereum", "aud");

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    lv_btc_amt = parseCryptoAud(body, "bitcoin");
                    lv_eth_amt = parseCryptoAud(body, "ethereum");
                    updateUI();
                } else {
                    appendApiErrorLog("Crypto API unsuccessful. HTTP=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                appendApiErrorLog("Crypto API failure: " + t.getClass().getSimpleName() + " - " + t.getMessage());
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
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void setupViews() {
        mContext = MainActivity.this;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
        }

        et_slv_slv_price_amount = findViewById(R.id.et_slv_slv_price_amount);
        et_slv_gld_amount = findViewById(R.id.et_slv_gld_amount);
        tv_hld_slv_price_amount = findViewById(R.id.tv_hld_slv_price_amount);
        tv_hld_gld_amount = findViewById(R.id.tv_hld_gld_amount);
        tv_hld_ttl_amount = findViewById(R.id.tv_hld_ttl_amount);
        tvPriceHeading = findViewById(R.id.tv_price_heading);
        llDynamicHoldings = findViewById(R.id.ll_dynamic_holdings);
        btn_holding_update = findViewById(R.id.btn_holding_update);
        btn_silver_update = findViewById(R.id.btn_silver_update);
        tvDate = findViewById(R.id.tvDate);
        tvAppVersion = findViewById(R.id.tv_app_version);
        darkModeSwitch = findViewById(R.id.dark_mode_switch);

        if (tvAppVersion != null) {
            tvAppVersion.setText("Version " + BuildConfig.VERSION_NAME);
        }

        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(preferences.getBoolean(PREF_DARK_MODE, false));
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                preferences.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                recreate();
            });
        }

        btn_holding_update.setOnClickListener(this);
        btn_silver_update.setOnClickListener(this);

        silverDB = new SilverDB(mContext);
        silverT_db = new SilverT_DB(mContext);
        goldT_db = new GoldT_DB(mContext);
        holdingsDb = new HoldingsDb(mContext);
    }

    private void refreshHoldingSummaries() {
        dynamicHoldings.clear();

        Map<String, Float> totalsByType = new LinkedHashMap<>();
        for (HoldingItem item : holdingsDb.getAllItems()) {
            String normalizedType = HoldingsDb.normalizeItemType(item.getItemType());
            float rowTotal = safeParse(item.getTotal());
            float running = totalsByType.containsKey(normalizedType) ? totalsByType.get(normalizedType) : 0;
            totalsByType.put(normalizedType, running + rowTotal);
        }

        silver_total = totalsByType.containsKey(TYPE_SILVER) ? totalsByType.get(TYPE_SILVER) : 0;
        gold_total = totalsByType.containsKey(TYPE_GOLD) ? totalsByType.get(TYPE_GOLD) : 0;

        for (Map.Entry<String, Float> entry : totalsByType.entrySet()) {
            String type = entry.getKey();
            if (TYPE_SILVER.equalsIgnoreCase(type) || TYPE_GOLD.equalsIgnoreCase(type)) {
                continue;
            }
            dynamicHoldings.add(new HoldingSummary(type, entry.getValue()));
        }

        dynamicHoldings.sort(Comparator.comparing(summary -> summary.itemType, String.CASE_INSENSITIVE_ORDER));
        updateUI();
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
            totalHolding += getHoldingValue(summary);
        }

        tv_hld_slv_price_amount.setText(priceFormat.format(silverHolding));
        tv_hld_gld_amount.setText(priceFormat.format(goldHolding));
        renderDynamicHoldingRows(priceFormat);
        tv_hld_ttl_amount.setText(priceFormat.format(totalHolding));

        if (lv_slv_amt > 0) {
            generateData(priceFormat.format(totalHolding));
        }
    }

    private void renderDynamicHoldingRows(DecimalFormat formatter) {
        llDynamicHoldings.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (HoldingSummary summary : dynamicHoldings) {
            float value = getHoldingValue(summary);
            View row = inflater.inflate(R.layout.view_main_dynamic_holding_row, llDynamicHoldings, false);
            TextView title = row.findViewById(R.id.tv_dynamic_type);
            TextView amount = row.findViewById(R.id.tv_dynamic_amount);

            title.setText(summary.itemType);
            amount.setText(formatter.format(value));

            llDynamicHoldings.addView(row);
        }
    }

    private float getHoldingValue(HoldingSummary summary) {
        float currentPrice = getCurrentPriceByType(summary.itemType);
        if (isApiLookupType(summary.itemType) && currentPrice > 0) {
            return summary.totalUnits * currentPrice;
        }
        // For custom/non-API items, screen 2 already stores calculated total value.
        return summary.totalUnits;
    }

    private boolean isApiLookupType(String itemType) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        return TYPE_SILVER.equalsIgnoreCase(normalizedType)
                || TYPE_GOLD.equalsIgnoreCase(normalizedType)
                || "Copper".equalsIgnoreCase(normalizedType)
                || "Platinum".equalsIgnoreCase(normalizedType)
                || "Palladium".equalsIgnoreCase(normalizedType)
                || "Lead".equalsIgnoreCase(normalizedType)
                || "Aluminum".equalsIgnoreCase(normalizedType)
                || "Nickel".equalsIgnoreCase(normalizedType)
                || "Zinc".equalsIgnoreCase(normalizedType)
                || "Bitcoin".equalsIgnoreCase(normalizedType)
                || "Ethereum".equalsIgnoreCase(normalizedType);
    }


    private float getCurrentPriceByType(String itemType) {
        String normalizedType = HoldingsDb.normalizeItemType(itemType);
        if (TYPE_SILVER.equalsIgnoreCase(normalizedType)) return d_silver_amt > 0 ? d_silver_amt : lv_slv_amt;
        if (TYPE_GOLD.equalsIgnoreCase(normalizedType)) return d_gold_amt > 0 ? d_gold_amt : lv_gold_amt;
        if ("Copper".equalsIgnoreCase(normalizedType)) return d_copper_amt > 0 ? d_copper_amt : lv_cpr_amt;
        if ("Platinum".equalsIgnoreCase(normalizedType)) return d_platinum_amt > 0 ? d_platinum_amt : lv_platinum_amt;
        if ("Palladium".equalsIgnoreCase(normalizedType)) return d_palladium_amt > 0 ? d_palladium_amt : lv_palladium_amt;
        if ("Lead".equalsIgnoreCase(normalizedType)) return d_lead_amt > 0 ? d_lead_amt : lv_lead_amt;
        if ("Aluminum".equalsIgnoreCase(normalizedType)) return d_aluminum_amt > 0 ? d_aluminum_amt : lv_aluminum_amt;
        if ("Nickel".equalsIgnoreCase(normalizedType)) return d_nickel_amt > 0 ? d_nickel_amt : lv_nickel_amt;
        if ("Zinc".equalsIgnoreCase(normalizedType)) return d_zinc_amt > 0 ? d_zinc_amt : lv_zinc_amt;
        if ("Bitcoin".equalsIgnoreCase(normalizedType)) return d_btc_amt > 0 ? d_btc_amt : lv_btc_amt;
        if ("Ethereum".equalsIgnoreCase(normalizedType)) return d_eth_amt > 0 ? d_eth_amt : lv_eth_amt;
        return 0;
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

        // Auto-refresh only once per app opening; manual refresh button remains available.
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
        if (tvPriceHeading != null) {
            tvPriceHeading.setText(R.string.today_price);
        }
        if (alSilver != null && !alSilver.isEmpty()) {
            tvDate.setText(getString(R.string.cached_prices_with_date, alSilver.get(0).getDate()));
        } else {
            tvDate.setText(R.string.live_prices_unavailable);
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

    private void insertdatainDB() {
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss a", Locale.US);
        time = mdformat.format(calendar.getTime());
        if (tvPriceHeading != null) {
            tvPriceHeading.setText(R.string.latest_price);
        }
        tvDate.setText(time);

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

    public void generateData(String total) {
        if (total == null || total.equals("0") || total.equals(".00")) {
            return;
        }
        pendingDataToWrite = date + "," + total;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        executeFileWriting();
    }

    private void executeFileWriting() {
        if (pendingDataToWrite == null) return;
        String getPreferenceDate = preferences.getString("Date", "");
        if (getPreferenceDate.isEmpty() || !getPreferenceDate.equals(date)) {
            writeTextFile(pendingDataToWrite);
        }
        pendingDataToWrite = null;
    }

    @SuppressLint("NewApi")
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_ID_MULTIPLE_PERMISSIONS);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
}
