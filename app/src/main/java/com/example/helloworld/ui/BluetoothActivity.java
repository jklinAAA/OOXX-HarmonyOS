package com.example.helloworld.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.helloworld.R;
import com.example.helloworld.bluetooth.BluetoothConnection;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothConnection bluetoothConnection;
    private ListView pairedDevicesListView;
    private ListView discoveredDevicesListView;
    private Button btnDiscoverDevices;
    private Button btnCancelSearch;
    private TextView tvConnectionStatus;

    private ArrayAdapter<String> pairedDevicesAdapter;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    private ArrayList<BluetoothDevice> discoveredDevicesList;
    private boolean isDiscovering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // 初始化UI组件
        pairedDevicesListView = findViewById(R.id.paired_devices_list);
        discoveredDevicesListView = findViewById(R.id.discovered_devices_list);
        btnDiscoverDevices = findViewById(R.id.btn_discover_devices);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);

        // 初始化适配器
        pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesList = new ArrayList<>();

        pairedDevicesListView.setAdapter(pairedDevicesAdapter);
        discoveredDevicesListView.setAdapter(discoveredDevicesAdapter);

        // 获取蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 检查并请求蓝牙权限
        checkBluetoothPermissions();

        // 初始化蓝牙连接
        bluetoothConnection = new BluetoothConnection(bluetoothAdapter);
        bluetoothConnection.setConnectionListener(new BluetoothConnection.ConnectionListener() {
            @Override
            public void onConnectionStateChanged(int state) {
                updateConnectionStatus(state);
            }

            @Override
            public void onGameDataReceived(String data) {
                // 处理接收到的游戏数据
                handleGameData(data);
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(BluetoothActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });

        // 设置按钮点击事件
        btnDiscoverDevices.setOnClickListener(v -> startDiscovery());
        btnCancelSearch.setOnClickListener(v -> cancelDiscovery());

        // 设置列表项点击事件
        pairedDevicesListView.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = pairedDevicesAdapter.getItem(position);
            if (deviceInfo != null) {
                String address = deviceInfo.substring(deviceInfo.length() - 17);
                connectToDevice(address);
            }
        });

        discoveredDevicesListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < discoveredDevicesList.size()) {
                BluetoothDevice device = discoveredDevicesList.get(position);
                connectToDevice(device.getAddress());
            }
        });

        // 显示已配对设备
        showPairedDevices();
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12及以上需要精确位置权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                enableBluetooth();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-11需要位置权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                enableBluetooth();
            }
        } else {
            enableBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                enableBluetooth();
            } else {
                Toast.makeText(this, "需要蓝牙权限才能使用蓝牙对战功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startBluetoothServer();
        }
    }

    private void startBluetoothServer() {
        // 启动蓝牙服务器线程
        bluetoothConnection.startServer();
        runOnUiThread(() -> {
            tvConnectionStatus.setText("等待连接...");
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // 蓝牙已启用
                startBluetoothServer();
            } else {
                Toast.makeText(this, "需要启用蓝牙才能使用蓝牙对战功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showPairedDevices() {
        pairedDevicesAdapter.clear();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add("没有已配对的设备");
        }
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 发现新设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !device.getName().equals(bluetoothAdapter.getName())) {
                    discoveredDevicesList.add(device);
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // 搜索完成
                isDiscovering = false;
                runOnUiThread(() -> {
                    btnDiscoverDevices.setText("搜索设备");
                    btnDiscoverDevices.setEnabled(true);
                    btnCancelSearch.setVisibility(View.GONE);
                    if (discoveredDevicesAdapter.isEmpty()) {
                        discoveredDevicesAdapter.add("未发现新设备");
                    }
                });
            }
        }
    };

    private void startDiscovery() {
        if (isDiscovering) {
            return;
        }

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);

        // 开始搜索设备
        discoveredDevicesAdapter.clear();
        discoveredDevicesList.clear();
        isDiscovering = true;
        btnDiscoverDevices.setText("搜索中...");
        btnDiscoverDevices.setEnabled(false);
        btnCancelSearch.setVisibility(View.VISIBLE);
        bluetoothAdapter.startDiscovery();
    }

    private void cancelDiscovery() {
        if (isDiscovering) {
            bluetoothAdapter.cancelDiscovery();
            isDiscovering = false;
            btnDiscoverDevices.setText("搜索设备");
            btnDiscoverDevices.setEnabled(true);
            btnCancelSearch.setVisibility(View.GONE);
        }
    }

    private void connectToDevice(String address) {
        if (isDiscovering) {
            cancelDiscovery();
        }

        // 停止服务器线程
        bluetoothConnection.stopServer();

        // 连接到指定设备
        runOnUiThread(() -> {
            tvConnectionStatus.setText("正在连接...");
        });
        bluetoothConnection.connect(address);
    }

    private void updateConnectionStatus(final int state) {
        runOnUiThread(() -> {
            switch (state) {
                case BluetoothConnection.STATE_NONE:
                    tvConnectionStatus.setText("未连接");
                    break;
                case BluetoothConnection.STATE_LISTEN:
                    tvConnectionStatus.setText("等待连接...");
                    break;
                case BluetoothConnection.STATE_CONNECTING:
                    tvConnectionStatus.setText("正在连接...");
                    break;
                case BluetoothConnection.STATE_CONNECTED:
                    tvConnectionStatus.setText("已连接，准备开始游戏");
                    // 连接成功后，跳转到游戏界面并传递棋盘大小和难度
                    showBoardSizeDialog();
                    break;
            }
        });
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
        builder.setCancelable(false);
        builder.show();
    }

    private void showDifficultyDialog(int size) {
        String[] difficulties = {"简单", "困难"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("选择难度");
        builder.setItems(difficulties, (dialog, which) -> {
            startBluetoothGame(size, which);
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void startBluetoothGame(int size, int difficultyIndex) {
        // 发送游戏设置给对方
        String gameSettings = "GAME_SETTINGS:" + size + ":" + difficultyIndex;
        bluetoothConnection.write(gameSettings.getBytes());

        // 跳转到游戏界面
        Intent intent = new Intent(this, GameScreenActivity.class);
        intent.putExtra("BOARD_SIZE", size);
        intent.putExtra("DIFFICULTY", difficultyIndex == 0 ? "EASY" : "HARD");
        intent.putExtra("BLUETOOTH_MODE", true);
        intent.putExtra("BLUETOOTH_CONNECTION", bluetoothConnection);
        startActivity(intent);
    }

    private void handleGameData(String data) {
        // 处理从蓝牙接收到的游戏数据
        if (data.startsWith("GAME_SETTINGS:")) {
            // 解析游戏设置
            String[] parts = data.split(":");
            if (parts.length >= 3) {
                int size = Integer.parseInt(parts[1]);
                int difficultyIndex = Integer.parseInt(parts[2]);
                // 跳转到游戏界面
                Intent intent = new Intent(this, GameScreenActivity.class);
                intent.putExtra("BOARD_SIZE", size);
                intent.putExtra("DIFFICULTY", difficultyIndex == 0 ? "EASY" : "HARD");
                intent.putExtra("BLUETOOTH_MODE", true);
                intent.putExtra("BLUETOOTH_CONNECTION", bluetoothConnection);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止蓝牙搜索
        cancelDiscovery();
        // 注销广播接收器
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 关闭蓝牙连接
        if (bluetoothConnection != null) {
            bluetoothConnection.stop();
        }
    }
}