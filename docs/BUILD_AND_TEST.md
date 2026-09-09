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

APK：`app/build/outputs/apk/debug/app-debug.apk`。它是可调试的开发版本；GitHub发布使用下文的专用签名release构建。

数据库集成测试：

```sh
./gradlew assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -e class com.olevod.tv.StorageIntegrationTest com.olevod.tv.test/androidx.test.runner.AndroidJUnitRunner
```

## 测试入口

debug可用 `--es screen home|browse|search|player|live|history|favorites|account` 直接开页；`--ez preview true` 展示明确标注的界面样本。正常启动使用真实API。release不接受这些调试入口。

登录集成测试是可选测试：在用户提供当前验证码或授权本次识别后，把本地`.secrects`账号通过`run-as`标准输入注入应用私有临时文件。测试先删除临时文件，再请求登录并验证Keystore加密保存。测试需显式 liveLogin=true；原生验证码测试会等待输入，缺输入或未完成登录不能当作通过。密码不进入APK、Git、命令行参数或测试输出。

## UI v2 回归与运行截图

当前 `codex/ui-v2` 最初交付时的45项UI/状态、30项JVM、真实API/普通首帧与未覆盖分支见 [V2-10](verification/v2/V2-10.md)。后续 [Chromecast 真机验证](verification/v2/CHROMECAST_V2.md) 跨批次累计56个不同用例通过，最终修复包19项定向回归通过；完整JVM45项、lint与构建通过。VIP重登读取与真实解码通过；会话修复基线的30分钟播放采样、实际Home暂停及后台进程重建完成。现场听感／同步等仍待确认。模拟器、各真机批次与下面的旧版结果分别记录。

新版UI采用 `V2*` 测试类作为回归入口，精确类清单与结果见V2-10。`V2AccountReadTest` / `V2LivePlaybackTest` 分别需要显式 `liveLogin=true` / `liveV2=true`，不包含在45项夹具回归中。仓库保留旧版的UI测试和服务测试供历史参照，本轮没有执行整个未筛选的 instrumentation 包，也没有将其声明为全部通过。

`V2DevicePlaybackTest` 需显式 `liveDevicePlayback=true`，使用普通网络媒体与隔离历史，检查实际30/300秒seek、1.5倍速、全屏会话/位置、Activity ON_STOP暂停和释放；测试不会修改账号或收藏。它通过instrumentation临时采用媒体控制读取权限，结束后释放。不要把夹具测试、真实媒体测试和真实账号测试合为未筛选的全包运行。

[新版24张截图](verification/v2/screenshots/README.md) 通过 `--ez preview true` 运行正式页面组件，使用公开素材和虚拟状态。Android Studio运行默认入口使用真实服务；preview不是媒体播放成功的证据。

## v0.1 历史验证与限制

- 真实普通点播、搜索、美国筛选、精确±30秒、1.5倍速、全屏返回、后台暂停：有独立agent报告。
- 25条历史、数据库重开、账号隔离：模拟器测试通过。
- 账号原生验证码登录、跨进程加密会话、VIP原生播放、影视收藏增删、云历史读取/实际观看同步回读已通过。
- 频道取消收藏返回code0后等待60秒仍未生效；App提示未确认，不假更新状态。影视收藏通过。
- 独立agent确认会员授权源CCTV13、东方卫视（重试恢复）及午夜新闻回看均实际原生播放。游客/临时媒体源可能报错，可重试。
- 模拟器无音频输出，不代替Chromecast音画同步、4K/HDR、音频格式、内存和长时间播放测试。
- 实机已连接：Google Chromecast with Google TV，设备代号sabrina，Android 14，32位armeabi-v7a用户态；显示报告3840×2160、逻辑1920×1080。用户已确认普通点播画面和声音正常。片源4K/HDR和长时间稳定性另行验收。

## 实机验收

使用Android Studio的设备配对或已配置的ADB连接实机。UI v2指定基线已完成30分钟普通播放采样、实际Home与后台进程恢复；各APK散列和测试命令见上述报告。现场音画／同步、真实4K/HDR及完整性能目标继续待测。直播和回看进一步稳定性验收按用户要求暂缓。请以设备协商出来的解码能力为准，不把网站“蓝光”栏目名称当成4K/HDR保证。

## 暂停后继续

先读 `PROGRESS.md` 当前状态表，再看 `git status --short` 和最近提交，恢复未验收项。截图在 `docs/verification/screenshots/`；中间可批注截图在本机 `artifacts/screenshots/`。

## Wireless Chromecast development

Discover current ports with `adb mdns services`. Use `adb pair IP:PAIR_PORT`, enter the temporary code interactively, then `adb connect IP:CONNECT_PORT`. The ports are different and can change; do not assume 5555.

Always pass `adb -s IP:CONNECT_PORT` when the emulator is also connected. The private login helper accepts `ANDROID_SERIAL=IP:CONNECT_PORT`; it writes credentials to a temporary private file and atomically renames it only after transfer completes.

The supplied Chromecast has been paired, installed and logged in on the device. Read `verification/CHROMECAST_REGRESSION.md` for physical-device outcomes, separate from simulator results.

## 发布构建与签名

当前待发布版本名0.2.0、versionCode2，对应 GitHub tag `v0.2`；沿用v0.1发布证书。`release` 关闭调试入口，使用本机私有发布证书；debug 继续使用开发证书。各次发布结果分别记录在 `verification/RELEASE_V0_1.md` 和 `verification/RELEASE_V0_2.md`。

首次为自己的分发创建签名（只执行一次）：

```sh
source scripts/android-env.sh
python3 scripts/init-release-signing.py
```

脚本创建 `.secrets/olevod-release.jks` 和 `.secrets/release-signing.properties`，拒绝覆盖已有签名。**安全备份这两个文件；不要提交、上传或丢失它们。** 本仓库 `.gitignore` 排除整个 `.secrets/`。自行生成的新证书不能覆盖本仓库作者签名的安装包。

已有签名时，恢复备份而不是重新生成。构建并校验：

```sh
bash scripts/build-release.sh v0.2
```

输出到 `artifacts/releases/v0.2/`：`olevod-tv-v0.2.apk`、`SHA256SUMS.txt`。脚本检查tag与versionName匹配、拒绝覆盖已有版本目录，再执行release构建、release单元测试、lint与签名校验；临时产物完整后才移动到版本目录。没有签名配置时直接停止，不会生成新签名或自动上传文件。后续版本先递增versionCode/versionName，再使用对应tag构建；不得覆盖v0.1等历史产物。

首次从 debug 版迁移到发布签名 APK 需卸载旧版，会清本机数据；详见 [安装说明](INSTALL.md)。正式签名 APK 在独立干净 API34 TV 模拟器上验收，不为测试清除已有 Chromecast 的账号和历史。
