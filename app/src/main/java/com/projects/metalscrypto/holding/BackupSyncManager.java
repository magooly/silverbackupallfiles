package com.projects.metalscrypto.holding;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BackupSyncManager {
    private static final String TAG = "BackupSyncManager";
    private static final String PREFS_NAME = "backup_sync";
    private static final String PREF_LAST_IMPORTED_TS = "last_imported_ts";
    private static final String PREF_LAST_EXPORTED_AT = "last_exported_at";
    private static final String PREF_MERGED_BACKUP_FINGERPRINTS = "merged_backup_fingerprints";
    private static final String BACKUP_FILE_NAME = "holdings_backup.json";
    private static final String ABOUT_FILE_NAME = "about.txt";
    private static final String APP_PREFS_STOCK_PRICE_PREFIX = "stock_price_";
    private static final String APP_PREFS_CRYPTO_PRICE_PREFIX = "crypto_price_";
    private static final String APP_PREFS_CRYPTO_ID_PREFIX = "crypto_id_";

    private BackupSyncManager() {
    }

    public enum RestoreMode {
        MERGE,
        REPLACE
    }

    public static final class BackupResult {
        private final boolean success;
        private final String message;

        private BackupResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static void exportSnapshot(Context context) {
        BackupResult result = exportSnapshotResult(context, BACKUP_FILE_NAME, false);
        if (!result.isSuccess()) {
            Log.e(TAG, "exportSnapshot failed: " + result.getMessage());
        }
    }

    public static void exportSnapshot(Context context, String backupFileName) {
        BackupResult result = exportSnapshotResult(context, backupFileName, false);
        if (!result.isSuccess()) {
            Log.e(TAG, "exportSnapshot failed: " + result.getMessage());
        }
    }

    public static BackupResult exportSnapshotResult(Context context) {
        return exportSnapshotResult(context, BACKUP_FILE_NAME, true);
    }

    public static BackupResult exportSnapshotResult(Context context, String backupFileName) {
        return exportSnapshotResult(context, backupFileName, true);
    }

    public static BackupResult exportSnapshotResult(Context context, String backupFileName, boolean exportPublicFile) {
        if (context == null) {
            return new BackupResult(false, "Context unavailable.");
        }
        String resolvedBackupFileName = resolveBackupFileName(backupFileName);
        String publicWriteWarning = "";
        try {
            HoldingsDb holdingsDb = new HoldingsDb(context);
            List<HoldingItem> items = holdingsDb.getAllItems();

            JSONObject root = new JSONObject();
            root.put("schema", 1);
            root.put("exportedAt", nowDateTime());
            root.put("aboutText", readInternalAboutText(context));
            root.put("lookupPrefs", exportLookupPreferences(context));

            JSONArray itemArray = new JSONArray();
            for (HoldingItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("itemType", HoldingsDb.normalizeItemType(item.getItemType()));
                obj.put("no", safe(item.getNumber()));
                obj.put("weight", safe(item.getWeight()));
                obj.put("what", safe(item.getWhat()));
                obj.put("total", safe(item.getTotal()));
                obj.put("weightUnit", safe(item.getWeightUnit()));
                obj.put("date", safe(item.getUpdateDate()));
                obj.put("time", safe(item.getUpdateTime()));
                itemArray.put(obj);
            }
            root.put("items", itemArray);

            String json = root.toString();
            writeInternal(context, resolvedBackupFileName, json);

            if (exportPublicFile) {
                File externalFile = getPublicBackupFile(context, resolvedBackupFileName);
                if (externalFile != null) {
                    try {
                        writeFile(externalFile, json);
                    } catch (Exception externalError) {
                        Log.w(TAG, "Public backup write failed for " + externalFile.getAbsolutePath(), externalError);
                        publicWriteWarning = " (internal backup saved; public export unavailable)";
                    }
                }
            }

            SharedPreferences.Editor exportPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(PREF_LAST_EXPORTED_AT, System.currentTimeMillis());
            if (BACKUP_FILE_NAME.equals(resolvedBackupFileName)) {
                File exportedSnapshot = getReadableBackupFile(context, BACKUP_FILE_NAME);
                if (exportedSnapshot != null) {
                    exportPrefs.putLong(PREF_LAST_IMPORTED_TS, exportedSnapshot.lastModified());
                }
            }
            exportPrefs.apply();
            return new BackupResult(true, "Backup written to " + resolvedBackupFileName + publicWriteWarning);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export backup " + resolvedBackupFileName, e);
            return new BackupResult(false, e.getMessage() == null ? "Unknown export error." : e.getMessage());
        }
    }

    public static void importAndMergeOnStart(Context context) {
        BackupResult result = importAndMerge(context, false);
        if (!result.isSuccess()) {
            Log.w(TAG, "importAndMergeOnStart skipped/failed: " + result.getMessage());
        }
    }

    public static boolean restoreNow(Context context) {
        return restoreNowResult(context, BACKUP_FILE_NAME).isSuccess();
    }

    public static boolean restoreNow(Context context, String backupFileName) {
        return restoreNowResult(context, backupFileName).isSuccess();
    }

    public static BackupResult restoreNowResult(Context context) {
        return restoreNowResult(context, BACKUP_FILE_NAME, RestoreMode.MERGE);
    }

    public static BackupResult restoreNowResult(Context context, String backupFileName) {
        return restoreNowResult(context, backupFileName, RestoreMode.MERGE);
    }

    public static BackupResult restoreNowResult(Context context, String backupFileName, RestoreMode mode) {
        return importAndMerge(context, true, resolveBackupFileName(backupFileName),
                mode == null ? RestoreMode.MERGE : mode);
    }

    public static boolean hasBackupFile(Context context, String backupFileName) {
        if (context == null) {
            return false;
        }
        File backupFile = getReadableBackupFile(context, resolveBackupFileName(backupFileName));
        return backupFile != null && backupFile.exists();
    }

    public static String normalizeBackupFileName(String backupFileName) {
        return resolveBackupFileName(backupFileName);
    }

    public static boolean hasBackupFile(Context context) {
        if (context == null) {
            return false;
        }
        File backupFile = getReadableBackupFile(context, BACKUP_FILE_NAME);
        return backupFile != null && backupFile.exists();
    }

    public static List<String> listAvailableBackupFiles(Context context) {
        List<String> names = new ArrayList<>();
        if (context == null) {
            return names;
        }
        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();

        File[] internalFiles = context.getFilesDir() == null ? null : context.getFilesDir().listFiles();
        if (internalFiles != null) {
            for (File file : internalFiles) {
                if (file != null && file.isFile() && file.getName().toLowerCase(Locale.US).endsWith(".json")) {
                    uniqueNames.add(file.getName());
                }
            }
        }

        File externalDir = buildPublicBackupDirectory();
        File[] externalFiles = externalDir == null ? null : externalDir.listFiles();
        if (externalFiles != null) {
            for (File file : externalFiles) {
                if (file != null && file.isFile() && file.getName().toLowerCase(Locale.US).endsWith(".json")) {
                    uniqueNames.add(file.getName());
                }
            }
        }

        names.addAll(uniqueNames);
        Collections.sort(names, String::compareToIgnoreCase);
        return names;
    }

    private static BackupResult importAndMerge(Context context, boolean force) {
        return importAndMerge(context, force, BACKUP_FILE_NAME, RestoreMode.MERGE);
    }

    private static BackupResult importAndMerge(Context context, boolean force, String backupFileName) {
        return importAndMerge(context, force, backupFileName, RestoreMode.MERGE);
    }

    private static BackupResult importAndMerge(Context context, boolean force, String backupFileName,
            RestoreMode mode) {
        if (context == null) {
            return new BackupResult(false, "Context unavailable.");
        }

        File backupFile = getReadableBackupFile(context, backupFileName);
        if (backupFile == null || !backupFile.exists()) {
            return new BackupResult(false, "Backup file not found.");
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastImported = prefs.getLong(PREF_LAST_IMPORTED_TS, -1L);
        long fileTs = backupFile.lastModified();
        if (!force && fileTs > 0 && lastImported == fileTs) {
            return new BackupResult(false, "No changes since last import.");
        }

        try {
            String raw = readFile(backupFile);
            if (TextUtils.isEmpty(raw)) {
                return new BackupResult(false, "Backup file is empty.");
            }

            JSONObject root = new JSONObject(raw);
            String aboutText = root.optString("aboutText", "");
            if (!TextUtils.isEmpty(aboutText)) {
                writeInternal(context, ABOUT_FILE_NAME, aboutText);
            }
            applyLookupPreferences(context, root.optJSONObject("lookupPrefs"));

            JSONArray items = root.optJSONArray("items");
            String itemFingerprint = BackupMergeFingerprint.create(items == null ? "[]" : items.toString());
            Set<String> mergedFingerprints = new HashSet<>();
            Set<String> storedFingerprints = prefs.getStringSet(PREF_MERGED_BACKUP_FINGERPRINTS, null);
            if (storedFingerprints != null) {
                mergedFingerprints.addAll(storedFingerprints);
            }
            if (BackupMergeFingerprint.shouldSkipMerge(mode == RestoreMode.MERGE, itemFingerprint,
                    mergedFingerprints)) {
                prefs.edit().putLong(PREF_LAST_IMPORTED_TS, fileTs).apply();
                return new BackupResult(true, "Backup contents were already merged; no changes made.");
            }

            HoldingsDb holdingsDb = new HoldingsDb(context);
            if (mode == RestoreMode.REPLACE) {
                holdingsDb.clearAllData();
            }
            BackupNonDetailedAccumulator nonDetailedTotals = new BackupNonDetailedAccumulator();
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject obj = items.optJSONObject(i);
                    if (obj == null) {
                        continue;
                    }

                    String type = HoldingsDb.normalizeItemType(obj.optString("itemType", ""));
                    String no = obj.optString("no", "");
                    String weight = obj.optString("weight", "");
                    String what = obj.optString("what", "");
                    String total = obj.optString("total", "");
                    String weightUnit = obj.optString("weightUnit", "");
                    String date = obj.optString("date", nowDate());
                    String time = obj.optString("time", nowTime());

                    if (isDetailedType(type)) {
                        if (!hasDetailedDuplicate(holdingsDb, type, no, weight, weightUnit, what, total)) {
                            holdingsDb.insertData(type, no, weight, what, total, date, time, weightUnit);
                        }
                    } else {
                        nonDetailedTotals.add(type, total, what, date, time);
                    }
                }
                for (java.util.Map.Entry<String, BackupNonDetailedAccumulator.Summary> entry : nonDetailedTotals
                        .getByType()
                        .entrySet()) {
                    BackupNonDetailedAccumulator.Summary summary = entry.getValue();
                    mergeNonDetailedType(
                            holdingsDb,
                            entry.getKey(),
                            BackupNonDetailedAccumulator.formatTotal(summary.total),
                            summary.date == null ? nowDate() : summary.date,
                            summary.time == null ? nowTime() : summary.time,
                            summary.what,
                            mode);
                }
                // Continue to restore and deduplicate existing data for each merged
                // non-detailed type.
                for (String mergedType : nonDetailedTotals.getByType().keySet()) {
                    List<HoldingItem> existing = holdingsDb.getItemsByType(mergedType);
                    for (int i = 1; i < existing.size(); i++) {
                        holdingsDb.deleteData(existing.get(i).getId());
                    }
                }
            }

            mergedFingerprints.add(itemFingerprint);
            prefs.edit()
                    .putLong(PREF_LAST_IMPORTED_TS, fileTs)
                    .putStringSet(PREF_MERGED_BACKUP_FINGERPRINTS, mergedFingerprints)
                    .apply();
            return new BackupResult(true, "Restore completed from " + backupFile.getName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to import backup " + backupFileName, e);
            return new BackupResult(false, e.getMessage() == null ? "Unknown restore error." : e.getMessage());
        }
    }

    private static boolean hasDetailedDuplicate(HoldingsDb db, String type, String no, String weight, String weightUnit,
            String what,
            String total) {
        List<HoldingItem> existing = db.getItemsByType(type);
        String normalizedWeightUnit = HoldingsDb.normalizeWeightUnit(weightUnit, type);
        for (HoldingItem item : existing) {
            if (numericOrTextEquals(item.getNumber(), no)
                    && numericOrTextEquals(item.getWeight(), weight)
                    && safe(HoldingsDb.normalizeWeightUnit(item.getWeightUnit(), type))
                            .equals(safe(normalizedWeightUnit))
                    && safe(item.getWhat()).equals(safe(what))
                    && numericOrTextEquals(item.getTotal(), total)) {
                return true;
            }
        }
        return false;
    }

    private static JSONObject exportLookupPreferences(Context context) {
        JSONObject root = new JSONObject();
        JSONObject stockPrices = new JSONObject();
        JSONObject cryptoPrices = new JSONObject();
        JSONObject cryptoIds = new JSONObject();
        if (context == null) {
            return root;
        }

        SharedPreferences appPrefs = context.getSharedPreferences(context.getString(R.string.app_name),
                Context.MODE_PRIVATE);
        for (java.util.Map.Entry<String, ?> entry : appPrefs.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith(APP_PREFS_STOCK_PRICE_PREFIX)) {
                putFloatSafely(stockPrices, key.substring(APP_PREFS_STOCK_PRICE_PREFIX.length()),
                        parsePreferenceFloat(value));
            } else if (key.startsWith(APP_PREFS_CRYPTO_PRICE_PREFIX)) {
                putFloatSafely(cryptoPrices, key.substring(APP_PREFS_CRYPTO_PRICE_PREFIX.length()),
                        parsePreferenceFloat(value));
            } else if (key.startsWith(APP_PREFS_CRYPTO_ID_PREFIX) && value instanceof String) {
                String cryptoId = ((String) value).trim();
                if (!cryptoId.isEmpty()) {
                    putStringSafely(cryptoIds, key.substring(APP_PREFS_CRYPTO_ID_PREFIX.length()), cryptoId);
                }
            }
        }

        if (stockPrices.length() > 0) {
            putObjectSafely(root, "stockPrices", stockPrices);
        }
        if (cryptoPrices.length() > 0) {
            putObjectSafely(root, "cryptoPrices", cryptoPrices);
        }
        if (cryptoIds.length() > 0) {
            putObjectSafely(root, "cryptoIds", cryptoIds);
        }
        return root;
    }

    private static void applyLookupPreferences(Context context, JSONObject lookupPrefs) {
        if (context == null || lookupPrefs == null) {
            return;
        }
        SharedPreferences appPrefs = context.getSharedPreferences(context.getString(R.string.app_name),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = appPrefs.edit();
        applyFloatPreferences(editor, APP_PREFS_STOCK_PRICE_PREFIX, lookupPrefs.optJSONObject("stockPrices"));
        applyFloatPreferences(editor, APP_PREFS_CRYPTO_PRICE_PREFIX, lookupPrefs.optJSONObject("cryptoPrices"));
        applyStringPreferences(editor, APP_PREFS_CRYPTO_ID_PREFIX, lookupPrefs.optJSONObject("cryptoIds"));
        editor.apply();
    }

    private static void applyFloatPreferences(SharedPreferences.Editor editor, String prefix, JSONObject object) {
        if (object == null) {
            return;
        }
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            float value = parseFloat(object.optString(key, ""));
            if (value > 0) {
                editor.putFloat(prefix + key, value);
            }
        }
    }

    private static void applyStringPreferences(SharedPreferences.Editor editor, String prefix, JSONObject object) {
        if (object == null) {
            return;
        }
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = object.optString(key, "").trim();
            if (!value.isEmpty()) {
                editor.putString(prefix + HoldingsDb.normalizeItemType(key), value);
            }
        }
    }

    private static boolean numericOrTextEquals(String left, String right) {
        Float leftNumber = tryParseFloat(left);
        Float rightNumber = tryParseFloat(right);
        if (leftNumber != null && rightNumber != null) {
            return Math.abs(leftNumber - rightNumber) < 0.0001F;
        }
        return safe(left).equals(safe(right));
    }

    private static float parsePreferenceFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value instanceof String) {
            Float parsed = tryParseFloat((String) value);
            return parsed == null ? 0 : parsed;
        }
        return 0;
    }

    private static float parseFloat(String value) {
        Float parsed = tryParseFloat(value);
        return parsed == null ? 0 : parsed;
    }

    private static Float tryParseFloat(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Float.parseFloat(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void putFloatSafely(JSONObject object, String key, float value) {
        if (value <= 0 || key == null || key.trim().isEmpty()) {
            return;
        }
        try {
            object.put(key, value);
        } catch (Exception e) {
            Log.w(TAG, "Failed to write float preference to backup: " + key, e);
        }
    }

    private static void putStringSafely(JSONObject object, String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            object.put(HoldingsDb.normalizeItemType(key), value);
        } catch (Exception e) {
            Log.w(TAG, "Failed to write string preference to backup: " + key, e);
        }
    }

    private static void putObjectSafely(JSONObject object, String key, JSONObject value) {
        try {
            object.put(key, value);
        } catch (Exception e) {
            Log.w(TAG, "Failed to write lookup preference section: " + key, e);
        }
    }

    private static void mergeNonDetailedType(HoldingsDb db, String type, String total, String date, String time,
            String preferredWhat, RestoreMode mode) {
        List<HoldingItem> existing = db.getItemsByType(type);
        BigDecimal incomingTotal = parseDecimal(total);
        BigDecimal existingTotal = existing.isEmpty() ? BigDecimal.ZERO : parseDecimal(existing.get(0).getTotal());
        BigDecimal mergedTotal = mode == RestoreMode.REPLACE ? incomingTotal : existingTotal.add(incomingTotal);
        String normalizedTotal = BackupNonDetailedAccumulator.formatTotal(mergedTotal);
        String compactLabel = resolveCompactLabel(type, preferredWhat);
        if (existing.isEmpty()) {
            db.insertData(type, "1", normalizedTotal, compactLabel, normalizedTotal, date, time);
            return;
        }

        HoldingItem keeper = existing.get(0);
        boolean changed = !safe(keeper.getTotal()).equals(normalizedTotal)
                || !safe(keeper.getWeight()).equals(normalizedTotal)
                || !compactLabel.equalsIgnoreCase(safe(keeper.getWhat()));
        if (changed) {
            db.updateData(keeper.getId(), type, "1", normalizedTotal, compactLabel, normalizedTotal, date, time);
        }
    }

    private static BigDecimal parseDecimal(String value) {
        if (TextUtils.isEmpty(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean isDetailedType(String type) {
        return "Silver".equalsIgnoreCase(type) || "Gold".equalsIgnoreCase(type);
    }

    private static String nowDateTime() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private static String nowDate() {
        return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
    }

    private static String nowTime() {
        Calendar calendar = Calendar.getInstance();
        return new SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(calendar.getTime());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String resolveCompactLabel(String type, String preferredWhat) {
        if (HoldingsDb.isStockTicker(type)) {
            return "Shares";
        }
        String normalizedType = HoldingsDb.normalizeItemType(type);
        if ("Bitcoin".equalsIgnoreCase(normalizedType)
                || "Ethereum".equalsIgnoreCase(normalizedType)
                || "Amount".equalsIgnoreCase(safe(preferredWhat))) {
            return "Amount";
        }
        if ("Copper".equalsIgnoreCase(normalizedType)
                || "Platinum".equalsIgnoreCase(normalizedType)
                || "Palladium".equalsIgnoreCase(normalizedType)
                || "Lead".equalsIgnoreCase(normalizedType)
                || "Aluminum".equalsIgnoreCase(normalizedType)
                || "Nickel".equalsIgnoreCase(normalizedType)
                || "Zinc".equalsIgnoreCase(normalizedType)
                || "Units".equalsIgnoreCase(safe(preferredWhat))) {
            return "Units";
        }
        return "Total";
    }

    private static File getReadableBackupFile(Context context, String backupFileName) {
        File internal = new File(context.getFilesDir(), backupFileName);
        File external = buildPublicBackupFile(backupFileName);
        boolean hasInternal = internal.exists();
        boolean hasExternal = external.exists();
        if (hasInternal && hasExternal) {
            return internal.lastModified() >= external.lastModified() ? internal : external;
        }
        if (hasInternal) {
            return internal;
        }
        return hasExternal ? external : null;
    }

    public static long getLastImportedTimestamp(Context context) {
        if (context == null) {
            return -1L;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_LAST_IMPORTED_TS, -1L);
    }

    public static long getLastExportedAt(Context context) {
        if (context == null) {
            return -1L;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_LAST_EXPORTED_AT, -1L);
    }

    public static String getBackupPathForStatus(Context context) {
        if (context == null) {
            return "";
        }
        File readable = getReadableBackupFile(context, BACKUP_FILE_NAME);
        if (readable != null) {
            return readable.getAbsolutePath();
        }
        File externalTarget = buildPublicBackupFile(BACKUP_FILE_NAME);
        return externalTarget.getAbsolutePath();
    }

    private static File buildPublicBackupFile(String backupFileName) {
        File backupDir = buildPublicBackupDirectory();
        return new File(backupDir, backupFileName);
    }

    private static File buildPublicBackupDirectory() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        return new File(documentsDir, "SilverHolding");
    }

    private static File getPublicBackupFile(Context context, String backupFileName) {
        if (context == null) {
            return null;
        }
        if (!BackupFileNameUtil.isSafeFileName(backupFileName)) {
            return null;
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        File backupFile = buildPublicBackupFile(backupFileName);
        File backupDir = backupFile.getParentFile();
        if (backupDir == null || (!backupDir.exists() && !backupDir.mkdirs())) {
            return null;
        }
        try {
            File canonicalDir = backupDir.getCanonicalFile();
            File canonicalFile = backupFile.getCanonicalFile();
            String parentPath = canonicalDir.getPath() + File.separator;
            if (!canonicalFile.getPath().startsWith(parentPath)) {
                return null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to validate backup path", e);
            return null;
        }
        return backupFile;
    }

    private static String resolveBackupFileName(String backupFileName) {
        return BackupFileNameUtil.normalize(backupFileName, BACKUP_FILE_NAME);
    }

    private static void writeInternal(Context context, String name, String content) throws Exception {
        FileOutputStream fos = context.openFileOutput(name, Context.MODE_PRIVATE);
        fos.write(content.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    private static String readInternalAboutText(Context context) {
        try {
            File file = new File(context.getFilesDir(), ABOUT_FILE_NAME);
            if (!file.exists()) {
                return "";
            }
            FileInputStream fis = context.openFileInput(ABOUT_FILE_NAME);
            String value = readStream(fis);
            fis.close();
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read internal about text", e);
            return "";
        }
    }

    private static void writeFile(File file, String content) throws Exception {
        FileOutputStream fos = new FileOutputStream(file, false);
        fos.write(content.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    private static String readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        String value = readStream(fis);
        fis.close();
        return value;
    }

    private static String readStream(FileInputStream inputStream) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
