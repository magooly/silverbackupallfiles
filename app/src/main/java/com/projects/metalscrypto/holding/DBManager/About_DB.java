package com.projects.metalscrypto.holding.DBManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class About_DB extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "DBABOUT.db";
    public static final String TABLE_NAME = "TABLE_ABOUT";

    public static final String X_ID = "ID";
    public static final String X_ABOUT= "About";


    private static final String TAG = "TAG";



    public About_DB(Context context)
    {
        super(context, DATABASE_NAME, null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(" +
                X_ID + " TEXT PRIMARY KEY , " +
                X_ABOUT + " TEXT NOT NULL )"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String id, String about) {
        id = "1";
        Log.d(TAG, "insertData: c X_ID " + id);
        Log.d(TAG, "insertData: X_S_PRICE " + about);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        initialValues.put(X_ID, id);
        initialValues.put(X_ABOUT, about);


        long result = db.insert(TABLE_NAME, null, initialValues);

        return result != -1;

    }

    public boolean updateData(String id, String about) {

        Log.d(TAG, "updateData: c X_ID " + id);
        Log.d(TAG, "updateData: about " + about);


        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(X_ABOUT, about);
        db.update(TABLE_NAME, cv, X_ID + "=" + id, null);

        return true;

    }

    public List<AboutModel> getNoteData() {

        String selectQuery = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(selectQuery, null, null);

        List<AboutModel> cartData = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                AboutModel silverDBModel = new AboutModel();
                silverDBModel.setId(cursor.getString(cursor.getColumnIndex(About_DB.X_ID)));
                silverDBModel.setAbout_text(cursor.getString(cursor.getColumnIndex(About_DB.X_ABOUT)));

                cartData.add(silverDBModel);
            } while (cursor.moveToNext());
        }

        Log.d(TAG, "getSilverData: count " + cursor.getCount());

        cursor.close();

        db.close();
        return cartData;
    }

    public void deleteCartDB()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + TABLE_NAME);
    }

    public Cursor CheckID(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + "WHERE" + X_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        cursor.getCount();

        Log.d(TAG, "CheckID: count  "+cursor.getCount());
        return cursor;
    }

}
