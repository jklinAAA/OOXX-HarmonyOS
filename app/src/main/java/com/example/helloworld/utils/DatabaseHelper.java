package com.example.helloworld.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseHelper extends SQLiteOpenHelper {
    // 数据库名称和版本
    private static final String DATABASE_NAME = "ooxx_game.db";
    private static final int DATABASE_VERSION = 1;

    // 游戏历史记录表
    public static final String TABLE_GAME_HISTORY = "game_history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_PLAYER_NAME = "player_name";
    public static final String COLUMN_BOARD_SIZE = "board_size";
    public static final String COLUMN_DIFFICULTY = "difficulty";
    public static final String COLUMN_TIME_TAKEN = "time_taken";
    public static final String COLUMN_RESULT = "result";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_GAME_MODE = "game_mode";

    // 排行榜表
    public static final String TABLE_LEADERBOARD = "leaderboard";

    // 创建游戏历史记录表的SQL语句
    private static final String CREATE_TABLE_GAME_HISTORY = "CREATE TABLE " + TABLE_GAME_HISTORY + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_PLAYER_NAME + " TEXT NOT NULL, "
            + COLUMN_BOARD_SIZE + " INTEGER NOT NULL, "
            + COLUMN_DIFFICULTY + " TEXT NOT NULL, "
            + COLUMN_TIME_TAKEN + " INTEGER NOT NULL, "
            + COLUMN_RESULT + " TEXT NOT NULL, "
            + COLUMN_TIMESTAMP + " TEXT NOT NULL, "
            + COLUMN_GAME_MODE + " TEXT NOT NULL" + ");";

    // 创建排行榜表的SQL语句
    private static final String CREATE_TABLE_LEADERBOARD = "CREATE TABLE " + TABLE_LEADERBOARD + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_PLAYER_NAME + " TEXT NOT NULL, "
            + COLUMN_BOARD_SIZE + " INTEGER NOT NULL, "
            + COLUMN_DIFFICULTY + " TEXT NOT NULL, "
            + COLUMN_TIME_TAKEN + " INTEGER NOT NULL, "
            + COLUMN_TIMESTAMP + " TEXT NOT NULL" + ");";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建表
        db.execSQL(CREATE_TABLE_GAME_HISTORY);
        db.execSQL(CREATE_TABLE_LEADERBOARD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级数据库时删除旧表并创建新表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GAME_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEADERBOARD);
        onCreate(db);
    }

    // 插入游戏记录到历史记录
    public long insertGameHistory(String playerName, int boardSize, String difficulty,
                                 long timeTaken, String result, String gameMode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PLAYER_NAME, playerName);
        values.put(COLUMN_BOARD_SIZE, boardSize);
        values.put(COLUMN_DIFFICULTY, difficulty);
        values.put(COLUMN_TIME_TAKEN, timeTaken);
        values.put(COLUMN_RESULT, result);
        values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
        values.put(COLUMN_GAME_MODE, gameMode);

        long id = db.insert(TABLE_GAME_HISTORY, null, values);
        db.close();
        return id;
    }

    // 更新排行榜
    public long updateLeaderboard(String playerName, int boardSize, String difficulty, long timeTaken) {
        // 首先检查当前玩家在相同难度和棋盘大小下是否已经有记录
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_PLAYER_NAME + " = ? AND " + COLUMN_BOARD_SIZE + " = ? AND " + COLUMN_DIFFICULTY + " = ?";
        String[] selectionArgs = {playerName, String.valueOf(boardSize), difficulty};
        Cursor cursor = db.query(TABLE_LEADERBOARD, null, selection, selectionArgs, null, null, null);

        long id = -1;
        if (cursor != null && cursor.moveToFirst()) {
            // 有记录，检查是否需要更新（时间是否更短）
            int existingTimeIndex = cursor.getColumnIndex(COLUMN_TIME_TAKEN);
            long existingTime = cursor.getLong(existingTimeIndex);
            if (timeTaken < existingTime) {
                // 更新记录
                ContentValues values = new ContentValues();
                values.put(COLUMN_TIME_TAKEN, timeTaken);
                values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
                SQLiteDatabase writeDb = this.getWritableDatabase();
                id = writeDb.update(TABLE_LEADERBOARD, values, selection, selectionArgs);
                writeDb.close();
            }
            cursor.close();
        } else {
            // 没有记录，插入新记录
            ContentValues values = new ContentValues();
            values.put(COLUMN_PLAYER_NAME, playerName);
            values.put(COLUMN_BOARD_SIZE, boardSize);
            values.put(COLUMN_DIFFICULTY, difficulty);
            values.put(COLUMN_TIME_TAKEN, timeTaken);
            values.put(COLUMN_TIMESTAMP, getCurrentTimestamp());
            SQLiteDatabase writeDb = this.getWritableDatabase();
            id = writeDb.insert(TABLE_LEADERBOARD, null, values);
            writeDb.close();
        }

        db.close();
        return id;
    }

    // 获取所有游戏历史记录
    public Cursor getAllGameHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_GAME_HISTORY, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");
    }

    // 获取特定玩家的游戏历史记录
    public Cursor getPlayerGameHistory(String playerName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_PLAYER_NAME + " = ?";
        String[] selectionArgs = {playerName};
        return db.query(TABLE_GAME_HISTORY, null, selection, selectionArgs, null, null, COLUMN_TIMESTAMP + " DESC");
    }

    // 获取排行榜数据
    public Cursor getLeaderboard(int boardSize, String difficulty, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_BOARD_SIZE + " = ? AND " + COLUMN_DIFFICULTY + " = ?";
        String[] selectionArgs = {String.valueOf(boardSize), difficulty};
        return db.query(TABLE_LEADERBOARD, null, selection, selectionArgs, null, null, COLUMN_TIME_TAKEN + " ASC", String.valueOf(limit));
    }

    // 获取所有排行榜数据
    public Cursor getAllLeaderboard() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_LEADERBOARD, null, null, null, null, null, COLUMN_TIME_TAKEN + " ASC");
    }

    // 清除所有游戏历史记录
    public void clearAllGameHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GAME_HISTORY, null, null);
        db.close();
    }

    // 清除所有排行榜记录
    public void clearAllLeaderboard() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LEADERBOARD, null, null);
        db.close();
    }

    // 获取当前时间戳
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 获取总游戏次数
    public int getTotalGamesPlayed() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GAME_HISTORY, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return count;
    }

    // 获取玩家的最佳成绩
    public long getPlayerBestTime(String playerName, int boardSize, String difficulty) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_PLAYER_NAME + " = ? AND " + COLUMN_BOARD_SIZE + " = ? AND " + COLUMN_DIFFICULTY + " = ? AND " + COLUMN_RESULT + " = ?";
        String[] selectionArgs = {playerName, String.valueOf(boardSize), difficulty, "win"};
        Cursor cursor = db.query(TABLE_GAME_HISTORY, new String[]{"MIN(" + COLUMN_TIME_TAKEN + ")"}, selection, selectionArgs, null, null, null);
        long bestTime = -1;
        if (cursor != null && cursor.moveToFirst()) {
            bestTime = cursor.getLong(0);
            cursor.close();
        }
        db.close();
        return bestTime;
    }
}