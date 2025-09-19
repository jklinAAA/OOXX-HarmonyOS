import { router } from '@kit.ArkUI';
import { GameDifficulty } from '../model/GameModel';
import { LeaderboardRecord } from '../model/LeaderboardRecord';

@Entry
@Component
struct Leaderboard {
  @State leaderboardData: LeaderboardRecord[] = [];
  @State selectedSize: number = 4;
  @State selectedDifficulty: string = '中等模式';
  @State showFilterMenu: boolean = false;

  navigateBack(): void {
    router.back();
  }

  onPageShow(): void {
    this.loadLeaderboardData();
  }

  // 初始化一些排行榜测试数据（暗色表示）
  private initializeTestData(): void {
    try {
      // 先检查是否已有数据，如果没有则添加测试数据
      const existingData = AppStorage.get<string>('leaderboardData');
      if (!existingData || existingData === '[]') {
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
          },
          {
            rank: 4,
            playerName: '测试玩家4',
            boardSize: 8,
            difficulty: '困难模式',
            timeTaken: 360,
            timestamp: new Date().toLocaleString('zh-CN')
          },
          {
            rank: 5,
            playerName: '测试玩家5',
            boardSize: 6,
            difficulty: '困难模式',
            timeTaken: 300,
            timestamp: new Date().toLocaleString('zh-CN')
          }
        ];
        
        // 保存测试数据到应用存储
        const testDataStr = JSON.stringify(testData);
        console.log('初始化测试数据:', testDataStr);
        AppStorage.set<string>('leaderboardData', testDataStr);
      }
    } catch (error) {
      console.error('初始化测试数据失败:', error);
    }
  }

  private loadLeaderboardData(): void {
    try {
      // 首先初始化测试数据（如果需要）
      this.initializeTestData();
      
      // 直接从AppStorage获取数据，增加默认值处理
      const storedData: string = (AppStorage.get<string>('leaderboardData') as string) || '[]';
      console.log('加载的排行榜数据:', storedData);

      let allData: LeaderboardRecord[] = [];

      try {
        // JSON.parse with type assertion
        const parsedData = JSON.parse(storedData) as object;

        if (Array.isArray(parsedData)) {
          // Filter and validate items
          allData = parsedData.filter((item: object) => {
            // Check if item is a valid object
            if (item === null || typeof item !== 'object') {
              return false;
            }
            
            // Validate required fields through direct property checks
            return (
              (item['rank'] === undefined || typeof item['rank'] === 'number') &&
                typeof item['playerName'] === 'string' &&
                typeof item['boardSize'] === 'number' &&
                typeof item['difficulty'] === 'string' &&
                typeof item['timeTaken'] === 'number' &&
                typeof item['timestamp'] === 'string'
            );
          }).map((item: object) => item as LeaderboardRecord);
          console.log('解析后有效记录数:', allData.length);
        } else {
          console.error('排行榜数据格式错误：不是数组');
        }
      } catch (e) {
        console.error('解析排行榜数据失败:', e);
      }

      // 根据筛选条件过滤数据
      const filteredData: LeaderboardRecord[] = allData.filter(
        (item: LeaderboardRecord) =>
        item.boardSize === this.selectedSize &&
          item.difficulty === this.selectedDifficulty
      );
      console.log('筛选后的数据:', filteredData);

      this.leaderboardData = filteredData;
    } catch (error) {
      console.error('加载排行榜数据失败:', error);
      this.leaderboardData = [];
    }
  }

  formatTime(seconds: number): string {
    const mins: number = Math.floor(seconds / 60);
    const secs: number = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  getDifficultyColor(difficulty: string): string {
    switch (difficulty) {
      case '简单模式': return '#4CAF50';
      case '中等模式': return '#2196F3';
      case '困难模式': return '#FF5252';
      default: return '#9E9E9E';
    }
  }

  build() {
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
          Button('←', { type: ButtonType.Circle })
            .width(40)
            .height(40)
            .fontSize(20)
            .fontColor('#FFFFFF')
            .backgroundColor('transparent')
            .onClick(() => this.navigateBack())

          Text('游戏排行榜')
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor('#FFFFFF')
            .flexGrow(1)
            .textAlign(TextAlign.Center)

          Button('筛选', { type: ButtonType.Circle })
            .width(50)
            .height(40)
            .fontSize(14)
            .fontColor('#FFFFFF')
            .backgroundColor('#6A89CC')
            .onClick(() => this.showFilterMenu = true)
        }
        .width('100%')
        .height(80)
        .alignItems(VerticalAlign.Center)
        .linearGradient({
          direction: GradientDirection.Bottom,
          colors: [['#4A90E2', 0.0], ['#5D9CEC', 1.0]]
        })
        .shadow({ radius: 4, color: '#4A90E250', offsetX: 0, offsetY: 2 })

        // 当前筛选条件
        Row() {
          Text(`当前筛选: ${this.selectedSize}×${this.selectedSize} | ${this.selectedDifficulty}`)
            .fontSize(14)
            .fontColor('#666666')
            .padding({ left: 20, right: 20 })
        }
        .width('100%')
        .height(32)
        .backgroundColor('#F8F9FA')
        .justifyContent(FlexAlign.Start)

        // 排行榜列表
        Scroll() {
          Column() {
            if (this.leaderboardData.length === 0) {
              Column() {
                Image($r('app.media.paihangbang'))
                  .width(120)
                  .height(120)
                  .margin({ bottom: 16 })
                Text('暂无游戏记录')
                  .fontSize(18)
                  .fontColor('#666666')
                  .margin({ bottom: 8 })
                Text('快来玩几局吧！')
                  .fontSize(16)
                  .fontColor('#999999')
              }
              .width('100%')
              .height(300)
              .justifyContent(FlexAlign.Center)
              .alignItems(HorizontalAlign.Center)
            } else {
              ForEach(this.leaderboardData, (item: LeaderboardRecord, index: number) => {
                this.LeaderboardItem(item, index);
              });
            }
          }
          .width('100%')
          .padding(20)
        }
        .flexGrow(1)
      }
      .width('100%')
      .height('100%')

      // 筛选菜单
      if (this.showFilterMenu) {
        Column() {
          Text('筛选条件')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .margin({ bottom: 20 })

          Text('盘面尺寸:')
            .fontSize(16)
            .fontColor('#424242')
            .margin({ bottom: 10 })

          Row() {
            ForEach([4, 6, 8, 10], (size: number) => {
              Button(`${size}×${size}`, { type: ButtonType.Normal })
                .width(80)
                .height(40)
                .fontSize(14)
                .fontColor('#FFFFFF')
                .backgroundColor(this.selectedSize === size ? '#FF6B9D' : '#6A89CC')
                .margin({ right: 8 })
                .onClick(() => {
                  this.selectedSize = size;
                  this.showFilterMenu = false;
                  this.loadLeaderboardData();
                });
            });
          }
          .margin({ bottom: 20 })

          Text('难度模式:')
            .fontSize(16)
            .fontColor('#424242')
            .margin({ bottom: 10 })

          Row() {
            ForEach(['简单模式', '中等模式', '困难模式'], (difficulty: string) => {
              Button(
                difficulty === '简单模式' ? '简' :
                  difficulty === '困难模式' ? '难' : '中',
                { type: ButtonType.Normal }
              )
                .width(50)
                .height(40)
                .fontSize(14)
                .fontColor('#FFFFFF')
                .backgroundColor(this.selectedDifficulty === difficulty ? '#FF6B9D' : '#4ECDC4')
                .margin({ right: 8 })
                .onClick(() => {
                  this.selectedDifficulty = difficulty;
                  this.showFilterMenu = false;
                  this.loadLeaderboardData();
                });
            });
          }

          Button('关闭', { type: ButtonType.Normal })
            .width(120)
            .height(40)
            .fontSize(14)
            .fontColor('#FFFFFF')
            .backgroundColor('#9E9E9E')
            .margin({ top: 20 })
            .onClick(() => this.showFilterMenu = false)
        }
        .width(280)
        .height(320)
        .padding(20)
        .backgroundColor('#FFFFFF')
        .borderRadius(12)
        .shadow({ radius: 8, color: '#00000020', offsetX: 0, offsetY: 4 })
        .position({ x: '50%', y: '50%' })
        .offset({ x: -140, y: -160 })
      }
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  LeaderboardItem(item: LeaderboardRecord, index: number) {
    Row() {
      Text((item.rank ?? 0).toString())
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor((item.rank ?? 0) <= 3 ? '#FF6B9D' : '#666666')
        .width(40)
        .textAlign(TextAlign.Center)

      Column() {
        Row() {
          Text(item.playerName)
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .fontColor('#424242')
            .flexGrow(1)
        }

        Row() {
          Text(this.formatTime(item.timeTaken))
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor('#FF6B9D')
            .margin({ right: 12 })

          Text(`${item.boardSize}×${item.boardSize}`)
            .fontSize(12)
            .fontColor('#666666')
            .margin({ right: 8 })

          Text(item.difficulty)
            .fontSize(12)
            .fontColor(this.getDifficultyColor(item.difficulty))
            .margin({ right: 8 })

          Text(item.timestamp)
            .fontSize(10)
            .fontColor('#999999')
        }
      }
      .flexGrow(1)
      .margin({ left: 12 })
    }
    .width('100%')
    .height(60)
    .padding({ left: 16, right: 16 })
    .backgroundColor(index % 2 === 0 ? '#FFFFFF' : '#F8F9FA')
    .borderRadius(8)
    .margin({ bottom: 8 })
  }
}
