package com.example.helloworld.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.example.helloworld.game.GameLogic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;BluetoothConnection
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnection {
    private static final String TAG = "BluetoothConnection";
    private static final String APP_NAME = "OOXXGame";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;

    public static final int STATE_NONE = 0;       // 初始状态
    public static final int STATE_LISTEN = 1;     // 监听连接
    public static final int STATE_CONNECTING = 2; // 正在连接
    public static final int STATE_CONNECTED = 3;  // 已连接

    private final BluetoothConnectionListener listener;

    public interface BluetoothConnectionListener {
        void onDeviceDiscovered(BluetoothDevice device);
        void onConnectionStateChanged(int state);
        void onMessageReceived(String message);
        void onError(String error);
    }

    public BluetoothConnection(Context context, BluetoothConnectionListener listener) {
        this.context = context;
        this.listener = listener;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
    }

    // 同步状态变化
    @SuppressLint("MissingPermission")
    private synchronized void setState(int state) {
        this.state = state;
        if (listener != null) {
            listener.onConnectionStateChanged(state);
        }
    }

    // 获取当前状态
    public synchronized int getState() {
        return state;
    }

    // 开始服务（作为服务器）
    public synchronized void start() {
        Log.d(TAG, "start");

        // 停止任何正在运行的线程
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // 启动AcceptThread以监听连接请求  多线程
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    // 连接到特定设备
    @SuppressLint("MissingPermission")
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // 停止任何正在运行的线程
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // 启动ConnectThread尝试连接到设备
        connectThread = new ConnectThread(device);
        connectThread.start();

        setState(STATE_CONNECTING);
    }

    // 连接建立后开始管理通信
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        // 停止ConnectThread
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // 停止AcceptThread
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // 启动ConnectedThread来管理连接
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        setState(STATE_CONNECTED);
    }

    // 停止所有线程
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    // 发送消息
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        r.write(out);
    }

    // 发送游戏状态
    public void sendGameState(GameLogic gameLogic) {
        // 将游戏状态转换为可传输的格式
        StringBuilder message = new StringBuilder();
        message.append("GAME_STATE:");
        message.append(gameLogic.getSize());
        message.append(",");
        
        GameLogic.CellState[][] board = gameLogic.getBoard();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                message.append(board[i][j].ordinal());
                if (i < board.length - 1 || j < board[i].length - 1) {
                    message.append(",");
                }
            }
        }
        
        message.append(":").append(gameLogic.isGameCompleted() ? 1 : 0);
        message.append(":").append(gameLogic.getGameTime());
        
        write(message.toString().getBytes());
    }

    // 发送移动
    public void sendMove(int row, int col, GameLogic.CellState state) {
        String message = "MOVE:" + row + "," + col + "," + state.ordinal();
        write(message.toString().getBytes());
    }

    // 发送游戏结果
    public void sendGameResult(boolean isWinner) {
        String message = "GAME_RESULT:" + (isWinner ? 1 : 0);
        write(message.toString().getBytes());
    }

    // 发送退出通知
    public void sendQuitNotification() {
        String message = "QUIT_GAME";
        write(message.toString().getBytes());
    }

    // 发现已配对的设备
    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            devices.addAll(pairedDevices);
        }
        return devices;
    }

    // 开始发现设备
    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
        }
    }

    // 停止发现设备
    @SuppressLint("MissingPermission")
    public void stopDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    //  AcceptThread类用于监听传入的连接请求
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
                if (listener != null) {
                    listener.onError("监听连接失败: " + e.getMessage());
                }
            }
            serverSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread");
            setName("AcceptThread");

            BluetoothSocket socket = null;

            while (state != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    if (listener != null) {
                        listener.onError("接受连接失败: " + e.getMessage());
                    }
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothConnection.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    // ConnectThread类用于连接到远程设备
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
                if (listener != null) {
                    listener.onError("创建连接失败: " + e.getMessage());
                }
            }
            socket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // 取消发现，因为这会减慢连接速度
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                setState(STATE_LISTEN);
                if (listener != null) {
                    listener.onError("连接设备失败: " + e.getMessage());
                }
                return;
            }

            // 重置ConnectThread，因为我们已经完成了连接
            synchronized (BluetoothConnection.this) {
                connectThread = null;
            }

            // 启动连接管理
            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    // ConnectedThread类用于管理已建立的连接
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
                if (listener != null) {
                    listener.onError("创建IO流失败: " + e.getMessage());
                }
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String message = new String(buffer, 0, bytes);
                    Log.d(TAG, "收到消息: " + message);
                    if (listener != null) {
                        listener.onMessageReceived(message);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    if (listener != null) {
                        listener.onError("连接断开: " + e.getMessage());
                    }
                    setState(STATE_LISTEN);
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                if (listener != null) {
                    listener.onError("发送消息失败: " + e.getMessage());
                }
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}