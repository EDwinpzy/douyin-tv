# Douyin TV 非官方

面向 Android TV / 投影仪的抖音客户端实验项目，使用 Kotlin、Jetpack Compose 与
Media3 实现短视频、直播和遥控器交互。

> 当前状态：实验性项目，不对外发布，也不代表抖音官方客户端。

## 功能范围

- 短视频上下切换、暂停/继续和播放失败重试。
- 直播房间列表、房间播放和返回焦点恢复。
- 抖音网页扫码登录，登录 Cookie 使用 Android Keystore 加密后保存在本机。
- D-pad 焦点导航、返回键分层处理和 TV Leanback 启动入口。

## 环境要求

- Windows PowerShell
- JDK 17
- Android SDK Platform 35
- Android TV / 投影设备或模拟器，Android 8.0（API 26）及以上

## 构建与测试

在项目根目录执行：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Debug APK 输出到 `app\build\outputs\apk\debug\app-debug.apk`。安装到已连接设备：

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## 项目结构

```text
douyinTV/
├── app/src/main/java/com/dongpiandi/douyintv/
│   ├── MainActivity.kt              # TV 全屏入口
│   ├── data/                        # 会话、设备标识与本地存储
│   ├── network/                     # 抖音接口、签名与媒体解析
│   ├── model/                       # 内容模型
│   └── ui/                          # Compose TV 界面与状态管理
├── app/src/main/res/                # 主题、图标与 TV 清单资源
└── gradle/                          # Gradle wrapper
```

## 安全与数据

- 登录 Cookie 只保存在本机，使用 Android Keystore 中的 AES-GCM 密钥加密。
- 不提交 `local.properties`、签名材料、APK/AAB、抓包、截图或本地研究数据。
- `research/`、`captures/`、`screenshots/` 和构建目录均由 `.gitignore` 排除。

## 已知边界

- 依赖抖音网页/客户端接口的可用性，接口变化可能导致登录或播放失效。
- 当前没有自动化设备测试，遥控器焦点和真实设备播放需要人工验证。
- 尚未配置 release 签名；本项目不提供生产发布包。

## License

未声明开源许可证。未获得明确授权前不得对外分发。
