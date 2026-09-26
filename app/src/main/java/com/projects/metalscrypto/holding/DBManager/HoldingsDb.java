package com.projects.metalscrypto.holding.DBManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.projects.metalscrypto.holding.DBManager.HoldingItem;
import com.projects.metalscrypto.holding.DBManager.SilverT_DBModel;
import com.projects.metalscrypto.holding.R;

import java.util.ArrayList;
import java.util.List;

public class HoldingsDb extends SQLiteOpenHelper {

    private static final String TAG = "HoldingsDb";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "DBHOLDINGSDATA1.db";
    private static final String TABLE_NAME = "TABLEHOLDINGSDATA1";

    private static final String X_ID = "id";
    private static final String X_ITEM_TYPE = "item_type";
    private static final String X_ITEM_NO = "item_no";
    private static final String X_ITEM_WEIGHT = "item_weight";
    private static final String X_ITEM_WHAT = "item_what";
    private static final String X_ITEM_TOTAL = "item_total";
    private static final String X_ITEM_DATE = "item_date";
    private static final String X_ITEM_TIME = "item_time";
    private static final String X_LEGACY_SOURCE = "legacy_source";
    private static final String X_LEGACY_ID = "legacy_id";

    private final Context mContext;

    public HoldingsDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + X_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + X_ITEM_TYPE + " TEXT,"
                + X_ITEM_NO + " TEXT,"
                + X_ITEM_WEIGHT + " TEXT,"
                + X_ITEM_WHAT + " TEXT,"
                + X_ITEM_TOTAL + " TEXT,"
                + X_ITEM_DATE + " TEXT,"
                + X_ITEM_TIME + " TEXT,"
                + X_LEGACY_SOURCE + " TEXT,"
                + X_LEGACY_ID + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String itemType, String no, String weight, String what, String total, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = buildValues(itemType, no, weight, what, total, date, time, null, null);
        long result = db.insert(TABLE_NAME, null, values);
        db.close();
        return result != -1;
    }

    public boolean updateData(String id, String itemType, String no, String weight, String what, String total, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = buildValues(itemType, no, weight, what, total, date, time, null, null);
        int result = db.update(TABLE_NAME, values, X_ID + "=?", new String[]{id});
        db.close();
        return result > 0;
    }

    public boolean deleteData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NAME, X_ID + "=?", new String[]{id});
        db.close();
        return result > 0;
    }

    public int deleteByType(String itemType) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NAME, X_ITEM_TYPE + " COLLATE NOCASE = ?",
                new String[]{normalizeItemType(itemType)});
        db.close();
        return result;
    }

    public List<HoldingItem> getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null,
                X_ITEM_TYPE + " COLLATE NOCASE ASC, " + X_ID + " ASC");

        List<HoldingItem> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            items.add(readItem(cursor));
        }

        cursor.close();
        db.close();
        return items;
    }

    public List<HoldingItem> getItemsByType(String itemType) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, X_ITEM_TYPE + " COLLATE NOCASE = ?",
                new String[]{itemType}, null, null, X_ID + " ASC");

        List<HoldingItem> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            items.add(readItem(cursor));
        }

        cursor.close();
        db.close();
        return items;
    }

    public void importLegacyData(List<SilverT_DBModel> legacyItems, String itemType, String legacySource) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int insertedCount = 0;
            int skippedCount = 0;
            // Clean up historical duplicate imports before adding new rows.
            CleanupStats cleanupStats = removeLegacyDuplicates(db, legacySource);
            for (SilverT_DBModel legacyItem : legacyItems) {
                if (isLegacyItemAlreadyImported(db, legacyItem, itemType, legacySource)) {
                    skippedCount++;
                    continue;
                }
                ContentValues values = buildValues(
                        itemType,
                        legacyItem.getX_S_NO(),
                        legacyItem.getX_S_WEIGHT(),
                        legacyItem.getX_S_WHAT(),
                        legacyItem.getX_S_TOTAL(),
                        legacyItem.getX_S_DATE(),
                        legacyItem.getX_S_TIME(),
                        legacySource,
                        legacyItem.getX_ID()
                );
                long insertedRowId = db.insert(TABLE_NAME, null, values);
                if (insertedRowId != -1) {
                    insertedCount++;
                }
            }
            Log.d(TAG, "importLegacyData source=" + legacySource
                    + " legacyIdDeleted=" + cleanupStats.legacyIdDeleted
                    + " payloadDeleted=" + cleanupStats.payloadDeleted
                    + " totalDeleted=" + cleanupStats.getTotalDeleted()
                    + " inserted=" + insertedCount
                    + " skipped=" + skippedCount);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private CleanupStats removeLegacyDuplicates(SQLiteDatabase db, String legacySource) {
        if (legacySource == null || legacySource.trim().isEmpty()) {
            return new CleanupStats(0, 0);
        }

        // Keep the earliest row for each imported legacy id.
        String whereDeleteByLegacyId = X_LEGACY_SOURCE + "=?" +
                " AND " + X_LEGACY_ID + " IS NOT NULL" +
                " AND TRIM(" + X_LEGACY_ID + ")<>''" +
                " AND " + X_ID + " NOT IN (" +
                "SELECT MIN(" + X_ID + ") FROM " + TABLE_NAME +
                " WHERE " + X_LEGACY_SOURCE + "=?" +
                " AND " + X_LEGACY_ID + " IS NOT NULL" +
                " AND TRIM(" + X_LEGACY_ID + ")<>''" +
                " GROUP BY " + X_LEGACY_SOURCE + ", " + X_LEGACY_ID +
                ")";
        int legacyIdDeleted = db.delete(TABLE_NAME, whereDeleteByLegacyId, new String[]{legacySource, legacySource});

        // Fallback cleanup for rows where legacy id is missing.
        String whereDeleteByPayload = X_LEGACY_SOURCE + "=?" +
                " AND (" + X_LEGACY_ID + " IS NULL OR TRIM(" + X_LEGACY_ID + ")='')" +
                " AND " + X_ID + " NOT IN (" +
                "SELECT MIN(" + X_ID + ") FROM " + TABLE_NAME +
                " WHERE " + X_LEGACY_SOURCE + "=?" +
                " AND (" + X_LEGACY_ID + " IS NULL OR TRIM(" + X_LEGACY_ID + ")='')" +
                " GROUP BY " + X_ITEM_TYPE + ", " + X_ITEM_NO + ", " + X_ITEM_WEIGHT + ", " +
                X_ITEM_WHAT + ", " + X_ITEM_TOTAL + ", " + X_ITEM_DATE + ", " + X_ITEM_TIME +
                ")";
        int payloadDeleted = db.delete(TABLE_NAME, whereDeleteByPayload, new String[]{legacySource, legacySource});

        return new CleanupStats(legacyIdDeleted, payloadDeleted);
    }

    private static class CleanupStats {
        final int legacyIdDeleted;
        final int payloadDeleted;

        CleanupStats(int legacyIdDeleted, int payloadDeleted) {
            this.legacyIdDeleted = legacyIdDeleted;
            this.payloadDeleted = payloadDeleted;
        }

        int getTotalDeleted() {
            return legacyIdDeleted + payloadDeleted;
        }
    }

    private boolean isLegacyItemAlreadyImported(SQLiteDatabase db, SilverT_DBModel legacyItem,
                                                String itemType, String legacySource) {
        String legacyId = legacyItem.getX_ID();
        Cursor cursor = null;
        try {
            if (legacySource != null && legacyId != null && !legacyId.trim().isEmpty()) {
                cursor = db.query(
                        TABLE_NAME,
                        new String[]{X_ID},
                        X_LEGACY_SOURCE + "=? AND " + X_LEGACY_ID + "=?",
                        new String[]{legacySource, legacyId},
                        null,
                        null,
                        null,
                        "1"
                );
                if (cursor.moveToFirst()) {
                    return true;
                }
                cursor.close();
                cursor = null;
            }

            // Fallback for older rows imported before legacy metadata existed.
            cursor = db.query(
                    TABLE_NAME,
                    new String[]{X_ID},
                    X_ITEM_TYPE + "=? AND " +
                            X_ITEM_NO + "=? AND " +
                            X_ITEM_WEIGHT + "=? AND " +
                            X_ITEM_WHAT + "=? AND " +
                            X_ITEM_TOTAL + "=? AND " +
                            X_ITEM_DATE + "=? AND " +
                            X_ITEM_TIME + "=?",
                    new String[]{
                            normalizeItemType(itemType),
                            valueOrEmpty(legacyItem.getX_S_NO()),
                            valueOrEmpty(legacyItem.getX_S_WEIGHT()),
                            valueOrEmpty(legacyItem.getX_S_WHAT()),
                            valueOrEmpty(legacyItem.getX_S_TOTAL()),
                            valueOrEmpty(legacyItem.getX_S_DATE()),
                            valueOrEmpty(legacyItem.getX_S_TIME())
                    },
                    null,
                    null,
                    null,
                    "1"
            );
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private ContentValues buildValues(String itemType, String no, String weight, String what, String total,
                                    String date, String time, String legacySource, String legacyId) {
        ContentValues values = new ContentValues();
        values.put(X_ITEM_TYPE, normalizeItemType(itemType));
        values.put(X_ITEM_NO, no);
        values.put(X_ITEM_WEIGHT, weight);
        values.put(X_ITEM_WHAT, what);
        values.put(X_ITEM_TOTAL, total);
        values.put(X_ITEM_DATE, date);
        values.put(X_ITEM_TIME, time);
        values.put(X_LEGACY_SOURCE, legacySource);
        values.put(X_LEGACY_ID, legacyId);
        return values;
    }

    public static String normalizeItemType(String itemType) {
        if (itemType == null || itemType.trim().isEmpty()) {
            return "Unknown";
        }
        
        // Normalize common variations
        String normalized = itemType.trim();
        
        // Handle common metal variations
        if (normalized.equalsIgnoreCase("Silver") || normalized.equalsIgnoreCase("Ag") || 
            normalized.equalsIgnoreCase("XAG")) {
            return "Silver";
        }
        if (normalized.equalsIgnoreCase("Gold") || normalized.equalsIgnoreCase("Au") || 
            normalized.equalsIgnoreCase("XAU")) {
            return "Gold";
        }
        if (normalized.equalsIgnoreCase("Copper") || normalized.equalsIgnoreCase("Cu") || 
            normalized.equalsIgnoreCase("XCU")) {
            return "Copper";
        }
        if (normalized.equalsIgnoreCase("Platinum") || normalized.equalsIgnoreCase("Pt") || 
            normalized.equalsIgnoreCase("XPT")) {
            return "Platinum";
        }
        if (normalized.equalsIgnoreCase("Palladium") || normalized.equalsIgnoreCase("Pd") || 
            normalized.equalsIgnoreCase("XPD")) {
            return "Palladium";
        }
        if (normalized.equalsIgnoreCase("Lead") || normalized.equalsIgnoreCase("Pb") || 
            normalized.equalsIgnoreCase("XLD")) {
            return "Lead";
        }
        if (normalized.equalsIgnoreCase("Aluminum") || normalized.equalsIgnoreCase("Al") || 
            normalized.equalsIgnoreCase("XAL")) {
            return "Aluminum";
        }
        if (normalized.equalsIgnoreCase("Nickel") || normalized.equalsIgnoreCase("Ni") || 
            normalized.equalsIgnoreCase("XNI")) {
            return "Nickel";
        }
        if (normalized.equalsIgnoreCase("Zinc") || normalized.equalsIgnoreCase("Zn") || 
            normalized.equalsIgnoreCase("XZN")) {
            return "Zinc";
        }
        
        // Handle cryptocurrency variations
        if (normalized.equalsIgnoreCase("Bitcoin") || normalized.equalsIgnoreCase("BTC") || 
            normalized.equalsIgnoreCase("XBT")) {
            return "Bitcoin";
        }
        if (normalized.equalsIgnoreCase("Ethereum") || normalized.equalsIgnoreCase("ETH")) {
            return "Ethereum";
        }
        
        // Capitalize first letter of other items
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1).toLowerCase();
    }

    private HoldingItem readItem(Cursor cursor) {
        HoldingItem item = new HoldingItem();
        item.setId(cursor.getString(cursor.getColumnIndexOrThrow(X_ID)));
        item.setItemType(cursor.getString(cursor.getColumnIndexOrThrow(X_ITEM_TYPE)));
        item.setNumber(cursor.getString(cursor.getColumnIndexOrThrow(X_ITEM_NO)));
        item.setWeight(cursor.getString(cursor.getColumnIndexOrThrow(X_ITEM_WEIGHT)));
        item.setWhat(cursor.getString(cursor.getColumnIndexOrThrow(X_ITEM_WHAT)));
        item.setTotal(cursor.getString(cursor.getColumnIndexOrThrow(X_ITEM_TOTAL)));
        item.setUpdateDate(cursor.getString(cursor.getColumnIndexOrThrow(X_ITEM_DATE)));
        item.setUpdateTime(cursor.getString(cursor.getColumnIndexOrThrow(X_ITEM_TIME)));
        item.setLegacySource(cursor.getString(cursor.getColumnIndexOrThrow(X_LEGACY_SOURCE)));
        item.setLegacyId(cursor.getString(cursor.getColumnIndexOrThrow(X_LEGACY_ID)));
        return item;
    }
}
