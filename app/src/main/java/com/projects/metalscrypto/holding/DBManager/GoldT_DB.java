package com.projects.metalscrypto.holding.DBManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class GoldT_DB extends SQLiteOpenHelper
{

    public static final String DATABASE_NAME = "DBGOLDDATA1.db";
    public static final String TABLE_NAME = "TABLEGOLDDATA1";

    public static final String X_ID = "ID";
    public static final String X_S_NO = "s_no";
    public static final String X_S_WEIGHT = "s_weight";
    public static final String X_S_WHAT = "s_what";
    public static final String X_S_TOTAL = "s_total";
    public static final String X_S_DATE = "update_date";
    public static final String X_S_TIME = "update_time";


    private static final String TAG = "TAG";

    private static final String[] allColumns =
            {
                    GoldT_DB.X_ID,
                    GoldT_DB.X_S_NO,
                    GoldT_DB.X_S_WEIGHT,
                    GoldT_DB.X_S_WHAT,
                    GoldT_DB.X_S_TOTAL,
                    GoldT_DB.X_S_DATE,
                    GoldT_DB.X_S_TIME,
            };

    public GoldT_DB(Context context)
    {
        super(context, DATABASE_NAME, null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(" +
                X_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                X_S_NO + " TEXT NOT NULL , " +
                X_S_WEIGHT + " TEXT NOT NULL , " +
                X_S_WHAT + " TEXT NOT NULL , " +
                X_S_TOTAL + " TEXT NOT NULL , " +
                X_S_DATE + " TEXT NOT NULL , " +
                X_S_TIME + " TEXT NOT NULL )"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String no, String weight, String what, String total,String date, String time)
    {
        Log.d(TAG, "insertData: X_S_NO " + no);
        Log.d(TAG, "insertData: X_S_WEIGHT " + weight);
        Log.d(TAG, "insertData: X_S_WHAT " + what);
        Log.d(TAG, "insertData: X_S_TOTAL " + total);
        Log.d(TAG, "insertData: X_DATE " + date);
        Log.d(TAG, "insertData: X_TIME " + time);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        initialValues.put(X_S_NO, no);
        initialValues.put(X_S_WEIGHT, weight);
        initialValues.put(X_S_WHAT, what);
        initialValues.put(X_S_TOTAL, total);
        initialValues.put(X_S_DATE, date);
        initialValues.put(X_S_TIME, time);

        long result = db.insert(TABLE_NAME, null, initialValues);

//        long newRowId = db.insert(TABLE_NAME, null, initialValues);

//        Log.e("InsertData", Long.toString(newRowId));
        return result != -1;
    }


    public boolean updateData(String id,String no, String weight, String what, String total,String date, String time)
    {
        Log.d(TAG, "updateData: X_ID " + id);
        Log.d(TAG, "updateData: X_S_NO " + no);
        Log.d(TAG, "updateData: X_S_WEIGHT " + weight);
        Log.d(TAG, "updateData: X_S_WHAT " + what);
        Log.d(TAG, "updateData: X_S_TOTAL " + total);
        Log.d(TAG, "updateData: X_DATE " + date);
        Log.d(TAG, "updateData: X_TIME " + time);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(X_S_NO, no);
        cv.put(X_S_WEIGHT, weight);
        cv.put(X_S_WHAT, what);
        cv.put(X_S_TOTAL, total);
        cv.put(X_S_DATE, date);
        cv.put(X_S_TIME, time);

        db.update(TABLE_NAME, cv, X_ID + "=" + id, null);

        int cb=db.update(TABLE_NAME, cv, X_ID + "=" + id, null);


        Log.d(TAG, "updateData: queary "+db.update(TABLE_NAME, cv, X_ID + "=" + id, null));


        Log.d(TAG, "updateData: cb "+cb);


//        db.update(TABLE_NAME, cv, "x_QuestionID="+id, null);

        return true;
    }


    public List<SilverT_DBModel> getGoldList()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_NAME, allColumns, null, null, null, null, null);

        List<SilverT_DBModel> silverTDb = new ArrayList<>();
        if (cursor.getCount() > 0)
        {
            while (cursor.moveToNext())
            {
                SilverT_DBModel silverT_dbModel = new SilverT_DBModel();
                silverT_dbModel.setX_ID(String.valueOf(cursor.getLong(cursor.getColumnIndex(GoldT_DB.X_ID))));
                silverT_dbModel.setX_S_NO(String.valueOf(cursor.getLong(cursor.getColumnIndex(GoldT_DB.X_S_NO))));
                silverT_dbModel.setX_S_WEIGHT(String.valueOf(cursor.getString(cursor.getColumnIndex(GoldT_DB.X_S_WEIGHT))));
                silverT_dbModel.setX_S_WHAT(String.valueOf(cursor.getString(cursor.getColumnIndex(GoldT_DB.X_S_WHAT))));
                silverT_dbModel.setX_S_TOTAL(String.valueOf(cursor.getString(cursor.getColumnIndex(GoldT_DB.X_S_TOTAL))));
                silverT_dbModel.setX_S_DATE(String.valueOf(cursor.getString(cursor.getColumnIndex(GoldT_DB.X_S_DATE))));
                silverT_dbModel.setX_S_TIME(String.valueOf(cursor.getString(cursor.getColumnIndex(GoldT_DB.X_S_TIME))));
                silverTDb.add(silverT_dbModel);
            }
        }

        Log.d(TAG, "getGoldList:   silverT_dbModel  "+silverTDb.size());

        cursor.close();
        db.close();
        return silverTDb;
    }


}
