import prompt from '@ohos.promptAction';
import { router } from '@kit.ArkUI';
import { BluetoothManager, ConnectionState } from '../utils/BluetoothManager';
import { OOXXBoard } from '../components/OOXXBoard';
import { GameToolbar } from '../components/GameToolbar';
import { GameOverDialog } from '../components/GameOverDialog';
import { GameModel, GameMode, GameDifficulty, GameStatus } from '../model/GameModel';

// 使用GameModel中定义的GameState接口，确保类型一致
type GameState = import('../model/GameModel').GameState;

@Entry
@Component
export struct MultiplayerGame {
  @State gameModel: GameModel = new GameModel(4, GameMode.MULTIPLAYER, GameDifficulty.MEDIUM);
  @State bluetoothManager: BluetoothManager = BluetoothManager.getInstance();
  @State connectionState: ConnectionState = ConnectionState.DISCONNECTED;
  @State showDisconnectDialog: boolean = false;
  @State showGameOverDialog: boolean = false;
  @State isWinner: boolean = false;
  @State opponentName: string = 'Opponent';
  @State myTime: string = '00:00';
  @State opponentTime: string = '00:00';
  @State isMyTurn: boolean = true;

  // 保存回调函数引用以便后续移除
  private stateChangeCallback: ((state: ConnectionState) => void) | null = null;
  private gameStateReceivedCallback: ((gameState: GameState) => void) | null = null;

  aboutToAppear(): void {
    // 定义回调函数
    this.stateChangeCallback = (state: ConnectionState) => {
      this.connectionState = state;

      if (state === ConnectionState.DISCONNECTED) {
        // Handle disconnection
        this.showDisconnectDialog = true;
      }
    };

    this.gameStateReceivedCallback = (gameState: GameState) => {
      this.handleOpponentGameState(gameState);
    };

    // Set up Bluetooth event listeners
    this.bluetoothManager.onStateChange(this.stateChangeCallback);
    this.bluetoothManager.onGameStateReceived(this.gameStateReceivedCallback);

    // Start the game once connected
    if (this.connectionState === ConnectionState.CONNECTED) {
      this.startGame();
    }
  }

  aboutToDisappear(): void {
    // Clean up event listeners
    if (this.stateChangeCallback) {
      this.bluetoothManager.offStateChange(this.stateChangeCallback);
    }
    if (this.gameStateReceivedCallback) {
      this.bluetoothManager.offGameStateReceived(this.gameStateReceivedCallback);
    }

    // Pause the game when leaving the screen
    this.gameModel.pauseGame();
  }

  startGame(): void {
    this.gameModel.startGame();

    // Send initial game state to opponent
    this.sendGameState();
  }

  handleCellClick(row: number, col: number): void {
    if (this.gameModel.gameStatus !== GameStatus.IN_PROGRESS) {
      return;
    }

    const success = this.gameModel.toggleCell(row, col);
    if (success) {
      // Send updated game state to opponent
      this.sendGameState();

      // Check for game completion
      if (this.gameModel.checkGameComplete()) {
        this.gameModel.endGame('player1'); // Current player wins
        this.showGameOverDialog = true;
        this.isWinner = true;
        this.sendGameState();
      }
    }
  }

  sendGameState(): void {
    // 创建符合GameModel中GameState接口的对象
    const gameState: GameState = {
      board: this.gameModel.board,
      boardSize: this.gameModel.boardSize,
      gameStatus: this.gameModel.gameStatus,
      winner: this.gameModel.winner,
      playerNumber: 2,
      timestamp: Date.now(),
      gameMode: GameMode.MULTIPLAYER,
      timeElapsed: this.gameModel.timeElapsed,
      player1Time: this.gameModel.player1Time,
      player2Time: this.gameModel.player2Time
    };
    this.bluetoothManager.sendGameState(gameState);
  }

  handleOpponentGameState(gameState: GameState): void {
    // 更新对手的棋盘状态
    if (gameState.board) {
      this.gameModel.opponentBoard = gameState.board;
    }

    // 更新游戏状态
    this.gameModel.gameStatus = gameState.gameStatus;

    if (gameState.gameStatus === GameStatus.COMPLETED) {
      // 对手完成了游戏
      this.gameModel.winner = gameState.winner;
      this.showGameOverDialog = true;
      this.isWinner = gameState.winner === 'player1';
    } else if (gameState.gameStatus === GameStatus.IN_PROGRESS) {
      // 更新对手的时间
      if (gameState.timeElapsed) {
        this.opponentTime = this.formatTime(gameState.timeElapsed);
      }
    }

    // 更新玩家时间
    if (gameState.player1Time !== undefined) {
      this.myTime = this.formatTime(gameState.player1Time);
    }
  }

  handleDisconnectConfirm(): void {
    this.showDisconnectDialog = false;
    // Navigate back to the main menu
    router.back();
  }

  handleGameOverConfirm(): void {
    this.showGameOverDialog = false;
    // Navigate back to the main menu
    router.back();
  }

  private formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  private updateTimes(): void {
    this.myTime = this.formatTime(this.gameModel.player1Time);
    this.opponentTime = this.formatTime(this.gameModel.player2Time);
  }

  private handleGameOver(winner?: 'player1' | 'player2' | 'draw'): void {
    if (winner) {
      this.isWinner = winner === 'player1';
      this.updateTimes();
      this.showGameOverDialog = true;
    }
  }

  private handleRematch(): void {
    this.showGameOverDialog = false;
    // Reset game state for rematch
    this.gameModel.reset();
    this.gameModel.startGame();
    this.sendGameState();
  }

  build() {
    Stack() {
      // 主内容
      Column() {
        // Game header with status bar
        Row() {
          // Back button
          Button() {
            Image($r('app.media.fanhui'))
              .width(24)
              .height(24)
          }
          .onClick(() => {
            // Show confirmation before leaving
            router.back();
          })
          .margin({ left: 8 })
          .backgroundColor(Color.Transparent);

          Text('多人游戏')
            .fontSize(20)
            .fontWeight(FontWeight.Bold)
            .fontColor('#FFFFFF')
            .layoutWeight(1)
            .textAlign(TextAlign.Center);

          // Connection status indicator
          Row() {
            Circle()
              .width(10)
              .height(10)
              .margin({ right: 6 })
              .backgroundColor(this.connectionState === ConnectionState.CONNECTED ? '#4CAF50' : '#F44336');

            Text(this.connectionState === ConnectionState.CONNECTED ? '已连接' : '与对手断开连接')
              .fontSize(12)
              .fontColor('#E0E0E0')
          }
          .padding({ left: 12, right: 12, top: 6, bottom: 6 })
          .borderRadius(12)
          .backgroundColor('#333344')
          .margin({ right: 12 })
        }
        .width('100%')
        .height(60)
        .backgroundColor('#2196F3')
        .alignItems(VerticalAlign.Center)

        // Game stats with turn indicator
        Column() {
          // Turn indicator
          Row() {
            Text(this.isMyTurn ? '你的回合' : `对手回合`)
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor(this.isMyTurn ? '#4CAF50' : '#FF9800')
          }
          .width('90%')
          .height(36)
          .justifyContent(FlexAlign.Center)
          .backgroundColor(this.isMyTurn ? '#4CAF5020' : '#FF980020')
          .borderRadius(18)
          .margin({ top: 8, bottom: 8 })

          // Player stats
          Row() {
            // Player 1 (current player) stats
            Column() {
              Text('你')
                .fontSize(14)
                .fontWeight(FontWeight.Medium)
                .fontColor(this.isMyTurn ? '#4CAF50' : '#BDBDBD')

              Text(this.myTime)
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .fontColor('#FFFFFF')
                .margin({ top: 2 })
            }
            .padding(8)
            .layoutWeight(1)
            .alignItems(HorizontalAlign.Center)

            // VS indicator
            Column() {
              Text('VS')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor('#757575')
                .margin({ top: 12 })
            }
            .width(40)

            // Player 2 (opponent) stats
            Column() {
              Text(this.opponentName)
                .fontSize(14)
                .fontWeight(FontWeight.Medium)
                .fontColor(!this.isMyTurn ? '#FF9800' : '#BDBDBD')

              Text(this.opponentTime)
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .fontColor('#FFFFFF')
                .margin({ top: 2 })
            }
            .padding(8)
            .layoutWeight(1)
            .alignItems(HorizontalAlign.Center)
          }
          .width('100%')
          .margin({ top: 4, bottom: 8 })
        }

        // Game board with tabs
        Column() {
          // Tab bar
          Row() {
            // Your board tab
            Column() {
              Text('你的棋盘')
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor('#FFFFFF')
                .padding({ bottom: 8 })

              // 使用矩形作为指示器
              Column()
                .width('80%')
                .height(3)
                .backgroundColor(this.isMyTurn ? '#4CAF50' : 'transparent')
            }
            .layoutWeight(1)
            .onClick(() => {
              if (!this.isMyTurn) {
                // Show message that it's not their turn
                prompt.showToast({
                  message: `等待 ${this.opponentName} 完成`,
                  duration: 1500
                });
              }
            })

            // Opponent's board tab
            Column() {
              Text(`${this.opponentName}的棋盘`)
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor('#BDBDBD')
                .padding({ bottom: 8 })

              // 使用矩形作为指示器
              Column()
                .width('80%')
                .height(3)
                .backgroundColor(!this.isMyTurn ? '#FF9800' : 'transparent')
            }
            .layoutWeight(1)
            .onClick(() => {
              // Toggle to opponent's board view
              this.isMyTurn = false;
            })
          }
          .width('100%')
          .padding({ top: 8, bottom: 8 })
          .backgroundColor('#1E1E2D')

          // Game board area
          Stack() {
            // Your board
            OOXXBoard({
              board: this.gameModel.board,
              isActive: this.isMyTurn && this.gameModel.gameStatus === GameStatus.IN_PROGRESS,
              onCellClick: (row: number, col: number) => this.handleCellClick(row, col)
            })
              .opacity(this.isMyTurn ? 1 : 0)

            // Opponent's board
            OOXXBoard({
              board: this.gameModel.board, // 暂时使用相同棋盘，实际应该是对手棋盘
              isActive: false,
              onCellClick: (row: number, col: number) => {
                // Show message to switch back to their own board
                prompt.showToast({
                  message: '切换到你的棋盘进行操作',
                  duration: 1500
                });
              }
            })
              .opacity(!this.isMyTurn ? 1 : 0)
          }
          .width('100%')
          .height('60%')
          .margin({ top: 8, bottom: 16 })
        }
        .width('100%')
        .height('70%')

        // Game controls
        GameToolbar({
          onRulesClick: () => { /* Show rules */ },
          onResetClick: () => this.gameModel.reset(),
          onResetCurrentClick: () => this.gameModel.resetCurrent()
        })
      }
      .width('100%')
      .height('100%')
      .backgroundColor('#F5F5F5')

      // Disconnect dialog
      if (this.showDisconnectDialog) {
        Column() {
          // Semi-transparent background
          Column()
            .width('100%')
            .height('100%')
            .backgroundColor('rgba(0, 0, 0, 0.5)')

          // Dialog content
          Column() {
            Text('连接断开')
              .fontSize(20)
              .fontWeight(FontWeight.Bold)
              .margin({ top: 16, bottom: 8 })

            Text('与对手的连接已断开')
              .fontSize(16)
              .textAlign(TextAlign.Center)
              .margin({ bottom: 24, left: 24, right: 24 })

            Button('确定', { type: ButtonType.Normal })
              .width(120)
              .height(40)
              .onClick(() => this.handleDisconnectConfirm())
              .margin({ bottom: 16 })
          }
          .width('80%')
          .borderRadius(8)
          .backgroundColor(Color.White)
          .alignItems(HorizontalAlign.Center)
        }
        .position({ x: 0, y: 0 })
        .width('100%')
        .height('100%')
      }

      // Game over dialog
      GameOverDialog({
        show: this.showGameOverDialog,
        isWinner: this.isWinner,
        playerTime: this.myTime,
        opponentTime: this.opponentTime,
        onConfirm: () => this.handleGameOverConfirm(),
        onRematch: () => this.handleRematch()
      })
    }
    .width('100%')
    .height('100%')
  }
}