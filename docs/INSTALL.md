# 安装欧乐 TV v0.2

当前为发布候选版，GitHub v0.2 下载入口待发布后生效；已公开版本仍是 [v0.1](https://github.com/abdvl/olevod-tv-app/releases/tag/v0.1)。本地候选包位于 `artifacts/releases/v0.2/`。

## 设备要求与下载

- Android TV / Google TV，Android 8.0（API26）及以上；需要互联网连接和遥控器。
- 已实测 Google Chromecast with Google TV（sabrina / Android14）。其他设备的解码、遥控器和系统安装界面可能不同。
- APK 是独立安装包，不是手机应用，也不是 Play 商店版本。
- 在 [v0.2 Release](https://github.com/abdvl/olevod-tv-app/releases/tag/v0.2) 的 Assets 下载 **olevod-tv-v0.2.apk**。不要把 GitHub 自动生成的 Source code 压缩包当成安装包。
- 同页的 `SHA256SUMS.txt` 可用于检查下载是否完整。

## 从 v0.1 正式版升级

v0.2 使用与 v0.1 相同的发布证书，版本为0.2.0、versionCode2。已安装原 v0.1 正式 APK 时，直接打开新版 APK 选择更新，或使用下方 `adb install -r`；无需卸载。升级会保留本机数据，登录会话是否仍有效取决于网站。

## 从已有开发调试版迁移

v0.2 使用专用发布证书；此前 Android Studio/ADB 安装的 debug 版使用开发证书。两者包名相同（`com.olevod.tv`），签名不同，**不能直接覆盖安装**。Android更新包需要兼容的签名身份，见 [应用签名说明](https://developer.android.com/studio/publish/app-signing)。

如果设备已有开发调试版：

1. 先确认需要保留的观看记录已出现在网站云历史中。应用没有本机历史/保存密码的导出功能；尚未同步的本机记录不能保证迁移。
2. 在电视的设置 → 应用 → 欧乐 TV 中卸载旧版。**这会清除本机历史、搜索记录、登录状态以及记住的账号密码。** 不会因此删除网站账号或网站已有的云历史/收藏。
3. 按下面任一种方法安装 v0.2，再在电视上登录。

已安装同一发布签名的版本时，后续同签名、更高版本可覆盖升级；通常会保留应用数据。维护者必须保留原发布私钥并递增 versionCode。

## 方法一：电视文件管理器安装

1. 在电脑或手机上下载 APK。
2. 将 APK 传到电视可访问的位置：例如局域网文件共享、电视文件管理器支持的文件传输方式，或设备支持的 USB 存储。Chromecast 是否能使用 USB 取决于所接设备/扩展坞。
3. 用电视上的文件管理器打开 APK。如果系统提示不允许安装，按提示仅为这次使用的文件管理器启用“允许安装未知应用”，再重新打开 APK。
4. 选择“安装”。完成后在电视应用列表中打开 **欧乐 TV**，也可将它加入常用应用。

不同 Google TV 系统版本的设置名称和位置可能不同，以电视实际提示为准。

## 方法二：通过电脑 ADB 无线安装

电视的配对码式无线调试需要系统支持（Android 官方要求 TV Android13及以上），没有此选项可用文件管理器安装或设备支持的USB调试。参见 [ADB 官方说明](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi)。

先在电脑安装 [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)。Android Studio 用户也可以使用 SDK 自带的 `platform-tools/adb`。

1. 在电视 设置 → 系统 → 关于 中，多次点击 Android TV OS 版本号，开启开发者选项。
2. 进入开发者选项，开启无线调试。电脑与电视连接同一局域网。
3. 在电视选择“使用配对码配对设备”，记下 IP、**配对端口**及临时配对码。
4. 在电脑终端执行（将占位符替换为电视当前显示的数值）：

```sh
adb pair TV_IP:PAIR_PORT
# 按提示输入电视本次显示的配对码
adb connect TV_IP:DEBUG_PORT
adb devices
adb -s TV_IP:DEBUG_PORT install -r olevod-tv-v0.2.apk
adb -s TV_IP:DEBUG_PORT shell am start -n com.olevod.tv/.MainActivity
```

**配对端口与调试连接端口是两个不同端口，且可能变化；不要固定假设为5555。** `DEBUG_PORT` 取无线调试主页显示的 IP 地址和端口。USB 调试已连接的设备可使用 `adb devices` 显示的序列号代替 `TV_IP:DEBUG_PORT`。

如果设备不提供无线配对选项，使用该设备支持的 USB 调试方式，或使用方法一。此应用不要求 root。

## 首次登录与遥控器

- 顶部人像图标进入登录页，在电视输入账号、密码及图片验证码；验证码过期时刷新后重试。
- 应用会在本机加密记住输入的账号密码，下次自动填写。可以在账号页清除已保存的凭据。
- 中文搜索可使用系统输入法或手机遥控输入；语音可用性取决于电视的系统输入法。
- 方向键移动绿色焦点，确认键进入/播放；返回键逐级返回。搜索页返回先聚焦输入框，再按返回退出搜索页。
- 首页按返回会出现退出确认，默认选中“继续观看”。选择“退出应用”并确认后才退出。

## 常见安装问题

| 提示/问题 | 处理 |
| --- | --- |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 已装版本与 APK 签名不同。先阅读上面的开发版迁移说明，再决定是否卸载旧版。 |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | 设备已有更高 versionCode。使用适合的更新包；不要为绕过提示而随意清除数据。 |
| 不允许安装未知应用 | 只为实际打开 APK 的文件管理器启用安装权限，按系统提示重试。 |
| 应用未安装/解析失败 | 确认是完整 APK、设备为 Android TV/Google TV 且 Android≥8.0；重新下载并校验。 |
| ADB 显示 offline / 找不到设备 | 检查网络、无线调试开关及当前连接端口，必要时重新配对。 |
| 网站可看但 APP 播放失败 | 核对登录/会员状态并重试。片源、服务器响应和设备解码能力都会影响播放。 |

## 校验下载

将 APK 与 `SHA256SUMS.txt` 放在同一目录。macOS/Linux 可执行：

```sh
shasum -a 256 -c SHA256SUMS.txt
```

Windows PowerShell 可执行下列命令，并与 `SHA256SUMS.txt` 中的值对比：

```powershell
Get-FileHash .\olevod-tv-v0.2.apk -Algorithm SHA256
```

v0.2 发布证书 SHA-256：

```text
db2d039b4b6685678c5c71e397df7146d5e3bac038d438d55f496db1cdd060f4
```

证书指纹用于识别签名，与 APK 文件的 SHA-256 不是同一个值。
