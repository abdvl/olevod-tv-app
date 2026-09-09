# v0.2 分类榜单与评分位置独立回归

## 最新结论：最终限定 8 项全部通过

Google Chromecast with Google TV 实机已保留数据安装 **0.2.0 / versionCode 2** 开发包。最终限定复测 **OK (8 tests)**，78.122 秒，无失败或跳过：评分徽标1项、Mini3项、Root3项、真实公开VIP目录1项。先前首轮22项的21通过与1项测试语义错误分别保留在下文；不声称最终包重新执行完整22项。

- 最终应用 SHA-256：`2a9017f3a56481db2c88ea91036347019e92342d07a5808ed58a41e3099b23e0`。
- 最终测试 APK SHA-256：`66a0024d8c236b5729a1ec1a14568f6d8c7c8da49fe24843761e47d87a93ef3b`。
- 原始输出：`.tools/v02-ui/instrumentation-version2.txt`；构建：`build-version2.txt`；校验：`SHA256SUMS-version2.txt`。
- 测试结束后启动未携带 preview 参数的正常应用首页，设备读回 versionName=0.2.0/versionCode=2；未截取私人历史。实机控制权交还主代理，未操作发布验证模拟器 emulator-5556。
- 主代理独立采集的真实 VIP 全部年份及两行海报截图见 [实机截图说明](followup-screenshots/README.md)。这些截图来自首轮冻结应用；最终包版本元数据变化单列，不冒充重新拍摄。


## 范围

用户要求每榜前二突出、后十部独立普通海报分两行五列；评分移到海报右上角，不再占用标题宽度。VIP 分类明确采用全部年份，其余分类采用当年。

本轮只运行明确筛选的测试，未运行全量 instrumentation；公开 VIP 目录测试显式启用 liveCatalog=true，不读取网站账号或发起收藏/历史修改。测试安装使用 `install -r` 保留应用数据。

## 构建与首轮反例

- 首次测试编译失败来自新断言使用不存在的 DpRect.width，改为 right-left；生产代码未修改。原输出 `.tools/v02-ui/build.txt` 保留。
- 修正后 assembleDebug、assembleDebugAndroidTest、testDebugUnitTest、lintDebug 成功。47 项 JVM 测试，0 失败、0 错误、0 跳过。
- 首轮冻结应用 SHA-256：`6c5950bfc7b3d4275c6f053c28d756690ba1c16164a2151a2c6c3c72e91c3bcb`；测试 APK：`54ef8d4ad9b913d35bc110a1104cf548f6e3053c93bc3b4363fc58014fe4b4b4`。
- 实机 Google Chromecast with Google TV，ADB 指定 `192.168.128.86:45053`。

## 首轮 22 项结果

152.711 秒，**21 通过、1 失败**。失败不是评分位置错误：新测试的徽标边界、右上角、长标题宽度、无评分隐藏断言均通过，最后预期原始 Text=9.3 的节点数量为1失败。生产通过 clearAndSetSemantics 将其替换为“评分 9.3”描述，因此原始 Text 不存在。测试修正为徽标 tag 唯一、描述正确，且没有旧标题行的裸评分 Text；未放宽位置或宽度检查。

| 测试类 | 数量 | 首轮结果 |
| --- | ---: | --- |
| V2FoundationUiTest | 4 | 3通过，1项测试语义断言需修正 |
| V2MiniHomeUiTest | 3 | 全通过 |
| V2HomeUiTest | 3 | 全通过 |
| V2CatalogUiTest | 4 | 全通过 |
| V2SearchUiTest | 4 | 全通过 |
| V2RootJourneyUiTest | 3 | 全通过 |
| V02VipMiniCatalogTest | 1 | 真实公开目录通过，无跳过 |

Mini 覆盖两榜各12个稳定影片ID、前二不重复为普通卡、后十部准确两行五列、末行文字可见、短榜/空榜入口和浏览全部返回；VIP验证全部年份文案及真实目录至少12个不同影片。Foundation保留三种图形比例四角像素检查，验证原图Fit仍完整。

原始构建日志、冻结APK、校验及 instrumentation 输出在本机忽略目录 `.tools/v02-ui/`。首轮结束已明确交还实机；最终版本元数据更新后仅按主代理指定复测8项，不宣称最终包完整重跑22项。

## 最终限定复测

已执行：评分徽标1项、Mini3项、Root3项、真实VIP目录1项，共8项，全部通过。首轮失败记录不覆盖，详见文首最终结果。
