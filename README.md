# Douyin TV（非官方）

面向 Android TV 与投影设备的第三方抖音客户端，使用 Kotlin、Jetpack Compose
和 AndroidX Media3 原生实现。应用只保留两条主路径：遥控器刷推荐短视频和观看
直播，并支持使用抖音 App 扫码登录自己的账号。

> 本项目与抖音/字节跳动没有关系，不提供或重新分发抖音内容。请阅读
> [免责声明](DISCLAIMER.md) 后再使用。

## 功能

- 推荐短视频：上下切换、暂停/继续、前后快进、自动连播和失败重试；播放中控制层
  4 秒无操作自动隐藏，任意遥控按键重新显示。
- 直播广场：响应式三列/四列网格，按 OK 进入直播间，返回后恢复原卡片焦点。
- 直播播放：支持 HLS/FLV 地址，竖屏直播保持原始比例并显示黑边，不拉伸。
- 账号登录：WebView 仅承载抖音官方扫码登录页，成功后使用自己的会话获取推荐。
- 账号菜单：显示登录状态并支持退出登录。
- TV 交互：全局 D-pad 焦点、分层 Back、全屏横屏和播放时保持亮屏。

短视频和直播均由原生 ExoPlayer 播放，不存在把完整抖音网页放进 WebView 的套壳
界面。

## 遥控器操作

| 场景 | 按键 | 行为 |
| --- | --- | --- |
| 通用 | 方向键 / OK | 移动焦点 / 激活当前按钮 |
| 推荐页 | 上 / 下 | 上一条 / 下一条视频 |
| 推荐页 | OK、Enter、播放暂停键 | 暂停或继续 |
| 推荐页 | 左 / 右 | 后退 / 前进 5 秒 |
| 推荐页 | Menu，或第一条视频再按上 | 显示顶部“推荐/直播/账号”导航 |
| 推荐页 | Back | 先收起顶栏，再退出应用 |
| 直播广场 | 方向键 / OK | 选择卡片 / 进入直播间 |
| 直播间 | OK、播放暂停键 | 暂停或继续 |
| 直播间 | Back | 返回直播广场并恢复原卡片焦点 |
| 登录页 | Back | 关闭登录并回到原页面 |

物理遥控器的厂商按键序列与长按行为尚未验证，详见下方 [验证状态](#验证状态)。

## 环境与构建

要求：

- Windows PowerShell
- JDK 17
- Android SDK Platform 35
- Android TV / 投影设备或模拟器，Android 8.0（API 26）及以上

构建和检查：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Debug APK 位于 `app\build\outputs\apk\debug\app-debug.apk`。安装并启动：

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.dongpiandi.douyintv/.MainActivity
```

当前只配置 Debug 构建，没有 Release 签名。仓库与 README 均不提供生产 APK；
未经重新签名和完整验证，不应把 Debug 包用于公开分发。

## 原生边界

- **界面**：Jetpack Compose，按电视横屏设计，无手机触摸布局。
- **短视频/直播播放**：Media3 ExoPlayer，使用 HTTPS 媒体地址。
- **登录**：WebView 加载官方 `https://www.douyin.com/user/self` 页面，桌面 UA；
  检测到有效会话 Cookie 后关闭。
- **数据**：不包含自建后端，不上传会话；请求直接发往抖音相关服务。
- **会话**：Cookie 由 Android Keystore 中的 AES-GCM 密钥加密后保存在本机。

项目没有内置抖音 APK、反编译产物、抓包数据、账号 Cookie 或签名材料。媒体地址
和请求参数可能随抖音服务变化，详见 [架构说明](docs/architecture.md)。

## 项目结构

```text
douyinTV/
├── app/src/main/java/com/dongpiandi/douyintv/
│   ├── MainActivity.kt              # TV 全屏入口
│   ├── data/                        # 设备标识与会话加密
│   ├── network/                     # 接口、签名、媒体数据源
│   ├── model/                       # 内容模型
│   └── ui/                          # Compose 页面、状态与主题
├── app/src/main/res/                # 主题、图标与 TV 清单资源
├── docs/                            # 架构、ADR 与文档索引
└── gradle/                          # Gradle wrapper
```

更多文档见 [docs/README.md](docs/README.md)。

## 验证状态

验证日期：2026-11-07。仅使用 AOSP TV 模拟器，没有触碰投影仪或实体电视。

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| Debug 构建 | PASS | `:app:assembleDebug` 成功 |
| JVM 单元测试 | PASS | 2/2 通过 |
| Android Lint | PASS | 0 errors，13 warnings |
| 模拟器崩溃缓冲 | PASS | 未发现应用 FATAL |
| 1280×720 短视频 | PASS | 切换、暂停/继续、顶栏导航 |
| 控制层自动隐藏 | PASS | 720p/1080p 播放中隐藏，按键唤出，暂停时保留 |
| 1280×720 直播 | PASS | 三列网格、竖屏等比播放、返回焦点恢复 |
| 1920×1080 短视频 | PASS | 布局与播放正常 |
| 1920×1080 直播 | PASS | 四列网格与直播播放正常 |
| 登录弹层适配 | PASS | 720p/1080p 居中，二维码完整可见，刷新可恢复 |
| 真实手机扫码与登录完成 | NOT_RUN | 未使用真实账号完成扫码、Cookie 持久化、退出登录复测 |
| 实体遥控器与 OEM 长按 | NOT_RUN | 仅验证模拟器按键注入，不等于硬件遥控器证据 |
| 投影仪 / S621 | NOT_RUN | 按本轮要求未进行设备实测 |
| Release 签名与生产包 | NOT_RUN | 未配置签名，只有 Debug 构建 |
| 长时间播放、弱网、并发切换 | NOT_RUN | 尚无持续运行与网络扰动测试 |

## 已知限制

- 依赖抖音网页接口和签名逻辑，服务端调整后可能需要更新代码。
- 二维码依赖官方登录页 DOM；页面结构变化可能影响自动展开和定位。
- 当前不包含评论、点赞、收藏、关注、搜索、发布或私信功能。
- 没有自动化 TV UI 测试，也没有跨厂商设备矩阵。

## 安全

不要在公开 Issue 中提交 Cookie、带凭证的日志或设备信息。更多说明见
[SECURITY.md](SECURITY.md)。

## 许可证

本项目使用 [MIT License](LICENSE)。改编的 `abogus@2.1.1` 签名实现及其 MIT
许可证全文见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
