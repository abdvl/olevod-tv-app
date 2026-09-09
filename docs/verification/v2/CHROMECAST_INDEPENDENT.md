# UI v2 Chromecast 独立回归

2026-09-08（America/Los_Angeles；最后环境读取 2026-09-09 01:34:27 UTC）。独立验证 agent 在主执行 agent 完成保留数据的安装后独占设备运行；结束后明确交回设备，双方没有同时发送遥控键。

**真机基础批 UI／状态夹具 45/45、真实目录服务 1/1、普通实际媒体 1/1 通过，均无失败、错误或跳过；后续实际媒体控制专项修正测试初始化问题后 1/1 通过。** 首轮专项初始化失败完整保留在文末。本报告证明下述具体断言在 Chromecast 执行成功；不将其扩展为声音、音画同步、VIP／4K／HDR或全部设计要求通过。

## 环境与冻结包

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
