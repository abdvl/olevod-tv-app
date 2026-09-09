# UI v2 Chromecast 真机验证

2026-09-08 · 分支 `codex/ui-v2` · 测试基线提交 `c8e50e7`。

**按最终用例结果计：48 项通过；保存账号的 VIP 片源检查 1 项失败。** 已保留数据覆盖安装新版。普通播放与核心 UI 回归通过，VIP、现场音画与长时稳定性尚未完成验收。

## 环境与安装

| 项目 | 实际读数 |
| --- | --- |
| 设备 | Google Chromecast with Google TV，`sabrina` |
| 系统 / ABI | Android 14；`armeabi-v7a,armeabi` |
| 显示 | 物理 3840×2160；override 1920×1080；density 320 |
| App APK SHA-256 | `e14387a733c072d46a78b7af722336893115dd7409ba8deaf549cbed725587ce` |
| 基础批 Test APK SHA-256 | `6e1eb0ea9ad0215d6a9d08daef4df3da0712b9b6778c794cb71eea1e6b712915` |
| 安装 | App 与测试包分别 `install -r` 成功；未卸载或清除数据 |
| 签名 | 原安装与新 APK 的 debug 证书 SHA-256 一致：`65e3a78be6cfa68cabc71ed91c4b4c76253e612193cead838cc1f2d287c13f6e` |

旧连接端口拒绝连接，原生 Bonjour 后发现当前连接服务；沿用已有 ADB 配对成功连接。无线调试的连接端口会变化，不将配对端口当成连接端口。未将配对码、账号凭据或播放签名 URL 记入本报告。

## 独立验证

另一 agent 独占设备执行 12 个 V2 测试类的 45 项界面／状态回归，再单独执行真实目录与普通播放测试。完整范围见 [独立报告](CHROMECAST_INDEPENDENT.md)，原始日志仅保存在本机 `.tools/chromecast-v2/`。

| 验证 | 结果 |
| --- | --- |
| 12 类 V2 界面／状态 | 45/45，0 失败／跳过，225.692 秒 |
| CatalogIntegrationTest | 1/1，9.597 秒；真实分类、排序、分页和搜索 |
| V2LivePlaybackTest | 1/1，19.508 秒；普通首帧、正宽高、进度、全屏显隐／返回、隔离历史 |
| V2DevicePlaybackTest | 1/1，29.889 秒；四种实际 seek、1.5 倍速、全屏连续性、Activity ON_STOP 暂停和离开保存／释放 |
| V2AccountReadTest | 0/1；VIP 样本返回空地址，收藏／云历史断言尚未执行 |

控制专项首轮因测试方法返回类型导致JUnit初始化失败，修正后通过；该首轮及VIP两次失败日志均保留，详见独立报告与下文，未从记录中删除。

控制专项增加前后生产 App 散列相同。新增测试包运行通过时 SHA-256 为 `78e6633c161b790f1e5ac50cbf74ac241a0dac028df903bb02508e8793cf7415`；最终补充 VIP 诊断断言后的测试包为 `fa8807579e3e11a5338be0376515f67708d44072f1744188608efa8658ff3cf0`，构建成功并安装。未把最终测试包说成重新运行过45项。

`V2DevicePlaybackTest` 显式启用方式：

```sh
adb -s "$ANDROID_SERIAL" shell am instrument -w -r \
  -e class com.olevod.tv.V2DevicePlaybackTest -e liveDevicePlayback true \
  com.olevod.tv.test/androidx.test.runner.AndroidJUnitRunner
```

实际平台位置为：+5分钟 `0→300000`、−30秒 `300000→270000`、+30秒 `270000→300000`、−5分钟 `300000→0`；全屏显隐两轮均保持 `300000`。后台返回 `305196→305196`。测试允许1000ms误差，本轮恰好相等；不扩展为全部片源的逐帧精确seek。ActivityScenario触发ON_STOP并不等同真实系统Home或系统杀进程。

夹具使用隔离存储；实际普通媒体测试的历史也写入隔离空间。没有运行整个未筛选的测试包，以避免旧收藏／云历史测试改变用户数据。

## 实际应用旅程

| 旅程 | 实际结果 |
| --- | --- |
| 真实首页 | 单行导航默认首页；原本五部最近播放与各自集数／时间均保留；精选推荐使用横幅和大标题。 |
| 真实搜索 | 自绘键盘输入 MN；向右到首联想“魔女”，确认后第一张结果卡有焦点；一次 Back 回输入框，保留36部结果。 |
| 真实目录 | 最近更新状态显示4125部，五个紧凑筛选入口及完整竖海报／标题／评分可见。进一步手动排序／深列表操作的前台状态发生变化，未将该段记录为通过。排序API及焦点夹具另已通过。 |

账号和既有私人观看记录截图仅留本地，不混入公开截图集。UiAutomator 在部分采样中返回 null root；这些失败采样不用于确认焦点，也不复用旧 XML，改用本次新截图及独立 instrumentation 断言。

在手动目录阶段，后续画面出现播放器和电视剧榜单，与预期按键路径不一致。已询问是否存在同步遥控操作并暂停继续发键；没有把来源不确定的截图当成该目录步骤通过。独立批次在此之前已明确交回设备。

## VIP 未通过项

使用设备原有保存会话，不重新登录，也不读取或输出账号凭据。`V2AccountReadTest` 的 token非空与VIP详情存在剧集断言通过，随后“每条地址为HTTPS”断言失败。为定位问题仅改进测试错误文字，保持原断言；第二轮输出 `VIP source schemes (URLs omitted): {empty=1}`，2.461秒、1项失败。

因此当前证据是ID80632的一条剧集地址为空；尚不能区分会话权益、站点策略或该样本资源原因，也不能归因Chromecast解码器。该方法在VIP失败后终止，后面的真实收藏、云历史读取没有运行。没有通过放宽HTTPS断言、跳过失败或更改用户账号来伪装成功。

原始失败日志保存在 `.tools/chromecast-v2/saved-account-read.log` 和 `saved-account-read-2.log`。后续先核对有效登录／会员权益与官网同ID返回，再重跑VIP和独立私有只读检查。现有生产播放器对空地址显示不可用提示；本轮不据未确认原因改写服务协议。

## 截图

以下均来自 Chromecast 实际应用、真实公开片单及本轮明确输入的查询，未经图片编辑；散列与尺寸见 [manifest.json](chromecast-screenshots/manifest.json)。

![真实MN联想](chromecast-screenshots/search-suggestion.png)

![确认后首结果聚焦](chromecast-screenshots/search-result.png)

![返回到输入框](chromecast-screenshots/search-return-input.png)

![真实电影目录](chromecast-screenshots/catalog.png)

## 证据范围

- 在实机执行夹具测试证明该设备上的布局、焦点与状态行为；夹具播放器不证明真实解码。
- 实际媒体的首帧回调、播放状态和解码格式与现场听感分开报告。
- 4K 屏幕信息、VIP 栏目或 1080p 媒体不能证明 4K/HDR 片源通过。
- 既有 v0.1 用户音画反馈不用于宣称 UI v2 音画已确认。
- 本轮没有完成30分钟持续播放、真实4K/HDR源、听感／同步、真实系统Home／进程重建、全套放大字体及屏幕阅读器验收；这些继续保持待测。未发布新版本或改变正式签名。
