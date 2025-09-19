import { CellState } from '../model/GameModel';

@Component
export struct OOXXBoard {
  // 输入属性
  @Prop board: CellState[][] = [];
  @Prop customCellSize: number = 0; // 重命名以避免冲突，0表示自动计算
  @Prop isOpponent: boolean = false;
  @Prop isActive: boolean = true;

  // 回调函数
  onCellClick: (row: number, col: number) => void = () => {};

  // 状态变量
  @State calculatedCellSize: number = 0; // 重命名计算后的尺寸
  @State fontSize: number = 0;
  @State touchStates: boolean[][] = [];

  aboutToAppear() {
    this.calculatedCellSize = this.getCellSize();
    this.fontSize = this.getCellFontSize();
    // 初始化触摸状态
    this.touchStates = Array(this.board.length).fill(0).map(() =>
    Array(this.board[0]?.length || 0).fill(false)
    );
  }

  private getCellSize(): number {
    if (this.customCellSize > 0) {
      return this.customCellSize;
    }

    // 自动计算基于棋盘大小
    const boardSize = this.board.length;
    const screenWidth = vp2px(100); // 100% 视口宽度
    const padding = 40; // 总水平内边距
    const maxCellSize = 80; // 最大格子尺寸

    // 基于可用宽度计算格子尺寸
    const calculatedSize = Math.min(
      maxCellSize,
      (screenWidth - padding) / boardSize
    );

    return Math.max(30, calculatedSize); // 最小格子尺寸30
  }

  private getCellFontSize(): number {
    if (this.board.length === 4) {
      return 38;
    } else if (this.board.length === 6) {
      return 28;
    } else if (this.board.length === 8) {
      return 22;
    } else if (this.board.length === 10) {
      return 18;
    } else {
      return 24;
    }
  }

  private getCellContent(cell: CellState): string {
    // 使用自定义SVG图标获得更科技感的外观
    if (cell === CellState.X || cell === CellState.FIXED_X) {
      return '×'; // 乘号表示X
    } else if (cell === CellState.O || cell === CellState.FIXED_O) {
      return '○'; // 圆圈表示O
    }
    return '';
  }

  private getCellColor(cell: CellState): ResourceColor {
    // 科技风格配色方案
    if (cell === CellState.X) {
      return '#FF5252'; // 亮红色表示X
    } else if (cell === CellState.O) {
      return '#4CAF50'; // 绿色表示O
    } else if (cell === CellState.FIXED_X) {
      return '#B71C1C'; // 深红色表示固定X
    } else if (cell === CellState.FIXED_O) {
      return '#1B5E20'; // 深绿色表示固定O
    }
    return Color.Transparent;
  }

  private getCellBackground(cell: CellState): ResourceColor {
    // 浅色主题背景色
    if (cell === CellState.FIXED_X || cell === CellState.FIXED_O) {
      return '#F5F5F5'; // 固定格子的浅色背景
    } else if (cell === CellState.X) {
      return '#FFEBEE'; // X的浅粉色背景
    } else if (cell === CellState.O) {
      return '#E8F5E8'; // O的浅绿色背景
    }
    return '#FFFFFF'; // 空格子的白色背景
  }

  private getCellBorderColor(cell: CellState): ResourceColor {
    // 浅色主题的边框颜色
    if (cell === CellState.FIXED_X || cell === CellState.FIXED_O) {
      return '#BDBDBD'; // 固定格子的灰色边框
    } else if (cell === CellState.X) {
      return '#FF5252'; // 红色边框
    } else if (cell === CellState.O) {
      return '#4CAF50'; // 绿色边框
    }
    return '#E0E0E0'; // 空格子的浅灰色边框
  }

  private getCellGradient(cell: CellState): string {
    // 返回浅色主题的纯色
    if (cell === CellState.FIXED_X || cell === CellState.FIXED_O) {
      return '#F5F5F5';
    } else if (cell === CellState.X) {
      return '#FFEBEE';
    } else if (cell === CellState.O) {
      return '#E8F5E8';
    }
    return '#FFFFFF';
  }

  build() {
    // 根容器
    Column() {
      ForEach(this.board, (row: CellState[], rowIndex: number) => {
        Row() {
          ForEach(row, (cell: CellState, colIndex: number) => {
            // 创建具有科技感外观的格子
            Stack() {
              // 格子背景带微妙渐变
              Column()
                .width(this.calculatedCellSize)
                .height(this.calculatedCellSize)
                .backgroundColor(this.getCellGradient(cell) as string)
                .border({
                  width: 1,
                  color: this.getCellBorderColor(cell),
                  style: BorderStyle.Solid
                })
                .opacity(this.isActive ? 1.0 : 0.6)

              // 格子内容（X或O）
              if (cell !== CellState.EMPTY) {
                Text(this.getCellContent(cell))
                  .fontSize(this.fontSize)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(this.getCellColor(cell))
                  .textShadow({
                    radius: 2,
                    color: '#00000080',
                    offsetX: 0,
                    offsetY: 1
                  })
              }

              // 交互式覆盖层
              if (cell === CellState.EMPTY && this.isActive) {
                Column()
                  .width(this.calculatedCellSize)
                  .height(this.calculatedCellSize)
                  .backgroundColor(this.touchStates[rowIndex]?.[colIndex] ? '#FFFFFF20' : '#00000000')
                  .onClick(() => this.onCellClick(rowIndex, colIndex))
                  .onTouch((event: TouchEvent) => {
                    if (event.type === TouchType.Down) {
                      // 更新触摸状态
                      this.touchStates[rowIndex][colIndex] = true;
                      this.touchStates = [...this.touchStates];
                    } else if (event.type === TouchType.Up || event.type === TouchType.Cancel) {
                      // 重置触摸状态
                      this.touchStates[rowIndex][colIndex] = false;
                      this.touchStates = [...this.touchStates];
                    }
                  });
              }
            }
            .borderRadius(16)
            .margin(2)
            .onClick(() => {
              console.log(`点击格子: (${rowIndex}, ${colIndex}), 状态: ${cell}`);
              this.onCellClick(rowIndex, colIndex);
            })
          })
        }
        .justifyContent(FlexAlign.Center)
      })
    }
    .padding(20)
    .borderRadius(24)
    .backgroundColor('#FAFAFA')
    .shadow({
      radius: 12,
      color: '#00000010',
      offsetX: 0,
      offsetY: 4
    })
    .margin({ top: 20, bottom: 20 })
  }
}