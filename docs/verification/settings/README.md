# 设置与应用更新

## 行为

头像左侧的齿轮图标打开设置页，首期只有“应用 / 检查新版本”。入口为纯图标，保留“设置”无障碍描述。遥控器下键进入检查按钮，上键回到设置入口，返回键沿用应用页面规则回到首页。

检查 `https://api.github.com/repos/abdvl/olevod-tv-app/releases/latest`，仅接受正式发布，数字比较版本，`v0.2` 与 `0.2.0` 相等。发现更新后展示版本、大小和最多六行更新说明，用户选择下载并安装。相同或较旧版本不会下载。

下载到应用私有 cache/updates，显示进度；校验附件大小、GitHub SHA-256（若存在）、APK 包名、签名、最低系统版本、内部 versionCode 递增及 versionName 与标签一致后，通过限定目录的 FileProvider 交给 Android 安装器。首次需要用户允许安装未知来源应用，返回后继续打开安装器，最终由用户确认安装。取消安装可以重试，拒绝权限、下载失败、限流、缺失或多个 APK 等都有说明。无需 GitHub token 或存储权限。

更新状态由独立 SettingsViewModel 管理，切换页面与 Activity 重建不会重启下载。系统杀死进程后重新检查并下载；不承诺后台持续下载或断点续传。未完成文件不会送入安装器，重试覆盖临时文件。系统缓存清理后可重新检查下载。APK 签名策略要求保持同一签名，不支持签名轮换。

## 扩展

- `SettingsContent` 仅渲染 `SettingsSection` / `SettingsEntry`，用稳定 key 保持选项身份。新增播放器分组及其行组件不需要修改更新服务。
- 后续开关、单选、多选等使用独立行组件；实际增加持久化选项时再接入统一偏好仓库（如 DataStore），由播放器订阅，避免将播放器偏好耦合到更新状态。
- `GitHubUpdateRepository` 负责发布解析与下载，`SettingsViewModel` 负责状态，设置页面负责导航和系统安装授权。

## 发布约定

每次发布递增 `app/build.gradle.kts` 中 versionCode，versionName 与 Release tag 对应（允许省略 patch）；上传一个使用原正式签名的通用 APK，GitHub 的 draft/prerelease 保持 false。保留 SHA256SUMS.txt 供人工验证；应用优先使用 GitHub asset digest。当前单 APK 规则可避免未来错误挑选 ABI 分包，拆分 APK 时需明确增加设备匹配策略。

参考：[GitHub Releases API](https://docs.github.com/en/rest/releases/releases)、[Android 安装授权](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls())。

## 验证记录（2026-09-11）

- 实时 GitHub：最新 `v0.2`，`olevod-tv-v0.2.apk`，14,341,714 字节；包含 SHA-256 digest。当前本地版本 0.2.0 / versionCode 2。
- assembleDebug、assembleDebugAndroidTest、testDebugUnitTest、lintDebug 通过；52 个单元测试无失败，含版本归一化、数字比较、预发布及错误附件拒绝、缺失 digest、HTTP 成功与错误处理。
- Android TV 模拟器 emulator-5554：SettingsNavigationTest、顶部导航完整遍历测试均通过（2/2）；设置按钮位于头像左侧，向下进入选项、向上回到入口。
- 模拟器实际点击检查，访问 GitHub 后显示“已是最新版本”；[界面截图](settings.png) 已人工查看，顶部图标及设置内容完整可见。
- 尚未执行真实更高版本的下载覆盖安装、安装授权往返及物理电视验收；当前正式 Release 与本地版本相等。正式发布前应在同签名旧版本上验证新包的安装确认、拒绝权限与取消安装后重试。
