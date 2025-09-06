package com.example.helloworld;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityB extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_b);

        // 接收从ActivityA传递过来的数据
        Intent intent = getIntent();
        String message = intent.getStringExtra("message");

        TextView tvMessage = findViewById(R.id.tv_message);
        tvMessage.setText(message);

        Toast.makeText(this, "Received: " + message, Toast.LENGTH_SHORT).show();

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }
}