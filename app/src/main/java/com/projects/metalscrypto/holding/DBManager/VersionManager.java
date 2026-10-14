package com.projects.metalscrypto.holding.DBManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.projects.metalscrypto.holding.BuildConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VersionManager {

    private static final String TAG = "VersionManager";
    private static final String TABLE_VERSION_HISTORY = "version_history";
    private static final String COL_ID = "id";
    private static final String COL_VERSION_NAME = "version_name";
    private static final String COL_VERSION_CODE = "version_code";
    private static final String COL_UPDATE_DATE = "update_date";
    private static final String COL_TIMESTAMP = "timestamp";

    private final HoldingsDb holdingsDb;
    private final Context context;

    public VersionManager(Context context) {
        this.context = context;
        this.holdingsDb = new HoldingsDb(context);
    }

    /**
     * Ensures version_history table exists (called on first use)
     */
    public void initializeTable() {
        SQLiteDatabase db = holdingsDb.getWritableDatabase();
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{TABLE_VERSION_HISTORY}
            );
            boolean tableExists = cursor.getCount() > 0;
            cursor.close();

            if (!tableExists) {
                String createTable = "CREATE TABLE " + TABLE_VERSION_HISTORY + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COL_VERSION_NAME + " TEXT NOT NULL," +
                        COL_VERSION_CODE + " INTEGER," +
                        COL_UPDATE_DATE + " TEXT," +
                        COL_TIMESTAMP + " LONG" +
                        ")";
                db.execSQL(createTable);
                Log.d(TAG, "Version history table created");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing version table", e);
        }
    }

    /**
     * Returns true when a new version entry should be recorded.
     * This covers fresh installs and cases where the stored version is blank or stale.
     */
    public static boolean shouldRecordVersion(String currentVersion, String lastVersion) {
        if (currentVersion == null || currentVersion.trim().isEmpty()) {
            return false;
        }
        if (lastVersion == null || lastVersion.trim().isEmpty()) {
            return true;
        }
        return !currentVersion.trim().equals(lastVersion.trim());
    }

    /**
     * Check if current app version differs from last recorded version.
     * If so, add new entry to history.
     */
    public void checkAndUpdateVersion() {
        initializeTable();

        String currentVersion = BuildConfig.VERSION_NAME;
        int currentVersionCode = BuildConfig.VERSION_CODE;
        String lastVersion = getLastVersion();

        if (shouldRecordVersion(currentVersion, lastVersion)) {
            addVersionEntry(currentVersion, currentVersionCode);
            Log.d(TAG, "Version updated: " + lastVersion + " -> " + currentVersion);
        }
    }

    /**
     * Get the most recently recorded version
     */
    public String getLastVersion() {
        SQLiteDatabase db = holdingsDb.getReadableDatabase();
        String lastVersion = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_VERSION_HISTORY,
                    new String[]{COL_VERSION_NAME},
                    null,
                    null,
                    null,
                    null,
                    COL_TIMESTAMP + " DESC",
                    "1"
            );

            if (cursor.moveToFirst()) {
                lastVersion = cursor.getString(cursor.getColumnIndexOrThrow(COL_VERSION_NAME));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last version", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return lastVersion;
    }

    /**
     * Add a new version entry to history
     */
    private void addVersionEntry(String versionName, int versionCode) {
        SQLiteDatabase db = holdingsDb.getWritableDatabase();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date());
            long timestamp = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put(COL_VERSION_NAME, versionName);
            values.put(COL_VERSION_CODE, versionCode);
            values.put(COL_UPDATE_DATE, dateStr);
            values.put(COL_TIMESTAMP, timestamp);

            db.insert(TABLE_VERSION_HISTORY, null, values);
            Log.d(TAG, "Version entry added: " + versionName + " on " + dateStr);
        } catch (Exception e) {
            Log.e(TAG, "Error adding version entry", e);
        }
    }

    /**
     * Get all version history records (newest first)
     */
    public List<VersionEntry> getAllVersions() {
        List<VersionEntry> versions = new ArrayList<>();
        
        initializeTable();
        
        SQLiteDatabase db = holdingsDb.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_VERSION_HISTORY,
                    new String[]{COL_VERSION_NAME, COL_UPDATE_DATE, COL_TIMESTAMP},
                    null,
                    null,
                    null,
                    null,
                    COL_TIMESTAMP + " DESC"
            );

            while (cursor.moveToNext()) {
                String versionName = cursor.getString(cursor.getColumnIndexOrThrow(COL_VERSION_NAME));
                String updateDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_UPDATE_DATE));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));

                versions.add(new VersionEntry(versionName, updateDate, timestamp));
            }
            Log.d(TAG, "Retrieved " + versions.size() + " version entries");
        } catch (Exception e) {
            Log.e(TAG, "Error getting all versions", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return versions;
    }

    /**
     * Inner class to represent a version entry
     */
    public static class VersionEntry {
        public final String versionName;
        public final String updateDate;
        public final long timestamp;

        public VersionEntry(String versionName, String updateDate, long timestamp) {
            this.versionName = versionName;
            this.updateDate = updateDate;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "Version " + versionName + "  " + updateDate;
        }
    }
}
