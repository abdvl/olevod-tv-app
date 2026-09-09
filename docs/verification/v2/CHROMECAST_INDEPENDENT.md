# UI v2 Chromecast 独立回归

最后更新：2026-09-08 20:07（America/Los_Angeles；2026-09-09 03:07 UTC）。独立验证 agent 在每次明确交接后独占设备运行；结束后明确交回，双方没有同时发送遥控键。下方基础批与各修复批分别保留对应包和原始结果。

**最新修复包 `8778ae82…` 在 Chromecast 的指定 19/19 项回归通过，126.455 秒，0 失败／跳过；保留数据安装后，独立拉取已安装 APK 验证 SHA 一致。** 按测试类和方法去重，跨批次共有 **56 个真机用例最终通过**，不是同一版本的一次 56 项全量运行。初始初始化错误、失效登录以及模拟器发现的焦点／滚动问题均保留首次失败证据。

VIP 源实际 1920×1040 解码、音视频输出 buffer 已有独立验证；`2af5e7a1…` 基线包的约 30 分钟真实 UI 时钟采样和系统 Home／后台进程恢复记录已独立审计。它们不等同于最终包的 30 分钟重复长播，也不证明电视听感、音画同步、4K／HDR或全部无障碍验收。最终批和证据边界见文末。

## 首次基础批环境与冻结包

| 项目 | 记录 |
| --- | --- |
| 源码 | `c8e50e72c52c61a7e5b666b4abcb730a0c039e12`，`codex/ui-v2` |
| 型号／代号 | Chromecast / `sabrina` |
| Android | 14 / API 34 |
| ABI | `armeabi-v7a` |
| 显示 | 物理 3840×2160，系统 override 1920×1080，density 320 |
| 字体 | `settings get system font_scale` 返回 `null`（未显式设置）；本批未更改字体配置，不能视为放大字体覆盖 |
| App APK SHA-256 | `e14387a733c072d46a78b7af722336893115dd7409ba8deaf549cbed725587ce` |
| Test APK SHA-256 | `6e1eb0ea9ad0215d6a9d08daef4df3da0712b9b6778c794cb71eea1e6b712915` |
| 安装 | 主执行 agent 交接：原设备与新版 debug 签名一致，两个 APK `install -r` 成功，保留应用数据；独立 agent 没有卸载或清数据 |

设备属性由独立 agent 重新读取，保存于本机 `.tools/chromecast-v2/independent-device.txt`。源码 SHA 由独立 agent 读取；APK 散列和保留数据安装证据来自主执行 agent 交接。运行期间没有修改源码、测试断言或 APK。

## 执行结果

| 批次 | 完成 / 预期 | 时间 | 实际范围 |
| --- | --- | --- | --- |
| UI／状态夹具 | 45 / 45 | 225.692 秒 | 正式 Composable、遥控键路线、布局／焦点／状态；数据和媒体状态使用隔离夹具 |
| `CatalogIntegrationTest` | 1 / 1 | 9.597 秒 | 实际分类、四排序第 1/2 页、组合筛选、中文搜索 API |
| `V2LivePlaybackTest` | 1 / 1 | 19.508 秒 | 普通点播真实片源，由 NativePlayer / Media3 报告首帧、进度和格式，验证全屏显隐／返回及隔离历史保存 |

三次 runner 分别返回 `OK (45 tests)`、`OK (1 test)`、`OK (1 test)`。逐项状态码共 47 次开始 (`1`)、47 次成功 (`0`)，没有跳过或失败状态码。测试耗时来自 instrumentation 输出，不是应用冷启动或帧性能指标。

| UI／状态类 | 成功项数 |
| --- | --- |
| V2FoundationUiTest | 3 |
| V2FocusRestorationUiTest | 1 |
| V2HomeUiTest | 3 |
| V2MiniHomeUiTest | 2 |
| V2CatalogUiTest | 4 |
| V2SearchUiTest | 4 |
| V2PlayerUiTest | 4 |
| V2HistoryUiTest | 7 |
| V2AccountUiTest | 6 |
| V2AccountStateTest | 4 |
| V2FavoritesUiTest | 4 |
| V2RootJourneyUiTest | 3 |

核心真实遥控路线包括单行 Header、首页最近播放上下往返、相同影片的准确来源恢复、mini 浏览全部回程、年份及更多筛选草稿、MN 联想确认到结果并返回输入、八播放按钮与视频/Header往返、覆盖层不改变视频边界、十集分组、两列历史及尾部错误回程、验证码键盘、收藏深列表恢复和退出确认。历史双来源测试仍保留既有前置 RequestFocus：用于离开深列表到来源标签而不改变原锚点，随后的切源、进入正文和恢复使用遥控键；不把这一步描述为全部纯遥控。

## 数据范围

- 12 个 UI／状态类使用 fixture、临时 cache 图片或 namespaced SharedPreferences / SQLite；关闭后清理测试命名空间。删除历史、清凭据和收藏变更断言不操作用户真实数据。
- `CatalogIntegrationTest` 使用未登录公开 API：分类 1/2/3/4/6/14，更新前十，`update/desc/hot/score` 两页，电影／美国／2025／会员／评分组合及“早春晴朗”搜索。
- `V2LivePlaybackTest` 使用普通点播 ID `83927`，未登录；真实媒体播放进度仅写 `V2FixtureViewModel("ordinary-media")` 的隔离历史，结束后清理。
- 本批未传 `liveLogin`，未运行真实登录、收藏写入、云历史同步、账号切换或直播频道测试。没有读取测试用户名／密码、设备真实观看记录或会话内容。

## 实际媒体断言的限度

`V2LivePlaybackTest` 实际断言：视频语义报告首帧；缓冲提示消失；进度越过 00:01；进入全屏后出现正数宽高；上隐藏控制、下唤醒并聚焦退出全屏；两次返回退出播放器；隔离历史至少保存 1000 ms。

这些证据不能证明音频实际从电视输出或音画同步，也未量化 seek 误差、帧率、丢帧、码率准确性、同一 Player 实例及精确连续进度。方法名中的 `KeepsItsPosition` 不能替代精确位置连续性断言。所测媒体不保证是 4K／HDR／VIP；显示物理尺寸为 4K 也不等于解码 4K 成功。

`V2PlayerUiTest` 验证的全屏几何与按钮/分组使用注入播放器状态；真实媒体用例没有再做同帧逐像素几何测量。音画、实际 VIP、长时播放、系统后台／进程恢复、完整放大字体和无障碍仍由对应实机旅程单独记录，不沿用旧 v0.1 的音画反馈。

## 重跑命令

先 `source scripts/android-env.sh`，把 `CHROMECAST_SERIAL` 设为当前已配对设备连接地址。测试时独占设备前台。

```sh
adb -s "$CHROMECAST_SERIAL" shell am instrument -w -r \
  -e class com.olevod.tv.V2FoundationUiTest,com.olevod.tv.V2FocusRestorationUiTest,com.olevod.tv.V2HomeUiTest,com.olevod.tv.V2MiniHomeUiTest,com.olevod.tv.V2CatalogUiTest,com.olevod.tv.V2SearchUiTest,com.olevod.tv.V2PlayerUiTest,com.olevod.tv.V2HistoryUiTest,com.olevod.tv.V2AccountUiTest,com.olevod.tv.V2AccountStateTest,com.olevod.tv.V2FavoritesUiTest,com.olevod.tv.V2RootJourneyUiTest \
  com.olevod.tv.test/androidx.test.runner.AndroidJUnitRunner

adb -s "$CHROMECAST_SERIAL" shell am instrument -w -r \
  -e liveCatalog true -e class com.olevod.tv.CatalogIntegrationTest \
  com.olevod.tv.test/androidx.test.runner.AndroidJUnitRunner

adb -s "$CHROMECAST_SERIAL" shell am instrument -w -r \
  -e liveV2 true -e class com.olevod.tv.V2LivePlaybackTest \
  com.olevod.tv.test/androidx.test.runner.AndroidJUnitRunner
```

必须保留类白名单；整个 package 包含真实账号写入与旧直播收藏测试，不能通过全包执行替代本批。

## 原始证据

完整日志留在本机 `.tools/chromecast-v2/`，不包含账号密码或签名播放 URL，不进入 Git。

| 日志 | SHA-256 |
| --- | --- |
| `independent-ui-45.log` | `6b90c247cf33be85ebea45616b9588e964938ac2cc2b2dcf525e67a0d64f23fc` |
| `independent-live-catalog.log` | `dbecf698b53bf9c0c1413050111c615a6c76dbb8f96b19c0879a62c32e190fa3` |
| `independent-live-playback.log` | `efdc6a84921be40198f30401857dd7de2c45a0e1978171bf6288304178ba70ca` |

此独立批没有失败后重试、调整超时、删断言或生产修补，也没有新增运行截图；手动实机截图和旅程由主执行 agent 另行记录。

## 新增实际媒体控制测试：首次初始化失败

上述 47 项完成后，主执行 agent 新增 `V2DevicePlaybackTest`，目标是通过真实 D-pad 和平台 MediaController 验证 30／300 秒 seek、1.5 倍速、全屏位置／会话与容器几何、Activity ON_STOP 暂停以及释放／保存。生产 App APK 保持不变；首次新增测试 APK SHA-256 为 `c073dbb4aa72cc0f8afd124cc48de3cd1e65cff6807c56ef0662d8492fdf38d0`。

独立静态审查发现暂停态平台 PlaybackState 速率为 0 的问题，并由主执行 agent 在首跑前将 1.5 平台速率断言移至 PLAYING；暂停时保留 UI 倍速断言。本地 Media3 1.7.1 `PlayerWrapper.createPlaybackStateCompat` 字节码确认其发布速率为 `isPlaying ? playbackParameters.speed : 0`，没有修改生产播放器。

独立首跑使用 `-e liveDevicePlayback true -e class com.olevod.tv.V2DevicePlaybackTest`，结果 **1 项初始化失败，0.053 秒**，媒体与控制步骤未执行。JUnit 报 `Method realSeekSpeedFullscreenBackgroundAndRelease() should be void`：表达式体 `runBlocking` 的最后 `use` 返回 `Log.i` 的 Int，造成测试方法不是 void。应显式声明 Unit 返回值；这属于测试签名错误，不是播放器运行失败。独立 agent 在报告后交回设备，未自行修改或重试。

完整首轮日志：`.tools/chromecast-v2/independent-controls-1.log`，SHA-256 `1a6998ab26943d92824ae2bf58108f2c9ad72fb03a66fd351c4a108ead1ac233`。没有用前述 47 项成功覆盖此失败。

## 新增控制专项：第二轮通过

主执行 agent 将测试返回类型显式声明为 Unit，并通过 javap 核对生成 void 方法；新增测试 APK SHA-256 `78e6633c161b790f1e5ac50cbf74ac241a0dac028df903bb02508e8793cf7415`，`install -r` 后交回独立 agent 执行相同命令。生产 App 保持 `e14387a7…`，不改生产代码或原测试断言。

第二轮 **1/1 通过，29.889 秒，0 失败或跳过**。专用 `V2DevicePlayback` 日志只记录数值和摘要，不读取通用媒体 URL／账号日志。

| 实际遥控动作 | 期望位置 / 实测位置（ms） |
| --- | --- |
| 片头 −30 秒，限至 0 | 0 / 0 |
| +5 分钟 | 300000 / 300000 |
| 片中 −30 秒 | 270000 / 270000 |
| +30 秒 | 300000 / 300000 |
| −5 分钟 | 0 / 0 |
| 再 +5 分钟 | 300000 / 300000 |
| 全屏控制显隐第 1 轮 | 300000 / 300000 |
| 全屏控制显隐第 2 轮 | 300000 / 300000 |
| Activity ON_STOP 后返回 | 305196 / 305196 |

同时通过：暂停位置不前进；UI 选择 1.5 后真实 PLAYING 状态平台速率为 1.5；显隐前后 `player-video` 容器 bounds 完全相同且平台 sessionToken 保持；恢复播放后进度增加；ActivityScenario 进入 CREATED 触发 ON_STOP，平台进入 PAUSED，等待 1.5 秒位置保持，回 RESUMED 仍暂停；两次返回退出播放器后隔离历史包含该暂停位置（断言容差 1000 ms），原平台 session 被释放。

该测试位置断言容差为 1000 ms；表中本次恰好数值相等，不表示所有片源精确至毫秒。ActivityScenario 生命周期迁移不能替代真实系统 Home、Activity 重建或进程终止。容器 bounds 不表示视频内容同帧比较；同一 sessionToken 是平台会话连续性证据，不单独等同播放器对象身份。声音、音画同步、VIP／4K／HDR及长时稳定性仍未由该专项证明。

| 第二轮日志 | SHA-256 |
| --- | --- |
| `.tools/chromecast-v2/independent-controls-2.log` | `e5ebb5b39f5d3bb91cad0cf6367ee7f443fbd5de48c5691c419126bb909c8c3d` |
| `.tools/chromecast-v2/independent-controls-2-values.log` | `a9ca0782e45d4e8aee110f57deb91a83834bfa1fd07fab26e6b5e06b49c0756c` |

第二轮结束后独立 agent 明确交回设备；不重复运行已通过且未受影响的 45 项夹具。

## 保存账号：旧会话失败与重新登录后通过

主执行 agent 在旧保存会话下执行增补诊断：`pub/user/info` 与独立私有列表用例均返回“登录已失效，请重新登录”，两项失败，2.773 秒。独立 agent 只读复核该原始日志；这比仅凭 token 存在或详情空 URL 更明确地证明旧会话已失效。随后主执行 agent 按用户已有授权在真实 AccountScreen 退出旧会话、使用已记住的账号密码和新验证码显式登录，并将设备交给独立 agent；登录 UI 操作和验证码不由独立 agent 执行。

独立执行 `V2AccountReadTest -e liveLogin true`，两个独立测试方法 **2/2 通过，4.569 秒，0 失败／跳过**，随后明确交回设备。实际完成：

- `/pub/user/info` 接受新保存会话，返回有效账号标识；仅记录会员组数值 `3`，不记录标识值或其他资料。
- VIP 样本 ID 80632：`baseHttps=1 baseEmpty=0 premiumCount=0 premiumHttps=0 restricted=true`；正常 mapper 的剧集存在且每条为 HTTPS 断言通过。本样本没有 `vip_urls` 被丢弃导致空片源的证据。
- 第一方法中的收藏第一页和云历史第一页断言均执行通过；第二方法独立再次读取这两个列表并通过，不再被 VIP 前置失败阻断。

旧会话明确失效、重新登录后原样本立即恢复 HTTPS 的组合证据支持会话失效是本次空地址的原因；不是 Chromecast 解码问题的证据。未播放 VIP 媒体，不能由地址读取通过推定 VIP 首帧、音频或同步通过。本批只读真实服务，没有写收藏、云历史或账户信息，也不输出 token、用户名、密码、完整 JSON 或签名 URL。

本批使用的本地测试 APK SHA-256 为 `8dea01b70242adf29b47cc3ad1cab2509487c6fc2884c0cb9136ec85661c0c2a`，生产 App 保持最初冻结包。

| 日志 | SHA-256 |
| --- | --- |
| `.tools/chromecast-v2/session-diagnostic-tests.log`（主执行 agent 的旧会话失败，独立只读复核） | `a5ed01dddc0022aab195e4c585ffb5fd255a1c63811d7bb79a1fdd57394e070b` |
| `.tools/chromecast-v2/account-refreshed-tests.log`（独立重登后测试） | `3b315afa7e84972a060676d3b2f3141375a48b24f88101bb2a0a38917613717c` |
| `.tools/chromecast-v2/account-refreshed-values.log`（仅 `V2AccountRead` 专用标签） | `eef867b864d07dc8f13fccb153a2dfaf534db98370588065da98b0c0f03f22c1` |

## 受限空片源诊断修复：独立 JVM 与构建

在上述实机批次后，主执行 agent 为 `OlevodApi.detail` 增加受限空基础片源诊断：仅剧集列表非空、基础 URI 全空且影片／剧集带 VIP 标记时触发。游客得到登录提示；保存会话通过用户信息端点验证；实际过期状态沿用已有注销回调；缺失用户字段、非会员和会员但无可用源分别给出明确错误。健康 HTTPS、混合片源、无剧集或未标受限的空源保持原请求路径；不自动改选 `vip_urls`。

独立 agent 只新增 [OlevodVipSourceGuardTest.kt](../../../app/src/test/java/com/olevod/tv/data/OlevodVipSourceGuardTest.kt)，使用 MockWebServer 合成响应，不读保存凭据，不访问真实服务，也未操作 Chromecast。源码审阅与测试重点是额外请求边界、清会话条件、两阶段账号竞争和错误脱敏。

首轮 14 项，**13 通过／1 失败，0.300 秒**：旧账户用户信息响应在 token 已换成新账号后返回 code 13，未注销新账号，但旧错误仍作为 ApiException 传播。独立测试要求取消旧结果，保留失败断言；主执行 agent 在用户信息请求 catch 增加判断，变为新账户时取消，但本次真正过期回调将原 token 清成 null 时仍传播登录错误。

补充“手动退出到 null 同时用户信息返回 HTTP 503”的边界后，第二轮 **15/15 通过，0.279 秒**；该新增场景正确取消旧错误。覆盖包括：

- 普通／VIP 健康 HTTPS、混合 URI、空剧集、非受限空 URI、不支持的非空 HTTP URI均不额外探测账号。
- 影片或剧集 VIP 标记下游客空源抛 code 12，仅发详情请求。
- 13／14／16 实际注销原会话；code 12 不清已有会话；所有错误不泄露合成服务 echo、token 或 URI。
- 有效非会员提示权益不足；有效会员基础空源提示播放源不可用，即使合成 `vip_urls` 有 HTTPS 也不自动选择。
- 10 种用户字段缺失／无效情况保持“无法确认”且不注销；HTTP 503 保留服务错误，不冒充过期或无会员。
- 详情返回时切账号、用户信息成功时切账号、旧用户信息 code 13 时切新账号，以及用户信息 HTTP 失败同时退出至 null，均取消旧结果；旧响应不能注销新账号。

随后独立一次运行 `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`：**全 JVM 45/45，0 失败／错误／跳过，lint 和两个 APK 构建通过，Gradle 19 秒**。未再次运行原 45 项 Android 夹具；修复后实际播放与账号回归由主执行 agent 安装新包后单独记录。

| 交付／证据 | SHA-256 |
| --- | --- |
| 修复后 App APK | `2af5e7a174200a90d70f21f3b0808b58f73d27fc3d2631d2c0b999592793f3ae` |
| Test APK | `8dea01b70242adf29b47cc3ad1cab2509487c6fc2884c0cb9136ec85661c0c2a` |
| 被审生产 `OlevodApi.kt` | `c85ca92ae13b5eda8127039892ff5e9850f26cad10611c5dec07bf52596871bd` |
| 新增 `OlevodVipSourceGuardTest.kt` | `e30fb681b5ba3d9cb45add6285fe5f00a91f6756c564dd4ce21976f4acc35a5d` |
| `.tools/chromecast-v2/vip-guard-jvm-1.xml`（首次失败完整报告） | `8abe23fcf12ac970daf0d29b3e209112ef4a793967dc55ffe6de17d480f597ba` |
| `.tools/chromecast-v2/vip-guard-jvm-2.xml` | `ccda3604c5798de305e05e3ede930a43038332a06bc6375f1e090eefb685bab9` |
| `.tools/chromecast-v2/vip-guard-final-build.log` | `5a1935e90f0986eaef89753da04bdd6e8ae5c7b793e93fab6e6f3deae982ec5f` |

对应纯文本日志 `vip-guard-jvm-1.log`、`vip-guard-jvm-2.log` 与逐类总表 `vip-guard-final-jvm-summary.json` 同存本机目录。此修复针对受限且全部基础地址为空的详情形态，不宣称已改善所有混合列表中的单集缺源，也不让仅凭 token 的账号页主动进行会话有效性验证；未改变正常播放按钮、布局或媒体解码逻辑。

## 修复包真机复测与真实评分目录

主执行 agent 保留数据安装修复 App `2af5e7a1…` 后，独立 agent 在 Chromecast 运行明确指定的 5 个类：AccountRead 2、LivePlayback 1、PlayerUi 4、RootJourneyUi 3、AccountUi 6。**16/16 通过，112.443 秒，0 失败／跳过**。这是受影响路线复测，不是全部原 45 项重新执行，也不作为 16 个新增用例重复计入总数。测试 APK 仍为 `8dea01b7…`。

随后独占真实应用目录，以单个 D-pad 键、等待、新生成 UiAutomator XML 和同状态截图交替确认；没有依靠发送键数推断行号。排序浮层逐步确认“最近更新”→“人气最高”→“评分最高”焦点，明确提交后触发器显示“排序：评分最高”、结果仍为 4125 部。进入正文后顺序观察每行实际六张卡，左侧焦点为：

| 行 | 实际聚焦影片 | 评分 | 聚焦边界（px） |
| --- | --- | --- | --- |
| 1 | 毕正明的证明 | 10.0 | `[80,368][354,908]` |
| 2 | 逆冰之行 | 10.0 | `[80,532][354,1072]` |
| 3 | 正义联盟：无限地球危机(下) | 9.5 | `[80,532][354,1072]` |
| 4 | 浪浪山小妖怪 | 9.3 | `[80,532][354,1072]` |
| 5 | 黑水 | 9.3 | `[80,532][354,1072]` |
| 6 | 亲密 | 9.2 | `[80,532][354,1072]` |

第五行之后继续到第六行，新影片已累计加入且可聚焦；没有点击翻页按钮。随后 Up 回到第五行“黑水”，旧内容仍保留。第六行截图独立目视确认完整海报、标题、元信息与绿框均处于 1920×1080 画面内。这证明实际连续列表导航和累计内容可用；本手动路线没有拦截网络请求次数，因此不单独证明“下一页恰好只请求一次”。

**保留一个非阻断观察：** 提交评分排序后，焦点已回触发器，但截图的当前网格显示第四行“浪浪山小妖怪”；按 Down 进入正文后才滚到首片“毕正明的证明”。所以本轮不将“提交新筛选立即让背景列表复位首行”记通过。证据分别为 `independent-catalog-score` 与 `independent-score-row1`；此前涉及未知外部遥控输入的目录片段仍独立保留，不能由本次补测倒推其通过。

本机证据目录 `.tools/chromecast-v2/`：`post-guard-device-regression.log`（SHA-256 `eaa1bc4c6d8ab2b3cc280616f683e4c2eab7908588ffd7552c7e88c78dcef2fd`）；目录路线 `independent-sort-open`、`independent-sort-hot-focus`、`independent-sort-score-focus`、`independent-catalog-score`、`independent-score-row1`～`row6`、`independent-score-return-row5` 各保存新 XML／JSON／PNG。未将失败的新树获取替换成旧 XML。

## VIP 实际解码独立补测

静态审查 `V2VipDecodeTest` 后，由主执行 agent 将潜在失败时的异常对象断言改成布尔检查，避免异常字符串回显。独立 agent 安装新版测试 APK `8dd48499c8b291ea71d33b4eca59cdec6c39fb3eaff029ef2a3b2adde16555c3`，生产 App 保持 `2af5e7a1…`，执行 `-e liveLogin true -e class com.olevod.tv.V2VipDecodeTest`。

**1/1 通过，15.451 秒，0 失败／跳过**。专用数值日志为：首帧 true，实际格式 1920×1040，真实进度 `0→4770 ms`，视频 rendered output buffers `115`，音频 rendered output buffers `242`。

测试从已授权保存会话取得 ID 80632 的 HTTPS 基础源，以与 NativePlayer 相同的 User-Agent／Referer 配置运行 ExoPlayer 和 PlayerView。退出后释放 player，不写账号、收藏、观看历史或云端同步。原生 Player UI 连接另由前述正式 NativePlayer 用例覆盖；该补测验证 VIP 源本身实际解码，不把独立 ExoPlayer 拼装当作整条正式 VIP 路由通过。

buffer 数字证明解码与音频处理完成，不证明电视扬声器听感、音画同步、4K／HDR或长时稳定性；本样本实际宽高是 1920×1040。完整日志与数值日志均仅留本机：

| 日志 | SHA-256 |
| --- | --- |
| `.tools/chromecast-v2/independent-vip-decode.log` | `e4daa4bda2dcaf4b63c2d37c6483cf1c4ad324c6212ca23c3f02ca1b80271fc5` |
| `.tools/chromecast-v2/independent-vip-decode-values.log` | `b838a1652f38e6d7fdf7e4cf03c8b5d1c2c998592ecb3430963e9a2ae2457282` |

结束后独立 agent 再次明确交回 Chromecast，主执行 agent 才开始长时实际播放。错误态“登录后继续”／重试的遥控器焦点入口仍需定向测试，不能仅根据出现按钮文字宣称完整重新登录引导已通过。

## 播放错误操作焦点：模拟器初轮复现

Chromecast 交回用于长时实际播放后，独立 agent 仅接管 `emulator-5554`；新增 `V2PlayerErrorFocusTest.kt`，使用正式 PlayerContent、合成错误和模拟视频，不初始化 ExoPlayer、不访问网络／账号／存储。未修改已有 `V2PlayerUiTest` 或生产代码。

用未修改错误焦点的 App `2af5e7a1…` 与新增 Test APK `a36da8bb30e2f7886a95aaa652631db6394d79f74ddb2d4b6611818462cb0383` 执行，**4 项均失败，5.295 秒**。断言明确检查真实 Focused 语义，不使用触摸点击或 RequestFocus 跳过导航：

1. 登录错误到达后未聚焦主操作“登录后继续”。
2. 普通服务错误未聚焦可执行的重试。
3. 全屏登录错误未进入恢复操作。
4. 焦点在 Header 时错误到达没有抢焦点，但 Header Down 仍进入视频父容器，未进入登录操作。

按钮文字和点击语义均已存在、可见，但 `Focused=false`。专用日志复现当前路线：`Up→player-video→Up→nav:home`，错误到达后 `Down→player-video`，说明“看见登录按钮”不能作为遥控器登录引导通过的证据。

主执行 agent 已接收失败证据并负责生产修复；预期是内部错误的主要操作可直接进入、Header 不被异步错误抢焦点、登录→重试→控制的纵向路径明确、全屏错误保留恢复操作、重试进入加载前交还有效焦点。首轮日志 `.tools/chromecast-v2/error-focus-baseline-tests.log`（SHA-256 `cb36584bf6c7836bef5b034d30df41ec8ebc455345fbfb33bce93b7388db9880`）与 `error-focus-baseline-values.log` 保留，不以重试覆盖；初轮没有对 Chromecast 发命令。

## 播放错误焦点修复：独立模拟器复验

主执行 agent 完成 PlayerContent 的焦点修复后，独立 agent 审阅生产差异，以原先失败的同一份测试和断言复验。只在 `emulator-5554` 安装 App `ddf80ab0b90d2be1be96fe8386fe4aa3ff1b2497b2960d4929a03f275f5397d1`；Test APK 仍是 `a36da8bb30e2f7886a95aaa652631db6394d79f74ddb2d4b6611818462cb0383`，Chromecast 的 `2af5e7a1…` 长时播放未被安装或按键打断。

**错误焦点 4/4 通过，6.907 秒；原有 PlayerUi 4 + RootJourneyUi 3 共 7/7 通过，19.670 秒；均无失败／跳过。** 新增四项是模拟器验证结果，不合并为 Chromecast 新增通过数。

真实 D-pad 事件和 Focused 语义共同确认：内部错误到达转到主要恢复操作；Header 上的焦点不被晚到错误抢走，但 Header Down 明确进入主操作；登录→重试→全屏控制的 Down 路径及控制 Up→登录可达；全屏错误等待 6 秒后保持恢复操作，Up 不隐藏它，Back 返回普通布局仍聚焦主操作；登录确认仅调用一次，普通重试确认后进入加载且焦点回全屏控制。测试没有通过触摸或直接 RequestFocus 访问错误按钮。

生产审阅确认移除了错误视频区域的可点击父焦点拦截，为主／次恢复操作和简介建立明确方向，并在重试导致按钮消失前交还控制焦点。原有回归确认正常视频 Up／Down 隐藏与显示全屏控制、视频边界不变、八控制顺序、禁用跳转、选集、速度，以及实际 Root 返回来源与退出确认仍工作。没有修改已有 PlayerUi 或 RootJourneyUi 断言。

| 本机证据 | SHA-256 |
| --- | --- |
| `error-focus-fixed-build.log` | `6892c8fccd76a5bb01c768e55b059b43e943ed8c0aaab520424330515804dd7b` |
| `error-focus-fixed-tests.log` | `b16df280f11464cdda7903efe339781131760e628410044b3d4e4b681ec5fce6` |
| `error-focus-fixed-values.log` | `9aca96580f7e655844ebb2d49190a70d0e965adc3cb6e41eab025a0fc6ec354b` |
| `error-focus-normal-regression.log` | `55dd32b895bae9876a7e8808b7a3778905c623ab50ec83a1e7c869d83396f9e4` |
| 被测 `V2PlayerErrorFocusTest.kt` | `45211cce08f00258f43cca8219c05d0cb1b502d8151b9ff39301db3c0015edfc` |
| 被测 `PlayerContent.kt` | `120b17c7b5e92d0eb2ee7f98eb81925ca58d92515e997ce31ebdcc864308639c` |

日志位于 `.tools/chromecast-v2/`；正常七项完整结果为 `error-focus-normal-regression.log`。本组使用合成错误和模拟视频，只证明正式播放器 UI 的遥控路径与回调，不证明真实账号重新认证、真实 VIP 错误重登后回放、解码或音画。

### 字体 1.3 的错误面板边界

针对新错误面板的可见性风险，仅在新增测试 helper 加入更强断言：每次聚焦主／重试操作时，未裁剪边界须完整位于 Root 内，且按钮底部不得超过第一个播放器控制的顶部。用 `expectedFontScale=1.3` 验证应用实际资源配置；可选 `errorFocusSnapshots=true` 只将合成页面截图写入应用专用 cache 并导出到本机，无真实账号内容。

在模拟器实际字体 `1.3` 下重跑新四项，**4/4 通过，10.026 秒**。App 仍为 `ddf80ab0…`，Test APK 为 `f983fffe7e29792e864fd6feab5b9be565aed8e8089e18eeee7f6c302a5c55fb`。Root 为 `960×540 dp`；普通布局登录按钮底部 `255 dp`、次要重试 `310 dp`；全屏登录底部 `123 dp`、次要重试 `178 dp`；控制顶部均为 `453.5 dp`。服务错误主重试底部 `282.5 dp`。已独立查看全屏重试与普通登录截图，文字、聚焦绿框完整且未被控制覆盖。

字体设置执行前 `1.0`、运行时 `1.3`、finally 恢复后 `1.0`；三份设置快照为 `error-focus-font-before.txt`、`error-focus-font-test.txt`、`error-focus-font-restored.txt`。此检查仍没有操作 Chromecast。

| 本机证据 | SHA-256 |
| --- | --- |
| `error-focus-large-font-tests.log` | `f391bd42318ecaf6d0ab0188e09cc17a476b8419fa178c360d8349604229eb53` |
| `error-focus-large-font-values.log` | `39edfa63059e55f742875e2e8afe55fe76fef9ef14306fda210ce34b33c68c62` |
| `error-focus-large-font-full-login.png` | `c8218889d9cd42dada90d476f35d9d833296e31c11a367aa2a39198da53ffa2b` |
| `error-focus-large-font-full-retry.png` | `40000ee7f81e829ef1bcb57bffd56190c43efd370f3f6d9cd39f7a09572f96bc` |
| `error-focus-large-font-window-login.png` | `7322f5c86b4e67f4647483f69142a795e677795997c7206b1b32388d74fc42be` |
| `error-focus-large-font-window-retry.png` | `733a51929b69c67ce759e737b58d184668298dac83c5503095174d0d091b93ea` |

四张截图为 1920×1080 的合成夹具，供错误态布局审核；不作为真实 VIP 故障或重登截图。目视核查另指出全屏错误态仍写“上键隐藏”，与已修复的 Up 恢复入口规则不符，已交由主执行 agent 修正文案；测试通过不掩盖这项观察。

### 最终构建交接

主执行 agent 将全屏错误态提示改为“返回键退出全屏”，正常播放的提示不变。独立 agent 确认这一处文案调整后，一次执行 `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:testDebugUnitTest`，**9 秒构建成功，JVM 45/45 通过，0 失败／错误／跳过**。Lint 完成，报告 **0 error、21 warning**；不将构建成功表述为零告警。未因该文案微调重复已经通过的模拟器 11 项。

| 最终交付／证据 | SHA-256 |
| --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | `ac0ca731f8d721f36e1baed06ca7325587407d21a399299d3d224976f2acdc4f` |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `f983fffe7e29792e864fd6feab5b9be565aed8e8089e18eeee7f6c302a5c55fb` |
| `error-focus-final-build.log` | `a0d7e4578075ed5b026f52ccefa7751ddd517a57f76e5ab19bcad03ba80d814a` |
| `error-focus-final-jvm-summary.json` | `ad41e963f6449f20cec1f4f19095a88d45821df268bc13a9f39e672c9e2061ee` |
| 最终 `PlayerContent.kt` | `05b06950312468da88ac587b983b35b83e77623078d30e677acc7ed1bc6b7743` |

本节日志仍位于 `.tools/chromecast-v2/`。前节 1.3 字体截图准确对应文案修正前的 `ddf80ab0…` 包，不替换或加工为最终包截图。最终包尚待主执行 agent 在 Chromecast 长时播放结束后保留数据安装并做指定真机回归；此次独立构建没有安装或控制任一设备，完成后构建与模拟器均交回，未提交代码。

## 排序后的目录滚动位置：模拟器独立复现

为前述真实评分目录观察新增 `V2CatalogViewportTest`，仅使用正式 CatalogPageContent、筛选浮层、ContentFocusScope 和 SaveableStateHolder，注入合成数据。预条件通过测试滚动 API 将网格置于第五行、保持固定排序触发器聚焦；之后选择“评分最高”、进入新结果和离开／返回均使用实际 D-pad 事件。测试设计同时要求新查询立即显示首行、追加不跳顶、返回同查询保留深处位置，没有通过按 Down 触发隐含复位来掩盖新排序的结果位置。

未改目录生产逻辑的 App `ac0ca731…` 与新增 Test APK `beb08425c10de7e8acd9d12e6d9cecbca211e8c98acb25a832cfdf5e9edd8302` 在模拟器运行，**首轮 1 项失败，2.600 秒**。原第五行 ID 25–30 可见；D-pad 确认评分后先经历空 loading，再返回部分共享 ID 的新排序，`filter:0` 聚焦正确，但新首行 ID 61 不可见，实际画面停在新列表第八行 ID 7–12。这重现了“触发器正确，结果仍留在深处”的问题，后续追加／返回断言尚未执行。

首轮完整日志 `.tools/chromecast-v2/catalog-viewport-baseline-tests.log`（SHA-256 `e77e710cf387fcbfacbd76508d1bfe9f74367abc54f32a85534fba40a249cf5f`）和数值日志 `catalog-viewport-baseline-values.log` 保留。合成前／后截图为 `catalog-viewport-baseline-before-sort.png` 与 `catalog-viewport-baseline-applied-sort.png`；后者 SHA-256 为 `23b15b3f744044e479f8beaff22525d8e44c7be4f52203ff886cb84133770e84`。独立 agent 已将失败交给主执行 agent 进行生产修复，未操作 Chromecast。

### 目录修复后的完整回归与最终包

主执行 agent 为每个 Filter 加入 saveable 的一次性 `pendingTopReset`，首批非空、非 loading 的结果挂载后滚至首行，滚动完成才消费标记。独立审阅确认 effect 取消时不会提前消费，追加不会重新初始化，SaveableStateHolder 返回同查询保留已消费状态。生产更改未动分页网络或焦点记忆协议。

安装修复 App `8778ae82…` 后，用完全相同 Test APK `beb08425…` 在模拟器运行新增 Viewport 1 + 原 CatalogUi 4，**5/5 通过，13.651 秒，0 失败／跳过**。新增完整路径均已执行：

1. 通过排序浮层确认后，焦点保持 `filter:0`；无需按 Down，首行 ID 61–66 立即可见且位于网格起点。
2. 实际 D-pad 下移至第五行 ID 37 后，注入一次 loading→累计增加 12 部的结果更新；ID 37 焦点和顶部坐标均未改变，首行不在画面内。
3. 选中 ID 37 进入合成目标，Back 返回原评分查询；ID 37 焦点及深处顶部坐标完全恢复，没有触发第二次跳顶。

排序取消、更多筛选提交／取消、年份四列导航、未知总数等旧四项同时通过。独立查看新排序首行和返回深处截图：首行 ID 61 正确，返回 ID 37 绿框及元信息完整。追加与返回两份**分别新生成**截图 SHA 相同，反映两次页面状态一致；不是复用旧截图。此测试的结果追加来自控制夹具，不单独证明真实 API 请求次数。

随后一次执行 `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`，**8 秒成功，JVM 45/45，Lint 0 error／21 warning**。此包覆盖之前错误焦点和提示文案修复，替代上节 `ac0ca731…` 作为待真机最终安装包。

| 最终交付／本机证据 | SHA-256 |
| --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | `8778ae82d97cc859f4aacf356834a7a144b96a3bc5d8406f5eec753e162499cc` |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `beb08425c10de7e8acd9d12e6d9cecbca211e8c98acb25a832cfdf5e9edd8302` |
| `catalog-viewport-fixed-tests.log` | `ed36b385a64f3623ef1293c7a45a569f1071a5c619736b2664b6bdd8f8403b12` |
| `catalog-viewport-fixed-values.log` | `ccb7e6efa3eafcaf20f793fdd9657ae716e1cb386838ecf2068fc52f862ac3cf` |
| `catalog-viewport-fixed-applied-sort.png` | `147987bc04fc08d28eb1d027c97f9c226436c7108c377fd95887701221c696a3` |
| `catalog-viewport-fixed-appended-deep.png` | `af840a40c8a524927e96ae50982073b9b7905e6e4bd4ab2d9d459e10dc882153` |
| `catalog-viewport-fixed-returned-deep.png` | `af840a40c8a524927e96ae50982073b9b7905e6e4bd4ab2d9d459e10dc882153` |
| `catalog-viewport-final-build.log` | `d9a0b3279ad80752962fc14738fa926b700e8b8139dcb1f93498ae43b75c317f` |
| `catalog-viewport-final-jvm-summary.json` | `2ee3859920fa4c14e99ca5c0b0bd1acd2209911126073633fcdfcbf04faed8d9` |
| 被测 `ConnectedBrowse.kt` | `a58c71309ea174f19d6e3288f010d8fe4181bff99dbd4189e673fbeff8ea402c` |
| 被测 `V2CatalogViewportTest.kt` | `e8ad84f5a7097e068232afe884b066417d9fc737a564d33c08651d561ccc45a5` |

日志／合成截图均在 `.tools/chromecast-v2/`；没有操作 Chromecast，新增一项不计入尚未执行的 Chromecast 用例数。构建与模拟器交回主执行 agent，未提交代码。

## 30 分钟真实播放记录：独立只读审计

长时观察结束后，独立 agent 仅读取 `soak-30min.jsonl`、`soak-30min.summary.json` 和生成脚本，重新解析全部记录并核对 summary 字段，未操作 ADB、构建或安装。**61 条记录完整，summary 的样本数、时长、进度、PID 与各异常计数均与独立重算一致。**

| 独立核查项 | 实际记录 |
| --- | --- |
| 采样开始时间 | 2026-09-09 02:28:10.656 UTC（本地 9 月 8 日 19:28） |
| 最后采样开始时间 | 2026-09-09 02:58:10.670 UTC（本地 19:58） |
| 首末采样起点跨度 | 1800.014 秒，约 30 分钟 |
| 相邻采样起点间隔 | 29.990–30.010 秒 |
| fresh UI 播放时钟 | `11:34→41:33`，实际推进 1799 秒 |
| 60 次 UI 时间变化 | 每次 29–31 秒，0 次不增／倒退 |
| UI 总片长 | 所有样本均为 `1:55:39`（6939 秒） |
| 平台 position 变化 | `689607→2489544 ms`，推进 1799937 ms；仅作交叉核对 |
| PID | 61 次均为 `21785` |
| 平台状态／速度 | 61 次均为 PLAYING `3`／`1.0×` |
| 前台／新 UI 树／暂停动作 | 61 次均确认前台、fresh dump 成功、显示“暂停” |
| PSS | `158840–166299 KiB`，即 `155.12–162.40 MiB` |
| 首尾 PSS | `158840→164755 KiB`，增加 `5.78 MiB`；过程中有 20 次下降 |
| 观察错误／采样缓冲提示 | 均为 0 |
| 脚本检测的新应用崩溃行 | 0 |

summary 中末次 `elapsed_seconds=1804.116` 包含最后一轮采集完成的耗时，不应写成精确 1804 秒连续画面观测；第一条采集完成时间是 `4.448`，首末完成时间差为 `1799.668` 秒。UI 时钟来自每次新建文件名的 hierarchy 内容，不是按墙钟外推的媒体 position；独立检查没有使用旧 XML 代替失败采集。

**证据范围：** 该记录支持“约 30 分钟采样内，实际显示的播放时钟持续前进，应用保持前台与同一观测 PID，没有观察到缓冲提示或被该脚本识别的崩溃”。30 秒采样间隔无法排除短暂卡顿、丢帧、音频中断或音画不同步；PSS 小范围波动也不是无内存泄漏证明。脚本只统计 crash buffer 最近 300 行内、带 `Process: com.olevod.tv,` 且时间不早于开始的新条目，不是完整 ANR／native tombstone 审核。

主执行 agent 已另行通过拉取已安装 APK 确认此轮为 `2af5e7a1…` 基线包；JSONL 本身不包含影片 ID、会员权益、片源 URL、HDR／4K 属性或 APK SHA，因此不能单凭这组记录认定这些身份。该长播结果**不外推成后来 `8778ae82…`（错误焦点／目录滚动修复包）的同等 30 分钟长播验证**，最终包仍由后续指定真机回归覆盖。

| 被审本机证据 | SHA-256 |
| --- | --- |
| `.tools/chromecast-v2/soak-30min.jsonl` | `902f1bcc9c19130fc4211d24506b2ea2de60bdcebf2936cb14d061fe8271907a` |
| `.tools/chromecast-v2/soak-30min.summary.json` | `0b244fbb7e1a27ace0ed7e6242419b41ba9d9a4f32a93aa65df7084926afdca9` |
| `scripts/chromecast-soak.py` | `0d8639715b5817ff451a2c554457662f39665163bfde286e06821ceea3c0e581` |

此审计只新增报告内容，未改原始日志或 summary，没有将观察过程作为新增 instrumentation 测试计数。

### 系统 Home 与后台进程恢复记录的只读核对

主执行 agent 在长播后先操作真实系统 Home，再对后台应用执行 `am kill`，随后重新进入原任务；独立 agent 未执行这些设备动作，仅读取摘要、前后媒体数值、新 hierarchy 和启动日志。摘要记录旧 PID `21785`，kill 后 `pidof` 返回码 1／空输出，新 PID `30594`；启动日志为 `LaunchState: COLD`、`TotalTime: 7420 ms`，其“原任务带到前台”提示不等同于原进程仍存活。

`process-recovery-before.json` 确认 Home 后媒体速度 `0.0`、位置 `2696275 ms`（约 44:56）。初次返回的 `after.json` 为位置 0／速度 0，说明异步准备尚未结束，不能据此判定从头播放；`settled.json` 随后为位置 `2706586 ms`、速度 `1.0`。新 `process-recovery-settled.xml` 含原影片“海洋奇缘：启航”、实际 UI `45:13`，聚焦子树明确为“全屏”按钮。

这些记录支持“真实 Home 暂停后，后台进程退出并冷启动原任务，恢复原影片并在保存位置附近继续推进”。不把不同采集时刻相差的约 10 秒解读为精确恢复偏差，也不宣称无自动播放、模拟了系统内存压力或保证最后一次云同步完成；此路线仍对应基线 `2af5e7a1…` 包。

| 主执行生成、独立核对的本机证据 | SHA-256 |
| --- | --- |
| `process-recovery-summary.json` | `08f2372d61cb115fdfc5e9db439f7b6b02e45eb22cfce0c4f10446a69a8f2c63` |
| `process-recovery-before.json` | `4ce128b74597e0eff139b56ebc2e0288a7e6e0ceaf64ffbef7c8ab94960b7e0c` |
| `process-recovery-after.json` | `02bdd9ef34993a1eb957d79ab963eff7f1ab18f14d82bed94881036651d2f568` |
| `process-recovery-settled.json` | `c0529432172102a9924c2387f8e9e7a0c77c3a0e7a33cfbd345f819d7c5129aa` |
| `process-recovery-launch.log` | `7537aeb7d060c907736b5df0bebe4dc3ea9841239e67128fc23bd0b5f7c937b4` |

上述证据位于 `.tools/chromecast-v2/`，未增加 instrumentation 用例数。

## 最终修复包：Chromecast 独立 19 项回归

主执行 agent 完成 Home／进程恢复并明确交接后，独立 agent 接管 `192.168.128.86:45053`，核对冻结的 `app-final.apk`／`test-final.apk` SHA，然后分别 `install -r`，两次均 Success，未卸载或清数据。随后通过 `pm path` 定位并拉取**电视实际安装的** `base.apk`，其 SHA 与冻结 App 完全一致。源码为 `d3ad6281c919de293642acb1e34bd3376dcdf315`；运行中没有构建、改代码、改断言或重新打包。

仅列明下表七个类，显式 `liveV2=true`、`liveLogin=true`，没有运行整个测试 package。**19/19 通过，126.455 秒；逐项状态码为 19 次开始、19 次成功，没有失败／跳过。**

| 最终包真机测试类 | 通过数 | 实际覆盖 |
| --- | --- | --- |
| `V2PlayerErrorFocusTest` | 4 | 错误主／次操作 D-pad、Header 不抢焦点、全屏 Back、恢复按钮边界与回调 |
| `V2CatalogViewportTest` | 1 | 第五行排序后立即显示首行、追加保持深处、离开返回原查询恢复焦点／位置 |
| `V2CatalogUiTest` | 4 | 筛选浮层、年份方向、更多筛选草稿、取消保留位置、未知总数 |
| `V2PlayerUiTest` | 4 | 八控制、全屏显隐与几何、十集分组、速度、禁用跳转导航 |
| `V2RootJourneyUiTest` | 3 | 真实 Root 路由、来源焦点与滚动恢复、搜索状态／退出确认 |
| `V2LivePlaybackTest` | 1 | 普通真实片源经正式 NativePlayer 首帧／进度／格式、全屏按键与隔离历史 |
| `V2AccountReadTest` | 2 | 保存会话 userinfo、VIP 基础 HTTPS 源、收藏和云历史只读 API |

安全数值标签确认：真机实际 fontScale `1.0`，全屏登录按钮底部 `110 dp`、重试底部 `158 dp`，控制顶部 `468 dp`，Root `960×540 dp`；Back 返回仍聚焦 `player-error-primary`。账号端点接受保存会话，group `3`；VIP 源投影 `baseHttps=1 / baseEmpty=0 / premiumCount=0 / premiumHttps=0 / restricted=true`，两项只读测试均完成，未跳过收藏或云历史读取。没有打印账号、密码或播放 URL。

本批前 16 项使用合成目录／媒体状态等隔离夹具，最后一项真实普通播放使用独立历史命名空间，账号两项只读；没有为了通过而写真实收藏、删除观看记录或重新登录。错误恢复的登录回调由夹具计数验证，不冒充“真实登录页面认证后返回同片”的完整网络旅程。目录追加夹具不证明实际分页请求次数；VIP 此批验证可读 HTTPS，解码和长播仍引用前述各自包的独立证据。

将基础 45、真实目录 1、普通媒体 1、控制专项 1、保存账号 2、VIP 解码 1，以及本批新增错误 4／Viewport 1 按 `(class, method)` 去重，**56 个用例在各自记录的批次最终通过**。本批 19 中的 14 个重复既有方法，不累计为新增用例；“本批失败 0”仅指本次 19 项，先前失败及其修复证据继续保留。

| 最终安装／本机证据 | SHA-256 |
| --- | --- |
| `.tools/chromecast-v2/app-final.apk` | `8778ae82d97cc859f4aacf356834a7a144b96a3bc5d8406f5eec753e162499cc` |
| `.tools/chromecast-v2/installed-final-app.apk`（由电视拉取） | `8778ae82d97cc859f4aacf356834a7a144b96a3bc5d8406f5eec753e162499cc` |
| `.tools/chromecast-v2/test-final.apk` | `beb08425c10de7e8acd9d12e6d9cecbca211e8c98acb25a832cfdf5e9edd8302` |
| `final-device-install.log` | `50daa2ba76a5c9784728abf9461823bc72d3c4835986fc7ba7cdd96abebea397` |
| `final-installed-sha.txt` | `0c072bbf01710154ac9bbd27beb2b43c99705454fcc417ba09d95d434bf4cfeb` |
| `final-device-regression.log` | `5a73f8cca0ddadfd0f7194cfdca5e214d506c28163bb1d4007efaeee35776823` |
| `final-device-values.log` | `67134919712d2574e2b7c2dbeb0897a9e219b291122bfc0f9cfdfc6db3cda5a6` |

日志均位于 `.tools/chromecast-v2/`。instrumentation 完成并读取安全标签后，独立 agent **明确交回 Chromecast 和所有构建／设备操作**，主执行 agent 才进行最终手动截图；之后独立 agent 仅解析本机文件并更新此报告，未提交。
