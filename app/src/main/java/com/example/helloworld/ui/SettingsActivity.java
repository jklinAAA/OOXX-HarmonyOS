package com.example.helloworld.ui;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.helloworld.R;

public class SettingsActivity extends AppCompatActivity {
    private Spinner themeSpinner;
    private CheckBox soundEffectsCheckBox;
    private CheckBox musicCheckBox;
    private SeekBar soundVolumeSeekBar;
    private TextView soundVolumeTextView;
    private SeekBar musicVolumeSeekBar;
    private TextView musicVolumeTextView;
    private CheckBox autoSaveCheckBox;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "GamePrefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SOUND_EFFECTS = "sound_effects";
    private static final String KEY_MUSIC = "music";
    private static final String KEY_SOUND_VOLUME = "sound_volume";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_AUTO_SAVE = "auto_save";

    private MediaPlayer previewMediaPlayer;
    private boolean isThemeChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 初始化UI组件
        themeSpinner = findViewById(R.id.theme_spinner);
        soundEffectsCheckBox = findViewById(R.id.sound_effects_checkbox);
        musicCheckBox = findViewById(R.id.music_checkbox);
        soundVolumeSeekBar = findViewById(R.id.sound_volume_seekbar);
        soundVolumeTextView = findViewById(R.id.sound_volume_textview);
        musicVolumeSeekBar = findViewById(R.id.music_volume_seekbar);
        musicVolumeTextView = findViewById(R.id.music_volume_textview);
        autoSaveCheckBox = findViewById(R.id.auto_save_checkbox);

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // 设置主题选择器
        String[] themes = {"默认主题", "深色主题", "霓虹主题"};
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, themes);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(themeAdapter);

        // 加载保存的设置
        loadSettings();

        // 设置监听器
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt(KEY_THEME, position);
                editor.apply();
                isThemeChanged = true;
                Toast.makeText(SettingsActivity.this, "主题已更改，下次启动时生效", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不执行任何操作
            }
        });

        soundEffectsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(KEY_SOUND_EFFECTS, isChecked);
            editor.apply();
        });

        musicCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(KEY_MUSIC, isChecked);
            editor.apply();
            updateMusicPreview(isChecked);
        });

        soundVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                soundVolumeTextView.setText("音效音量: " + progress + "%");
                editor.putInt(KEY_SOUND_VOLUME, progress);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 不执行任何操作
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 不执行任何操作
            }
        });

        musicVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                musicVolumeTextView.setText("音乐音量: " + progress + "%");
                editor.putInt(KEY_MUSIC_VOLUME, progress);
                editor.apply();
                updateMusicVolume(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 不执行任何操作
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 不执行任何操作
            }
        });

        autoSaveCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(KEY_AUTO_SAVE, isChecked);
            editor.apply();
        });

        // 返回按钮点击事件
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadSettings() {
        // 加载主题设置
        int theme = sharedPreferences.getInt(KEY_THEME, 0);
        themeSpinner.setSelection(theme);

        // 加载声音效果设置
        boolean soundEffects = sharedPreferences.getBoolean(KEY_SOUND_EFFECTS, true);
        soundEffectsCheckBox.setChecked(soundEffects);

        // 加载音乐设置
        boolean music = sharedPreferences.getBoolean(KEY_MUSIC, true);
        musicCheckBox.setChecked(music);

        // 加载音效音量设置
        int soundVolume = sharedPreferences.getInt(KEY_SOUND_VOLUME, 80);
        soundVolumeSeekBar.setProgress(soundVolume);
        soundVolumeTextView.setText("音效音量: " + soundVolume + "%");

        // 加载音乐音量设置
        int musicVolume = sharedPreferences.getInt(KEY_MUSIC_VOLUME, 50);
        musicVolumeSeekBar.setProgress(musicVolume);
        musicVolumeTextView.setText("音乐音量: " + musicVolume + "%");

        // 加载自动保存设置
        boolean autoSave = sharedPreferences.getBoolean(KEY_AUTO_SAVE, true);
        autoSaveCheckBox.setChecked(autoSave);
    }

    private void updateMusicPreview(boolean isEnabled) {
        if (isEnabled) {
            // 开始音乐预览
            try {
                if (previewMediaPlayer == null) {
                    previewMediaPlayer = MediaPlayer.create(this, R.raw.background_music);
                    previewMediaPlayer.setLooping(true);
                    int volume = sharedPreferences.getInt(KEY_MUSIC_VOLUME, 50);
                    float volumeLevel = volume / 100.0f;
                    previewMediaPlayer.setVolume(volumeLevel, volumeLevel);
                }
                if (!previewMediaPlayer.isPlaying()) {
                    previewMediaPlayer.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 停止音乐预览
            if (previewMediaPlayer != null && previewMediaPlayer.isPlaying()) {
                previewMediaPlayer.pause();
            }
        }
    }

    private void updateMusicVolume(int volume) {
        if (previewMediaPlayer != null) {
            float volumeLevel = volume / 100.0f;
            previewMediaPlayer.setVolume(volumeLevel, volumeLevel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放MediaPlayer资源
        if (previewMediaPlayer != null) {
            previewMediaPlayer.stop();
            previewMediaPlayer.release();
            previewMediaPlayer = null;
        }

        // 如果主题已更改，提示用户重启应用
        if (isThemeChanged) {
            Toast.makeText(this, "主题已更改，请重启应用以应用新主题", Toast.LENGTH_SHORT).show();
        }
    }
}