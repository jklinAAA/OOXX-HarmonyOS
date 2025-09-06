package com.example.helloworld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin, btnClear;
    private TextView tvLastLoginTime;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LAST_LOGIN = "last_login_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化控件
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnClear = findViewById(R.id.btn_clear);
        tvLastLoginTime = findViewById(R.id.tv_last_login_time);

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 恢复保存的用户名和密码
        String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
        String lastLoginTime = sharedPreferences.getString(KEY_LAST_LOGIN, "首次登录");

        etUsername.setText(savedUsername);
        etPassword.setText(savedPassword);
        tvLastLoginTime.setText("上次登录时间: " + lastLoginTime);

        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存用户名和密码到SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_PASSWORD, password);
            editor.putString(KEY_LAST_LOGIN, java.time.LocalDateTime.now().toString());
            editor.apply();

            Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show();

            // 跳转到主界面
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // 清除按钮点击事件
        btnClear.setOnClickListener(v -> {
            etUsername.setText("");
            etPassword.setText("");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            Toast.makeText(this, "已清除保存的登录信息", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前输入框状态
        outState.putString("current_username", etUsername.getText().toString());
        outState.putString("current_password", etPassword.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 恢复输入框状态
        if (savedInstanceState != null) {
            String savedUsername = savedInstanceState.getString("current_username", "");
            String savedPassword = savedInstanceState.getString("current_password", "");
            etUsername.setText(savedUsername);
            etPassword.setText(savedPassword);
        }
    }
} 