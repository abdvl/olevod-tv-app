# v0.2 发布验证与新版截图

2026-09-08（America/Los_Angeles）。**最终原签名包 `3018708e…` 已完成构建、保留数据安装和实际网络错误恢复验证：重试按钮遥控可达，恢复网络后返回“魔女”36部结果并聚焦首卡，Back回输入框。47项release JVM通过，lint无错误。[已发布 v0.2](https://github.com/abdvl/olevod-tv-app/releases/tag/v0.2)，公开附件与本地冻结文件逐字节一致。** 模拟器网络已恢复原值，应用已显式退出至系统Launcher。

最终包信息：

| 项目 | 最终结果 |
| --- | --- |
| 生产代码 | `1d10dbfe1ab92d49d0053725b7b611d27130c5fc` |
| APK | `artifacts/releases/v0.2/olevod-tv-v0.2.apk` |
| SHA-256 | `3018708ebb6b1e73d62f7f91fa56b3b0f7dd3c86215e5e923dd3f703108141c5` |
| 大小 / 版本 | 14,341,714 bytes；`0.2.0` / versionCode 2 |
| 原签名 | 证书 `db2d039b4b6685678c5c71e397df7146d5e3bac038d438d55f496db1cdd060f4`；APK v2 scheme验证通过 |
| 最终构建 | `bash scripts/build-release.sh v0.2`，8秒；47 JVM、0失败/错误/跳过；lint 0 Error / 0 Fatal、21 Warning |
| 安装包回读 | 已从5556安装路径拉回base.apk，SHA与最终产物完全相同；非debuggable |
| 最终实际复验 | 首页旧03:08测试记录保留；MN联想、首屏错误、Right/Back/重试恢复、正常结果、返回输入及退出 |

首页、搜索联想和搜索结果3张主图已由最终包重拍；其余9张保留相同布局的c43候选截图，逐图来源见manifest。普通播放及其余页面证据归属于c43；没有声称最终301包重跑全部页面、全部自动化或长播。

## 第一候选包与原资产保全

| 项目 | 值 |
| --- | --- |
| 生产代码 | `6e2d907bf94e2e45e497962837acf2482a069f70` |
| 应用 | `com.olevod.tv` / versionName `0.2.0` / versionCode `2` |
| APK SHA-256 | `c43c25698910ec41bea78af4bc62725ca63adfe2edc09b601c77fe91e5ad8d86` |
| APK 大小 | 14,341,714 bytes |
| 原发布证书 SHA-256 | `db2d039b4b6685678c5c71e397df7146d5e3bac038d438d55f496db1cdd060f4` |
| 构建 | `bash scripts/build-release.sh v0.2` 成功，8秒 |
| Release JVM | 47项，0 failure / 0 error / 0 skipped |
| Release lint | 0 Error / 0 Fatal，21 Warning |
| 签名校验 | APK v2 scheme 验证通过，一个签名者；证书与v0.1完全相同 |
| Release属性 | 非debuggable；minSdk26、targetSdk35、Leanback入口；4种ABI |

构建使用原签名配置，未读取或输出其内容，没有重新生成密钥。已审查发布脚本要求显式版本标签、核对应用版本并拒绝覆盖旧目录。

原 `artifacts/releases/v0.2/` 在确认目标不存在后完整重命名为 `artifacts/releases/candidates/v0.2-before-backdrop-001c33cf/`，然后才构建本轮候选。原报告 [RELEASE_V0_2.md](RELEASE_V0_2.md) 和原截图保留，未改写旧结果。

构建前后核对原资产与归档资产相同：

| 资产 | SHA-256 |
| --- | --- |
| v0.1 APK | `72291aa02800eb599f9d9261e94a39586eb77880be88b15373772f1c5d845e3d` |
| v0.1 SHA256SUMS.txt | `f014ebb18d73e93d7509e915ecaa8ef43bd51621afa7180e146e523a254feb84` |
| 归档001c候选APK | `001c33cf20736cbe675347e1e5a13c56d2eb6eca72796fcf504432723a8d89ab` |
| 归档001c候选SHA256SUMS.txt | `d36f55b996090bcba68bc6cddcc91cb8369b6ac0f45d44d7abd0f95fe82a5c2a` |

最终搜索修复构建前，c43目录也完整归档至 `artifacts/releases/candidates/v0.2-before-search-retry-c43c2569/`，确认目标不存在且未覆盖。其APK及校验文件、上述v0.1和001c各资产在最终构建前后再次逐项核对相同，记录在 `.tools/v02-final-release/search-fix/before-build-assets.json`。

## 专用模拟器覆盖安装

仅使用 `Olevod_Release_v01` / `emulator-5556`，Android TV API34、arm64-v8a、1920×1080、density320。没有操作Chromecast、开发模拟器5554、真实账号或凭据；没有卸载、clear或wipe数据。

本轮从此前001c候选 `install -r` 覆盖为c43候选，返回 `Success`。从安装路径拉回的 `base.apk` 与c43构建产物SHA完全相同。安装前后 `appId=10122`、`dataDir=/data/user/0/com.olevod.tv`、`firstInstallTime=2026-09-07 22:24:44` 均未改变；设备输出的 `lastUpdateTime` 从 `2026-09-08 20:37:46` 更新为 `2026-09-08 21:25:31`。

首页和历史页都保留《黑猫和魔女的课堂》第1集02:01的既有烟测记录。本轮实际从该记录继续播放到约03:07，返回后保存为03:08。旧记录来自专用模拟器的发布烟测，来源已由 [v0.1报告](RELEASE_V0_1.md)、[原v0.1历史截图](../screenshots/09-history.png) 及前一轮升级报告交叉确认；不是用户真实账户历史。此证据支持本次样本数据保留，不外推为全部用户凭据迁移已经测试。

## 第一候选实际旅程与首次问题（c43）

| 范围 | 本轮证据 |
| --- | --- |
| 首页 | 默认首页焦点；单行导航、隔离最近播放、横幅叠加大标题的精选推荐 |
| 电影小首页 | `2026 人气最高 / 前12部`；Top2完整竖海报与柔化模糊背景实际加载；评分在海报右上角 |
| VIP小首页 | `全部年份 人气最高 / 前12部`；Top2完整海报与模糊背景可见，无错误“当前年份”标注 |
| 目录 | 电影目录实际4125部；真实海报、评分、年份和地区；排序面板与更多筛选都可打开及返回取消 |
| 搜索联想 | 遥控键盘输入MN后，魔女、眠等真实联想显示 |
| 搜索首次失败 | 确认魔女后联想和搜索均出现网络连接失败；该轮没有首结果，因此未计为成功 |
| 首次错误焦点缺口 | 零结果错误状态从联想词按Right无变化，不能进入结果栏重试按钮；源码 `enterResults` 只处理非空列表，问题已交主代理修复 |
| 新查询 | 从真实热词选择凡人修仙传后返回3部结果，首卡聚焦；Back准确回到输入框。该成功不抵销前面的错误焦点问题 |
| 普通播放 | 从隔离历史续播，实际视频画面和时钟推进：02:02→02:15→02:37→03:07；总长23:40 |
| 播放控制界面 | 全屏默认聚焦，8个按钮与十集分组选集可见；本轮没有重新执行全部快进、倍速、收藏操作 |
| 全屏 | 正常进入；Up隐藏、Down恢复覆盖控制栏；显示1280×720及峰值2.26Mbps；Back回普通播放器，再Back回历史来源 |
| 历史 | 单条隔离烟测记录含完整海报、元数据与观看进度，播放后更新为03:08 |
| 账号 | 未登录页面、账号密码空白、验证码图片与数字键盘完整；未输入、识别填写验证码或提交登录 |
| 退出 | 默认继续观看；确定取消成功；再次打开后右移退出应用并确定，实际前台为系统TV Launcher |

首次网络失败及无焦点变化保存为 `.tools/v02-final-release/search-results-ready.*`、`search-error-right.*`。再次确认相同词不会重新加载已有失败feed；这不是一次成功重试，不计为网络恢复证据。随后新关键词成功，说明该次失败不能直接归为持续站点不可用。本轮未修改生产代码或删除失败现场。

导航采集器的一次保护性停步也保留：进入历史页后实际焦点为顶部搜索，采集脚本先前预期“此设备”，因此停止后续键；重新读取实际焦点后按Down正常进入记录。它是采集脚本预期错误，不作为产品断言失败或默认历史标签焦点通过。

## 最终搜索修复与实际恢复复验（301）

最终包以 `install -r` 覆盖c43成功。`appId=10122`、dataDir与 `firstInstallTime=2026-09-07 22:24:44` 均保持；设备输出的lastUpdateTime更新为 `2026-09-08 21:49:46`。冷启动仍显示03:08的同一隔离测试记录。

本次只进行一次有界的隔离网络故障注入。只读检查发现5556网络实际走eth0，Wi-Fi未连接，因此没有改动Wi-Fi、宿主网络或真实设备。使用模拟器原生命令把该AVD临时限速到1000bits/s、延迟60000ms；脚本 `finally` 恢复无限速和0ms延迟，并在退出时再次读取确认恢复。没有第二次故障注入或扩大网络控制调查。

实际生产界面路径：

1. 在线输入MN，真实联想“魔女”出现；在受限网络下确认该词，观察到联想及首屏结果的网络失败。
2. 联想词按Right，焦点进入结果栏“重试”；Back回到“魔女”搜索输入框；再Right可重新进入“重试”。
3. 网络恢复后确定重试，真实36部结果返回，首张《黑猫和魔女的课堂》获得焦点。结果请求恢复不假称联想请求也已自动恢复。
4. 从结果Left进入“重试联想”，确定后焦点稳定交回键盘R；联想请求恢复，正常词条显示。再Right到“魔女”，Right回首结果。
5. 正常结果Back回输入框，保留关键词和结果；再返回首页、通过退出确认显式退出，前台为 `com.google.android.tvlauncher/.MainActivity`。

上述是实际release操作证据，不是debug fixture。原始记录位于 `.tools/v02-final-release/search-fix/`：`network-probe.log`、`network-probe-summary.json`、`network-before.log`、`network-limit.log`、`network-restore.log`、`network-final.log`、`network-error-right.*`、`network-error-back.*`、`retry-confirm.*`、`retry-suggestions-confirm.*`、`search-results.*`、`search-back-input.*`、`upgrade-summary.json`。

独立模拟器7项搜索UI测试及其首轮失败/修复过程另见 [搜索重试回归](v2/SEARCH_RETRY.md)。本报告没有把这7项当成本轮再次执行，也没有把一次实际手动网络旅程计为额外instrumentation用例。

## 12张实际截图

图片来自本版本发布验证期间的真实release安装。每张原样1920×1080 PNG均已单独使用 `view_image` 检查，来源包、生产提交、尺寸、字节数、图片及对应XML的SHA记录在 [manifest.json](../screenshots/v0.2/manifest.json)。XML与图片顺序采集，不是原子同帧，播放秒数可能相差1秒。

| 文件 | 内容 |
| --- | --- |
| [home.png](../screenshots/v0.2/home.png) | 最近播放与精选推荐；隔离烟测记录 |
| [category-movie.png](../screenshots/v0.2/category-movie.png) | 当年电影Top2及模糊背景 |
| [category-vip.png](../screenshots/v0.2/category-vip.png) | 全部年份VIP Top2及模糊背景 |
| [catalog.png](../screenshots/v0.2/catalog.png) | 实际电影目录 |
| [catalog-filter.png](../screenshots/v0.2/catalog-filter.png) | 更多筛选浮层 |
| [search-suggestions.png](../screenshots/v0.2/search-suggestions.png) | MN联想 |
| [search-results.png](../screenshots/v0.2/search-results.png) | 最终包魔女36部真实结果、首卡焦点；两类请求分别恢复 |
| [player.png](../screenshots/v0.2/player.png) | 普通视频实际画面、控制及选集 |
| [fullscreen.png](../screenshots/v0.2/fullscreen.png) | 全屏实际画面与覆盖控制栏 |
| [history.png](../screenshots/v0.2/history.png) | 仅专用模拟器测试记录 |
| [account.png](../screenshots/v0.2/account.png) | 未登录账号及数字验证码键盘 |
| [exit.png](../screenshots/v0.2/exit.png) | 默认继续观看的退出确认 |

该目录不放旧版截图、debug preview、真实凭据或用户账号信息。片名、评分、数量是当时站点采样值，会变化。海报下方尚未滚入的行允许只显示一部分，不能将其误称为当前聚焦卡片被裁剪；目录首行、Top2、搜索首结果及历史聚焦卡片均完整显示。

## 范围及交接

构建、签名、覆盖安装、PNG与新鲜XML、升级身份摘要均保存在本机忽略目录 `.tools/v02-final-release/`，最终修复包证据位于其 `search-fix/` 子目录。两轮 `build-summary.json`、`upgrade-summary.json` 和 `before-build-assets.json` 分别记录精确身份。截图manifest逐图标明3张301来源、9张c43来源，不把其余截图改标为新包。原图及旧报告均保留。

本轮确认视频画面与实际时钟推进，未验证听感、音画同步、4K/HDR、真实账号登录/VIP解码、云端写入或30分钟稳定性。此前真机证据仍按 [Chromecast报告](v2/CHROMECAST_V2.md) 的APK与范围归属，不等同于本次release包在实体电视上安装或长播。

最终验证结束已显式退出至 `com.google.android.tvlauncher/.MainActivity`，网络原值再次核对无变化，交回模拟器及构建控制权。本子任务没有提交、tag、推送或发布；远端发布与资产校验仍由主代理完成。

## 正式发布与公开下载校验

- 发布于2026-09-08 22:25:19 PDT（2026-09-09T05:25:19Z），GitHub release ID385245245；正式版本，非草稿、非预发布，页面标为Latest。
- `v0.2`标签指向`b9cc6dd6524afbb097fa3f12f87879dd5a2d4aea`，与发布时远端main及codex/ui-v2一致；原生产代码仍为1d10dbf。发布后的文档校验记录不改变标签或APK。
- 从公开地址匿名下载APK及SHA256SUMS.txt，两者均与本地冻结文件逐字节一致；GitHub返回的asset digest也匹配。APK SHA-256为`3018708ebb6b1e73d62f7f91fa56b3b0f7dd3c86215e5e923dd3f703108141c5`。
- [公开发布信息与文件摘要](v0.2-publication.json)仅记录公开元数据，不含凭据或浏览器会话。
