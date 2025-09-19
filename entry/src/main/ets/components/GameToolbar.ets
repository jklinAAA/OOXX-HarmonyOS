@Component
export struct GameToolbar {
  onRulesClick: () => void = () => {};
  onResetClick: () => void = () => {};
  onResetCurrentClick: () => void = () => {};

  build() {
    Row() {
      // 游戏规则按钮
      Button({
        type: ButtonType.Normal,
        stateEffect: true
      }) {
        Row() {
          Text('📖')
            .fontSize(14)
            .margin({ right: 6 })
          Text('规则')
            .fontSize(13)
            .fontColor('#FFFFFF')
            .fontWeight(FontWeight.Medium)
        }
      }
      .height(36)
      .padding({ left: 14, right: 14 })
      .backgroundColor('#FFB6C1')
      .borderRadius(18)
      .shadow({
        radius: 4,
        color: '#FFB6C130',
        offsetX: 0,
        offsetY: 2
      })
      .onClick(() => {
        this.onRulesClick();
      })

      // 重置当前盘面按钮
      Button({
        type: ButtonType.Normal,
        stateEffect: true
      }) {
        Row() {
          Text('🔄')
            .fontSize(14)
            .margin({ right: 6 })
          Text('重置当前')
            .fontSize(13)
            .fontColor('#FFFFFF')
            .fontWeight(FontWeight.Medium)
        }
      }
      .height(36)
      .padding({ left: 14, right: 14 })
      .backgroundColor('#4ECDC4')
      .borderRadius(18)
      .margin({ left: 10 })
      .shadow({
        radius: 4,
        color: '#4ECDC430',
        offsetX: 0,
        offsetY: 2
      })
      .onClick(() => {
        this.onResetCurrentClick();
      })

      // 重置游戏按钮
      Button({
        type: ButtonType.Normal,
        stateEffect: true
      }) {
        Row() {
          Text('🌸')
            .fontSize(14)
            .margin({ right: 6 })
          Text('重置游戏')
            .fontSize(13)
            .fontColor('#FFFFFF')
            .fontWeight(FontWeight.Medium)
        }
      }
      .height(36)
      .padding({ left: 14, right: 14 })
      .backgroundColor('#FF6B9D')
      .borderRadius(18)
      .margin({ left: 10 })
      .shadow({
        radius: 4,
        color: '#FF6B9D30',
        offsetX: 0,
        offsetY: 2
      })
      .onClick(() => {
        this.onResetClick();
      })
    }
    .width('100%')
    .padding({ left: 20, right: 20, top: 16, bottom: 12 })
    .justifyContent(FlexAlign.SpaceEvenly)
  }
}