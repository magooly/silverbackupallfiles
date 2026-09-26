package com.projects.metalscrypto.holding;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.HoldingsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class BackupSyncManager {
    private static final String PREFS_NAME = "backup_sync";
    private static final String PREF_LAST_IMPORTED_TS = "last_imported_ts";
    private static final String PREF_LAST_EXPORTED_AT = "last_exported_at";
    private static final String BACKUP_FILE_NAME = "holdings_backup.json";
    private static final String ABOUT_FILE_NAME = "about.txt";

    private BackupSyncManager() {
    }

    public static void exportSnapshot(Context context) {
        if (context == null) {
            return;
        }
        try {
            HoldingsDb holdingsDb = new HoldingsDb(context);
            List<HoldingItem> items = holdingsDb.getAllItems();

            JSONObject root = new JSONObject();
            root.put("schema", 1);
            root.put("exportedAt", nowDateTime());
            root.put("aboutText", readInternalAboutText(context));

            JSONArray itemArray = new JSONArray();
            for (HoldingItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("itemType", HoldingsDb.normalizeItemType(item.getItemType()));
                obj.put("no", safe(item.getNumber()));
                obj.put("weight", safe(item.getWeight()));
                obj.put("what", safe(item.getWhat()));
                obj.put("total", safe(item.getTotal()));
                obj.put("date", safe(item.getUpdateDate()));
                obj.put("time", safe(item.getUpdateTime()));
                itemArray.put(obj);
            }
            root.put("items", itemArray);

            String json = root.toString();
            writeInternal(context, BACKUP_FILE_NAME, json);

            File externalFile = getPublicBackupFile(context);
            if (externalFile != null) {
                writeFile(externalFile, json);
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(PREF_LAST_EXPORTED_AT, System.currentTimeMillis())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public static void importAndMergeOnStart(Context context) {
        importAndMerge(context, false);
    }

    public static boolean restoreNow(Context context) {
        return importAndMerge(context, true);
    }

    public static boolean hasBackupFile(Context context) {
        if (context == null) {
            return false;
        }
        File backupFile = getReadableBackupFile(context);
        return backupFile != null && backupFile.exists();
    }

    private static boolean importAndMerge(Context context, boolean force) {
        if (context == null) {
            return false;
        }

        File backupFile = getReadableBackupFile(context);
        if (backupFile == null || !backupFile.exists()) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastImported = prefs.getLong(PREF_LAST_IMPORTED_TS, -1L);
        long fileTs = backupFile.lastModified();
        if (!force && fileTs > 0 && lastImported == fileTs) {
            return false;
        }

        try {
            String raw = readFile(backupFile);
            if (TextUtils.isEmpty(raw)) {
                return false;
            }

            JSONObject root = new JSONObject(raw);
            String aboutText = root.optString("aboutText", "");
            if (!TextUtils.isEmpty(aboutText)) {
                writeInternal(context, ABOUT_FILE_NAME, aboutText);
            }

            HoldingsDb holdingsDb = new HoldingsDb(context);
            JSONArray items = root.optJSONArray("items");
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
                    String date = obj.optString("date", nowDate());
                    String time = obj.optString("time", nowTime());

                    if (isDetailedType(type)) {
                        if (!hasDetailedDuplicate(holdingsDb, type, no, weight, what, total)) {
                            holdingsDb.insertData(type, no, weight, what, total, date, time);
                        }
                    } else {
                        mergeNonDetailedType(holdingsDb, type, total, date, time);
                    }
                }
            }

            prefs.edit().putLong(PREF_LAST_IMPORTED_TS, fileTs).apply();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasDetailedDuplicate(HoldingsDb db, String type, String no, String weight, String what, String total) {
        List<HoldingItem> existing = db.getItemsByType(type);
        for (HoldingItem item : existing) {
            if (safe(item.getNumber()).equals(safe(no))
                    && safe(item.getWeight()).equals(safe(weight))
                    && safe(item.getWhat()).equals(safe(what))
                    && safe(item.getTotal()).equals(safe(total))) {
                return true;
            }
        }
        return false;
    }

    private static void mergeNonDetailedType(HoldingsDb db, String type, String total, String date, String time) {
        List<HoldingItem> existing = db.getItemsByType(type);
        String normalizedTotal = safe(total);
        if (existing.isEmpty()) {
            db.insertData(type, "1", normalizedTotal, "Total", normalizedTotal, date, time);
            return;
        }

        HoldingItem keeper = existing.get(0);
        boolean changed = !safe(keeper.getTotal()).equals(normalizedTotal)
                || !safe(keeper.getWeight()).equals(normalizedTotal)
                || !"Total".equalsIgnoreCase(safe(keeper.getWhat()));
        if (changed) {
            db.updateData(keeper.getId(), type, "1", normalizedTotal, "Total", normalizedTotal, date, time);
        }
        for (int i = 1; i < existing.size(); i++) {
            db.deleteData(existing.get(i).getId());
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

    private static File getReadableBackupFile(Context context) {
        File external = buildPublicBackupFile();
        if (external.exists()) {
            return external;
        }
        File internal = new File(context.getFilesDir(), BACKUP_FILE_NAME);
        return internal.exists() ? internal : null;
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
        File readable = getReadableBackupFile(context);
        if (readable != null) {
            return readable.getAbsolutePath();
        }
        File externalTarget = buildPublicBackupFile();
        return externalTarget.getAbsolutePath();
    }

    private static File buildPublicBackupFile() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File backupDir = new File(documentsDir, "SilverHolding");
        return new File(backupDir, BACKUP_FILE_NAME);
    }

    private static File getPublicBackupFile(Context context) {
        if (context == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        File backupFile = buildPublicBackupFile();
        File backupDir = backupFile.getParentFile();
        if (backupDir == null || (!backupDir.exists() && !backupDir.mkdirs())) {
            return null;
        }
        return backupFile;
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
        } catch (Exception ignored) {
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

