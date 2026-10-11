package com.projects.metalscrypto.holding;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddListActivity extends AppCompatActivity {

    private static final String TAG = "TAG";
    private static final int REQUEST_WRITE_STORAGE = 113;
    private static final String TYPE_SILVER = "Silver";
    private static final String TYPE_GOLD = "Gold";
    private static final String TYPE_BITCOIN = "Bitcoin";
    private static final String TYPE_ETHEREUM = "Ethereum";
    private static final String WEIGHT_UNIT_OZ = "oz";
    private static final String WEIGHT_UNIT_KG = "kg";
    private static final float KG_TO_OZ = 32.150746F;
    private static final int CATEGORY_METALS = 1;
    private static final int CATEGORY_CRYPTO = 2;
    private static final int CATEGORY_STOCKS = 3;
    private static final int CATEGORY_OTHER = 4;

    private EditText etItemType, et_no, et_wight, et_what, et_total;
    private LinearLayout rowNo, rowWeight, rowWhat;
    private RadioGroup rgItemCategory;
    private TextView tvManualTotalHint, tvNoLabel, tvWeightLabel, tvWhatLabel, tvTotalLabel;
    private String itemType = "", no = "", weight = "", what = "", total = "", time = "", date = "";
    private Context mContext;
    private HoldingsDb holdingsDb;
    private float prd_no, prd_weight, total_price;
    private String detailedNoDraft = "";
    private String detailedWeightDraft = "";
    private String detailedWhatDraft = "";
    private String manualTotalDraft = "";
    private String selectedWeightUnit = WEIGHT_UNIT_OZ;
    private boolean isDetailedModeActive = false;
    private boolean suppressCategoryChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_list);

        // Enable back button in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Item");
        }

        setupViews();
        checkPermissions();
    }

    private void setupViews() {
        mContext = this;
        holdingsDb = new HoldingsDb(mContext);

        etItemType = findViewById(R.id.et_item_type);
        et_no = findViewById(R.id.et_no);
        et_wight = findViewById(R.id.et_wight);
        et_what = findViewById(R.id.et_what);
        et_total = findViewById(R.id.et_total);
        rowNo = findViewById(R.id.row_no);
        rowWeight = findViewById(R.id.row_weight);
        rowWhat = findViewById(R.id.row_what);
        rgItemCategory = findViewById(R.id.rg_item_category);
        tvManualTotalHint = findViewById(R.id.tv_manual_total_hint);
        tvNoLabel = findViewById(R.id.tv_no_label);
        tvWeightLabel = findViewById(R.id.tv_weight_label);
        tvWhatLabel = findViewById(R.id.tv_what_label);
        tvTotalLabel = findViewById(R.id.tv_total_label);
        Button btn_cancel = findViewById(R.id.btn_cancel);
        Button btn_clear = findViewById(R.id.btn_clear);
        Button btn_add = findViewById(R.id.btn_add);

        btn_cancel.setOnClickListener(view -> finish());
        btn_clear.setOnClickListener(view -> cleartext());
        btn_add.setOnClickListener(view -> addData());
        tvWeightLabel.setOnClickListener(v -> {
            if (isSilverDetailedType(HoldingsDb.normalizeItemType(etItemType.getText().toString()))) {
                selectedWeightUnit = WEIGHT_UNIT_KG.equals(selectedWeightUnit) ? WEIGHT_UNIT_OZ : WEIGHT_UNIT_KG;
                updateWeightLabel();
                checkData(et_no.getText().toString(), et_wight.getText().toString());
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkData(et_no.getText().toString(), et_wight.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };

        et_no.addTextChangedListener(textWatcher);
        et_wight.addTextChangedListener(textWatcher);
        rgItemCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (!suppressCategoryChange) {
                applyInputMode(HoldingsDb.normalizeItemType(etItemType.getText().toString()));
            }
        });

        etItemType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String normalizedType = HoldingsDb.normalizeItemType(s.toString());
                syncCategoryFromType(normalizedType);
                applyInputMode(normalizedType);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        String normalizedType = HoldingsDb.normalizeItemType(etItemType.getText().toString());
        syncCategoryFromType(normalizedType);
        applyInputMode(normalizedType);
    }

    private void applyInputMode(String normalizedType) {
        int selectedCategory = getSelectedCategory(normalizedType);
        boolean metalsCategory = selectedCategory == CATEGORY_METALS;
        boolean stockType = selectedCategory == CATEGORY_STOCKS;
        boolean cryptoType = selectedCategory == CATEGORY_CRYPTO;
        boolean otherType = selectedCategory == CATEGORY_OTHER;
        boolean detailedType = metalsCategory && isDetailedType(normalizedType);
        boolean silverDetailedType = detailedType && isSilverDetailedType(normalizedType);
        if (!silverDetailedType) {
            selectedWeightUnit = WEIGHT_UNIT_OZ;
        }
        tvNoLabel.setText(R.string.quantity_label);
        updateWeightLabel();
        tvWeightLabel.setClickable(silverDetailedType);
        tvWhatLabel.setText(R.string.description_label);

        if (detailedType) {
            tvTotalLabel.setText(R.string.total_units_oz_label);
            tvManualTotalHint.setVisibility(View.GONE);
        } else if (metalsCategory) {
            tvTotalLabel.setText(R.string.units_label);
            tvManualTotalHint.setText(R.string.metal_units_hint);
            tvManualTotalHint.setVisibility(View.VISIBLE);
        } else if (stockType) {
            tvTotalLabel.setText(R.string.shares_label);
            tvManualTotalHint.setText(R.string.stock_shares_hint);
            tvManualTotalHint.setVisibility(View.VISIBLE);
        } else if (cryptoType) {
            tvTotalLabel.setText(R.string.amount_label);
            tvManualTotalHint.setText(R.string.crypto_amount_hint);
            tvManualTotalHint.setVisibility(View.VISIBLE);
        } else if (otherType) {
            tvTotalLabel.setText(R.string.total_value_label);
            tvManualTotalHint.setText(R.string.total_value_hint);
            tvManualTotalHint.setVisibility(View.VISIBLE);
        }

        rowNo.setVisibility(detailedType ? View.VISIBLE : View.GONE);
        rowWeight.setVisibility(detailedType ? View.VISIBLE : View.GONE);
        rowWhat.setVisibility(detailedType ? View.VISIBLE : View.GONE);

        if (detailedType) {
            et_total.setEnabled(false);
            et_total.setInputType(InputType.TYPE_CLASS_NUMBER);
            checkData(et_no.getText().toString(), et_wight.getText().toString());
        } else {
            et_total.setEnabled(true);
            et_total.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }

        if (detailedType == isDetailedModeActive) {
            return;
        }

        if (isDetailedModeActive) {
            detailedNoDraft = et_no.getText().toString();
            detailedWeightDraft = et_wight.getText().toString();
            detailedWhatDraft = et_what.getText().toString();
        } else {
            manualTotalDraft = et_total.getText().toString();
        }

        isDetailedModeActive = detailedType;

        if (detailedType) {
            et_no.setText(detailedNoDraft);
            et_wight.setText(detailedWeightDraft);
            et_what.setText(detailedWhatDraft);
            checkData(et_no.getText().toString(), et_wight.getText().toString());
        } else {
            et_total.setText(manualTotalDraft);
            if (et_total.getText() != null) {
                et_total.setSelection(et_total.getText().length());
            }
        }
    }

    private void checkData(String no, String weight) {
        try {
            if (!no.isEmpty() && !weight.isEmpty()) {
                prd_no = Float.parseFloat(no);
                prd_weight = Float.parseFloat(weight);
                boolean silverDetailedType = isSilverDetailedType(HoldingsDb.normalizeItemType(etItemType.getText().toString()));
                float unitWeightOz = silverDetailedType && WEIGHT_UNIT_KG.equals(selectedWeightUnit)
                        ? prd_weight * KG_TO_OZ
                        : prd_weight;
                total_price = prd_no * unitWeightOz;
                et_total.setText(new DecimalFormat("#,##0.####").format(total_price));
            } else {
                et_total.setText("");
            }
        } catch (NumberFormatException e) {
            et_total.setText("");
        }
    }

    private void cleartext() {
        etItemType.setText("");
        et_no.setText("");
        et_wight.setText("");
        et_what.setText("");
        et_total.setText("");
        detailedNoDraft = "";
        detailedWeightDraft = "";
        detailedWhatDraft = "";
        manualTotalDraft = "";
        selectedWeightUnit = WEIGHT_UNIT_OZ;
        isDetailedModeActive = false;
        syncCategoryFromType(HoldingsDb.normalizeItemType(etItemType.getText().toString()));
        applyInputMode(HoldingsDb.normalizeItemType(etItemType.getText().toString()));
    }

    private void addData() {
        itemType = normalizeItemTypeForSelection(etItemType.getText().toString());
        no = et_no.getText().toString();
        weight = et_wight.getText().toString();
        what = et_what.getText().toString();
        total = et_total.getText().toString();
        int selectedCategory = getSelectedCategory(itemType);
        int inferredCategory = inferCategoryFromType(itemType);
        if (inferredCategory != CATEGORY_OTHER && selectedCategory != inferredCategory) {
            selectedCategory = inferredCategory;
            syncCategoryFromType(itemType);
        }
        boolean detailedType = selectedCategory == CATEGORY_METALS && isDetailedType(itemType);
        String compactLabel = getCompactLabelForCategory(selectedCategory);

        if (itemType.isEmpty()) {
            showToast(R.string.please_enter_item_type);
        } else if (detailedType && no.isEmpty()) {
            showToast(R.string.please_enter_number);
        } else if (detailedType && weight.isEmpty()) {
            showToast(R.string.please_enter_weight);
        } else if (detailedType && what.isEmpty()) {
            showToast(R.string.please_enter_what);
        } else if (total.isEmpty()) {
            showToast(R.string.please_enter_total);
        } else {
            if (!detailedType) {
                no = "1";
                weight = total;
                what = compactLabel;
            }
            addDataInDB(detailedType, compactLabel);
        }
    }

    private void addDataInDB(boolean detailedType, String compactLabel) {
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss a", Locale.getDefault());
        time = mdformat.format(calendar.getTime());

        boolean success;
        if (detailedType) {
            success = holdingsDb.insertData(itemType, no, weight, what, total, date, time, resolveWeightUnitForSave(itemType));
        } else {
            success = upsertSingleTypeHolding(compactLabel);
        }

        if (success) {
            BackupSyncManager.exportSnapshot(this);
            Toast.makeText(mContext, itemType + " data saved successfully.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(mContext, "Failed to save " + itemType + " data.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean upsertSingleTypeHolding(String compactLabel) {
        List<HoldingItem> existing = holdingsDb.getItemsByType(itemType);
        if (existing.isEmpty()) {
            return holdingsDb.insertData(itemType, no, weight, what, total, date, time);
        }

        HoldingItem keeper = existing.get(0);
        float combinedTotal = parseAmount(keeper.getTotal()) + parseAmount(total);
        String combinedTotalText = new DecimalFormat("#,##0.##").format(combinedTotal);

        boolean updated = holdingsDb.updateData(
                keeper.getId(),
                itemType,
                "1",
                combinedTotalText,
                compactLabel,
                combinedTotalText,
                date,
                time);
        for (int i = 1; i < existing.size(); i++) {
            holdingsDb.deleteData(existing.get(i).getId());
        }
        return updated;
    }

    private void updateWeightLabel() {
        boolean silverDetailedType = isSilverDetailedType(HoldingsDb.normalizeItemType(etItemType.getText().toString()));
        if (silverDetailedType) {
            tvWeightLabel.setText(getString(R.string.weight_per_item_toggle_label, selectedWeightUnit));
            return;
        }
        tvWeightLabel.setText(R.string.weight_per_item_oz_label);
    }

    private String resolveWeightUnitForSave(String normalizedType) {
        if (isSilverDetailedType(normalizedType)) {
            return selectedWeightUnit;
        }
        if (TYPE_GOLD.equalsIgnoreCase(normalizedType)) {
            return WEIGHT_UNIT_OZ;
        }
        return "";
    }

    private boolean isSilverDetailedType(String normalizedType) {
        return TYPE_SILVER.equalsIgnoreCase(normalizedType);
    }

    private float parseAmount(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Float.parseFloat(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isDetailedType(String normalizedType) {
        return TYPE_SILVER.equalsIgnoreCase(normalizedType) || TYPE_GOLD.equalsIgnoreCase(normalizedType);
    }

    private boolean isCryptoType(String normalizedType) {
        return TYPE_BITCOIN.equalsIgnoreCase(normalizedType) || TYPE_ETHEREUM.equalsIgnoreCase(normalizedType);
    }

    private boolean isApiMetalType(String normalizedType) {
        return "Copper".equalsIgnoreCase(normalizedType)
                || "Platinum".equalsIgnoreCase(normalizedType)
                || "Palladium".equalsIgnoreCase(normalizedType)
                || "Lead".equalsIgnoreCase(normalizedType)
                || "Aluminum".equalsIgnoreCase(normalizedType)
                || "Nickel".equalsIgnoreCase(normalizedType)
                || "Zinc".equalsIgnoreCase(normalizedType);
    }

    private String getCompactLabelForCategory(int selectedCategory) {
        if (selectedCategory == CATEGORY_STOCKS) {
            return "Shares";
        }
        if (selectedCategory == CATEGORY_CRYPTO) {
            return "Amount";
        }
        if (selectedCategory == CATEGORY_METALS) {
            return "Units";
        }
        return "Total";
    }

    private String normalizeItemTypeForSelection(String rawValue) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int selectedCategory = getSelectedCategory(HoldingsDb.normalizeItemType(trimmed));
        if (selectedCategory == CATEGORY_CRYPTO) {
            if (trimmed.equalsIgnoreCase("btc") || trimmed.equalsIgnoreCase("xbt")) {
                return "Bitcoin";
            }
            if (trimmed.equalsIgnoreCase("eth")) {
                return "Ethereum";
            }
            String lowered = trimmed.toLowerCase(Locale.US);
            if ("xrp".equals(lowered)) {
                return "Xrp";
            }
            return trimmed.substring(0, 1).toUpperCase(Locale.US) + trimmed.substring(1).toLowerCase(Locale.US);
        }
        if (selectedCategory == CATEGORY_STOCKS) {
            String stockTicker = HoldingsDb.normalizeStockTickerInput(trimmed);
            if (!stockTicker.isEmpty()) {
                return stockTicker;
            }
        }
        if (selectedCategory == CATEGORY_OTHER) {
            return trimmed.substring(0, 1).toUpperCase(Locale.US) + trimmed.substring(1).toLowerCase(Locale.US);
        }
        return HoldingsDb.normalizeItemType(trimmed);
    }

    private int getSelectedCategory(String normalizedType) {
        int checkedId = rgItemCategory.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_category_metals) {
            return CATEGORY_METALS;
        }
        if (checkedId == R.id.rb_category_crypto) {
            return CATEGORY_CRYPTO;
        }
        if (checkedId == R.id.rb_category_stocks) {
            return CATEGORY_STOCKS;
        }
        if (checkedId == R.id.rb_category_other) {
            return CATEGORY_OTHER;
        }
        return inferCategoryFromType(normalizedType);
    }

    private void syncCategoryFromType(String normalizedType) {
        int targetCategory = inferCategoryFromType(normalizedType);
        int targetId = R.id.rb_category_other;
        if (targetCategory == CATEGORY_METALS) {
            targetId = R.id.rb_category_metals;
        } else if (targetCategory == CATEGORY_CRYPTO) {
            targetId = R.id.rb_category_crypto;
        } else if (targetCategory == CATEGORY_STOCKS) {
            targetId = R.id.rb_category_stocks;
        }
        if (rgItemCategory.getCheckedRadioButtonId() != targetId) {
            suppressCategoryChange = true;
            rgItemCategory.check(targetId);
            suppressCategoryChange = false;
        }
    }

    private int inferCategoryFromType(String normalizedType) {
        if (isDetailedType(normalizedType) || isApiMetalType(normalizedType)) {
            return CATEGORY_METALS;
        }
        if (isCryptoType(normalizedType)) {
            return CATEGORY_CRYPTO;
        }
        if (HoldingsDb.isStockTicker(normalizedType)) {
            return CATEGORY_STOCKS;
        }
        return CATEGORY_OTHER;
    }

    private void showToast(int resId) {
        Toast.makeText(mContext, getString(resId), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        REQUEST_WRITE_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Storage permission is required to save data.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}