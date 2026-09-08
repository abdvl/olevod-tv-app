# UI v2 根路由旅程验证

2026-09-08 · **3 个根旅程均有通过记录，0 跳过；分两轮完成，不能表述为一次 3/3。** 同批播放器 4 例一次全部通过。首次 7 例运行 6 通过、1 失败；只修测试的跨窗口焦点定位后，在同一冻结 App APK 上重跑失败的 mini 根旅程，1/1 通过。

测试文件：`app/src/androidTest/java/com/olevod/tv/V2RootJourneyUiTest.kt`。

与此前页组件测试不同，三例均挂载真实 `OlevodApp(preview=true, vm=V2FixtureViewModel.vm)`，经过正式 Header、SaveableStateProvider、BackHandler 与来源路由；不使用替代目的页。所有导航、选择、返回均通过 `Instrumentation.sendKeyDownUpSync` 的遥控键完成。状态恢复仅使用 `StateRestorationTester.emulateSavedInstanceStateRestore()`。

| 用例 | 实质断言 | 规范关联 |
| --- | --- | --- |
| 最近第 3 项 → 实际播放器 → Back | 真实 PlayerContent 首焦为全屏；影片标题正确；回原第 3 项及原 y；Up 回首页、Down 再入同来源；预览记录未写库 | NAV 来源恢复、HOME、FULL-06 |
| 电影小首页 → 浏览全部 → 实际目录 → Back | 遥控逐行到末尾浏览全部；实际目录分类弹层选择电视剧；返回仍为电影小首页、原浏览全部焦点和 y；恢复后上下仍有效 | R08、NAV 来源恢复 |
| 搜索确认 → 首结果 → Back 输入 → 保存恢复 → 退出确认 | 已提交词及结果恢复；输入框保留焦点，不重放首结果聚焦或打开播放；第二次 Back 回首页；退出默认继续，Back 可取消，仅显式退出调用 onExit 一次 | SEARCH-03、STATE-01、EXIT-01/02 |

## 范围与已知约束

- ViewModel 的偏好与数据库使用独立命名空间，不读取真实账号；不访问 AccountPreviewFixture、不请求验证码、不登录、不发起影片 API 或媒体播放。公开预览海报可能由 Coil 加载，测试不以图片下载成功作为焦点断言的前提。
- PlayerPreviewFixture 使用实际 PlayerContent，但虚拟媒体状态固定；本文件不能证明 ExoPlayer 续播位置、音视频、释放或 Chromecast 行为。`pendingResume.movie.id` 只验证根入口保留最近来源影片。
- 写测试期间，主实现已将 CatalogPreview 接入根分类初值与分类回调，并为完整 Filter 添加 saver；只读确认已落地。第二例因此会实际改变根 category 和目录路由 key，再检查 Back 恢复电影来源。目录结果仍为公共静态样本，本测试不证明服务端按新分类返回结果。
- 本轮选择搜索验证保存恢复，未声称目录筛选已通过重建恢复。
- StateRestorationTester 验证 Compose 保存状态重建，不等同 Activity 销毁／进程被杀／后台恢复。主实现已用保存的 launched 标志限制首页冷启动焦点；本次根旅程已验证恢复到离开前的搜索输入框，不额外请求焦点掩盖故障。
- 退出通过 onExit 计数验证调用边界，不真正 finish 测试 Activity。

## 构建与冻结产物

第一回构建被另一份 `V2LivePlaybackTest.kt:38` 缺失 `getOrNull` import 阻断，日志 `.tools/v2-root-independent-build.log`；主实现补齐导入后，独立构建 `:app:assembleDebug :app:assembleDebugAndroidTest` 成功，日志 `.tools/v2-root-independent-build-2.log`。冻结 app/test APK 后才通知主执行 agent 继续公共源码。

| 冻结产物 | SHA-256 |
| --- | --- |
| `.tools/v2-root-verification/app-debug.apk`（两轮相同） | `8181421a3696e5c8002184a45c37907be60890852aa44712dfe0008e34ace97c` |
| `.tools/v2-root-verification/app-debug-androidTest.apk`（首轮） | `74908d2547de6121ddc6bd1b1275e91c8d5a9b66ffc99af8903b15ca59630d6e` |
| `.tools/v2-root-verification/rerun-2/app-debug-androidTest.apk`（定位修正） | `f0be4f656c717f7c04a986dd49641edab2d543ca5e1cbd641b8bd5797439c5b3` |

仅安装并操作 `emulator-5554`，安装均 Success，未更改设备字体、未使用真实 Chromecast。复验仅重建测试 APK（`.tools/v2-root-independent-build-3.log`），没有替换冻结的 App APK。

## 分轮结果

首轮限定 `com.olevod.tv.V2RootJourneyUiTest,com.olevod.tv.V2PlayerUiTest`，日志 `.tools/v2-root-independent-tests.log`：**7 例、6 通过、1 失败，24.067 秒，0 跳过**。

- 搜索保存恢复＋退出确认：通过。已提交词和结果保留；恢复后输入框仍聚焦、结果不聚焦、未打开播放器；默认继续与 Back 都未调用退出，明确选退出只调用一次。
- 最近第 3 项真实播放器往返：通过。播放器默认全屏控件获得焦点，电影正确；Back 回原第 3 项及原 y，Up/Down 往返仍有效。
- mini 根旅程：失败于 `Expected root focus option:1, actual`（空）。原测试 helper 使用 `onNode(isFocused())` 强制所有 Compose 窗口只存在一个聚焦节点，并吞掉查询异常；该方式不能可靠定位 Android Dialog 内的焦点。
- 播放器 4 例：均通过；详见 [V2-07.md](V2-07.md) 末尾回归记录。

修正测试 helper 为直接读取 `onNodeWithTag(tag)` 的 `Focused`，保留同样的目标和 `assertIsFocused()` 断言；新增失败时列出所有聚焦节点的诊断。没有直接请求焦点、跳过弹层或修改生产代码。

复验限定 `com.olevod.tv.V2RootJourneyUiTest#miniBrowseAllReturnsToMovieCategoryAndSameScrolledButton`，日志 `.tools/v2-root-independent-tests-2.log`：**Runner `OK (1 test)`，3.869 秒、0 失败、0 跳过**。分类弹层确实从电影选到电视剧并改变根目录分类，再 Back 回电影 mini 的原浏览全部按钮、原 y；恢复后向上进末排海报、向下回按钮。

本轮结果仅适用于以上冻结 APK；后续生产源码变化不自动获得通过结论。完成后已将设备与 Gradle 交回主执行 agent，独立验证只改测试与报告，未提交生产代码。
