# Project Instructions

## Agent Quick Card

- **Official source entry:** `app/src/main/java/com/dongpiandi/douyintv/MainActivity.kt`
- **Build:** `.\gradlew.bat :app:assembleDebug`
- **Test:** `.\gradlew.bat :app:testDebugUnitTest`
- **Static check:** `.\gradlew.bat :app:lintDebug`
- **Public repository:** `https://github.com/EDwinpzy/douyin-tv`（`origin`）
- **Generated / ignored:** `.gradle/`, `.kotlin/`, `**/build/`, `local.properties`, `captures/`, `research/`, `screenshots/`
- **Release / signing gate:** release 签名材料存放于仓库外（`%USERPROFILE%\.android\douyintv-release`），通过 `DOUYINTV_KEYSTORE_PROPERTIES` 或根目录 `keystore.properties` 注入；签名材料与 APK/AAB 不入库
- **Do not modify:** `main` 发布历史、`app/build/`、Android Keystore 导出材料、任何密钥或 Cookie

## Project boundaries

- **Official product:** `app/`，包名 `com.dongpiandi.douyintv`
- **Experimental / legacy / verification entries:** 无
- **Multi-stack boundary:** 单一 Android TV 应用；`app/src/main` 为唯一源码入口

## Working rules

- 使用 JDK 17 和 Android SDK 35。
- 改动 Kotlin/Compose 后至少运行对应模块的编译与单元测试。
- 涉及登录、Cookie、签名或接口参数时必须检查敏感信息，不得打印或提交真实凭证。
- TV 可见交互变更需要补充 D-pad 焦点和返回键验证；条件不足时明确标记未验证。
- 推送 `main` 前必须确认票据、Cookie、签名材料和构建产物未进入暂存区。
