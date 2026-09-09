# 搜索首批失败后的遥控器重试回归

最终构建在 `emulator-5554` 上 **V2SearchUiTest 7/7 通过，17.855 秒**；`assembleDebug`、`assembleDebugAndroidTest`、`testDebugUnitTest`、`lintDebug` 全部成功，JVM **47/47 通过**（无失败、错误或跳过）。旧实现新增的 2 项测试均失败，首轮修复 6/6 通过，证据均保留。此前指出的一帧导航取消边界已修复，新增受控时钟回归通过。独立代理仅按主代理明确授权额外修改 Row 的窄取消条件；未操作 Chromecast 或 emulator-5556。

## 冻结构建

- App SHA-256：`9babf76b2d166ff9538cc76c07a6cf0a607d257f80631e0dc8b7823ca3361926`
- Test SHA-256：`e06015807c11ba4d2620cdba8f8a7017156697aeb5f051ffbecb1400889f027d`
- 独立核对 SHA 后使用 `adb -s emulator-5554 install -r` 安装两包，保留应用数据。
- 仅执行 `-e class com.olevod.tv.V2SearchUiTest`，未运行无筛选的测试集。
- 使用 `SearchFixture`、隔离的 `V2FixtureViewModel` 和受控 `CatalogFeed`；无真实搜索网络请求、登录或账号秘密。

## 旧实现反例

旧实现 `enterResults` 只有 movies 非空才执行，结果错误按钮没有可进入的显式焦点目标；同词确认没有显式重载失败的已有 feed。

旧实现测试运行 **11.077 秒，2/2 失败**：

1. `failedConfirmedSearchCanEnterRetryAndRestoresFocusAcrossBusySuccessAndBack`：输入 MN 并确认“魔女”，首批请求抛出受控 IOException、无卡片。在中栏按 Right 后，“重试”节点存在但 `Focused=false`，断言失败。
2. `confirmingSameSuggestionAfterFirstPageFailureRetriesExistingFeed`：相同错误状态再次确认“魔女”，等待第 2 次请求 5 秒超时，证明没有重试既有错误 feed。

原始结果（只含测试诊断，无账号）：`.tools/v02-ui/search-retry/search-retry-old-test.log`。旧构建首次因 sandbox 本地 socket 权限失败，授权使用本地 Gradle socket 后构建成功；该环境失败不算产品回归。

## 首轮修复 6 项结果

| 测试 | 结果与覆盖 |
|---|---|
| failedConfirmedSearchCanEnterRetryAndRestoresFocusAcrossBusySuccessAndBack | 通过：Right 进入重试，焦点导航不发请求；OK 后重试按钮移除前回输入；阻塞的第 2 请求成功后首卡获焦点；Back 回输入，不自动播放 |
| confirmingSameSuggestionAfterFirstPageFailureRetriesExistingFeed | 通过：确认同词明确发第 2 请求，成功首卡获焦点，Back 回输入，无意外播放 |
| mnSuggestionConfirmationWaitsForFirstResultAndBackReturnsToInput | 通过：成功响应前保持联想焦点，成功后首卡，Back 回输入 |
| backDuringConfirmedSearchCancelsLateFocusTransfer | 通过：请求中 Back 后迟到响应不抢焦点 |
| newerTypingCancelsOldFeedAndLateResultCannotReplaceOrStealFocus | 通过：新输入取消旧 feed，非协作的迟到响应既不填充旧 feed，也不替换当前结果或抢焦点 |
| suggestionFailureRetriesFromKeyboardWithoutClearingQueryOrLiteralResults | 通过：联想自身失败可重试，保留输入和字面结果，成功确认后正常进入首卡 |

首轮修复 App SHA：`1621d3ff5cc65d82f84396bf48ac6512627693895e97b0a0cdbfede967ad06f1`，Test SHA：`22d82ae97d40f19a38121b626e04c53c2ac591f273acafc8ac7c8c99ecb27aee`。结果：`.tools/v02-ui/search-retry/search-retry-fixed-test.log`，`OK (6 tests)`，`Time: 17`。本轮没有更改根路由，且搜索集已直接覆盖 Back，因此未重复无关的 Root3。真实接口可用性、网络恢复速度、末页重试及硬件设备体验不由这 6 项夹具测试证明。

## 独立源码审查

- 错误入口新增稳定 `retryResults`，滚到状态行后等待一帧再请求焦点；同时核对当前查询及空结果错误状态，避免旧查询结果目标直接被沿用。
- 重试回调先请求仍挂载的输入框，再发 `loadNext`；`loadNext` 同步设置 loading/清 error，因此按钮移除期间有明确焦点落点。仅空首批重试设置首卡焦点意图，尾页重试不强迫跳首卡。
- 同词失败确认显式调用当前 feed 重试；feed 的 loading/endReached 保护避免重复在途请求，取消 generation + ensureActive 拒绝旧查询迟到响应。
- 原有成功焦点意图在 Back/输入/方向键时撤销；上表两个迟到响应测试继续通过。

## 已处理的一帧导航取消边界与最终 7 项回归

初次源码审查指出：`enterResults` 的导航协程等待一帧时，如果用户立即 Back 或 Left，原代码没有取消该协程，可能迟到抢焦点。主代理随后为两个结果入口分支统一保存 `resultEntryJob`，并在 Back、输入变化、联想确认、旧 feed dispose 时取消。经明确授权，独立代理将 Row 的条件进一步收紧为任何新 KeyDown 都取消挂起导航，`focusIntent` 仍仅方向键清空，避免确认或打开输入法时继续执行旧导航。

新增 `backBeforeResultEntryFrameCancelsPendingRetryFocus`：

1. 受控首批失败，无卡片，焦点在已确认的“魔女”联想。
2. 设置 Compose `mainClock.autoAdvance=false`，发送 Right。
3. 在时钟仍暂停时断言重试未聚焦、联想仍聚焦，确保导航尚未跨过所等待的帧。
4. 发送真实 Back 键后推进时钟 500ms，恢复自动时钟。
5. 输入框保持焦点，重试仍存在且未获焦点，无播放动作。

该测试没有用 wall-clock sleep 碰运气；第 3 步明确核对测试所需的在途状态。最终同一冻结包执行全部 7 项搜索测试，**17.855 秒、7/7 通过**；新增测试证明错误入口 Right→Back 的取消路径。已有迟到搜索响应、输入变化和成功焦点测试继续通过。非空结果入口也使用相同可取消 Job；未单独穷举所有按键/每种内容的帧间组合，不能将此结果扩大为所有时序或硬件设备的验收。

最终日志：`.tools/v02-ui/search-retry/final-seven-test.log`；构建与 lint/JVM 日志：`.tools/v02-ui/search-retry/final-build.log`；哈希：`.tools/v02-ui/search-retry/final-sha256.txt`。47 项 JVM 测试的 XML 汇总为 0 failures / 0 errors / 0 skipped。当前审查范围内没有保留未解决的导航取消警告。

本次 token 统计截止未调整，也未更改 README 或 PROGRESS。
