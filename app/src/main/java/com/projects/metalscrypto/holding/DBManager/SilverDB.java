package com.projects.metalscrypto.holding.DBManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SilverDB extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "DBSILVER.db";
    public static final String TABLE_NAME = "TABLESILVER";

    public static final String X_S_PRICE = "Silver_Price";
    public static final String X_G_PRICE = "Gold_Price";
    public static final String X_DATE = "update_date";
    public static final String X_TIME = "update_time";

    private static final String TAG = "TAG";



    public SilverDB(Context context)
    {
        super(context, DATABASE_NAME, null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(" +
                X_S_PRICE + " TEXT NOT NULL , " +
                X_G_PRICE + " TEXT NOT NULL , " +
                X_DATE + " TEXT NOT NULL , " +
                X_TIME + " TEXT NOT NULL )"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String id, String silver, String gold, String date, String time) {
        Log.d(TAG, "insertData: X_S_PRICE " + silver);
        Log.d(TAG, "insertData: X_G_PRICE " + gold);
        Log.d(TAG, "insertData: X_DATE " + date);
        Log.d(TAG, "insertData: X_TIME " + time);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        initialValues.put(X_S_PRICE, silver);
        initialValues.put(X_G_PRICE, gold);
        initialValues.put(X_DATE, date);
        initialValues.put(X_TIME, time);

        long result = db.insert(TABLE_NAME, null, initialValues);

//        long newRowId = db.insert(TABLE_NAME, null, initialValues);
//
//        Log.e("InsertData", Long.toString(newRowId));

        return result != -1;

    }

    public boolean updateData(String id, String silver, String gold, String date, String time) {

        Log.d(TAG, "updateData: X_S_PRICE " + silver);
        Log.d(TAG, "updateData: X_G_PRICE " + gold);
        Log.d(TAG, "insertData: X_DATE " + date);
        Log.d(TAG, "insertData: X_TIME " + time);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(X_S_PRICE, silver);
        cv.put(X_G_PRICE, gold);
        cv.put(X_DATE, date);
        cv.put(X_TIME, time);
        int updated = db.update(TABLE_NAME, cv, X_DATE + "=?", new String[]{date});
        db.close();

        return updated > 0;

    }

    public List<SilverDBModel> getSilverData(String date) {

        String selectQuery = "SELECT  * FROM " + TABLE_NAME+" WHERE " + X_DATE + "='" + date+"'";

//        String selectQuery = "SELECT  * FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(selectQuery, null, null);

        List<SilverDBModel> cartData = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                SilverDBModel silverDBModel = new SilverDBModel();
                silverDBModel.setSilver(cursor.getString(cursor.getColumnIndex(SilverDB.X_S_PRICE)));
                silverDBModel.setGold(cursor.getString(cursor.getColumnIndex(SilverDB.X_G_PRICE)));
                silverDBModel.setDate(cursor.getString(cursor.getColumnIndex(SilverDB.X_DATE)));
                silverDBModel.setTime(cursor.getString(cursor.getColumnIndex(SilverDB.X_TIME)));
                cartData.add(silverDBModel);
            } while (cursor.moveToNext());
        }

        Log.d(TAG, "getSilverData: count " + cursor.getCount());

        cursor.close();

        db.close();
        return cartData;
    }



}
