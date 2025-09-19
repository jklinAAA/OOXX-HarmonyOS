export enum CellState {
  EMPTY = 0,
  X = 1,
  O = 2,
  FIXED_X = 3,
  FIXED_O = 4
}

export enum GameMode {
  SINGLE_PLAYER = 'single',
  MULTIPLAYER = 'multiplayer'
}

export enum GameDifficulty {
  EASY = 'easy',
  MEDIUM = 'medium',
  HARD = 'hard'
}

export enum GameStatus {
  NOT_STARTED = 'not_started',
  IN_PROGRESS = 'in_progress',
  PAUSED = 'paused',
  COMPLETED = 'completed',
  DISCONNECTED = 'disconnected'
}

export interface GameState {
  board: CellState[][];
  boardSize: number;
  gameStatus: GameStatus;
  winner?: 'player1' | 'player2' | 'draw';
  playerNumber: 1 | 2;
  timestamp: number;
  gameMode: GameMode;
  timeElapsed: number;
  player1Time: number;
  player2Time: number;
  currentPlayer?: 'player1' | 'player2';
}

export class GameModel {
  boardSize: number = 4;
  board: CellState[][] = [];
  history: CellState[][][] = [];
  initialFixedCells: CellState[][] = [];
  gameMode: GameMode = GameMode.SINGLE_PLAYER;
  gameStatus: GameStatus = GameStatus.NOT_STARTED;
  playerNumber: 1 | 2 = 1;
  opponentBoard: CellState[][] = [];
  winner?: 'player1' | 'player2' | 'draw';
  startTime: number = 0;
  timeElapsed: number = 0;
  player1Time: number = 0;
  player2Time: number = 0;
  lastMoveTime: number = 0;
  private timerId: number = 0;
  private difficulty: GameDifficulty = GameDifficulty.MEDIUM;
  currentPlayer?: 'player1' | 'player2';

  constructor(size: number = 4, mode: GameMode = GameMode.SINGLE_PLAYER, difficulty: GameDifficulty = GameDifficulty.MEDIUM) {
    this.boardSize = size;
    this.gameMode = mode;
    this.difficulty = difficulty;

    this.initializeGame();
  }

  private createEmptyBoard(size: number): CellState[][] {
    return Array(size).fill(0).map(() => Array(size).fill(CellState.EMPTY));
  }

  private copyBoard(board: CellState[][]): CellState[][] {
    return board.map((row: CellState[]) => [...row]);
  }

  private createInitialBoard(size: number): CellState[][] {
    const board: CellState[][] = this.createEmptyBoard(size);
    const fixedCount: number = this.getFixedCount(size);

    // 生成可解的初始盘面
    let attempts = 0;
    const maxAttempts = 200;

    while (attempts < maxAttempts) {
      // 清空棋盘
      for (let i = 0; i < size; i++) {
        for (let j = 0; j < size; j++) {
          board[i][j] = CellState.EMPTY;
        }
      }

      // 优化的固定符号放置策略
      let placedCount = 0;
      
      // 策略1: 先在对角线上放置一些固定符号，建立基本结构
      this.placeDiagonalFixedSymbols(board, size, fixedCount, this.difficulty);
      placedCount = this.countFixedSymbols(board, size);
      
      // 策略2: 使用启发式方法放置剩余的固定符号
      if (placedCount < fixedCount) {
        placedCount = this.placeRemainingFixedSymbols(board, size, fixedCount, placedCount);
      }

      // 额外检查：确保没有行或列出现三个相同符号
      if (this.hasAnyThreeInARow(board, size)) {
        attempts++;
        continue;
      }

      // 额外检查：确保每行和每列中x和o的数量不超过一半
      if (this.hasExcessiveSymbolsInRowOrColumn(board, size)) {
        attempts++;
        continue;
      }

      // 检查盘面是否可解（优化版）
      if (this.isSolvable(board)) {
        return board;
      }

      attempts++;
    }

    // 如果多次尝试都失败，返回一个简单的可解盘面
    return this.createFallbackBoard(size);
  }

  // 在对角线上放置固定符号
  private placeDiagonalFixedSymbols(board: CellState[][], size: number, fixedCount: number, difficulty: GameDifficulty): void {
    const diagonalCount = Math.min(Math.floor(size * 0.6), Math.floor(fixedCount * 0.5));
    
    // 主对角线
    for (let i = 0; i < diagonalCount; i++) {
      if (i >= size) break;
      const symbol = i % 2 === 0 ? CellState.FIXED_X : CellState.FIXED_O;
      board[i][i] = symbol;
    }
    
    // 副对角线
    for (let i = 0; i < diagonalCount / 2; i++) {
      if (i >= size) break;
      const symbol = i % 2 === 0 ? CellState.FIXED_O : CellState.FIXED_X;
      board[i][size - 1 - i] = symbol;
    }
  }

  // 计数固定符号数量
  private countFixedSymbols(board: CellState[][], size: number): number {
    let count = 0;
    for (let i = 0; i < size; i++) {
      for (let j = 0; j < size; j++) {
        if (board[i][j] === CellState.FIXED_X || board[i][j] === CellState.FIXED_O) {
          count++;
        }
      }
    }
    return count;
  }

  // 启发式放置剩余的固定符号
  private placeRemainingFixedSymbols(board: CellState[][], size: number, fixedCount: number, currentCount: number): number {
    const availablePositions: [number, number][] = [];
    
    // 找出所有可用位置
    for (let i = 0; i < size; i++) {
      for (let j = 0; j < size; j++) {
        if (board[i][j] === CellState.EMPTY) {
          availablePositions.push([i, j]);
        }
      }
    }
    
    // 随机打乱位置顺序
    for (let i = availablePositions.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      const temp = availablePositions[i];
      availablePositions[i] = availablePositions[j];
      availablePositions[j] = temp;
    }
    
    // 按位置优先级放置符号
    for (let k = 0; k < availablePositions.length && currentCount < fixedCount; k++) {
      const position = availablePositions[k];
      const row = position[0];
      const col = position[1];
      
      // 尝试两种符号
      const symbols = [CellState.FIXED_X, CellState.FIXED_O];
      for (const symbol of symbols) {
        // 尝试放置符号
        board[row][col] = symbol;
        
        // 检查是否违反规则
        if (!this.hasThreeInARowAtPosition(board, row, col) && 
            !this.hasExcessiveSymbolsInRowOrColumn(board, size)) {
          currentCount++;
          break; // 成功放置，继续下一个位置
        }
        
        // 违反规则，撤销放置
        board[row][col] = CellState.EMPTY;
      }
    }
    
    return currentCount;
  }

  // 检查指定位置是否有三个相同符号连续
  private hasThreeInARowAtPosition(board: CellState[][], row: number, col: number): boolean {
    const symbol = board[row][col];
    const size = board.length;

    // 检查水平方向
    if (col >= 2 &&
      board[row][col-1] === symbol &&
      board[row][col-2] === symbol) {
      return true;
    }
    if (col <= size-3 &&
      board[row][col+1] === symbol &&
      board[row][col+2] === symbol) {
      return true;
    }
    if (col >= 1 && col <= size-2 &&
      board[row][col-1] === symbol &&
      board[row][col+1] === symbol) {
      return true;
    }

    // 检查垂直方向
    if (row >= 2 &&
      board[row-1][col] === symbol &&
      board[row-2][col] === symbol) {
      return true;
    }
    if (row <= size-3 &&
      board[row+1][col] === symbol &&
      board[row+2][col] === symbol) {
      return true;
    }
    if (row >= 1 && row <= size-2 &&
      board[row-1][col] === symbol &&
      board[row+1][col] === symbol) {
      return true;
    }

    return false;
  }

  // 检查一行或一列是否有三个连续相同符号
  private hasThreeInARow(line: CellState[]): boolean {
    for (let i = 0; i <= line.length - 3; i++) {
      const cell1 = this.normalizeCellState(line[i]);
      const cell2 = this.normalizeCellState(line[i + 1]);
      const cell3 = this.normalizeCellState(line[i + 2]);

      if (cell1 !== '' && cell1 === cell2 && cell2 === cell3) {
        return true;
      }
    }
    return false;
  }

  // 检查整个棋盘是否有任何行或列出现三个连续相同符号
  private hasAnyThreeInARow(board: CellState[][], size: number): boolean {
    // 检查所有行
    for (let i = 0; i < size; i++) {
      if (this.hasThreeInARow(board[i])) {
        return true;
      }
    }
    
    // 检查所有列
    for (let j = 0; j < size; j++) {
      const column: CellState[] = [];
      for (let i = 0; i < size; i++) {
        column.push(board[i][j]);
      }
      if (this.hasThreeInARow(column)) {
        return true;
      }
    }
    
    return false;
  }

  // 检查每行和每列中x和o的数量是否超过一半
  private hasExcessiveSymbolsInRowOrColumn(board: CellState[][], size: number): boolean {
    // 检查每一行
    for (let i = 0; i < size; i++) {
      let xCount = 0;
      let oCount = 0;
      for (let j = 0; j < size; j++) {
        const cell = board[i][j];
        if (cell === CellState.FIXED_X || cell === CellState.X) {
          xCount++;
        } else if (cell === CellState.FIXED_O || cell === CellState.O) {
          oCount++;
        }
      }
      // 如果x或o的数量超过一半，返回true（表示违反规则）
      if (xCount > Math.floor(size / 2) || oCount > Math.floor(size / 2)) {
        return true;
      }
    }

    // 检查每一列
    for (let j = 0; j < size; j++) {
      let xCount = 0;
      let oCount = 0;
      for (let i = 0; i < size; i++) {
        const cell = board[i][j];
        if (cell === CellState.FIXED_X || cell === CellState.X) {
          xCount++;
        } else if (cell === CellState.FIXED_O || cell === CellState.O) {
          oCount++;
        }
      }
      // 如果x或o的数量超过一半，返回true（表示违反规则）
      if (xCount > Math.floor(size / 2) || oCount > Math.floor(size / 2)) {
        return true;
      }
    }

    return false;
  }

  // 检查盘面是否可解（优化版本）
  private isSolvable(board: CellState[][]): boolean {
    const size = board.length;
    const tempBoard = this.copyBoard(board);

    // 先进行快速预检查
    if (!this.preliminaryChecks(tempBoard, size)) {
      return false;
    }

    // 尝试填充棋盘，检查是否能完成
    return this.trySolve(tempBoard, 0, 0);
  }

  // 预检查：快速排除明显不可解的情况
  private preliminaryChecks(board: CellState[][], size: number): boolean {
    // 检查每行和每列中已有的X和O数量是否合理
    for (let i = 0; i < size; i++) {
      let xCount = 0;
      let oCount = 0;
      for (let j = 0; j < size; j++) {
        const cell = board[i][j];
        if (cell === CellState.FIXED_X || cell === CellState.X) {
          xCount++;
        } else if (cell === CellState.FIXED_O || cell === CellState.O) {
          oCount++;
        }
      }
      // 如果X或O的数量已经超过一半，返回false
      if (xCount > Math.floor(size / 2) || oCount > Math.floor(size / 2)) {
        return false;
      }
    }

    // 检查每列
    for (let j = 0; j < size; j++) {
      let xCount = 0;
      let oCount = 0;
      for (let i = 0; i < size; i++) {
        const cell = board[i][j];
        if (cell === CellState.FIXED_X || cell === CellState.X) {
          xCount++;
        } else if (cell === CellState.FIXED_O || cell === CellState.O) {
          oCount++;
        }
      }
      if (xCount > Math.floor(size / 2) || oCount > Math.floor(size / 2)) {
        return false;
      }
    }

    return true;
  }

  private trySolve(board: CellState[][], row: number, col: number): boolean {
    const size = board.length;

    if (row === size) {
      return true; // 成功完成
    }

    if (col === size) {
      return this.trySolve(board, row + 1, 0);
    }

    // 如果当前格子已经有固定符号，跳过
    if (board[row][col] === CellState.FIXED_X || board[row][col] === CellState.FIXED_O) {
      return this.trySolve(board, row, col + 1);
    }

    // 尝试放置X
    board[row][col] = CellState.X;
    if (!this.hasThreeInARowAtPosition(board, row, col) && this.trySolve(board, row, col + 1)) {
      return true;
    }

    // 尝试放置O
    board[row][col] = CellState.O;
    if (!this.hasThreeInARowAtPosition(board, row, col) && this.trySolve(board, row, col + 1)) {
      return true;
    }

    // 回溯
    board[row][col] = CellState.EMPTY;
    return false;
  }

  // AI自动解题功能 - 实时展示解题过程
  autoSolve(): boolean {
    const tempBoard = this.copyBoard(this.board);
    
    // 先检查是否可解
    if (!this.isSolvable(tempBoard)) {
      return false;
    }
    
    // 重新复制当前棋盘（不包含用户已填的格子）
    const initialBoard = this.createEmptyBoard(this.boardSize);
    for (let i = 0; i < this.boardSize; i++) {
      for (let j = 0; j < this.boardSize; j++) {
        // 只保留固定的格子
        if (this.board[i][j] === CellState.FIXED_X || this.board[i][j] === CellState.FIXED_O) {
          initialBoard[i][j] = this.board[i][j];
        } else {
          initialBoard[i][j] = CellState.EMPTY;
        }
      }
    }
    
    // 使用回溯法求解，并实时更新界面
    if (this.autoSolveWithVisualization(initialBoard, 0, 0)) {
      // 复制求解结果到当前棋盘
      for (let i = 0; i < this.boardSize; i++) {
        for (let j = 0; j < this.boardSize; j++) {
          if (this.board[i][j] !== CellState.FIXED_X && this.board[i][j] !== CellState.FIXED_O) {
            this.board[i][j] = initialBoard[i][j];
          }
        }
      }
      // 强制更新UI
      this.board = [...this.board];
      
      // 检查游戏是否完成
      if (this.checkGameComplete()) {
        this.endGame('player1');
      }
      
      return true;
    }
    
    return false;
  }

  // 带可视化的自动解题方法
  private autoSolveWithVisualization(board: CellState[][], row: number, col: number): boolean {
    const size = board.length;

    if (row === size) {
      return true; // 成功完成
    }

    if (col === size) {
      return this.autoSolveWithVisualization(board, row + 1, 0);
    }

    // 如果当前格子已经有固定符号，跳过
    if (board[row][col] === CellState.FIXED_X || board[row][col] === CellState.FIXED_O) {
      return this.autoSolveWithVisualization(board, row, col + 1);
    }

    // 尝试放置X
    board[row][col] = CellState.X;
    if (!this.hasThreeInARowAtPosition(board, row, col) && 
        !this.hasExcessiveSymbolsInRowOrColumn(board, size) && 
        this.autoSolveWithVisualization(board, row, col + 1)) {
      return true;
    }

    // 尝试放置O
    board[row][col] = CellState.O;
    if (!this.hasThreeInARowAtPosition(board, row, col) && 
        !this.hasExcessiveSymbolsInRowOrColumn(board, size) && 
        this.autoSolveWithVisualization(board, row, col + 1)) {
      return true;
    }

    // 回溯
    board[row][col] = CellState.EMPTY;
    return false;
  }

  // 创建备用的简单可解盘面
  private createFallbackBoard(size: number): CellState[][] {
    const board = this.createEmptyBoard(size);

    // 放置一些简单的固定符号，确保可解
    if (size >= 4) {
      // 放置对角线的固定符号
      board[0][0] = CellState.FIXED_X;
      board[1][1] = CellState.FIXED_O;
      board[2][2] = CellState.FIXED_X;
      board[3][3] = CellState.FIXED_O;
    }

    return board;
  }

  private getFixedCount(size: number): number {
    let baseCount: number;
    if (size === 4) baseCount = 6;
    else if (size === 6) baseCount = 10;
    else if (size === 8) baseCount = 16;
    else if (size === 10) baseCount = 22;
    else baseCount = size;

    // 根据难度调整预设格子数量
    switch (this.difficulty) {
      case GameDifficulty.EASY:
        return Math.floor(baseCount * 1.3); // 简单模式：较多预设格子，更容易
      case GameDifficulty.MEDIUM:
        return baseCount; // 中等模式：标准预设格子
      case GameDifficulty.HARD:
        return Math.floor(baseCount * 0.7); // 困难模式：较少预设格子，更具挑战性
      default:
        return baseCount;
    }
  }

  private initializeGame(): void {
    this.board = this.createInitialBoard(this.boardSize);  //调用方法生成可解盘面
    this.initialFixedCells = this.copyBoard(this.board);
    this.history = [];
    this.gameStatus = GameStatus.NOT_STARTED;
    this.winner = undefined;
    this.timeElapsed = 0;
    this.player1Time = 0;
    this.player2Time = 0;
    this.lastMoveTime = 0;

    if (this.gameMode === GameMode.MULTIPLAYER) {
      this.opponentBoard = this.createEmptyBoard(this.boardSize);
      this.currentPlayer = 'player1';
    }
  }

  startGame(): void {
    this.gameStatus = GameStatus.IN_PROGRESS;
    this.startTime = Date.now();
    this.lastMoveTime = Date.now();
    
    console.log('游戏开始，重置计时器');

    // Start timer
    if (this.timerId) {
      clearInterval(this.timerId);
    }
    
    this.timerId = setInterval(() => {
      const currentTime = Date.now();
      this.timeElapsed = Math.floor((currentTime - this.startTime) / 1000);
      console.log('计时器更新，当前时间:', this.timeElapsed);

      if (this.gameMode === GameMode.MULTIPLAYER) {
        if (this.isMyTurn()) {
          this.player1Time = Math.floor((currentTime - this.lastMoveTime) / 1000);
        } else {
          this.player2Time = Math.floor((currentTime - this.lastMoveTime) / 1000);
        }
      }
    }, 1000) as number;
  }

  pauseGame(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = 0;
    }
    this.gameStatus = GameStatus.PAUSED;
  }

  toggleCell(row: number, col: number): boolean {
    // 检查是否是当前玩家的回合
    if (this.gameMode === GameMode.MULTIPLAYER && !this.isMyTurn()) {
      return false;
    }

    const success = this.toggleCellInternal(row, col, false);
    if (success && this.gameMode === GameMode.MULTIPLAYER) {
      // 切换当前玩家
      this.currentPlayer = this.currentPlayer === 'player1' ? 'player2' : 'player1';
      this.lastMoveTime = Date.now();
    }
    return success;
  }

  toggleOpponentCell(row: number, col: number): boolean {
    return this.toggleCellInternal(row, col, true);
  }

  isMyTurn(): boolean {
    if (this.gameMode === GameMode.SINGLE_PLAYER) {
      return true;
    }
    return this.currentPlayer === `player${this.playerNumber}`;
  }

  private toggleCellInternal(row: number, col: number, isOpponent: boolean): boolean {
    // Check if row and column are within bounds
    if (row < 0 || row >= this.boardSize || col < 0 || col >= this.boardSize) {
      return false;
    }

    const targetBoard = isOpponent ? this.opponentBoard : this.board;
    const currentCell = targetBoard[row][col];

    // Only allow modifying non-fixed cells
    if (currentCell === CellState.EMPTY ||
      currentCell === CellState.X ||
      currentCell === CellState.O) {

      // Save history if it's the player's move
      if (!isOpponent) {
        this.history.push(this.copyBoard(this.board));
      }

      // Toggle state: EMPTY -> X -> O -> EMPTY
      let newState: CellState;
      if (currentCell === CellState.EMPTY) {
        newState = CellState.X;
      } else if (currentCell === CellState.X) {
        newState = CellState.O;
      } else {
        newState = CellState.EMPTY;
      }

      targetBoard[row][col] = newState;

      // Force update
      if (isOpponent) {
        this.opponentBoard = [...this.opponentBoard];
      } else {
        this.board = [...this.board];
      }

      // Check for game completion after each move
      if (this.checkGameOver()) {
        this.endGame();
      }

      return true;
    }

    return false;
  }

  undo(): void {   //撤销逻辑
    if (this.history.length > 0) {
      const lastState = this.history.pop();
      if (lastState) {
        this.board = lastState;
      }
    }
  }

  reset(): void {  //重置游戏
    this.initializeGame();
  }

  resetCurrent(): void {  //充值当前
    this.board = this.initialFixedCells.map((row: CellState[]) => {
      return row.map((cell: CellState) => {
        return cell === CellState.FIXED_X || cell === CellState.FIXED_O ? cell : CellState.EMPTY;
      });
    });
    this.history = [];
  }

  checkGameComplete(): boolean {
    // Check if the board is fully filled
    for (let i = 0; i < this.boardSize; i++) {
      for (let j = 0; j < this.boardSize; j++) {
        if (this.board[i][j] === CellState.EMPTY) {
          return false;
        }
      }
    }

    // Check all Classic OOXX game rules
    return this.areAllRowsAndColumnsUnique() &&
      !this.hasThreeInRowOrColumn(this.board) &&
    this.areRowsAndColumnsBalanced(this.board);
  }

  setDifficulty(difficulty: GameDifficulty): void {
    this.difficulty = difficulty;
  }

  getDifficulty(): GameDifficulty {
    return this.difficulty;
  }

  getGameState(): GameState {
    return {
      board: this.copyBoard(this.board),
      boardSize: this.boardSize,
      gameStatus: this.gameStatus,
      winner: this.winner,
      playerNumber: this.playerNumber,
      timestamp: Date.now(),
      gameMode: this.gameMode,
      timeElapsed: this.timeElapsed,
      player1Time: this.player1Time,
      player2Time: this.player2Time,
      currentPlayer: this.currentPlayer
    };
  }

  applyGameState(state: GameState): void {
    if (state.boardSize !== this.boardSize) {
      this.boardSize = state.boardSize;
      this.initializeGame();
    }

    this.board = state.board;
    this.gameStatus = state.gameStatus;
    this.winner = state.winner;
    this.timeElapsed = state.timeElapsed;
    this.player1Time = state.player1Time || 0;
    this.player2Time = state.player2Time || 0;
    this.currentPlayer = state.currentPlayer;

    if (this.gameStatus === GameStatus.IN_PROGRESS && !this.timerId) {
      this.startGame();
    } else if (this.gameStatus !== GameStatus.IN_PROGRESS && this.timerId) {
      clearInterval(this.timerId);
      this.timerId = 0;
    }
  }

  private areAllRowsAndColumnsUnique(): boolean {
    const size = this.boardSize;
    const rowStrings: string[] = [];
    const colStrings: string[] = [];

    for (let i = 0; i < size; i++) {
      // 处理行
      const row: string[] = [];
      for (let j = 0; j < size; j++) {
        const cell = this.board[i][j];
        row.push(this.normalizeCellState(cell));
      }
      rowStrings.push(row.join(','));

      // 处理列
      const col: string[] = [];
      for (let j = 0; j < size; j++) {
        const cell = this.board[j][i];
        col.push(this.normalizeCellState(cell));
      }
      colStrings.push(col.join(','));
    }

    return new Set(rowStrings).size === size && new Set(colStrings).size === size;
  }

  private normalizeCellState(cell: CellState): string {
    switch (cell) {
      case CellState.FIXED_X: return 'X';
      case CellState.FIXED_O: return 'O';
      case CellState.X: return 'X';
      case CellState.O: return 'O';
      default: return '';
    }
  }

  private areRowsAndColumnsBalanced(board: CellState[][]): boolean {
    const size = board.length;

    // Check each row and column for X and O balance
    for (let i = 0; i < size; i++) {
      let rowXCount = 0, rowOCount = 0;
      let colXCount = 0, colOCount = 0;

      for (let j = 0; j < size; j++) {
        // Count X and O in row
        if (board[i][j] === CellState.X || board[i][j] === CellState.FIXED_X) {
          rowXCount++;
        } else if (board[i][j] === CellState.O || board[i][j] === CellState.FIXED_O) {
          rowOCount++;
        }

        // Count X and O in column
        if (board[j][i] === CellState.X || board[j][i] === CellState.FIXED_X) {
          colXCount++;
        } else if (board[j][i] === CellState.O || board[j][i] === CellState.FIXED_O) {
          colOCount++;
        }
      }

      // Each row and column must have equal number of X and O
      if (rowXCount !== rowOCount || colXCount !== colOCount) {
        return false;
      }
    }

    return true;
  }

  private hasThreeInRowOrColumn(board: CellState[][]): boolean {
    const size = board.length;

    // Check rows for three consecutive X or O
    for (let i = 0; i < size; i++) {
      for (let j = 0; j <= size - 3; j++) {
        const cell1 = this.normalizeCellState(board[i][j]);
        const cell2 = this.normalizeCellState(board[i][j + 1]);
        const cell3 = this.normalizeCellState(board[i][j + 2]);

        if (cell1 !== '' && cell1 === cell2 && cell2 === cell3) {
          return true;
        }
      }
    }

    // Check columns for three consecutive X or O
    for (let j = 0; j < size; j++) {
      for (let i = 0; i <= size - 3; i++) {
        const cell1 = this.normalizeCellState(board[i][j]);
        const cell2 = this.normalizeCellState(board[i + 1][j]);
        const cell3 = this.normalizeCellState(board[i + 2][j]);

        if (cell1 !== '' && cell1 === cell2 && cell2 === cell3) {
          return true;
        }
      }
    }

    return false;
  }

  endGame(winner?: 'player1' | 'player2' | 'draw'): void {
    // 保存最终的时间
    const finalTime = this.timeElapsed;
    console.log('游戏结束，最终用时:', finalTime);
    
    this.gameStatus = GameStatus.COMPLETED;
    this.winner = winner;
    this.pauseGame();
    
    // 确保timeElapsed保留最终值
    this.timeElapsed = finalTime;
  }

  checkGameOver(): boolean {
    return this.checkGameComplete();
  }
}