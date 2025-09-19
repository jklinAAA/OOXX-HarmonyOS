package com.example.helloworld.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.helloworld.R;
import com.example.helloworld.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LeaderboardScreenActivity extends AppCompatActivity {
    private ListView leaderboardListView;
    private Spinner difficultySpinner;
    private Spinner boardSizeSpinner;
    private TextView noRecordsTextView;

    private LeaderboardAdapter adapter;
    private List<LeaderboardRecord> leaderboardRecords;
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // 初始化UI组件
        leaderboardListView = findViewById(R.id.leaderboard_list);
        difficultySpinner = findViewById(R.id.difficulty_spinner);
        boardSizeSpinner = findViewById(R.id.board_size_spinner);
        noRecordsTextView = findViewById(R.id.no_records_textview);

        // 初始化数据库帮助类
        databaseHelper = new DatabaseHelper(this);
        database = databaseHelper.getReadableDatabase();

        // 初始化排行榜记录列表
        leaderboardRecords = new ArrayList<>();

        // 初始化适配器
        adapter = new LeaderboardAdapter(this, leaderboardRecords);
        leaderboardListView.setAdapter(adapter);

        // 设置难度和棋盘大小选择器的监听器
        difficultySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                loadLeaderboardData();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 不执行任何操作
            }
        });

        boardSizeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                loadLeaderboardData();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 不执行任何操作
            }
        });

        // 加载排行榜数据
        loadLeaderboardData();

        // 返回按钮点击事件
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadLeaderboardData() {
        // 清空当前记录列表
        leaderboardRecords.clear();

        // 获取选中的难度和棋盘大小
        String difficulty = difficultySpinner.getSelectedItem().toString();
        String boardSize = boardSizeSpinner.getSelectedItem().toString().replace("x", "");

        // 构建查询语句
        String selection = DatabaseHelper.COLUMN_DIFFICULTY + " = ? AND " + DatabaseHelper.COLUMN_BOARD_SIZE + " = ?";
        String[] selectionArgs = {difficulty, boardSize};
        String orderBy = DatabaseHelper.COLUMN_TIME_TAKEN + " ASC";

        // 执行查询
        Cursor cursor = database.query(
                DatabaseHelper.TABLE_LEADERBOARD,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy,
                "10" // 只显示前10名
        );

        // 处理查询结果
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String playerName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PLAYER_NAME));
                int boardSizeValue = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_BOARD_SIZE));
                String difficultyValue = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DIFFICULTY));
                long timeTaken = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME_TAKEN));
                String timestamp = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP));

                LeaderboardRecord record = new LeaderboardRecord(
                        playerName,
                        boardSizeValue,
                        difficultyValue,
                        timeTaken,
                        timestamp
                );
                leaderboardRecords.add(record);
            } while (cursor.moveToNext());
            cursor.close();
        }

        // 更新UI
        adapter.notifyDataSetChanged();

        // 如果没有记录，显示提示信息
        if (leaderboardRecords.isEmpty()) {
            leaderboardListView.setVisibility(View.GONE);
            noRecordsTextView.setVisibility(View.VISIBLE);
        } else {
            leaderboardListView.setVisibility(View.VISIBLE);
            noRecordsTextView.setVisibility(View.GONE);
        }
    }

    private static class LeaderboardRecord {
        private String playerName;
        private int boardSize;
        private String difficulty;
        private long timeTaken;
        private String timestamp;

        public LeaderboardRecord(String playerName, int boardSize, String difficulty, long timeTaken, String timestamp) {
            this.playerName = playerName;
            this.boardSize = boardSize;
            this.difficulty = difficulty;
            this.timeTaken = timeTaken;
            this.timestamp = timestamp;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getBoardSize() {
            return boardSize;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public long getTimeTaken() {
            return timeTaken;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getFormattedTime() {
            long minutes = timeTaken / 60;
            long seconds = timeTaken % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private static class LeaderboardAdapter extends BaseAdapter {
        private Context context;
        private List<LeaderboardRecord> records;
        private LayoutInflater inflater;

        public LeaderboardAdapter(Context context, List<LeaderboardRecord> records) {
            this.context = context;
            this.records = records;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return records.size();
        }

        @Override
        public Object getItem(int position) {
            return records.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.leaderboard_item, parent, false);
                holder = new ViewHolder();
                holder.rankTextView = convertView.findViewById(R.id.rank_textview);
                holder.playerNameTextView = convertView.findViewById(R.id.player_name_textview);
                holder.timeTextView = convertView.findViewById(R.id.time_textview);
                holder.dateTextView = convertView.findViewById(R.id.date_textview);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            LeaderboardRecord record = records.get(position);
            holder.rankTextView.setText(String.valueOf(position + 1));
            holder.playerNameTextView.setText(record.getPlayerName());
            holder.timeTextView.setText(record.getFormattedTime());
            holder.dateTextView.setText(record.getTimestamp());

            // 设置前三名的特殊样式
            if (position == 0) {
                // 第一名
                holder.rankTextView.setTextColor(context.getResources().getColor(R.color.gold));
                holder.rankTextView.setTextSize(20);
            } else if (position == 1) {
                // 第二名
                holder.rankTextView.setTextColor(context.getResources().getColor(R.color.silver));
                holder.rankTextView.setTextSize(18);
            } else if (position == 2) {
                // 第三名
                holder.rankTextView.setTextColor(context.getResources().getColor(R.color.bronze));
                holder.rankTextView.setTextSize(18);
            } else {
                // 其他名次
                holder.rankTextView.setTextColor(context.getResources().getColor(android.R.color.black));
                holder.rankTextView.setTextSize(16);
            }

            return convertView;
        }

        private static class ViewHolder {
            TextView rankTextView;
            TextView playerNameTextView;
            TextView timeTextView;
            TextView dateTextView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭数据库连接
        if (database != null && database.isOpen()) {
            database.close();
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}