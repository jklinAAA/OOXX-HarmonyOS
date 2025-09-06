package com.example.helloworld;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class MyService extends Service {
    private String message = "Initial message from Service";

    public class MyBinder extends Binder {
        public String getMessageFromService() {
            return message;
        }

        public void setMessageFromActivity(String newMessage) {
            message = newMessage;
            Toast.makeText(MyService.this, "Received from Activity: " + newMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private final MyBinder binder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "Service Bound", Toast.LENGTH_SHORT).show();
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Service Created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT).show();
    }
}
