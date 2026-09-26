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

    private EditText etItemType, et_no, et_wight, et_what, et_total;
    private LinearLayout rowNo, rowWeight, rowWhat;
    private TextView tvManualTotalHint, tvTotalLabel;
    private String itemType = "", no = "", weight = "", what = "", total = "", time = "", date = "";
    private Context mContext;
    private HoldingsDb holdingsDb;
    private float prd_no, prd_weight, total_price;
    private String detailedNoDraft = "";
    private String detailedWeightDraft = "";
    private String detailedWhatDraft = "";
    private String manualTotalDraft = "";
    private boolean isDetailedModeActive = false;

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
        tvManualTotalHint = findViewById(R.id.tv_manual_total_hint);
        tvTotalLabel = findViewById(R.id.tv_total_label);
        Button btn_cancel = findViewById(R.id.btn_cancel);
        Button btn_clear = findViewById(R.id.btn_clear);
        Button btn_add = findViewById(R.id.btn_add);

        btn_cancel.setOnClickListener(view -> finish());
        btn_clear.setOnClickListener(view -> cleartext());
        btn_add.setOnClickListener(view -> addData());

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkData(et_no.getText().toString(), et_wight.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        et_no.addTextChangedListener(textWatcher);
        et_wight.addTextChangedListener(textWatcher);

        etItemType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyInputMode(HoldingsDb.normalizeItemType(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        applyInputMode(HoldingsDb.normalizeItemType(etItemType.getText().toString()));
    }

    private void applyInputMode(String normalizedType) {
        boolean detailedType = isDetailedType(normalizedType);
        tvTotalLabel.setText(detailedType ? R.string.total_oz : R.string.total_label);

        if (detailedType == isDetailedModeActive) {
            tvManualTotalHint.setVisibility(detailedType ? View.GONE : View.VISIBLE);
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

        rowNo.setVisibility(detailedType ? View.VISIBLE : View.GONE);
        rowWeight.setVisibility(detailedType ? View.VISIBLE : View.GONE);
        rowWhat.setVisibility(detailedType ? View.VISIBLE : View.GONE);
        tvManualTotalHint.setVisibility(detailedType ? View.GONE : View.VISIBLE);

        if (detailedType) {
            et_no.setText(detailedNoDraft);
            et_wight.setText(detailedWeightDraft);
            et_what.setText(detailedWhatDraft);
            et_total.setEnabled(false);
            et_total.setInputType(InputType.TYPE_CLASS_NUMBER);
            checkData(et_no.getText().toString(), et_wight.getText().toString());
        } else {
            et_total.setEnabled(true);
            et_total.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
                total_price = prd_no * prd_weight;
                et_total.setText(new DecimalFormat("#,##0").format(total_price));
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
        isDetailedModeActive = false;
        applyInputMode(HoldingsDb.normalizeItemType(etItemType.getText().toString()));
    }

    private void addData() {
        itemType = HoldingsDb.normalizeItemType(etItemType.getText().toString());
        no = et_no.getText().toString();
        weight = et_wight.getText().toString();
        what = et_what.getText().toString();
        total = et_total.getText().toString();
        boolean detailedType = isDetailedType(itemType);

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
                what = "Total";
            }
            addDataInDB();
        }
    }

    private void addDataInDB() {
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss a", Locale.getDefault());
        time = mdformat.format(calendar.getTime());

        boolean success;
        if (isDetailedType(itemType)) {
            success = holdingsDb.insertData(itemType, no, weight, what, total, date, time);
        } else {
            success = upsertSingleTypeHolding();
        }

        if (success) {
            BackupSyncManager.exportSnapshot(this);
            Toast.makeText(mContext, itemType + " data saved successfully.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(mContext, "Failed to save " + itemType + " data.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean upsertSingleTypeHolding() {
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
                "Total",
                combinedTotalText,
                date,
                time
        );
        for (int i = 1; i < existing.size(); i++) {
            holdingsDb.deleteData(existing.get(i).getId());
        }
        return updated;
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
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
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