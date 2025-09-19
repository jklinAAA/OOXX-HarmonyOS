package com.example.helloworld.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.helloworld.R;
import com.example.helloworld.bluetooth.BluetoothConnection;
import com.example.helloworld.game.GameLogic;

import java.util.ArrayList;
import java.util.List;

public class GameScreenActivity extends AppCompatActivity implements BluetoothConnection.BluetoothConnectionListener {
    private GameLogic gameLogic;
    private BluetoothConnection bluetoothConnection;
    private LinearLayout gameGridLayout;
    private Button[][] cellButtons;
    private TextView timerTextView;
    private TextView statusTextView;
    private Button btnHint;
    private Button btnSolve;
    private Button btnUndo;
    private Button btnRedo;
    private Button btnQuit;

    private boolean isBluetoothMode = false;
    private boolean isHost = false;
    private long startTime;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private List<Move> moveHistory = new ArrayList<>();
    private List<Move> redoHistory = new ArrayList<>();

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_screen);

        // 初始化UI组件
        gameGridLayout = findViewById(R.id.game_grid_layout);
        timerTextView = findViewById(R.id.timer_text_view);
        statusTextView = findViewById(R.id.status_text_view);
        btnHint = findViewById(R.id.btn_hint);
        btnSolve = findViewById(R.id.btn_solve);
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnQuit = findViewById(R.id.btn_quit);

        // 获取游戏参数
        Intent intent = getIntent();
        int size = intent.getIntExtra("BOARD_SIZE", 6);
        GameLogic.Difficulty difficulty = GameLogic.Difficulty.valueOf(intent.getStringExtra("DIFFICULTY"));
        isBluetoothMode = intent.getBooleanExtra("BLUETOOTH_MODE", false);
        isHost = intent.getBooleanExtra("IS_HOST", false);

        // 初始化游戏逻辑
        gameLogic = new GameLogic(size);
        gameLogic.generateNewGame(difficulty);

        // 初始化蓝牙连接（如果是蓝牙模式）
        if (isBluetoothMode) {
            initializeBluetooth();
            statusTextView.setText("等待连接...");
        } else {
            statusTextView.setText("单人模式");
        }

        // 创建游戏棋盘
        createGameGrid(size);

        // 开始计时器
        startTime = System.currentTimeMillis();
        startTimer();

        // 设置按钮点击事件
        btnHint.setOnClickListener(v -> showHint());
        btnSolve.setOnClickListener(v -> solveAutomatically());
        btnUndo.setOnClickListener(v -> undoMove());
        btnRedo.setOnClickListener(v -> redoMove());
        btnQuit.setOnClickListener(v -> quitGame());
    }

    private void initializeBluetooth() {
        bluetoothConnection = new BluetoothConnection(this, this);

        // 检查蓝牙是否可用
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 检查蓝牙是否已启用
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            if (isHost) {
                startBluetoothServer();
            }
        }
    }

    private void startBluetoothServer() {
        bluetoothConnection.start();
        statusTextView.setText("等待对手连接...");
    }

    private void createGameGrid(int size) {
        // 设置网格布局参数
        gameGridLayout.setOrientation(LinearLayout.VERTICAL);
        gameGridLayout.removeAllViews();

        cellButtons = new Button[size][size];

        for (int i = 0; i < size; i++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1
            ));

            for (int j = 0; j < size; j++) {
                Button cellButton = new Button(this);
                final int row = i;
                final int col = j;

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );
                params.setMargins(2, 2, 2, 2);
                cellButton.setLayoutParams(params);
                cellButton.setTextSize(24);
                cellButton.setAllCaps(false);

                // 设置按钮点击事件
                cellButton.setOnClickListener(v -> {
                    if (!isBluetoothMode || (isBluetoothMode && !gameLogic.isGameCompleted())) {
                        makeMove(row, col);
                    }
                });

                cellButtons[i][j] = cellButton;
                rowLayout.addView(cellButton);
            }

            gameGridLayout.addView(rowLayout);
        }

        updateGameGrid();
    }

    private void makeMove(int row, int col) {
        // 如果单元格已填充，则切换状态
        GameLogic.CellState currentState = gameLogic.getBoard()[row][col];
        GameLogic.CellState nextState;

        if (currentState == GameLogic.CellState.EMPTY) {
            nextState = GameLogic.CellState.X;
        } else if (currentState == GameLogic.CellState.X) {
            nextState = GameLogic.CellState.O;
        } else {
            nextState = GameLogic.CellState.EMPTY;
        }

        // 记录移动前的状态用于撤销
        moveHistory.add(new Move(row, col, currentState));
        redoHistory.clear(); // 清除重做历史

        // 执行移动
        if (gameLogic.makeMove(row, col, nextState)) {
            updateGameGrid();
            updateButtonStates();

            // 如果是蓝牙模式，发送移动
            if (isBluetoothMode && bluetoothConnection.getState() == BluetoothConnection.STATE_CONNECTED) {
                bluetoothConnection.sendMove(row, col, nextState);
            }

            // 检查游戏是否完成
            checkGameCompletion();
        }
    }

    private void undoMove() {
        if (!moveHistory.isEmpty()) {
            Move lastMove = moveHistory.removeLast();
            redoHistory.add(new Move(lastMove.row, lastMove.col, gameLogic.getBoard()[lastMove.row][lastMove.col]));
            gameLogic.makeMove(lastMove.row, lastMove.col, lastMove.state);
            updateGameGrid();
            updateButtonStates();
        }
    }

    private void redoMove() {
        if (!redoHistory.isEmpty()) {
            Move nextMove = redoHistory.removeLast();
            moveHistory.add(new Move(nextMove.row, nextMove.col, gameLogic.getBoard()[nextMove.row][nextMove.col]));
            gameLogic.makeMove(nextMove.row, nextMove.col, nextMove.state);
            updateGameGrid();
            updateButtonStates();
        }
    }

    private void showHint() {
        int[] hint = gameLogic.getHint();
        if (hint != null) {
            int row = hint[0];
            int col = hint[1];
            GameLogic.CellState state = GameLogic.CellState.values()[hint[2]];

            // 闪烁提示单元格
            cellButtons[row][col].setBackgroundColor(getResources().getColor(R.color.hint_color));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                updateGameGrid();
            }, 1000);

            Toast.makeText(this, "提示: 在位置(" + (row + 1) + "," + (col + 1) + ")放置" + 
                    (state == GameLogic.CellState.X ? "X" : "O"), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "没有可用提示", Toast.LENGTH_SHORT).show();
        }
    }

    private void solveAutomatically() {
        new AlertDialog.Builder(this)
                .setTitle("自动解题")
                .setMessage("确定要使用自动解题功能吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    if (gameLogic.solveAutomatically()) {
                        updateGameGrid();
                        checkGameCompletion();
                    } else {
                        Toast.makeText(this, "无法自动解决此谜题", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateGameGrid() {
        GameLogic.CellState[][] board = gameLogic.getBoard();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                switch (board[i][j]) {
                    case X:
                        cellButtons[i][j].setText("X");
                        cellButtons[i][j].setBackgroundResource(R.drawable.cell_x_background);
                        break;
                    case O:
                        cellButtons[i][j].setText("O");
                        cellButtons[i][j].setBackgroundResource(R.drawable.cell_o_background);
                        break;
                    default:
                        cellButtons[i][j].setText("");
                        cellButtons[i][j].setBackgroundResource(R.drawable.cell_empty_background);
                        break;
                }
            }
        }
    }

    private void updateButtonStates() {
        btnUndo.setEnabled(!moveHistory.isEmpty());
        btnRedo.setEnabled(!redoHistory.isEmpty());
    }

    private void checkGameCompletion() {
        if (gameLogic.isGameCompleted()) {
            timerHandler.removeCallbacksAndMessages(null);
            long gameTime = gameLogic.getGameTime() / 1000;
            int minutes = (int) (gameTime / 60);
            int seconds = (int) (gameTime % 60);
            String timeString = String.format("%02d:%02d", minutes, seconds);

            String message = "恭喜你完成了谜题！用时: " + timeString;

            if (isBluetoothMode && bluetoothConnection.getState() == BluetoothConnection.STATE_CONNECTED) {
                bluetoothConnection.sendGameResult(true);
                message = "你赢了！用时: " + timeString;
            }

            new AlertDialog.Builder(this)
                    .setTitle("游戏完成")
                    .setMessage(message)
                    .setPositiveButton("返回主菜单", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!gameLogic.isGameCompleted()) {
                    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    int minutes = (int) (elapsedTime / 60);
                    int seconds = (int) (elapsedTime % 60);
                    timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 0);
    }

    private void quitGame() {
        new AlertDialog.Builder(this)
                .setTitle("退出游戏")
                .setMessage("确定要退出游戏吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    if (isBluetoothMode && bluetoothConnection.getState() == BluetoothConnection.STATE_CONNECTED) {
                        bluetoothConnection.sendQuitNotification();
                    }
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                if (isHost) {
                    startBluetoothServer();
                }
            } else {
                Toast.makeText(this, "蓝牙未启用，无法进入蓝牙对战模式", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (isBluetoothMode && bluetoothConnection != null) {
            bluetoothConnection.stop();
        }
    }

    @Override
    public void onDeviceDiscovered(android.bluetooth.BluetoothDevice device) {
        // 可以在这里处理设备发现事件
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onConnectionStateChanged(int state) {
        runOnUiThread(() -> {
            switch (state) {
                case BluetoothConnection.STATE_LISTEN:
                    statusTextView.setText("等待连接...");
                    break;
                case BluetoothConnection.STATE_CONNECTING:
                    statusTextView.setText("正在连接...");
                    break;
                case BluetoothConnection.STATE_CONNECTED:
                    statusTextView.setText("已连接，开始游戏！");
                    // 如果是主机，发送初始游戏状态
                    if (isHost) {
                        bluetoothConnection.sendGameState(gameLogic);
                    }
                    break;
                default:
                    statusTextView.setText("未连接");
                    break;
            }
        });
    }

    @Override
    public void onMessageReceived(String message) {
        runOnUiThread(() -> {
            if (message.startsWith("MOVE:")) {
                // 处理移动消息
                String[] parts = message.substring(5).split(",");
                if (parts.length == 3) {
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    GameLogic.CellState state = GameLogic.CellState.values()[Integer.parseInt(parts[2])];
                    gameLogic.makeMove(row, col, state);
                    updateGameGrid();
                    checkGameCompletion();
                }
            } else if (message.startsWith("GAME_STATE:")) {
                // 处理游戏状态消息（非主机接收初始状态）
                if (!isHost) {
                    String[] parts = message.substring(11).split(":");
                    if (parts.length >= 2) {
                        String[] boardData = parts[0].split(",");
                        int size = Integer.parseInt(boardData[0]);
                        
                        // 重建游戏逻辑和棋盘
                        gameLogic = new GameLogic(size);
                        GameLogic.CellState[][] newBoard = new GameLogic.CellState[size][size];
                        
                        for (int i = 0; i < size; i++) {
                            for (int j = 0; j < size; j++) {
                                int index = 1 + i * size + j;
                                if (index < boardData.length) {
                                    newBoard[i][j] = GameLogic.CellState.values()[Integer.parseInt(boardData[index])];
                                }
                            }
                        }
                        
                        gameLogic.setBoard(newBoard);
                        createGameGrid(size);
                        startTime = System.currentTimeMillis() - Long.parseLong(parts[2]);
                        startTimer();
                    }
                }
            } else if (message.startsWith("GAME_RESULT:")) {
                // 处理游戏结果消息
                boolean isWinner = message.substring(12).equals("1");
                if (!isWinner) {
                    timerHandler.removeCallbacksAndMessages(null);
                    new AlertDialog.Builder(this)
                            .setTitle("游戏结束")
                            .setMessage("很遗憾，你输了！")
                            .setPositiveButton("返回主菜单", (dialog, which) -> {
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                }
            } else if (message.equals("QUIT_GAME")) {
                // 处理退出通知
                timerHandler.removeCallbacksAndMessages(null);
                new AlertDialog.Builder(this)
                        .setTitle("游戏结束")
                        .setMessage("对手已退出，你获胜了！")
                        .setPositiveButton("返回主菜单", (dialog, which) -> {
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    // 移动历史记录类
    private static class Move {
        int row;
        int col;
        GameLogic.CellState state;

        Move(int row, int col, GameLogic.CellState state) {
            this.row = row;
            this.col = col;
            this.state = state;
        }
    }
}