@Component
export struct RulesDialog {
  onDismiss: () => void = () => {};
  index: number = 0;
  text: string = '';

  build() {
    Column() {
      // 弹窗内容
      Column() {
        // 标题
        Row() {
          Text('🌸📖🌸')
            .fontSize(28)
        }
        .width('100%')
        .justifyContent(FlexAlign.Center)
        .margin({ bottom: 16 })

        Text('游戏规则')
          .fontSize(22)
          .fontWeight(FontWeight.Bold)
          .fontColor('#424242')
          .margin({ bottom: 20 })

        // 规则项
        RuleItem({ index: 1, text: '每行/列不能有超过2个连续的X或O' })
        RuleItem({ index: 2, text: '每行/列中的X和O数量相同' })
        RuleItem({ index: 3, text: '每行/列都是唯一的' })

        Button({
          type: ButtonType.Normal,
          stateEffect: true
        }) {
          Text('我知道了')
            .fontSize(14)
            .fontColor('#FFFFFF')
        }
        .width('100%')
        .height(40)
        .margin({ top: 20 })
        .backgroundColor('#FFB6C1')
        .borderRadius(20)
        .onClick(() => {
          this.onDismiss();
        })
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

@Component
struct RuleItem {
  index: number = 0;
  text: string = '';

  build() {
    Row() {
      // 序号圆圈
      Text(this.index.toString())
        .fontSize(12)
        .fontWeight(FontWeight.Bold)
        .fontColor('#FFFFFF')
        .textAlign(TextAlign.Center)
        .width(24)
        .height(24)
        .backgroundColor('#FFB6C1')
        .borderRadius(12)
        .margin({ right: 12 })

      // 规则文本
      Text(this.text)
        .fontSize(14)
        .fontColor('#424242')
        .flexGrow(1)
    }
    .margin({ top: 6, bottom: 6 })
    .width('100%')
    .alignItems(VerticalAlign.Center)
  }
}