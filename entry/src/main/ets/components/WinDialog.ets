@Component
export struct WinDialog {
  onDismiss: () => void = () => {};
  onNewGame: () => void = () => {};
  onSaveScore: () => void = () => {};

  build() {
    Column() {
      // 弹窗内容
      Column() {
        // 胜利图标
        Row() {
          Text('🌸🎉🌸')
            .fontSize(40)
        }
        .width('100%')
        .justifyContent(FlexAlign.Center)
        .margin({ bottom: 16 })

        Text('恭喜获胜!')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor('#424242')
          .margin({ bottom: 12 })

        Text('你成功解决了这个OOXX谜题!')
          .fontSize(15)
          .fontColor('#9E9E9E')
          .textAlign(TextAlign.Center)
          .margin({ bottom: 28 })

        // 按钮组
        Row() {
          Button({
            type: ButtonType.Normal,
            stateEffect: true
          }) {
            Text('继续游戏')
              .fontSize(14)
              .fontColor('#9E9E9E')
          }
          .height(40)
          .flexGrow(1)
          .backgroundColor('#F5F5F5')
          .borderRadius(20)
          .margin({ right: 8 })
          .onClick(() => {
            this.onDismiss();
          })

          Button({
            type: ButtonType.Normal,
            stateEffect: true
          }) {
            Text('保存成绩')
              .fontSize(14)
              .fontColor('#FFFFFF')
          }
          .height(40)
          .flexGrow(1)
          .backgroundColor('#66BB6A')
          .borderRadius(20)
          .margin({ right: 8 })
          .onClick(() => {
            this.onSaveScore();
          })

          Button({
            type: ButtonType.Normal,
            stateEffect: true
          }) {
            Text('新游戏')
              .fontSize(14)
              .fontColor('#FFFFFF')
          }
          .height(40)
          .flexGrow(1)
          .backgroundColor('#FFB6C1')
          .borderRadius(20)
          .margin({ left: 0 })
          .onClick(() => {
            this.onNewGame();
          })
        }
        .width('100%')
      }
      .width('80%')
      .padding(28)
      .backgroundColor('#FFFFFF')
      .borderRadius(20)
      .shadow({
        radius: 12,
        color: '#FFB6C120',
        offsetX: 0,
        offsetY: 6
      })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#00000060')
    .onClick(() => {
      this.onDismiss();
    })
  }
}