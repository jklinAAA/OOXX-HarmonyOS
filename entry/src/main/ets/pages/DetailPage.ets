@Entry
@Component
struct DetailPage {
  @State message: string = '鸿蒙系统';

  build() {
    RelativeContainer() {
      Text(this.message)
        .id('DetailPageHelloWorld')
        .fontSize($r('app.float.page_text_font_size'))
        .fontWeight(FontWeight.Bold)
        .alignRules({
          center: { anchor: '__container__', align: VerticalAlign.Center },
          middle: { anchor: '__container__', align: HorizontalAlign.Center }
        })
        .onClick(() => {
          this.message = '欢迎使用';
        })
    }
    .height('100%')
    .width('100%')
  }
}