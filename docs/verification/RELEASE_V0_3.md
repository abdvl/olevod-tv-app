# v0.3 正式包验证

日期：2026-09-11（America/Los_Angeles）。

- 版本：0.3.0 / versionCode 3；tag：v0.3。
- APK：`olevod-tv-v0.3.apk`，14,375,134 字节。
- SHA-256：`419002360d85457f6c7572e26130b414134a5ded0ef5f5e5e8d087dce473aae8`。
- `bash scripts/build-release.sh v0.3` 完成 assembleRelease、testReleaseUnitTest、lintRelease；52 个测试无失败。apksigner 校验通过（APK Signature Scheme v2），沿用既有正式签名。
- 功能实现时已通过 debug 构建、52 个单元测试与 lint；SettingsNavigationTest 和顶部导航完整遍历（2/2）在 emulator-5554 通过。
- emulator-5556：升级前 versionName 0.2.0、versionCode 2，`adb install -r` 成功；升级后 0.3.0 / 3，firstInstallTime 保持 `2026-09-07 22:24:44`，lastUpdateTime 更新为 `2026-09-11 19:56:38`。未卸载或清除数据；未逐项审计全部私有数据。
- 正式版本不启用 debug 页面参数，通过顶部齿轮进入设置。设置页截图见 [v0.3 设置页](settings/settings-v0.3.png)。
- 应用内下载更高版本、首次安装授权往返及物理电视仍未验收；覆盖升级验证使用 ADB，不能等同于完整应用内安装流程。

[设置与更新设计](settings/README.md) · [发布说明](../releases/v0.3.md)
