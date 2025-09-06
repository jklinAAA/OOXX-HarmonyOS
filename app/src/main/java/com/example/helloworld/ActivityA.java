package com.example.helloworld;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class ActivityA extends AppCompatActivity {
    private MyService.MyBinder myBinder;
    private boolean isBound = false;
    private TextView tvServiceMsg;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (MyService.MyBinder) service;
            isBound = true;
            Toast.makeText(ActivityA.this, "Service已连接", Toast.LENGTH_SHORT).show();
            // 连接后立即获取Service的消息并显示
            String message = myBinder.getMessageFromService();
            tvServiceMsg.setText("Service返回: " + message);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            tvServiceMsg.setText("Service 断开连接");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity);

        // 标题和Service消息显示区
        tvServiceMsg = findViewById(R.id.tv_service_msg);
        tvServiceMsg.setText("Service 消息显示区");

        Button btnToB = findViewById(R.id.btn_to_b);
        btnToB.setOnClickListener(v -> {
            Intent intent = new Intent(ActivityA.this, ActivityB.class);
            intent.putExtra("message", "Hello from ActivityA");
            startActivity(intent);
        });

        Button btnBindService = findViewById(R.id.btn_bind_service);
        btnBindService.setOnClickListener(v -> {
            if (isBound) {
                Toast.makeText(ActivityA.this, "Service 已绑定，无需重复绑定", Toast.LENGTH_SHORT).show();
                tvServiceMsg.setText("Service 已绑定");
            } else {
                tvServiceMsg.setText("正在绑定 Service...");
                Intent intent = new Intent(ActivityA.this, MyService.class);
                bindService(intent, connection, BIND_AUTO_CREATE);
            }
        });

        Button btnUnbindService = findViewById(R.id.btn_unbind_service);
        btnUnbindService.setOnClickListener(v -> {
            if (isBound) {
                unbindService(connection);
                isBound = false;
                Toast.makeText(ActivityA.this, "Service Unbound", Toast.LENGTH_SHORT).show();
                tvServiceMsg.setText("Service 已解绑");
            } else {
                Toast.makeText(ActivityA.this, "Service 未绑定", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnSendToService = findViewById(R.id.btn_send_to_service);
        btnSendToService.setOnClickListener(v -> {
            if (isBound && myBinder != null) {
                myBinder.setMessageFromActivity("Hello from ActivityA to Service");
                Toast.makeText(ActivityA.this, "消息已发送到Service", Toast.LENGTH_SHORT).show();
                // 发送后立即获取Service的消息并显示
                String msg = myBinder.getMessageFromService();
                tvServiceMsg.setText("Service返回: " + msg);
            } else {
                Toast.makeText(ActivityA.this, "请先绑定Service", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}