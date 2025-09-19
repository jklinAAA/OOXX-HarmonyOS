import { BluetoothManager, ConnectionState, DeviceInfo } from '../utils/BluetoothManager';
import { router } from '@kit.ArkUI';
import { promptAction } from '@kit.ArkUI';

@Entry
@Component
export struct MultiplayerLobby {
  @State bluetoothManager: BluetoothManager = BluetoothManager.getInstance();
  @State connectionState: ConnectionState = ConnectionState.DISCONNECTED;
  @State discoveredDevices: DeviceInfo[] = [];
  @State isScanning: boolean = false;
  @State showConnectionDialog: boolean = false;
  @State connectingDevice: DeviceInfo | null = null;

  aboutToAppear(): void {
    // Set up Bluetooth event listeners
    this.bluetoothManager.onStateChange((state: ConnectionState) => {
      this.connectionState = state;
      this.isScanning = state === ConnectionState.SCANNING;

      if (state === ConnectionState.CONNECTED) {
        // Navigate to the game screen when connected
        router.pushUrl({
          url: 'pages/MultiplayerGame'
        }, router.RouterMode.Single);
      }
    });

    this.bluetoothManager.onDeviceDiscovered((device: DeviceInfo) => {
      if (!this.discoveredDevices.some(d => d.deviceId === device.deviceId)) {
        this.discoveredDevices = [...this.discoveredDevices, device];
      }
    });
  }

  aboutToDisappear(): void {
    // Clean up event listeners
    this.bluetoothManager.offStateChange(() => {});
    this.bluetoothManager.offDeviceDiscovered(() => {});
    // 停止扫描 when leaving the screen
    if (this.isScanning) {
      this.bluetoothManager.stopDeviceDiscovery();
    }
  }

  toggleScan(): void {
    if (this.isScanning) {
      this.bluetoothManager.stopDeviceDiscovery();
      this.isScanning = false;
    } else {
      this.discoveredDevices = [];
      this.bluetoothManager.startDeviceDiscovery();
    }
  }

  connectToDevice(device: DeviceInfo): void {
    this.connectingDevice = device;
    this.showConnectionDialog = true;

    this.bluetoothManager.connectToDevice(device.deviceId)
      .then((success: boolean) => {
        if (!success) {
          // Show error message
          promptAction.showToast({
            message: `连接失败: ${device.deviceName}`,
            duration: 2000
          });
        }
      })
      .finally(() => {
        this.showConnectionDialog = false;
        this.connectingDevice = null;
      });
  }

  build() {
    Column() {
      // Header
      Row() {
        Button() {
          Image($r('app.media.fanhui'))
            .width(24)
            .height(24)
        }
        .onClick(() => {
          // Navigate back
          router.back();
        })
        .margin({ left: 8 })
        .backgroundColor(Color.Transparent);

        Text('多人游戏大厅')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .margin({ left: 16 })
          .layoutWeight(1);
      }
      .width('100%')
      .height(60)
      .backgroundColor('#2196F3')
      .alignItems(VerticalAlign.Center)

      // Connection status
      Row() {
        Circle()
          .width(12)
          .height(12)
          .margin({ right: 8 })
          .backgroundColor(this.getStatusColor())

        Text(this.getStatusText())
          .fontSize(16)
      }
      .width('100%')
      .padding(16)
      .justifyContent(FlexAlign.Center)

      // Scan button
      Button(this.isScanning ? '停止扫描' : '扫描设备')
        .width('80%')
        .height(48)
        .margin(16)
        .onClick(() => this.toggleScan())
        .backgroundColor('#4CAF50')
        .fontColor(Color.White)

      // Device list
      if (this.discoveredDevices.length > 0) {
        Text('可用设备')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ top: 16, bottom: 8 })

        List({ space: 8 }) {
          ForEach(this.discoveredDevices, (device: DeviceInfo) => {
            ListItem() {
              Column() {
                Text(device.deviceName || 'Unknown Device')
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .margin({ bottom: 4 })

                Text(`信号强度: ${device.rssi} dBm`)
                  .fontSize(12)
                  .fontColor('#757575')
              }
              .width('100%')
              .padding(12)
              .borderRadius(8)
              .backgroundColor('#FFFFFF')
              .onClick(() => this.connectToDevice(device))
            }
          }, (device: DeviceInfo) => device.deviceId)
        }
        .width('100%')
        .layoutWeight(1)
        .divider({ strokeWidth: 1, color: '#E0E0E0' })
        .margin({ left: 16, right: 16, bottom: 16 })
      } else if (this.isScanning) {
        // Loading indicator while scanning
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .margin({ bottom: 16 })

          Text('扫描中...')
            .fontSize(16)
            .fontColor('#757575')
        }
        .width('100%')
        .height(200)
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
      } else {
        // Empty state
        Column() {
          Image($r('app.media.lanya'))
            .width(80)
            .height(80)
            .margin({ bottom: 16 })
            .foregroundColor('#BDBDBD')

          Text('未找到设备')
            .fontSize(16)
            .fontColor('#757575')
            .margin({ bottom: 8 })

          Text('请确保蓝牙已开启且设备可被发现。')
            .fontSize(14)
            .fontColor('#9E9E9E')
            .textAlign(TextAlign.Center)
            .padding({ left: 32, right: 32 })
        }
        .width('100%')
        .height(200)
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
      }

      // Connection dialog - 使用条件渲染替代customDialog
      if (this.showConnectionDialog && this.connectingDevice !== null) {
        Column() {
          Text('连接中')
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .margin({ bottom: 12 })

          Text(`正在连接到 ${this.connectingDevice?.deviceName || '设备'}...`)
            .fontSize(16)
        }
        .width('80%')
        .padding(24)
        .backgroundColor(Color.White)
        .borderRadius(12)
        .position({ x: '10%', y: '30%' })
        .zIndex(999)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  private getStatusColor(): ResourceColor {
    switch (this.connectionState) {
      case ConnectionState.CONNECTED: return '#4CAF50';
      case ConnectionState.CONNECTING: return '#FFC107';
      case ConnectionState.SCANNING: return '#2196F3';
      case ConnectionState.DISCONNECTED: return '#F44336';
      case ConnectionState.ERROR: return '#F44336';
      default: return '#9E9E9E';
    }
  }

  private getStatusText(): string {
    switch (this.connectionState) {
      case ConnectionState.CONNECTED: return '已连接';
      case ConnectionState.CONNECTING: return '连接中...';
      case ConnectionState.SCANNING: return '正在扫描设备...';
      case ConnectionState.DISCONNECTED: return '未连接';
      case ConnectionState.ERROR: return '连接错误';
      default: return '未知状态';
    }
  }
}