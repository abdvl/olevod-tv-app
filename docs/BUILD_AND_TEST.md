# 构建、安装与恢复

## Android Studio

打开仓库根目录，等待 Gradle Sync。当前固定 AGP8.9.2 / Gradle8.11.1 / Kotlin2.1.20 / SDK35 / JDK17。

这台电脑的 Gradle JDK 请选 `.tools/jdk17/Contents/Home`，SDK使用 `.tools/android-sdk`。Android Studio内置JBR25不用于这个固定工具链。`.tools`与`local.properties`不提交；换电脑时通过Android Studio安装SDK35/build-tools35并配置本机JDK17。

选择 `app` 运行配置和 Android TV 模拟器或 Chromecast，再点 Run。当前TV模拟器：API34 ARM64、1920×1080、320dpi。所有界面是原生Compose，没有WebView播放替身。

## 命令行

在仓库根目录：

```sh
source scripts/android-env.sh
./gradlew assembleDebug testDebugUnitTest lintDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.olevod.tv/.MainActivity
```

APK：`app/build/outputs/apk/debug/app-debug.apk`。它是可调试的个人测试版本，未作为商店/正式签名发布。

数据库集成测试：

```sh
./gradlew assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -e class com.olevod.tv.StorageIntegrationTest com.olevod.tv.test/androidx.test.runner.AndroidJUnitRunner
```

## 测试入口

debug可用 `--es screen home|browse|search|player|live|history|favorites|account` 直接开页；`--ez preview true` 展示明确标注的界面样本。正常启动使用真实API。release不接受这些调试入口。

登录集成测试是可选测试：在用户提供当前验证码或授权本次识别后，把本地`.secrects`账号通过`run-as`标准输入注入应用私有临时文件。测试先删除临时文件，再请求登录并验证Keystore加密保存。测试需显式 liveLogin=true；原生验证码测试会等待输入，缺输入或未完成登录不能当作通过。密码不进入APK、Git、命令行参数或测试输出。

## 已验证与限制

- 真实普通点播、搜索、美国筛选、精确±30秒、1.5倍速、全屏返回、后台暂停：有独立agent报告。
- 25条历史、数据库重开、账号隔离：模拟器测试通过。
- 账号原生验证码登录、跨进程加密会话、VIP原生播放、影视收藏增删、云历史读取/实际观看同步回读已通过。
- 频道取消收藏返回code0后等待60秒仍未生效；App提示未确认，不假更新状态。影视收藏通过。
- 独立agent确认会员授权源CCTV13、东方卫视（重试恢复）及午夜新闻回看均实际原生播放。游客/临时媒体源可能报错，可重试。
- 模拟器无音频输出，不代替Chromecast音画同步、4K/HDR、音频格式、内存和长时间播放测试。
- 实机型号为Google Chromecast with Google TV，4K/HD具体版本仍未确认；界面按1080p TV验证。

## 实机验收

使用Android Studio的设备配对或已配置的ADB连接实机。应用安装后检查遥控器所有入口、点播/VIP/两直播频道/回看、返回焦点、续播、待机恢复及至少30分钟连续播放。请以设备协商出来的解码能力为准，不把网站“蓝光”栏目名称当成4K/HDR保证。

## 暂停后继续

先读 `PROGRESS.md` 当前状态表，再看 `git status --short` 和最近提交，恢复未验收项。截图在 `docs/verification/screenshots/`；中间可批注截图在本机 `artifacts/screenshots/`。
