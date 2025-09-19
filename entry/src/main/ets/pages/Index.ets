import prompt from '@ohos.promptAction';
import { router } from '@kit.ArkUI';

@Entry
@Component
struct Index {
  navigateToSinglePlayer(): void {
    // 显示提示信息
    prompt.showToast({ message: '正在启动单人游戏...' });

    // 跳转到单人游戏页面 - 使用新的router方法
    router.pushUrl({ url: 'pages/SinglePlayerGame' });
  }

  navigateToMultiplayer(): void {
    // 显示提示信息
    prompt.showToast({ message: '正在进入多人游戏大厅...' });

    // 跳转到多人游戏大厅页面 - 使用新的router方法
    router.pushUrl({ url: 'pages/MultiplayerLobby' });
  }

  showHighScores(): void {
    // 显示提示信息
    prompt.showToast({ message: '正在加载排行榜...' });

    // 跳转到排行榜页面
    router.pushUrl({ url: 'pages/Leaderboard' });
  }

  showSettings(): void {
    // 显示提示信息
    prompt.showToast({ message: '设置功能开发中...' });
  }

  build() {
    Column() {
      // 背景色
      Column() {
        // 留白
      }
      .width('100%')
      .height('100%')
      .backgroundColor('#667eea')
      .position({ x: 0, y: 0 })
      .zIndex(-1)

      // 主内容区域
      Scroll() {
        Column() {
          // Logo和标题
          Column() {
            Image($r('app.media.startIcon'))
              .width(120)
              .height(120)
              .margin({ top: 30, bottom: 10 })
              .borderRadius(24)
              .border({ width: 2, color: Color.White })

            Text('OOXX 游戏')
              .fontSize(32)
              .fontWeight(FontWeight.Bold)
              .fontColor(Color.White)
              .margin({ bottom: 30 })
          }
          .width('100%')
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)

          // 主菜单卡片
          Column() {
            // 游戏模式选择
            Column() {
              Text('选择游戏模式')
                .fontSize(20)
                .fontWeight(FontWeight.Bold)
                .fontColor('#2D3748')
                .margin({ bottom: 24 })

              // 单人游戏按钮
              Column() {
                this.createMenuButton('单人游戏', $r('app.media.danrenyouxi'), '#5D9CEC', () => this.navigateToSinglePlayer())
              }
              .margin({ bottom: 16 })

              // 多人游戏按钮
              Column() {
                this.createMenuButton('多人游戏', $r('app.media.duorenduizhan'), '#FF7043', () => this.navigateToMultiplayer())
              }
              .margin({ bottom: 16 })
            }
            .width('100%')
            .padding(24)
            .margin({ top: 10, bottom: 16 })
            .backgroundColor(Color.White)
            .borderRadius(20)
            .shadow({ radius: 16, color: '#00000010', offsetX: 0, offsetY: 4 })
            .opacity(0.95)

            // 其他功能
            Column() {
              Text('其他功能')
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .fontColor('#2D3748')
                .margin({ bottom: 16, left: 4 })

              // 排行榜按钮
              Column() {
                this.createSecondaryButton('排行榜', $r('app.media.paihangbang'), '#66BB6A', () => this.showHighScores())
              }
              .margin({ bottom: 12 })

              // 设置按钮
              Column() {
                this.createSecondaryButton('游戏设置', $r('app.media.shezhi'), '#78909C', () => this.showSettings())
              }
              .margin({ bottom: 4 })
            }
            .width('100%')
            .padding(24)
            .margin({ bottom: 24 })
            .backgroundColor(Color.White)
            .borderRadius(20)
            .shadow({ radius: 16, color: '#00000010', offsetX: 0, offsetY: 4 })
            .opacity(0.95)
          }
          .width('90%')
          .alignItems(HorizontalAlign.Center)

          // 页脚
          Text('v1.0.0')
            .fontSize(12)
            .fontColor('#FFFFFF99')
            .margin({ top: 8, bottom: 16 })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Center)
      }
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
  }

  // 创建主菜单按钮
  @Builder
  createMenuButton(text: string, icon: Resource, bgColor: ResourceColor, onClick: () => void) {
    Button() {
      Row() {
        Image(icon)
          .width(24)
          .height(24)
          .margin({ right: 12 })
        Text(text)
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
      }
      .justifyContent(FlexAlign.Center)
      .width('100%')
    }
    .width('100%')
    .height(60)
    .backgroundColor(bgColor)
    .fontColor(Color.White)
    .borderRadius(16)
    .border({ width: 1, color: '#00000020' })
    .onClick(() => {
      onClick();
    })
  }

  // 创建次要功能按钮
  @Builder
  createSecondaryButton(text: string, icon: Resource, bgColor: ResourceColor, onClick: () => void) {
    Button() {
      Row() {
        Image(icon)
          .width(20)
          .height(20)
          .margin({ right: 10 })
        Text(text)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
      }
      .justifyContent(FlexAlign.Center)
      .width('100%')
    }
    .width('100%')
    .height(52)
    .backgroundColor(bgColor)
    .fontColor(Color.White)
    .borderRadius(14)
    .onClick(() => {
      onClick();
    })
  }
}