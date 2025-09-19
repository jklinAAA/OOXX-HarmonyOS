@Component
export struct GameOverDialog {
  @Prop show: boolean = false;
  @Prop isWinner: boolean = false;
  @Prop playerTime: string = '00:00';
  @Prop opponentTime: string = '00:00';
  @Prop onConfirm: () => void = () => {};
  @Prop onRematch: () => void = () => {};

  build() {
    Stack() {
      if (!this.show) {
        // 当不显示时返回一个空视图
        Column().width(0).height(0);
      } else {
        // Semi-transparent overlay
        Column()
          .width('100%')
          .height('100%')
          .backgroundColor('rgba(0, 0, 0, 0.7)')
          .onClick(() => {}); // Prevent clicks from passing through

        // Dialog content
        Column() {
          // Result icon
          Image(this.isWinner ? $r('app.media.win') : $r('app.media.lose'))
            .width(80)
            .height(80)
            .margin({ top: 24, bottom: 16 });

          // Result text
          Text(this.isWinner ? '你赢了！' : '游戏结束')
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor('#FFFFFF')
            .margin({ bottom: 8 });

          // Time comparison
          Column() {
            Text('你的时间: ' + this.playerTime)
              .fontSize(16)
              .fontColor('#E0E0E0')
              .margin({ bottom: 4 });

            Text('对手时间: ' + this.opponentTime)
              .fontSize(16)
              .fontColor('#E0E0E0');
          }
          .margin({ top: 16, bottom: 24 });

          // Buttons
          Row() {
            // Rematch button
            Button('再来一局', { type: ButtonType.Normal })
              .width(120)
              .height(44)
              .backgroundColor('#4CAF50')
              .fontColor(Color.White)
              .borderRadius(22)
              .onClick(() => this.onRematch())
              .margin({ right: 12 });

            // Main menu button
            Button('主菜单', { type: ButtonType.Normal })
              .width(120)
              .height(44)
              .backgroundColor('#2196F3')
              .fontColor(Color.White)
              .borderRadius(22)
              .onClick(() => this.onConfirm());
          }
          .margin({ bottom: 24 });
        }
        .width('80%')
        .backgroundColor('#2D2D3A')
        .borderRadius(16)
        .alignItems(HorizontalAlign.Center);
      }
    }
    .width('100%')
    .height('100%')
    .position({ x: 0, y: 0 });
  }
}