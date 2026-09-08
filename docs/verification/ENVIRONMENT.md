# 独立验证：环境与风险

2026-09-07。验证 agent 独立于主实现；未读取测试凭据。

## 环境发现

- macOS arm64，Android Studio 已安装，内置 JBR 25.0.3；系统未注册独立 JDK。
- 本机 SDK 初始只有 platform android-37.0、build-tools 36.0.0、emulator 与 platform-tools 37.0.1。
- 初始无 AVD/系统镜像，无已连接 Android 设备。ADB 本地服务需沙盒提升权限后可运行。
- Google 官方镜像清单没有 API35 Android TV arm64；选择 API34 Android TV arm64 用于功能测试。
- 主实现采用单独 JDK17、Gradle8.11.1、AGP8.9.2、compileSdk35；需实际编译证明版本组合。

## 验证顺序

1. 独立 assembleDebug / 单元测试 / lint，检查 TV manifest、无触摸依赖和凭据忽略规则。
2. 安装模拟器，冷启动截图检查 16:9、中文、焦点边框、安全边距；以 D-pad/确定/返回遍历首页、筛选、搜索、播放、历史、登录。
3. 验证焦点无陷阱、按键无重复消费、切页/分页/返回恢复焦点、IME 先关闭后返回。
4. API 验证覆盖业务 code、空结果与错误区分、分页去重、取消旧搜索、筛选单变量对照。
5. 播放验证标准 VOD/VIP/直播/回看，检查 ±30s、倍速、切全屏保持位置、离开暂停、媒体键、历史续播。
6. Chromecast 真机保留验收：4K/HD 设备确认、解码/HDR/音频、30分钟播放、性能与内存。模拟器不替代真机结论。

## 关键风险 / 未验收闸门

- 登录目前只有网站源码证据，原生验证码、会话和过期恢复未证实。
- 全部云历史、进度单位、分页、同步冲突仍未确定；不得以本地历史称全部云端记录。
- 筛选路径部分位置未实测；UI 不能假装筛选已应用。
- 拼音缩写尚无支持证据；中文输入必须可用。
- HTTP 媒体 URL 与 CDN 跳转策略需要原生验证，凭据不得跨域转发或写日志。
- VIP 单样本浏览器 HLS 成功不证明所有片源/格式原生支持。
- 直播 seek/倍速必须按实际 DVR 能力开放，赛事不可用频道结果替代。
- UI 原型、编译成功、截图通过、真实接口通过、真机通过分别记录，不混淆完成等级。

## 环境准备结果

- SDK 工具 v23 的 sdkmanager 改用 Android CLI bootstrap，在本机被系统终止。稳定版 v19 安装成功；`.tools/android-sdk/cmdline-tools/latest` 指向 19.0。
- 使用既有 SDK license、关闭 stdin，正常 sdkmanager 安装了 API35/platform、build-tools35 与 API34 TV ARM64 镜像，无自动接受新条款。
- AVD：`.tools/avd/Olevod_TV_API34.avd`，Google `tv_1080p` 配置，1920×1080 / 320dpi。
- 模拟器使用本机 emulator、swiftshader、无窗口启动；ADB 序列号 `emulator-5554`。
- APK 与真实功能尚待主实现就绪；本报告不构成功能验收。
