package com.example.helloworld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {
    private TextView tvWelcome, tvLastCloseTime, tvMusicStatus, tvDatabaseRecords;
    private Button btnRecordTime, btnSelectMusic, btnViewRecords, btnClearData;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MainPrefs";
    private static final String KEY_LAST_CLOSE_TIME = "last_close_time";
    private static final String KEY_MUSIC_FILE = "music_file_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        tvWelcome = findViewById(R.id.tv_welcome);
        tvLastCloseTime = findViewById(R.id.tv_last_close_time);
        tvMusicStatus = findViewById(R.id.tv_music_status);
        tvDatabaseRecords = findViewById(R.id.tv_database_records);
        btnRecordTime = findViewById(R.id.btn_record_time);
        btnSelectMusic = findViewById(R.id.btn_select_music);
        btnViewRecords = findViewById(R.id.btn_view_records);
        btnClearData = findViewById(R.id.btn_clear_data);

        // 初始化数据库和SharedPreferences
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 显示欢迎信息
        String username = getIntent().getStringExtra("username");
        if (username != null) {
            tvWelcome.setText("欢迎, " + username + "!");
        }

        // 显示上次关闭时间
        String lastCloseTime = sharedPreferences.getString(KEY_LAST_CLOSE_TIME, "首次启动");
        tvLastCloseTime.setText("上次关闭时间: " + lastCloseTime);

        // 显示音乐文件状态
        String musicPath = sharedPreferences.getString(KEY_MUSIC_FILE, "");
        if (!musicPath.isEmpty()) {
            File musicFile = new File(musicPath);
            if (musicFile.exists()) {
                tvMusicStatus.setText("音乐文件: " + musicFile.getName() + " (已保存)");
            } else {
                tvMusicStatus.setText("音乐文件: 未找到");
            }
        } else {
            tvMusicStatus.setText("音乐文件: 未选择");
        }

        // 记录当前启动时间
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        databaseHelper.insertTimeRecord(currentTime, "应用启动");

        // 按钮点击事件
        btnRecordTime.setOnClickListener(v -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            databaseHelper.insertTimeRecord(time, "手动记录");
            Toast.makeText(this, "时间已记录: " + time, Toast.LENGTH_SHORT).show();
            updateDatabaseDisplay();
        });

        btnSelectMusic.setOnClickListener(v -> {
            showMusicOptionsDialog();
        });

        btnViewRecords.setOnClickListener(v -> {
            updateDatabaseDisplay();
        });

        btnClearData.setOnClickListener(v -> {
            databaseHelper.clearAllRecords();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            Toast.makeText(this, "所有数据已清除", Toast.LENGTH_SHORT).show();
            updateDatabaseDisplay();
            tvLastCloseTime.setText("上次关闭时间: 首次启动");
            tvMusicStatus.setText("音乐文件: 未选择");
        });

        // 初始显示数据库记录
        updateDatabaseDisplay();
    }

    private void updateDatabaseDisplay() {
        Cursor cursor = databaseHelper.getAllRecords();
        StringBuilder sb = new StringBuilder("数据库记录:\n");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String timestamp = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));
                String action = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ACTION));
                sb.append(action).append(": ").append(timestamp).append("\n");
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            sb.append("暂无记录");
        }
        tvDatabaseRecords.setText(sb.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedMusic = data.getData();
            if (selectedMusic != null) {
                copyMusicToPrivateDirectory(selectedMusic);
            }
        }
    }

    private void copyMusicToPrivateDirectory(Uri musicUri) {
        try {
            // 获取音乐文件名
            String fileName = getFileName(musicUri);
            if (fileName == null) {
                fileName = "music_" + System.currentTimeMillis() + ".mp3";
            }

            // 创建应用私有目录中的文件
            File privateDir = getFilesDir();
            File musicFile = new File(privateDir, fileName);

            // 复制文件
            InputStream inputStream = getContentResolver().openInputStream(musicUri);
            OutputStream outputStream = new FileOutputStream(musicFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            // 保存文件路径到SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_MUSIC_FILE, musicFile.getAbsolutePath());
            editor.apply();

            tvMusicStatus.setText("音乐文件: " + fileName + " (已保存到私有目录)");
            Toast.makeText(this, "音乐文件已保存: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "保存音乐文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }



    private void showMusicOptionsDialog() {
        String[] options = {"从系统选择音乐", "从网络下载音乐"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("选择音乐文件方式");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // 从系统选择音乐
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 1);
            } else if (which == 1) {
                // 从网络下载音乐
                downloadMusicFromNetwork();
            }
        });
        builder.show();
    }

    private void downloadMusicFromNetwork() {
        // 使用一个公开的音乐文件URL进行测试
        String musicUrl = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav";
        String fileName = "downloaded_music_" + System.currentTimeMillis() + ".wav";
        
        new Thread(() -> {
            try {
                // 创建应用私有目录中的文件
                File privateDir = getFilesDir();
                File musicFile = new File(privateDir, fileName);

                // 下载文件
                java.net.URL url = new java.net.URL(musicUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(musicFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();
                connection.disconnect();

                // 在主线程更新UI
                runOnUiThread(() -> {
                    // 保存文件路径到SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_MUSIC_FILE, musicFile.getAbsolutePath());
                    editor.apply();

                    tvMusicStatus.setText("音乐文件: " + fileName + " (已下载)");
                    Toast.makeText(MainActivity.this, "音乐文件下载成功: " + fileName, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

        Toast.makeText(this, "正在下载音乐文件...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 记录应用关闭时间
        String closeTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        databaseHelper.insertTimeRecord(closeTime, "应用关闭");
        
        // 保存关闭时间到SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_CLOSE_TIME, closeTime);
        editor.apply();
    }
} 