# UI v2 独立验证计划

2026-09-08 · 基线提交 `141878d22acf3563e24eb27627bf0cd79aa6c495`。

对应 [设计规范](../../design-v2/DESIGN_SPEC.md)、[验收矩阵](../../design-v2/ACCEPTANCE.md) 和 [视觉参数](../../design-v2/tokens.json)。本文是独立验证的执行安排，不是测试通过记录。实现按 V2-01 至 V2-10 推进；这里只记录准备工作和后续门槛，不能用旧版通过结果代替新版验收。

## 1. 已核实的验证环境

- 仓库提供 `scripts/android-env.sh`，配置本地 JDK 17、Android SDK 和 Gradle 缓存；Android 模块使用 compileSdk 35、minSdk 26、Compose for TV、Media3。
- 本机 ADB 二进制可运行，版本 `1.0.41 / 37.0.1-15733141`。本次独立只读探测 `adb devices -l` 因沙箱不允许监听本机 5037 端口失败，不能由此推断没有设备。
- 主执行 agent 随后以沙箱提升方式确认 `emulator-5554`（`sdk_google_atv64_arm64`）在线；这是主执行 agent 的设备观测，尚不是本次独立设备验收。
- 当前模拟器由主执行 agent 用于 V2-01 构建和截图。独立验证须等其明确交接构建、设备和测试窗口后再执行，避免两个 agent 同时发送遥控按键、安装或改变配置。
- 本次准备未启动 Gradle，未安装、启动或控制设备上的 App，未读取 `.secrets`、`.secrects` 或原始会话日志。

## 2. 最关键的基线风险

下面区分源码已存在的旧行为与需要用运行测试验证的风险。它们不是声称已在 v2 构建中复现的缺陷。

| 优先级 / 阶段 | 基线证据 | 重构风险与验证门槛 |
| --- | --- | --- |
| P0 · V2-01 | `OlevodApp.kt` 当前同时渲染 `Header` 和 `Navigation`，分别使用 `homeFocus`、`navigationHomeFocus`、`categoryNavigationFocus` | 删除第二行后不能遗留未挂载的焦点目标。只保留一个首页；搜索在首页左侧；默认首页；正文上、Header 下闭环可达；直播不留入口。 |
| P0 · V2-01 | `HeaderIcon` 聚焦后 `animateContentSize` 展开文字 | 工具展开可能挤出右侧工具或短剧；检查每个工具聚焦状态的整体边界，而不是只拍默认首页。左右仅移动焦点，确定才切页。 |
| P0 · V2-01 | `PosterCard` 使用 `ContentScale.Crop`、焦点倍率 1.035、单行片名，调用方传入 `.72/.74/.78/1.5` 等比例 | 统一 2:3 / Fit 后卡片明显变高。不能只改图片参数而忽略父行、滚动和文字区域；完整竖图、图外元信息、整卡单焦点以及最底行边界都需核对。 |
| P0 · V2-01/02 | `HomeMovieGroup` 对整节及聚焦卡片延迟调用 bringIntoView；卡片自身也会请求滚入视口 | 高竖图使一节大于视口，节级与卡级滚动可能竞争。选中末行时必须以当前整卡的联合边界为准，不能回拉整节顶部。 |
| P0 · V2-02 | `SaveableStateProvider(screen)`、单个 `backScreen`、`LocalPosterFocus` 的 Long 值；最近播放以负 movieId 区分 | 同一影片在多节或两榜出现、分类切换、列表重新挂载时，不能恢复到错误副本。测试 route / section / entity / action 与滚动锚点，不用下标或标题代表身份。 |
| P0 · V2-02 | `CatalogFeed` 按 ID 合并并有 loading 守卫；不同 Filter/query 使用独立缓存实例 | 当前实例隔离通常可避免普通查询串屏，公共 feed 重构不能丢失这个保护；旧任务尚无显式取消/代次约束，缓存键也不包含账号。必须测试迟到响应、切账号和缓存复用。 |
| P0 · V2-02 | `CatalogFeed` 只根据空页或 `page.hasMore` 判断结束 | 服务器声明还有页但返回整页重复 ID 时会继续推进。必须有界处理，避免无限自动请求；失败不推进页号，重试不丢前页。 |
| P0 · V2-05 | 当前 `ConnectedBrowse` 将七行筛选与结果置于同一个 LazyColumn | 改为固定筛选条＋覆盖浮层后，旧的 LazyColumn 索引阈值和上下邻居不再适用。打开/取消保持背景，提交新条件回首行且焦点回触发器。 |
| P0 · V2-06 | `ConnectedSearch` 单一 query 同时驱动联想和结果，以 selectedSuggestion 触发首结果焦点 | 确认词后用户继续输入、返回、向其他区域移动时，旧请求不能抢焦点；首结果只在有效确认意图下聚焦一次，分页和图片补全不重复执行。 |
| P0 · V2-06 | 搜索三栏使用 FocusRequester group，现有词条含联想和结果标题合并 | 中栏无结果、结果为空、网络失败以及新建议替换旧建议时必须有合法退路；MN 联想不能被当作影片结果，返回先到输入框且不弹 IME。 |

源码入口：[OlevodApp.kt](../../../app/src/main/java/com/olevod/tv/OlevodApp.kt)、[ConnectedHome.kt](../../../app/src/main/java/com/olevod/tv/ConnectedHome.kt)、[ConnectedBrowse.kt](../../../app/src/main/java/com/olevod/tv/ConnectedBrowse.kt)、[CatalogFeed.kt](../../../app/src/main/java/com/olevod/tv/CatalogFeed.kt)、[ConnectedSearch.kt](../../../app/src/main/java/com/olevod/tv/ConnectedSearch.kt)、[AppViewModel.kt](../../../app/src/main/java/com/olevod/tv/AppViewModel.kt)。

## 3. 现有测试可用范围

| 测试 | 可复用价值 | 限制 / 处理 |
| --- | --- | --- |
| `CatalogFeedTest` | 已覆盖追加、重复调用守卫、503 保页、同页重试和末页停止 | 尚无迟到响应、账号/查询代次、整页重复、未知或异常 total 用例；V2-02 补充。 |
| `OlevodApiTest`、`OlevodAdapterContractTest` | API 纯逻辑和适配契约回归 | 使用本机 MockWebServer 可能需要本机端口权限；不等同真实网站验证。 |
| `PlaybackJumpTest`、`VideoTrackInfoTest` | 跳转 clamp 与实际轨道显示 | 可作为共享 UI 改动的便宜回归，不证明 Media3 实际播放。 |
| `PlayerVideoStageTest`、`PlayerKeyInputTest` | 无真实账号的组件几何和隐藏控制焦点回归 | 当前测试使用轻量视频占位；后续仍需真实播放显隐对照。 |
| `EpisodePickerTest`、`ExitConfirmationDialogTest` | 分组行为、返回、退出默认项 | 部分状态以 performClick/RequestFocus 建立，只能证明组件局部契约；不能替代整页遥控器路线。 |
| `HomeNavigationUiTest` | 旧首页深滚动重挂载与回程场景有参考价值 | 显式断言两层首页和直播，v2 后已过时；必须改写，不能以强行保留旧 UI 使旧测试通过。 |
| `SearchFocusUiTest` | 魔女联想、缓存重复确认、两次返回有参考价值 | 使用真实网络、performClick 和 RequestFocus 跳过部分路线；新增确定性纯 D-pad 用例作为主验收。 |
| `CatalogSortUiTest`、`MiniCategoryHomeUiTest` | 公共接口与界面契约补充 | live 开关默认未启用；旧筛选点击路径需更新。未启用是跳过，不是通过。 |
| 登录、收藏、历史同步、直播集成测试 | 后期已授权真实会话验证 | V2-01 不运行这些类；不因执行整个 androidTest 集合意外触发或改变持久化测试数据。直播仍不属 v2 开发范围。 |

测试根目录：[JVM 测试](../../../app/src/test/java/com/olevod/tv)、[Android 测试](../../../app/src/androidTest/java/com/olevod/tv)。

## 4. V2-01 最小验收批次

先在隔离的组件/测试 Activity 中验证新 Header 和 PosterTile，再验证 App 集成。现有 debug `preview=true` 使用旧的独立预览页面，不能默认视为新 Connected 页面真实行为；只有确认两条路径都使用新共享组件时，预览截图才可证明该共享组件的外观。

### 4.1 建议补齐的确定性测试

以下名称为建议，准备本计划时尚未存在，不应执行不存在的类后记录“零失败”。测试仅覆盖有回归价值的交互和几何，不为每个颜色常量单独写测试。

| 建议测试 | 最小数据 / 操作 | 必须断言 | 对应验收 |
| --- | --- | --- | --- |
| `UnifiedHeaderUiTest` | 从真实默认焦点连续左右经过所有项，逐项按确定；fixture 统计导航回调 | 唯一首页、搜索顺序、短剧最后一个分类；方向键不切页；工具展开无越界；没有第二行/直播 | NAV-01/02/03、LIVE-01 |
| `HeaderContentFocusUiTest` | 正文放两行可滚动假影片；首页下、正文上，重复十次；深滚动离开再返回 | 同一窗口仅一处焦点；没有未挂载 Requester 异常；Header 下目标可见 | NAV-04、HOME-03 |
| `PosterTileUiTest` | 本地 2:3、3:4、极长图片，四角有可识别标记；两行长中文标题；缺图后补图 | 原图四角完整且等比；图外文字；只一次点击目标；加载前后布局和当前焦点不变 | IMG-01/03 |
| `PosterGridBoundsUiTest` | 六列至少两页，末行不足六项；仅用 D-pad 到底部 | 图片/标题/元信息/外框的联合 bounds 完整在结果视口安全区；不因整节 bringIntoView 回跳；末行左右不落空 | IMG-02、R08 |
| `HeaderPosterIntegrationUiTest` | 从单行 Header 进入首页影片，打开占位播放器，返回原影片 | 原入口仍可打开，返回有可见焦点；至少覆盖最近节与普通节的同一 movieId | NAV-05 的基础烟测，完整恢复留 V2-02 |

- Fixture 使用合成 ID、片名、进度和本地图片，不写真实账号信息。Header 的 `contentDescription` 保留“首页”“搜索”等可用名称；同名副本使用稳定 testTag/语义身份区分。
- 遥控闭环从默认焦点起，只用方向、确定、返回。`performClick`、`RequestFocus`、`performScrollToIndex` 可以用于独立局部状态测试的准备，但不能用于绕过闭环待测步骤。
- 断言应读取实际 focused 语义和动作计数；颜色截图用于补充，不把绿色下划线误认为当前焦点。
- 图片完整性需截图四角检查，不能只检查 Compose 树上声明了 `Fit`。测量联合 bounds 时包含文字与外扩焦点，不能仅检查图片框。
- 基准 960×540 dp 对照实际设备 density；记录默认字体和 fontScale 1.3。大字体导航可横向滚入，仍不能出现第二行或不可达项。

### 4.2 首阶段可运行的现有命令

以下命令待主执行 agent 交接后运行，本次未执行。工作目录为仓库根目录。

```bash
source scripts/android-env.sh
./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

现有 JVM 测试可全跑；离线依赖缺失应记录环境失败，再由主执行者处理依赖，不标产品失败。如果主执行 agent 已对完全相同代码完成构建，只接收其提交/产物 hash 与构建记录，独立设备回归不必无理由重复全量构建。

交接时确认实际模拟器序列号与已安装构建后，限定运行不涉及账号的现有组件测试。`V2_TEST_SERIAL` 应由交接记录设置；不得把历史实机地址或序列号当作当前已确认值。

```bash
source scripts/android-env.sh
ANDROID_SERIAL="${V2_TEST_SERIAL:?先设置交接确认的模拟器序列号}" ./gradlew --offline :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.olevod.tv.PlayerVideoStageTest,com.olevod.tv.PlayerKeyInputTest,com.olevod.tv.EpisodePickerTest,com.olevod.tv.ExitConfirmationDialogTest
```

此组证明共享改动未破坏已有组件；它们全部通过也不足以完成 V2-01。上表的新 Header/海报测试或同等真实 D-pad 手动步骤必须另有结果。接入新测试类后按已存在的实际全限定类名追加，不能预先声称覆盖。

### 4.3 首阶段截图与退出门槛

交付 `v2-01-home-default.png`、工具图标聚焦状态、至少一个完整竖图网格末行、`v2-01-large-font.png`；每张附对应状态、fixture、viewport/density/fontScale 和构建 commit/hash。必要时录制一段 D-pad 往返以解释焦点问题。

V2-01 可提交的最小门槛：构建及相关 JVM 检查通过；NAV-01/02、NAV-04 的已接入页面、IMG-01/02/04、LIVE-01 和共享组件回归通过；NAV-03、IMG-03、大字体有明确结果。尚未改版的页不能因规格未全部满足阻止这个可恢复检查点，但必须记录未完成项归属 V2-02 至 V2-10，不能将整个 UI v2 标完成。

## 5. V2-02 / V2-06 必测竞态和来源焦点

### 5.1 Feed 与账号/查询代次

使用 `CatalogFeed(loadPage=...)` 的可注入请求函数，配合 CompletableDeferred 或受控 MockWebServer 响应，让请求完成顺序可重复；不以随机 sleep 证明竞态不存在。

1. 同页并发两次：只发一个请求；首请求成功才 nextPage 加一。
2. 第二页失败再重试：第一页面数据、原 ID 和焦点保留，重试仍请求第二页；去重后顺序正确。
3. A 查询/账号请求挂起 → 切 B → B 成功 → A 最后成功：只允许 B 修改可见 feed、错误与自动焦点意图。
4. A → B → A 缓存返回：恢复的是 A 的对应 feed/锚点，不能误用 B 的 total/错误或重复首项聚焦。
5. 声称有更多的整页重复 ID：不得持续自动推进请求；给出有界终止或明确重试状态。空页、未知 total、异常 total 分别验证。
6. 退出页面或切账号时取消旧任务；即使适配层响应不能及时取消，代次校验仍须防旧响应提交。旧取消回调不能把新代次的 loading 清掉。
7. 尾页刚追加时用户在 Header/其他区域：内容追加但不抢焦点；只在用户仍等候下移时消费一次待处理意图。

### 5.2 来源焦点

- 同片 movieId 同时出现在首页最近、推荐、电影节：各自打开/返回，逐一核对 sectionKey，不用标题判断。
- 分类双榜同片、两个不同分类共享某片：从不同榜进入播放器再返回，不恢复到另一个榜；“浏览全部”返回原小首页按钮。
- 深滚动后来源项已经暂时卸载：先定位列表和布局，再请求该项焦点，不反复向未挂载节点请求。
- 原来源项消失或记录被删：选合法邻居；空集合回主操作；不因相同下标而打开/聚焦别片。
- Activity 重建保留 query/分类/锚点；进程重启不自动重放登录、收藏、删除。账号私有状态不得交叉恢复。

### 5.3 搜索主闭环

Fixture 固定 MN 的首个建议为“魔女”、字面结果为空，魔女有至少两页结果。完整操作为默认首页 → 左到搜索并确定 → 输入框下到键盘 → M、N → 右边界到建议 → 确定 → 等正式结果首项焦点 → 返回到输入框（无 IME）→ 再返回来源。

还必须覆盖：确认词后立刻返回或输入新字、慢建议覆盖旧建议、再次确认已缓存同词、正式结果为空/失败、建议为空但字面结果存在、结果分页后焦点不回第一张、IME 打开时 Back 先关 IME/编辑层。使用完整 36 键邻接；不能以直接给 M/N/魔女 RequestFocus 代替导航步骤。

## 6. 记录与交接

每次独立运行写一份阶段结果，记录基线 commit、被测 APK/测试 APK hash、设备/OS/显示密度、执行命令、真实用例数、通过/失败/跳过、截图路径和复现按键。失败需说明属于环境、过时测试断言、真实产品问题还是待实现范围；修复后只重复受影响闭环及必要共享回归。

实机 Chromecast 留给明确交接的集成验证；登录、VIP、云历史等沿用既有授权与本地配置，不把账号写入验证文档。ADB 无法连接时先报告环境证据，不抹除应用数据、解除配对或安装不同签名版本作为默认修复。没有执行的用例始终记“未执行”，不能记通过。
