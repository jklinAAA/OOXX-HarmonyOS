package com.example.helloworld;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "TimeDatabase";
    private static final int DATABASE_VERSION = 1;
    
    // 表名和列名
    public static final String TABLE_TIME = "time_records";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_ACTION = "action";

    // 创建表的SQL语句
    private static final String CREATE_TABLE_TIME = 
        "CREATE TABLE " + TABLE_TIME + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_TIMESTAMP + " TEXT NOT NULL, " +
        COLUMN_ACTION + " TEXT NOT NULL)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TIME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME);
        onCreate(db);
    }

    // 插入时间记录
    public long insertTimeRecord(String timestamp, String action) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_ACTION, action);
        return db.insert(TABLE_TIME, null, values);
    }

    // 获取最后一条记录
    public String getLastRecord() {
        SQLiteDatabase db = this.getReadableDatabase();
        String result = "无记录";
        
        Cursor cursor = db.query(
            TABLE_TIME,
            new String[]{COLUMN_TIMESTAMP, COLUMN_ACTION},
            null,
            null,
            null,
            null,
            COLUMN_ID + " DESC",
            "1"
        );

        if (cursor != null && cursor.moveToFirst()) {
            String timestamp = cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP));
            String action = cursor.getString(cursor.getColumnIndex(COLUMN_ACTION));
            result = action + ": " + timestamp;
            cursor.close();
        }
        
        return result;
    }

    // 获取所有记录
    public Cursor getAllRecords() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
            TABLE_TIME,
            new String[]{COLUMN_ID, COLUMN_TIMESTAMP, COLUMN_ACTION},
            null,
            null,
            null,
            null,
            COLUMN_ID + " DESC"
        );
    }

    // 清除所有记录
    public void clearAllRecords() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TIME, null, null);
    }
} 