import { OOXXBoard } from '../components/OOXXBoard';
import { GameToolbar } from '../components/GameToolbar';
import { WinDialog } from '../components/WinDialog';
import { RulesDialog } from '../components/RulesDialog';
import { router } from '@kit.ArkUI';
import { LeaderboardRecord } from '../model/LeaderboardRecord';
import { GameModel, GameMode, GameDifficulty, GameStatus, CellState } from '../model/GameModel';
import prompt from '@ohos.promptAction';


@Entry
@Component
struct SinglePlayerGame {
  @State gameModel: GameModel | null = null;
  @State showWinDialog: boolean = false;
  @State showSizeMenu: boolean = false;
  @State showDifficultyMenu: boolean = false;
  @State showRulesDialog: boolean = false;
  @State selectedSize: number = 4;
  @State selectedDifficulty: string = '中等模式';
  @State hasInitialized: boolean = false;

  aboutToAppear(): void {
    // 初始化游戏模型，但不立即开始游戏
    this.initializeGameModel();
  }

  // 初始化游戏模型
  private initializeGameModel(): void {
    // 将难度映射到GameDifficulty枚举
    let gameDifficulty: GameDifficulty;
    switch (this.selectedDifficulty) {
      case '简单模式':
        gameDifficulty = GameDifficulty.EASY;
        break;
      case '困难模式':
        gameDifficulty = GameDifficulty.HARD;
        break;
      default:
        gameDifficulty = GameDifficulty.MEDIUM;
    }
    
    this.gameModel = new GameModel(this.selectedSize, GameMode.SINGLE_PLAYER, gameDifficulty);
    this.hasInitialized = true;
  }

  checkGameCompletion(): void {
    if (this.gameModel && this.gameModel.checkGameComplete()) {
      this.showWinDialog = true;
    }
  }

  onCellClick(row: number, col: number): void {
    if (!this.gameModel) return;
    
    console.log(`SinglePlayerGame: 点击格子: (${row}, ${col})`);
    this.gameModel.toggleCell(row, col);
    this.checkGameCompletion();
  }

  onUndo(): void {
    if (this.gameModel) {
      this.gameModel.undo();
    }
  }

  onReset(): void {
    if (this.gameModel) {
      this.gameModel.reset();
      this.gameModel.startGame();
    }
  }

  onAutoSolve(): void {
    if (!this.gameModel) return;
    
    const success = this.gameModel.autoSolve();
    if (success) {
      prompt.showToast({ message: '解题成功！' });
    } else {
      prompt.showToast({ message: '当前盘面无解或已完成' });
    }
  }

  onResetCurrent(): void {
    if (this.gameModel) {
      this.gameModel.resetCurrent();
    }
  }

  onChangeSize(size: number): void {
    this.selectedSize = size;
    this.initializeGameModel();
    this.gameModel?.startGame();
    this.showSizeMenu = false;
  }

  onChangeDifficulty(difficulty: string): void {
    this.selectedDifficulty = difficulty;
    this.initializeGameModel();
    this.gameModel?.startGame();
    this.showDifficultyMenu = false;
  }

  saveGameScore(): void {
    if (!this.gameModel) return;
    
    // 获取游戏用时（秒）
    const timeTaken = this.gameModel.timeElapsed;
    console.log('保存成绩，用时:', timeTaken);
    
    // 获取难度模式
    const difficulty = this.selectedDifficulty;
    
    // 获取棋盘尺寸
    const boardSize = this.selectedSize;
    
    // 获取当前时间戳
    const timestamp = new Date().toLocaleString('zh-CN');
    
    // 创建新的排行榜记录
    const newRecord: LeaderboardRecord = {
      rank: 0, // 排名将在排序后计算
      playerName: '玩家' + Math.floor(Math.random() * 1000), // 临时使用随机玩家名
      boardSize: boardSize,
      difficulty: difficulty as 'easy' | 'medium' | 'hard' | '简单模式' | '中等模式' | '困难模式',
      timeTaken: timeTaken,
      timestamp: timestamp
    };
    
    // 保存到本地存储
    this.saveToLocalStorage(newRecord);
    
    // 显示成功提示
    prompt.showToast({ message: `成绩已保存！用时: ${this.formatTime(timeTaken)}` });
    
    // 关闭胜利弹窗
    this.showWinDialog = false;
  }
  
  private saveToLocalStorage(record: LeaderboardRecord): void {
    try {
      // 获取现有的排行榜数据
      const existingData = AppStorage.get<string>('leaderboardData') || '[]';
      console.log('保存前现有数据:', existingData);
      let leaderboardData: LeaderboardRecord[] = [];
      
      try {
        // 解析JSON数据
        const parsedData = JSON.parse(existingData) as object;
        
        // 验证是否为数组
        if (Array.isArray(parsedData)) {
          // 过滤并验证有效的LeaderboardRecord元素
          leaderboardData = parsedData.filter((item: object) => {
            // 确保item是对象
            if (item === null || typeof item !== 'object') {
              return false;
            }
            
            // 验证所有必需的属性和类型
            return (
              (item['rank'] === undefined || typeof item['rank'] === 'number') &&
              typeof item['playerName'] === 'string' &&
              typeof item['boardSize'] === 'number' &&
              typeof item['difficulty'] === 'string' &&
              typeof item['timeTaken'] === 'number' &&
              typeof item['timestamp'] === 'string'
            );
          }).map((item: object) => item as LeaderboardRecord);
        }
      } catch (e) {
        console.error('解析数据格式错误:', e);
      }
      
      // 添加新记录
      leaderboardData.push(record);
      
      // 按时间排序（时间越短排名越高）
      leaderboardData.sort((a: LeaderboardRecord, b: LeaderboardRecord) => a.timeTaken - b.timeTaken);
      
      // 更新排名
      leaderboardData.forEach((item: LeaderboardRecord, index: number) => {
        item.rank = index + 1;
      });
      
      // 保存回应用存储
      const newData = JSON.stringify(leaderboardData);
      console.log('保存的新数据:', newData);
      AppStorage.set<string>('leaderboardData', newData);
      
    } catch (error) {
      console.error('保存成绩失败:', error);
    }
  }
  
  // 测试方法：添加模拟排行榜数据（已移除按钮）
  private addTestLeaderboardData(): void {
    try {
      // 创建模拟数据
      const testData: LeaderboardRecord[] = [
        {
          rank: 1,
          playerName: '测试玩家1',
          boardSize: 4,
          difficulty: '中等模式',
          timeTaken: 120,
          timestamp: new Date().toLocaleString('zh-CN')
        },
        {
          rank: 2,
          playerName: '测试玩家2',
          boardSize: 4,
          difficulty: '中等模式',
          timeTaken: 180,
          timestamp: new Date().toLocaleString('zh-CN')
        },
        {
          rank: 3,
          playerName: '测试玩家3',
          boardSize: 6,
          difficulty: '简单模式',
          timeTaken: 240,
          timestamp: new Date().toLocaleString('zh-CN')
        }
      ];
      
      // 保存测试数据到应用存储
      const testDataStr = JSON.stringify(testData);
      console.log('保存测试数据:', testDataStr);
      AppStorage.set<string>('leaderboardData', testDataStr);
      
      // 显示成功提示
      prompt.showToast({ message: '测试数据已添加到排行榜！' });
    } catch (error) {
      console.error('添加测试数据失败:', error);
      prompt.showToast({ message: '添加测试数据失败' });
    }
  }

  private formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  navigateBack(): void {
    router.back();
  }

  build(): void {
    Stack() {
      // 主背景
      Column() {}
      .width('100%')
      .height('100%')
      .backgroundImage($r('app.media.win'), ImageRepeat.NoRepeat)
      .opacity(0.1)

      // 主内容区域
      Column() {
        // 顶部标题栏
        Row() {
          Button({
            type: ButtonType.Circle,
            stateEffect: true
          }) {
            Text('←')
              .fontSize(20)
              .fontColor('#FFFFFF')
          }
          .width(40)
          .height(40)
          .backgroundColor('transparent')
          .onClick(() => this.navigateBack())
          
          Text('单人游戏')
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor('#FFFFFF')
            .flexGrow(1)
            .textAlign(TextAlign.Center)
          
          // 占位元素，保持标题居中
          Column()
            .width(40)

          // 尺寸选择按钮
          Button({
            type: ButtonType.Circle,
            stateEffect: true
          }) {
            Text(`${this.selectedSize}×${this.selectedSize}`)
              .fontSize(14)
              .fontColor('#FFFFFF')
          }
          .width(50)
          .height(40)
          .backgroundColor('#6A89CC')
          .margin({ right: 10 })
          .onClick(() => this.showSizeMenu = true)

          // 难度选择按钮
          Button({
            type: ButtonType.Circle,
            stateEffect: true
          }) {
            Text(this.selectedDifficulty === '简单模式' ? '简' : this.selectedDifficulty === '困难模式' ? '难' : '中')
              .fontSize(14)
              .fontColor('#FFFFFF')
          }
          .width(40)
          .height(40)
          .backgroundColor('#4ECDC4')
          .onClick(() => this.showDifficultyMenu = true)
        }
        .width('100%')
        .height(80)
        .alignItems(VerticalAlign.Center)
        .linearGradient({
          direction: GradientDirection.Bottom,
          colors: [['#4A90E2', 0.0], ['#5D9CEC', 1.0]]
        })
        .shadow({ radius: 4, color: '#4A90E250', offsetX: 0, offsetY: 2 });

        // 游戏工具栏
        GameToolbar({
          onRulesClick: () => { this.showRulesDialog = true; },
          onResetClick: () => this.onReset(),
          onResetCurrentClick: () => this.onResetCurrent()
        })

        // 游戏网格
        if (this.gameModel) {
          OOXXBoard({
            board: this.gameModel.board,
            onCellClick: (row: number, col: number) => this.onCellClick(row, col)
          })
        } else {
          Column() {
            Text('请先选择尺寸和难度开始游戏')
              .fontSize(18)
              .fontColor('#666666')
              .margin({ top: 100 })
          }
          .width('100%')
          .height(300)
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)
        }

        // 操作按钮区域
        if (this.gameModel) {
          Row() {
            Button({
              type: ButtonType.Normal,
              stateEffect: true
            }) {
              Row() {
                Text('↶')
                  .fontSize(18)
                  .fontColor('#FFFFFF')
                  .margin({ right: 6 })
                Text('撤销')
                  .fontSize(14)
                  .fontColor('#FFFFFF')
                  .fontWeight(FontWeight.Medium)
              }
            }
            .height(48)
            .flexGrow(1)
            .backgroundColor(this.gameModel.history.length > 0 ? '#FF6B9D' : '#E0E0E0')
            .borderRadius(24)
            .margin({ right: 10 })
            .enabled(this.gameModel.history.length > 0)
            .onClick(() => this.onUndo())

            Button({
              type: ButtonType.Normal,
              stateEffect: true
            }) {
              Row() {
                Text('💡')
                  .fontSize(18)
                  .fontColor('#FFFFFF')
                  .margin({ right: 6 })
                Text('自动解题')
                  .fontSize(14)
                  .fontColor('#FFFFFF')
                  .fontWeight(FontWeight.Medium)
              }
            }
            .height(48)
            .flexGrow(1)
            .backgroundColor('#FFA726')
            .borderRadius(24)
            .margin({ right: 10 })
            .onClick(() => this.onAutoSolve())

            Button({
              type: ButtonType.Normal,
              stateEffect: true
            }) {
              Row() {
                Text('🌸')
                  .fontSize(18)
                  .fontColor('#FFFFFF')
                  .margin({ right: 6 })
                Text('重置')
                  .fontSize(14)
                  .fontColor('#FFFFFF')
                  .fontWeight(FontWeight.Medium)
              }
            }
            .height(48)
            .flexGrow(1)
            .backgroundColor('#4ECDC4')
            .borderRadius(24)
            .margin({ left: 10 })
            .onClick(() => this.onReset())
          }
          .width('100%')
          .padding({ left: 20, right: 20, top: 16, bottom: 20 })
        }
      }
      .width('100%')
      .height('100%')

      // 胜利弹窗
      if (this.showWinDialog) {
        WinDialog({
          onDismiss: () => { this.showWinDialog = false; },
          onNewGame: () => {
            this.showWinDialog = false;
            this.onReset();
          },
          onSaveScore: () => {
            this.saveGameScore();
          }
        })
      }

      // 尺寸选择菜单
      if (this.showSizeMenu) {
        Column() {
          Text('选择盘面尺寸')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .margin({ bottom: 20 })
          
          Row() {
            ForEach([4, 6, 8, 10], (size: number) => {
              Button({
                type: ButtonType.Normal,
                stateEffect: true
              }) {
                Text(`${size}×${size}`)
                  .fontSize(16)
                  .fontColor('#FFFFFF')
              }
              .width(60)
              .height(50)
              .backgroundColor(this.selectedSize === size ? '#FF6B9D' : '#6A89CC')
              .margin({ right: 10 })
              .onClick(() => this.onChangeSize(size))
            })
          }
        }
        .width(300)
        .height(150)
        .padding(20)
        .backgroundColor('#FFFFFF')
        .borderRadius(12)
        .shadow({ radius: 8, color: '#00000020', offsetX: 0, offsetY: 4 })
        .position({ x: '50%', y: '50%' })
        .margin({ top: -75 })
      }

      // 难度选择菜单
      if (this.showDifficultyMenu) {
        Column() {
          Text('选择难度模式')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .margin({ bottom: 20 })
          
          Row() {
            ForEach(['简单模式', '中等模式', '困难模式'], (difficulty: string) => {
              Button({
                type: ButtonType.Normal,
                stateEffect: true
              }) {
                Text(difficulty === '简单模式' ? '简' : difficulty === '困难模式' ? '难' : '中')
                  .fontSize(16)
                  .fontColor('#FFFFFF')
              }
              .width(60)
              .height(50)
              .backgroundColor(this.selectedDifficulty === difficulty ? '#FF6B9D' : '#4ECDC4')
              .margin({ right: 10 })
              .onClick(() => this.onChangeDifficulty(difficulty))
            })
          }
        }
        .width(300)
        .height(150)
        .padding(20)
        .backgroundColor('#FFFFFF')
        .borderRadius(12)
        .shadow({ radius: 8, color: '#00000020', offsetX: 0, offsetY: 4 })
        .position({ x: '50%', y: '50%' })
        .margin({ top: -75 })
      }

      // 规则对话框
      if (this.showRulesDialog) {
        RulesDialog({
          onDismiss: () => { this.showRulesDialog = false; }
        })
      }
    }
    .width('100%')
    .height('100%')
  }
}