package com.example.helloworld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.helloworld.game.GameLogic;
import com.example.helloworld.ui.BluetoothActivity;
import com.example.helloworld.ui.GameScreenActivity;
import com.example.helloworld.ui.LeaderboardScreenActivity;
import com.example.helloworld.ui.SettingsActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnSinglePlayer;
    private Button btnBluetoothMode;
    private Button btnLeaderboard;
    private Button btnSettings;
    private Button btnAbout;
    private TextView tvVersion;
    private ImageView ivLogo;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "GamePrefs";
    private static final String KEY_THEME = "theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        btnSinglePlayer = findViewById(R.id.btn_single_player);
        btnBluetoothMode = findViewById(R.id.btn_bluetooth_mode);
        btnLeaderboard = findViewById(R.id.btn_leaderboard);
        btnSettings = findViewById(R.id.btn_settings);
        btnAbout = findViewById(R.id.btn_about);
        tvVersion = findViewById(R.id.tv_version);
        ivLogo = findViewById(R.id.iv_logo);

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 应用保存的主题
        applyTheme();

        // 设置版本信息
        String versionName = BuildConfig.VERSION_NAME;
        tvVersion.setText("版本: " + versionName);

        // 设置按钮点击事件
        btnSinglePlayer.setOnClickListener(v -> startSinglePlayerMode());
        btnBluetoothMode.setOnClickListener(v -> startBluetoothMode());
        btnLeaderboard.setOnClickListener(v -> showLeaderboard());
        btnSettings.setOnClickListener(v -> openSettings());
        btnAbout.setOnClickListener(v -> showAbout());
    }

    private void applyTheme() {
        int theme = sharedPreferences.getInt(KEY_THEME, 0); // 0为默认主题
        switch (theme) {
            case 1:
                // 应用深色主题
                setTheme(R.style.Theme_HelloWorld_Dark);
                break;
            case 2:
                // 应用霓虹主题
                setTheme(R.style.Theme_HelloWorld_Neon);
                break;
            default:
                // 应用默认主题
                setTheme(R.style.Theme_HelloWorld);
                break;
        }
    }

    private void startSinglePlayerMode() {
        // 显示棋盘大小选择对话框
        showBoardSizeDialog();
    }

    private void startBluetoothMode() {
        Intent intent = new Intent(this, BluetoothActivity.class);
        startActivity(intent);
    }

    private void showLeaderboard() {
        Intent intent = new Intent(this, LeaderboardScreenActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showAbout() {
        Toast.makeText(this, "OOXX游戏 v" + BuildConfig.VERSION_NAME + "\n支持单机和蓝牙联机对战\n拥有多种难度级别和棋盘尺寸", Toast.LENGTH_LONG).show();
    }

    private void showBoardSizeDialog() {
        String[] sizes = {"4x4", "6x6", "8x8", "10x10"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("选择棋盘大小");
        builder.setItems(sizes, (dialog, which) -> {
            int size;
            switch (which) {
                case 0:
                    size = 4;
                    break;
                case 1:
                    size = 6;
                    break;
                case 2:
                    size = 8;
                    break;
                case 3:
                    size = 10;
                    break;
                default:
                    size = 6;
                    break;
            }
            showDifficultyDialog(size);
        });
        builder.show();
    }

    private void showDifficultyDialog(int size) {
        String[] difficulties = {"简单", "困难"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("选择难度");
        builder.setItems(difficulties, (dialog, which) -> {
            GameLogic.Difficulty difficulty = (which == 0) ? GameLogic.Difficulty.EASY : GameLogic.Difficulty.HARD;
            startGame(size, difficulty);
        });
        builder.show();
    }

    private void startGame(int size, GameLogic.Difficulty difficulty) {
        Intent intent = new Intent(this, GameScreenActivity.class);
        intent.putExtra("BOARD_SIZE", size);
        intent.putExtra("DIFFICULTY", difficulty.name());
        intent.putExtra("BLUETOOTH_MODE", false);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新应用主题，以防在设置中更改了主题
        applyTheme();
    }
}
}