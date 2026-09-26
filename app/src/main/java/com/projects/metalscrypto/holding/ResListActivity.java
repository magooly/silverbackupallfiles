package com.projects.metalscrypto.holding;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.projects.metalscrypto.holding.Adapter.HoldingListAdapter;
import com.projects.metalscrypto.holding.DBManager.GoldT_DB;
import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;
import com.projects.metalscrypto.holding.DBManager.SilverT_DB;
import com.projects.metalscrypto.holding.Retrofit.ApiHelperClass;
import com.projects.metalscrypto.holding.Retrofit.CryptoApiHelperClass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResListActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final String TYPE_SILVER = "Silver";
    private static final String TYPE_GOLD = "Gold";
    private static final String API_ERROR_LOG_FILE = "api_errors.txt";

    private Context mContext;
    private LinearLayout holdingsSectionsContainer;
    private HoldingsDb holdingsDb;
    private SilverT_DB silverTDb;
    private GoldT_DB goldTDb;
    private EditText etItemType;
    private EditText etNo;
    private EditText etWeight;
    private EditText etWhat;
    private EditText etTotal;
    private Button btnCancel;
    private Button btnAdd;
    private Button btnDelete;
    private FloatingActionButton btAdd;
    private float prdNo;
    private float prdWeight;
    private float totalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_res_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setUpViews();
        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is required to save items.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setUpViews() {
        mContext = ResListActivity.this;
        holdingsDb = new HoldingsDb(mContext);
        silverTDb = new SilverT_DB(mContext);
        goldTDb = new GoldT_DB(mContext);
        holdingsSectionsContainer = findViewById(R.id.holdings_sections_container);
        btAdd = findViewById(R.id.bt_add);

        migrateLegacyData();
        loadHoldingSections();

        btAdd.setOnClickListener(view -> startActivity(new Intent(mContext, AddListActivity.class)));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        migrateLegacyData();
        loadHoldingSections();
    }

    private void migrateLegacyData() {
        holdingsDb.importLegacyData(silverTDb.getSilverList(), "Silver", "silver");
        holdingsDb.importLegacyData(goldTDb.getGoldList(), "Gold", "gold");
    }

    private void loadHoldingSections() {
        holdingsSectionsContainer.removeAllViews();

        // Keep Silver/Gold as detailed rows; collapse other types into one aggregate row.
        consolidateNonDetailedTypes();

        List<HoldingItem> allItems = holdingsDb.getAllItems();
        if (allItems.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(R.string.no_items_added);
            emptyView.setPadding(24, 24, 24, 24);
            holdingsSectionsContainer.addView(emptyView);
            return;
        }

        Map<String, List<HoldingItem>> groupedItems = new LinkedHashMap<>();
        for (HoldingItem item : allItems) {
            String itemType = HoldingsDb.normalizeItemType(item.getItemType());
            if (!groupedItems.containsKey(itemType)) {
                groupedItems.put(itemType, new ArrayList<>());
            }
            groupedItems.get(itemType).add(item);
        }

        List<String> itemTypes = new ArrayList<>(groupedItems.keySet());
        Collections.sort(itemTypes, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return getTypeSortOrder(left) - getTypeSortOrder(right) != 0
                        ? getTypeSortOrder(left) - getTypeSortOrder(right)
                        : left.compareToIgnoreCase(right);
            }
        });

        LayoutInflater inflater = LayoutInflater.from(this);
        for (String itemType : itemTypes) {
            View sectionView = inflater.inflate(R.layout.view_holding_section, holdingsSectionsContainer, false);
            TextView titleView = sectionView.findViewById(R.id.tv_section_title);
            TextView totalView = sectionView.findViewById(R.id.tv_section_total);
            TextView headerNo = sectionView.findViewById(R.id.tv_header_no);
            TextView headerWeight = sectionView.findViewById(R.id.tv_header_weight);
            TextView headerWhat = sectionView.findViewById(R.id.tv_header_what);
            TextView headerTotal = sectionView.findViewById(R.id.tv_header_total);
            View topDivider = sectionView.findViewById(R.id.view_top_divider);
            View headerRow = sectionView.findViewById(R.id.row_section_headers);
            View bottomDivider = sectionView.findViewById(R.id.view_bottom_divider);
            View footerLayout = sectionView.findViewById(R.id.layout_section_footer);
            RecyclerView recyclerView = sectionView.findViewById(R.id.rv_holdings);

            List<HoldingItem> sectionItems = groupedItems.get(itemType);
            titleView.setText(itemType);
            totalView.setText(formatTotal(calculateTotal(sectionItems)));

            boolean detailedType = isDetailedType(itemType);
            if (detailedType) {
                titleView.setVisibility(View.VISIBLE);
                topDivider.setVisibility(View.VISIBLE);
                headerRow.setVisibility(View.VISIBLE);
                bottomDivider.setVisibility(View.VISIBLE);
                footerLayout.setVisibility(View.VISIBLE);
                headerNo.setVisibility(View.VISIBLE);
                headerWeight.setVisibility(View.VISIBLE);
                headerWhat.setVisibility(View.VISIBLE);
                headerNo.setText(R.string.no);
                headerTotal.setText(R.string.total_oz);
            } else {
                titleView.setVisibility(View.GONE);
                topDivider.setVisibility(View.GONE);
                headerRow.setVisibility(View.GONE);
                bottomDivider.setVisibility(View.GONE);
                footerLayout.setVisibility(View.GONE);
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setNestedScrollingEnabled(false);
            recyclerView.setAdapter(new HoldingListAdapter(this, sectionItems, this));

            holdingsSectionsContainer.addView(sectionView);
        }
    }

    private void consolidateNonDetailedTypes() {
        List<HoldingItem> items = holdingsDb.getAllItems();
        Map<String, List<HoldingItem>> groupedItems = new LinkedHashMap<>();
        for (HoldingItem item : items) {
            String itemType = HoldingsDb.normalizeItemType(item.getItemType());
            if (!groupedItems.containsKey(itemType)) {
                groupedItems.put(itemType, new ArrayList<>());
            }
            groupedItems.get(itemType).add(item);
        }

        for (Map.Entry<String, List<HoldingItem>> entry : groupedItems.entrySet()) {
            String itemType = entry.getKey();
            List<HoldingItem> typeItems = entry.getValue();
            if (isDetailedType(itemType) || typeItems.size() <= 1) {
                continue;
            }

            HoldingItem keeper = typeItems.get(0);
            float total = 0;
            for (HoldingItem item : typeItems) {
                total += parseAmount(item.getTotal());
            }

            String nowDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            Calendar calendar = Calendar.getInstance();
            String nowTime = new SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(calendar.getTime());
            String totalText = formatTotal(total);

            holdingsDb.updateData(keeper.getId(), itemType, "1", totalText, "Total", totalText, nowDate, nowTime);

            for (int i = 1; i < typeItems.size(); i++) {
                holdingsDb.deleteData(typeItems.get(i).getId());
            }
        }
    }

    private boolean isDetailedType(String itemType) {
        String normalized = HoldingsDb.normalizeItemType(itemType);
        return TYPE_SILVER.equalsIgnoreCase(normalized) || TYPE_GOLD.equalsIgnoreCase(normalized);
    }

    private int getTypeSortOrder(String itemType) {
        if ("Silver".equalsIgnoreCase(itemType)) {
            return 0;
        }
        if ("Gold".equalsIgnoreCase(itemType)) {
            return 1;
        }
        if ("Copper".equalsIgnoreCase(itemType)) {
            return 2;
        }
        if ("Platinum".equalsIgnoreCase(itemType)) {
            return 3;
        }
        if ("Palladium".equalsIgnoreCase(itemType)) {
            return 4;
        }
        if ("Lead".equalsIgnoreCase(itemType)) {
            return 5;
        }
        if ("Aluminum".equalsIgnoreCase(itemType)) {
            return 6;
        }
        if ("Nickel".equalsIgnoreCase(itemType)) {
            return 7;
        }
        if ("Zinc".equalsIgnoreCase(itemType)) {
            return 8;
        }
        if ("Bitcoin".equalsIgnoreCase(itemType)) {
            return 9;
        }
        if ("Ethereum".equalsIgnoreCase(itemType)) {
            return 10;
        }
        return 11;
    }

    private float calculateTotal(List<HoldingItem> items) {
        float total = 0;
        for (HoldingItem item : items) {
            total += parseAmount(item.getTotal());
        }
        return total;
    }

    private float parseAmount(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Float.parseFloat(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatTotal(float total) {
        return new DecimalFormat("#,##0.##").format(total);
    }

    public void displayPopUp(final String itemType, final String id, String no, String weight, String what, String total) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext, R.style.MyDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.update_item_popup, null);
        dialogBuilder.setView(dialogView);

        btnAdd = dialogView.findViewById(R.id.btn_add);
        btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnDelete = dialogView.findViewById(R.id.btn_delete);
        etItemType = dialogView.findViewById(R.id.et_item_type);
        etNo = dialogView.findViewById(R.id.et_no);
        etWeight = dialogView.findViewById(R.id.et_wight);
        etWhat = dialogView.findViewById(R.id.et_what);
        etTotal = dialogView.findViewById(R.id.et_total);
        View rowNo = dialogView.findViewById(R.id.row_no);
        View rowWeight = dialogView.findViewById(R.id.row_weight);
        View rowWhat = dialogView.findViewById(R.id.row_what);

        etItemType.setText(itemType);
        etNo.setText(no);
        etWeight.setText(weight);
        etWhat.setText(what);
        etTotal.setText(total);
        etItemType.setEnabled(false);

        boolean detailedType = isDetailedType(itemType);
        if (detailedType) {
            rowNo.setVisibility(View.VISIBLE);
            rowWeight.setVisibility(View.VISIBLE);
            rowWhat.setVisibility(View.VISIBLE);
            etTotal.setEnabled(false);
        } else {
            rowNo.setVisibility(View.GONE);
            rowWeight.setVisibility(View.GONE);
            rowWhat.setVisibility(View.GONE);
            etTotal.setEnabled(true);
            etTotal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if (etTotal.getText() != null) {
                etTotal.setSelection(etTotal.getText().length());
            }
        }

        final AlertDialog dialog = dialogBuilder.create();
        dialog.setTitle(itemType);

        dialog.show();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> validateData(
                HoldingsDb.normalizeItemType(etItemType.getText().toString()),
                id,
                etNo.getText().toString(),
                etWeight.getText().toString(),
                etWhat.getText().toString(),
                etTotal.getText().toString(),
                dialog
        ));
        
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(mContext)
                    .setTitle("Delete " + itemType)
                    .setMessage("Are you sure you want to delete this " + itemType + " item?")
                    .setPositiveButton("Delete", (deleteDialog, which) -> {
                        if (holdingsDb.deleteData(id)) {
                            BackupSyncManager.exportSnapshot(this);
                            Toast.makeText(mContext, itemType + " item deleted successfully.", Toast.LENGTH_SHORT).show();
                            deleteDialog.dismiss();
                            loadHoldingSections();
                        } else {
                            Toast.makeText(mContext, "Failed to delete " + itemType + " item.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", (cancelDialog, which) -> cancelDialog.dismiss())
                    .show();
        });

        if (detailedType) {
            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    checkData(etNo.getText().toString(), etWeight.getText().toString());
                }

                @Override
                public void afterTextChanged(Editable editable) {}
            };

            etNo.addTextChangedListener(textWatcher);
            etWeight.addTextChangedListener(textWatcher);
        }
    }

    private void checkData(String no, String weight) {
        prdNo = 0;
        prdWeight = 0;
        totalPrice = 0;
        if (!no.isEmpty() && !weight.isEmpty()) {
            try {
                prdNo = Float.parseFloat(no);
                prdWeight = Float.parseFloat(weight);
                totalPrice = prdNo * prdWeight;
                etTotal.setText(new DecimalFormat("#,##0.##").format(totalPrice));
            } catch (NumberFormatException e) {
                etTotal.setText("");
            }
        } else {
            etTotal.setText("");
        }
    }

    private void validateData(String itemType, String id, String no, String weight, String what, String total, AlertDialog dialog) {
        boolean detailedType = isDetailedType(itemType);
        if (itemType.isEmpty()) {
            Toast.makeText(mContext, getString(R.string.please_enter_item_type), Toast.LENGTH_SHORT).show();
        } else if (detailedType && no.isEmpty()) {
            Toast.makeText(mContext, "" + getString(R.string.please_enter_number), Toast.LENGTH_SHORT).show();
        } else if (detailedType && weight.isEmpty()) {
            Toast.makeText(mContext, "" + getString(R.string.please_enter_weight), Toast.LENGTH_SHORT).show();
        } else if (detailedType && what.isEmpty()) {
            Toast.makeText(mContext, "" + getString(R.string.please_enter_what), Toast.LENGTH_SHORT).show();
        } else if (total.isEmpty()) {
            Toast.makeText(mContext, "" + getString(R.string.please_enter_total), Toast.LENGTH_SHORT).show();
        } else {
            if (!detailedType) {
                no = "1";
                weight = total;
                what = "Total";
            }
            String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss a", Locale.getDefault());
            String time = mdformat.format(calendar.getTime());

            boolean update = holdingsDb.updateData(id, itemType, no, weight, what, total, date, time);
            if (update) {
                BackupSyncManager.exportSnapshot(this);
                Toast.makeText(mContext, itemType + " data updated successfully.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(mContext, "Failed to update " + itemType + " data.", Toast.LENGTH_SHORT).show();
            }
            loadHoldingSections();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        if (id == R.id.action_backup) {
            BackupSyncManager.exportSnapshot(this);
            Toast.makeText(this, R.string.backup_done, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_restore) {
            if (!BackupSyncManager.hasBackupFile(this)) {
                Toast.makeText(this, R.string.restore_no_file, Toast.LENGTH_SHORT).show();
            } else {
                BackupSyncManager.restoreNow(this);
                migrateLegacyData();
                loadHoldingSections();
                Toast.makeText(this, R.string.restore_done, Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        try {
            String aboutText = readAboutFile();
            String metalsSourceText = getString(R.string.metals_source_label, formatSourceHost(ApiHelperClass.BASE_URL));
            String apiErrorLog = readApiErrorLog();
            String cryptoSourceText = getString(R.string.crypto_source_label, formatSourceHost(CryptoApiHelperClass.BASE_URL));
            String backupStatusText = buildBackupStatusText();
            String displayText = aboutText
                    + "\n\n"
                    + metalsSourceText
                    + "\n"
                    + cryptoSourceText
                    + "\n\n"
                    + backupStatusText
                    + "\n\n"
                    + getString(R.string.api_error_log_heading)
                    + "\n"
                    + apiErrorLog;
            
            // Create a scrollable TextView to display the text
            ScrollView scrollView = new ScrollView(this);
            TextView textView = new TextView(this);
            textView.setText(displayText);
            textView.setPadding(32, 32, 32, 32);
            textView.setTextSize(16);
            scrollView.addView(textView);

            // Create and show the dialog
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("About")
                    .setView(scrollView)
                    .setPositiveButton("Close", (d, which) -> d.dismiss())
                    .setNegativeButton(getString(R.string.edit), (d, which) -> showEditAboutDialog(aboutText))
                    .setNeutralButton(getString(R.string.clear_api_log), (d, which) -> confirmClearApiErrorLog())
                    .create();
            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading about information", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatSourceHost(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "unknown";
        }
        try {
            Uri uri = Uri.parse(url.trim());
            String host = uri.getHost();
            return host != null && !host.trim().isEmpty() ? host : url;
        } catch (Exception ignored) {
            return url;
        }
    }

    private String buildBackupStatusText() {
        long lastImported = BackupSyncManager.getLastImportedTimestamp(this);
        long lastExported = BackupSyncManager.getLastExportedAt(this);
        String backupPath = BackupSyncManager.getBackupPathForStatus(this);

        return getString(R.string.backup_status_heading)
                + "\n"
                + getString(R.string.backup_last_import_label, formatBackupTime(lastImported))
                + "\n"
                + getString(R.string.backup_last_export_label, formatBackupTime(lastExported))
                + "\n"
                + getString(R.string.backup_path_label, backupPath);
    }

    private String formatBackupTime(long millis) {
        if (millis <= 0) {
            return getString(R.string.backup_time_never);
        }
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a", Locale.getDefault()).format(new Date(millis));
    }

    private void confirmClearApiErrorLog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_api_log)
                .setMessage(R.string.clear_api_log_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> clearApiErrorLog())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearApiErrorLog() {
        boolean deleted = deleteFile(API_ERROR_LOG_FILE);
        if (deleted || !new File(getFilesDir(), API_ERROR_LOG_FILE).exists()) {
            Toast.makeText(this, R.string.api_error_log_cleared, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.api_error_log_clear_failed, Toast.LENGTH_SHORT).show();
        }
        showAboutDialog();
    }

    private String readApiErrorLog() {
        File logFile = new File(getFilesDir(), API_ERROR_LOG_FILE);
        if (!logFile.exists()) {
            return getString(R.string.api_error_log_empty);
        }
        FileInputStream fis = null;
        try {
            fis = openFileInput(API_ERROR_LOG_FILE);
            String content = readStream(fis).trim();
            return content.isEmpty() ? getString(R.string.api_error_log_empty) : content;
        } catch (IOException e) {
            return getString(R.string.api_error_log_empty);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
    
    private String readAboutFile() throws IOException {
        // First try to read from internal storage
        try {
            return readInternalAboutFile();
        } catch (IOException e) {
            // If internal storage read fails, try external storage
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                try {
                    File aboutDir = new File(Environment.getExternalStorageDirectory(), "SilverHolding");
                    File aboutFile = new File(aboutDir, "about.txt");
                    
                    if (aboutFile.exists()) {
                        FileInputStream fis = new FileInputStream(aboutFile);
                        String content = readStream(fis);
                        fis.close();
                        return content;
                    }
                } catch (IOException ex) {
                    // Continue to assets if external storage read fails
                }
            }
            
            // If both internal and external storage fail, read from assets
            InputStream is = getAssets().open("about.txt");
            String defaultContent = readStream(is);
            is.close();
            
            // Save the default content to internal storage for next time
            saveToInternalStorage(defaultContent);
            
            return defaultContent;
        }
    }
    
    private String readInternalAboutFile() throws IOException {
        // Try to read from internal storage
        File internalFile = new File(getFilesDir(), "about.txt");
        if (internalFile.exists()) {
            FileInputStream fis = openFileInput("about.txt");
            String content = readStream(fis);
            fis.close();
            return content;
        }
        
        // If not found in internal storage, throw FileNotFoundException
        // to allow readAboutFile to try other sources
        throw new FileNotFoundException("About file not found in internal storage");
    }
    
    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
    
    private void showEditAboutDialog(String currentText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit About Text");
        
        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(8);
        input.setMaxLines(10);
        input.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        input.setText(currentText);
        input.setSelection(input.getText().length());
        
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newText = input.getText().toString();
            saveAboutText(newText);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void saveAboutText(String text) {
        // Always save to internal storage first (this works on all Android versions)
        try {
            saveToInternalStorage(text);
            
            // For Android 9 (API 28) and below, also save to external storage
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    try {
                        File aboutDir = new File(Environment.getExternalStorageDirectory(), "SilverHolding");
                        if (!aboutDir.exists()) {
                            aboutDir.mkdirs();
                        }
                        File aboutFile = new File(aboutDir, "about.txt");
                        FileOutputStream fos = new FileOutputStream(aboutFile);
                        fos.write(text.getBytes(StandardCharsets.UTF_8));
                        fos.close();
                        Toast.makeText(this, "About text saved to both internal and external storage", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // Ignore external storage errors on older Android versions
                    }
                }
            } else {
                // For Android 10+, we only use internal storage
                Toast.makeText(this, "About text saved", Toast.LENGTH_SHORT).show();
            }

            BackupSyncManager.exportSnapshot(this);
            
            showAboutDialog();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving about text: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void saveToInternalStorage(String text) throws IOException {
        FileOutputStream fos = openFileOutput("about.txt", Context.MODE_PRIVATE);
        fos.write(text.getBytes());
        fos.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }
}
