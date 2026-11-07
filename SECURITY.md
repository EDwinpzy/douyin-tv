# Security Policy

## Reporting

请不要在公开 Issue 中粘贴 Cookie、账号信息、完整日志中的请求头或设备凭证。
如仓库已启用 GitHub 私密漏洞报告，请优先使用该入口；否则通过仓库所有者的
GitHub 主页请求一个私密沟通渠道。

## Session handling

- 登录 Cookie 使用 Android Keystore 中的 AES-GCM 密钥加密后保存在应用私有
  存储中。
- 应用不把会话上传到自建服务器，也不包含遥测或广告 SDK。
- 退出登录会清除加密的 Cookie，并删除对应的 Keystore 密钥。
- 已获得 root 权限、调试权限或控制设备的攻击者仍可能读取应用数据；本项目的
  加密不能替代可信设备与安全的系统环境。

## Scope

本项目不处理支付、密码代填、账号找回或第三方登录插件。请只从本仓库源码构建，
不要使用来源不明的预编译 APK。
