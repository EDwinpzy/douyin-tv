# 架构说明

## 目标与边界

Douyin TV 是一个原生 Android TV 客户端：短视频和直播画面由 Media3/ExoPlayer
播放，界面由 Jetpack Compose 构建。WebView 只用于抖音官方网页的扫码登录，
不承载推荐流、直播列表或视频播放。

项目依赖抖音网页端的公开请求与运行时可观察行为，因此它不是稳定协议客户端。
接口参数、签名、风控、登录页 DOM 和媒体地址变化都可能导致部分功能失效。

## 运行时结构

```text
MainActivity
  └─ DouyinTvApp
      ├─ ShortsScreen          Compose + PlayerView + ExoPlayer
      │   ├─ DanmakuOverlay    五轨弹幕画布
      │   └─ CommentsPanel     右侧评论抽屉
      ├─ LiveGridScreen        Compose 响应式网格
      ├─ LivePlayerScreen      Compose + PlayerView + ExoPlayer(HLS)
      ├─ LoginScreen           WebView，仅处理扫码授权
      └─ AccountOverlay        登录状态与退出入口
```

主要模块：

| Path | Responsibility |
| --- | --- |
| `ui/DouyinTvApp.kt` | 页面状态机、账号浮层、Back 分层 |
| `ui/MainViewModel.kt` | 推荐流分页、直播加载、登录会话、焦点恢复状态 |
| `ui/screens/ShortsScreen.kt` | 短视频播放、D-pad、暂停、进度与评论开关 |
| `ui/components/DanmakuOverlay.kt` | 跟随播放器时间轴的五轨弹幕 |
| `ui/components/CommentsPanel.kt` | 评论列表、焦点滚动与返回 |
| `ui/screens/LiveScreens.kt` | 响应式直播网格、直播间播放 |
| `ui/screens/LoginScreen.kt` | 官方登录页、二维码探测、弹层校正 |
| `network/DouyinApi.kt` | 推荐、评论、弹幕、直播与媒体地址请求 |
| `network/AbogusSigner.kt` | 请求参数签名 |
| `network/DouyinMediaSourceFactory.kt` | Media3 HTTP 数据源与请求头 |
| `data/SessionStore.kt` | Keystore AES-GCM 会话加密 |

## 页面状态与返回键

`DouyinTvApp` 是顶层状态机。Back 的优先级固定为：

1. 关闭登录页。
2. 关闭账号浮层。
3. 关闭直播间。
4. 关闭推荐页评论面板，同时收起顶栏并恢复视频焦点。
5. 收起推荐页顶栏。
6. 在根页面退出应用。

直播间关闭时，ViewModel 记录房间 ID，并在网格重新挂载后恢复焦点，而不是保存
旧的可组合对象或索引。

## 短视频链路

1. ViewModel 拉取推荐页并预取接近列表尾部的下一页。
2. `ShortsScreen` 将当前视频地址交给 ExoPlayer；地址失败时切换下一个候选地址。
3. 视频使用 `RESIZE_MODE_FIT` 保持原始宽高比，封面作为低透明度背景填充剩余区域。
4. 播放器错误会显示可聚焦的重试面板；视频结束自动切下一条。
5. 生命周期进入后台时暂停，恢复前台时仅在没有用户暂停意图时继续播放。
6. 切换到新视频时并行加载评论和官方弹幕；评论只在该视频的详情面板中展示。
7. 打开评论面板时暂停视频并隐藏控制层；关闭后恢复原播放意图，且控制层保持隐藏，
   视频焦点立即恢复。

## 评论与弹幕链路

1. 评论通过公开移动网页评论接口读取，使用遥控器进入右侧面板；最多展示约 10 条
   热门评论，包括正文、作者、IP 属地、点赞数和回复数。
2. 评论列表的每一行都是稳定焦点目标，上下键逐条移动并自动滚入可视区；Back 或
   右上角关闭按钮退出面板。
3. 官方弹幕通过桌面网页接口请求，并按播放器 `currentPosition` 投影到五条轨道。
4. 官方弹幕为空或请求失败时，使用最多 12 条热门评论生成时间轴弹幕。
5. 每条轨道同一时刻只绘制一条消息，文字和底色被裁剪在弹幕区域；评论面板打开时
   弹幕整体隐藏。

## 直播链路

1. 直播广场加载房间卡片。
2. 屏幕宽度小于 `800.dp` 使用三列，否则使用四列。
3. 打开房间后解析 HLS/FLV 地址，优先使用 HTTPS HLS。
4. 直播画面使用 `RESIZE_MODE_FIT`，竖屏直播保留黑边而不拉伸。
5. 返回网格后按业务房间 ID 恢复焦点。

## 登录与会话

- 登录地址为 `https://www.douyin.com/user/self`，使用桌面 UA。
- 页面加载后尝试展开官方扫码登录弹层，并周期性校正弹层位置与尺寸。
- 检测到包含 `sessionid` 或 `sessionid_ss` 的 Cookie 后，关闭 WebView 并保存会话。
- Cookie 使用 Android Keystore 中的 AES-GCM 密钥加密，存放在应用私有
  SharedPreferences。
- 退出登录会删除密文和 Keystore 密钥，并清除 WebView Cookie。

这里没有账号密码代填，也没有自建认证服务器。WebView 的权限和能力只服务于
抖音官方登录页。

## 响应式布局

界面以电视横屏为基准：

- 登录页使用左右两栏，二维码 WebView 弹层在可用区域内垂直居中。
- 短视频底部信息面板宽度随屏幕宽度变化，最大宽度受限，不再放置遥控操作提示。
- 弹幕区从顶栏下方开始，优先占用画面上部安全区域，不覆盖底部视频信息。
- 评论面板在小于 `1100.dp` 时占右侧约 55%，更宽屏幕占 40% 且最大 `820.dp`。
- 直播网格按 `800.dp` 阈值切换三/四列。
- 所有主要操作均有可见焦点环；顶部导航、卡片、错误面板和账号浮层可按 D-pad
  到达。

## 失败与限制

- 官方接口或签名变化可能导致推荐/直播请求失败。
- 评论接口的可用字段和数量由公开移动网页接口决定，可能少于网页端完整评论区。
- 没有官方弹幕的视频会使用评论生成弹幕，不等同于创作者的原始弹幕。
- 登录页 DOM 变化可能导致二维码弹层探测失败。
- 媒体 CDN 可能校验 Referer、Cookie 或短期签名，旧播放地址会失效。
- 当前没有离线缓存、自动更新、崩溃上报或远程配置。
- `0.1.0` 只构建 Debug 包，未配置 Release 签名。
