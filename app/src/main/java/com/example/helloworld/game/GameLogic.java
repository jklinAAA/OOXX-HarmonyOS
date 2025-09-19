package com.example.helloworld.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameLogic {
    public enum CellState {
        EMPTY, X, O
    }

    public enum Difficulty {
        EASY, HARD
    }

    private CellState[][] board;
    private final int size;
    private boolean gameCompleted;
    private long startTime;
    private long endTime;

    public GameLogic(int size) {
        this.size = size;
        this.board = new CellState[size][size];
        initializeBoard();
        this.gameCompleted = false;
    }

    private void initializeBoard() {
        for (int i = 0; i < size; i++) {
            Arrays.fill(board[i], CellState.EMPTY);
        }
    }

    // 生成新的游戏板
    public void generateNewGame(Difficulty difficulty) {
        initializeBoard();
        gameCompleted = false;
        startTime = System.currentTimeMillis();

        // 根据难度生成初始棋盘
        int filledCells = size * size / (difficulty == Difficulty.EASY ? 3 : 2);
        generateRandomValidBoard();
        removeCells(filledCells);
    }

    // 生成随机有效的棋盘
    private void generateRandomValidBoard() {
        // 简化实现：生成一个可能需要回溯的随机棋盘
        Random random = new Random();
        boolean valid = false;

        while (!valid) {
            initializeBoard();
            valid = fillBoardRandomly(0, 0);
        }
    }

    // 随机填充棋盘（回溯法）
    private boolean fillBoardRandomly(int row, int col) {
        // 检查是否已完成填充
        if (row == size) {
            return validateBoard();
        }

        // 计算下一个位置
        int nextRow = (col == size - 1) ? row + 1 : row;
        int nextCol = (col == size - 1) ? 0 : col + 1;

        // 尝试放置X或O
        CellState[] possibleStates = {CellState.X, CellState.O};
        for (CellState state : possibleStates) {
            if (isValidMove(row, col, state)) {
                board[row][col] = state;
                if (fillBoardRandomly(nextRow, nextCol)) {
                    return true;
                }
                board[row][col] = CellState.EMPTY;
            }
        }

        return false;
    }

    // 移除部分单元格以创建谜题
    private void removeCells(int cellsToKeep) {
        Random random = new Random();
        List<int[]> positions = new ArrayList<>();

        // 创建所有位置的列表
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                positions.add(new int[]{i, j});
            }
        }

        // 随机移除单元格，只保留指定数量的单元格
        while (positions.size() > cellsToKeep) {
            int index = random.nextInt(positions.size());
            int[] pos = positions.remove(index);
            board[pos[0]][pos[1]] = CellState.EMPTY;
        }
    }

    // 检查移动是否有效
    public boolean isValidMove(int row, int col, CellState state) {
        // 检查单元格是否为空
        if (board[row][col] != CellState.EMPTY) {
            return false;
        }

        // 临时设置单元格状态
        board[row][col] = state;

        // 检查行
        boolean valid = checkRow(row) && checkColumn(col);

        // 恢复单元格状态
        board[row][col] = CellState.EMPTY;

        return valid;
    }

    // 执行移动
    public boolean makeMove(int row, int col, CellState state) {
        if (isValidMove(row, col, state)) {
            board[row][col] = state;
            checkGameCompletion();
            return true;
        }
        return false;
    }

    // 检查行是否有效
    private boolean checkRow(int row) {
        // 检查是否有超过两个连续的X或O
        int consecutiveCount = 1;
        for (int i = 1; i < size; i++) {
            if (board[row][i] != CellState.EMPTY && board[row][i] == board[row][i - 1]) {
                consecutiveCount++;
                if (consecutiveCount > 2) {
                    return false;
                }
            } else {
                consecutiveCount = 1;
            }
        }
        return true;
    }

    // 检查列是否有效
    private boolean checkColumn(int col) {
        // 检查是否有超过两个连续的X或O
        int consecutiveCount = 1;
        for (int i = 1; i < size; i++) {
            if (board[i][col] != CellState.EMPTY && board[i][col] == board[i - 1][col]) {
                consecutiveCount++;
                if (consecutiveCount > 2) {
                    return false;
                }
            } else {
                consecutiveCount = 1;
            }
        }
        return true;
    }

    // 验证整个棋盘是否有效
    private boolean validateBoard() {
        // 检查每一行
        for (int i = 0; i < size; i++) {
            if (!checkCompleteRow(i)) {
                return false;
            }
        }

        // 检查每一列
        for (int i = 0; i < size; i++) {
            if (!checkCompleteColumn(i)) {
                return false;
            }
        }

        // 检查行唯一性
        if (!checkRowsUnique()) {
            return false;
        }

        // 检查列唯一性
        if (!checkColumnsUnique()) {
            return false;
        }

        return true;
    }

    // 检查完整行是否满足所有条件
    private boolean checkCompleteRow(int row) {
        int xCount = 0, oCount = 0;
        for (int j = 0; j < size; j++) {
            if (board[row][j] == CellState.X) {
                xCount++;
            } else if (board[row][j] == CellState.O) {
                oCount++;
            }
        }

        // 检查X和O的数量是否相等
        if (xCount != oCount) {
            return false;
        }

        return checkRow(row);
    }

    // 检查完整列是否满足所有条件
    private boolean checkCompleteColumn(int col) {
        int xCount = 0, oCount = 0;
        for (int j = 0; j < size; j++) {
            if (board[j][col] == CellState.X) {
                xCount++;
            } else if (board[j][col] == CellState.O) {
                oCount++;
            }
        }

        // 检查X和O的数量是否相等
        if (xCount != oCount) {
            return false;
        }

        return checkColumn(col);
    }

    // 检查所有行是否唯一
    private boolean checkRowsUnique() {
        Set<String> rowPatterns = new HashSet<>();
        for (int i = 0; i < size; i++) {
            StringBuilder rowStr = new StringBuilder();
            for (int j = 0; j < size; j++) {
                rowStr.append(board[i][j].ordinal());
            }
            String pattern = rowStr.toString();
            if (rowPatterns.contains(pattern)) {
                return false;
            }
            rowPatterns.add(pattern);
        }
        return true;
    }

    // 检查所有列是否唯一
    private boolean checkColumnsUnique() {
        Set<String> colPatterns = new HashSet<>();
        for (int i = 0; i < size; i++) {
            StringBuilder colStr = new StringBuilder();
            for (int j = 0; j < size; j++) {
                colStr.append(board[j][i].ordinal());
            }
            String pattern = colStr.toString();
            if (colPatterns.contains(pattern)) {
                return false;
            }
            colPatterns.add(pattern);
        }
        return true;
    }

    // 检查游戏是否完成
    private void checkGameCompletion() {
        // 检查是否所有单元格都已填充
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == CellState.EMPTY) {
                    return;
                }
            }
        }

        // 检查棋盘是否有效
        if (validateBoard()) {
            gameCompleted = true;
            endTime = System.currentTimeMillis();
        }
    }

    // 自动解题功能
    public boolean solveAutomatically() {
        // 保存当前棋盘状态
        CellState[][] originalBoard = new CellState[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(board[i], 0, originalBoard[i], 0, size);
        }

        boolean solved = solveBacktracking(0, 0);

        if (solved) {
            gameCompleted = true;
            endTime = System.currentTimeMillis();
        } else {
            // 恢复原始棋盘状态
            board = originalBoard;
        }

        return solved;
    }

    // 回溯法求解
    private boolean solveBacktracking(int row, int col) {
        // 检查是否已完成填充
        if (row == size) {
            return validateBoard();
        }

        // 计算下一个位置
        int nextRow = (col == size - 1) ? row + 1 : row;
        int nextCol = (col == size - 1) ? 0 : col + 1;

        // 如果当前位置已经填充，直接处理下一个位置
        if (board[row][col] != CellState.EMPTY) {
            return solveBacktracking(nextRow, nextCol);
        }

        // 尝试放置X或O
        for (CellState state : new CellState[]{CellState.X, CellState.O}) {
            if (isValidMove(row, col, state)) {
                board[row][col] = state;
                if (solveBacktracking(nextRow, nextCol)) {
                    return true;
                }
                board[row][col] = CellState.EMPTY;
            }
        }

        return false;
    }

    // 获取提示
    public int[] getHint() {
        // 随机寻找一个空单元格并尝试放置
        List<int[]> emptyCells = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == CellState.EMPTY) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        if (emptyCells.isEmpty()) {
            return null; // 没有空单元格
        }

        Random random = new Random();
        while (!emptyCells.isEmpty()) {
            int index = random.nextInt(emptyCells.size());
            int[] cell = emptyCells.remove(index);
            int row = cell[0];
            int col = cell[1];

            // 尝试放置X或O
            for (CellState state : new CellState[]{CellState.X, CellState.O}) {
                if (isValidMove(row, col, state)) {
                    // 创建临时副本进行尝试
                    CellState[][] tempBoard = new CellState[size][size];
                    for (int i = 0; i < size; i++) {
                        System.arraycopy(board[i], 0, tempBoard[i], 0, size);
                    }

                    tempBoard[row][col] = state;
                    GameLogic tempLogic = new GameLogic(size);
                    tempLogic.board = tempBoard;

                    // 检查是否有解
                    if (tempLogic.solveAutomatically()) {
                        return new int[]{row, col, state.ordinal()};
                    }
                }
            }
        }

        return null; // 没有找到有效提示
    }

    // 获取游戏结果
    public boolean isGameCompleted() {
        return gameCompleted;
    }

    // 获取游戏用时（毫秒）
    public long getGameTime() {
        if (!gameCompleted) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    // 获取棋盘状态
    public CellState[][] getBoard() {
        CellState[][] copy = new CellState[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, size);
        }
        return copy;
    }

    // 设置棋盘状态（用于蓝牙同步）
    public void setBoard(CellState[][] newBoard) {
        if (newBoard.length == size && newBoard[0].length == size) {
            this.board = newBoard;
            checkGameCompletion();
        }
    }

    // 获取棋盘大小
    public int getSize() {
        return size;
    }

    // 重置游戏
    public void resetGame() {
        initializeBoard();
        gameCompleted = false;
        startTime = System.currentTimeMillis();
    }
}