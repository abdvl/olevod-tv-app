# 实施进度 / 恢复入口

最近更新：2026-09-07。

## 当前检查点

这是**可运行测试版**，不是所有首版需求均已验收。目标设备：Google Chromecast with Google TV（实机代号 sabrina，Android 14；用户已确认普通点播声音和画面正常）。

| 步骤 | 当前状态 |
| --- | --- |
| S01–S06 | 完成：工程/界面/API与真实首页 |
| S07–S08 | 真实分类、四种排序和跨页、多条件筛选、中文搜索通过；独立UI搜索与筛选通过 |
| S09 | 普通点播与控制已独立通过；VIP80632原生播放成功 |
| S10 | 原生验证码登录、磁盘加密和跨进程会话已实际通过 |
| S11–S12 | 独立通过：CCTV13、东方卫视（重试恢复）、午夜新闻回看原生播放 |
| S13 | 本机25条、重开与账号隔离通过；独立UI验证记录和返回焦点 |
| S14 | 影视收藏增删通过；频道列表可用，取消接口60秒回读未生效 |
| S15 | 云历史分页读取、新观看同步及真实回读通过；离线补传未实现 |
| S16 | 焦点、后台播放、账号竞争、异常恢复及云同步顺序已修，独立复查通过 |
| S17 | build/lint/20单元测试通过；Chromecast已配对安装、实机登录通过 |
| S19–S28 | 账号记忆、导航展开、分类/搜索重设计、无限滚动、播放控制、底部留白、历史卡片与明确方向键焦点已实现；新增项最终实机复核中 |

- 登录依赖已解除：用户授权本次登录调试代填验证码，Android同客户端验证码登录已成功；Chrome因Mac锁定暂不可用。
- 下一步：完成新增账号记忆、验证码数字键盘、导航与续播的实机验收。用户已明确暂缓直播稳定性排查，保留现有直播功能及问题记录。
- 用户已授权每步本地commit、使用Android Studio、独立agent验证和`.secrects`测试账号。未推送远端。
- 恢复时保留已完成提交，先检查未提交修改和本表的“待验收”。

## 已完成记录

### S01 — chore: establish resumable implementation checkpoints

- 保留用户 `.secrects` 忽略规则，补全 Android 缓存、构建产物与密钥忽略。
- 将执行计划拆为 S01–S17，每个可验证步骤独立提交。
- 启动独立 verification agent 检查环境与后续回归。
- 验证：仓库未跟踪凭据；忽略检查通过。
- 环境现状：Android Studio 已安装；JBR25；SDK 有 android-37.0 和 build-tools36；没有 TV AVD。准备独立稳定构建工具链，不改变用户 IDE 配置。

## 恢复方法

1. 阅读本文件、`IMPLEMENTATION_STEPS.md`、最近 5 个 commit；检查未提交修改。
2. 先恢复当前步骤，不从 S01 重新开始。
3. 以实际构建/测试报告为准。预览数据、浏览器播放、模拟器播放和 Chromecast 实机测试必须分别标注。
4. 遇到验证码或设备连接需求，可继续不依赖它的功能；不要假称登录/实机验收通过。

### S02 — build: bootstrap native Android TV application

- 固定 AGP8.9.2 / Gradle8.11.1 / Kotlin2.1.20 / JDK17 / SDK35；工程可由 Android Studio 打开。
- TV launcher、图标、横屏、无需触屏、基础主题和 Gradle wrapper。
- 实测 `assembleDebug` BUILD SUCCESSFUL，首个 APK 约20MB。
- 独立 agent 准备 Android TV API34 ARM64 1080p，emulator-5554 已启动。
- 下一步：原生预览页面与截图。

### S03 — feat: add native TV visual preview screens

- 首页推荐/双行卡片、浏览筛选、字母搜索键盘、左右播放器、直播、登录与空历史/收藏页面。
- 明确标注界面预览；公开影片样本不包含测试账号。
- `assembleDebug` 成功；模拟器实际启动和网络图片加载成功；修复 Activity 窗口初始化崩溃。
- 独立 agent `assembleDebug lintDebug` 通过。
- S04 已发现：搜索页底部裁切和聚焦输入框自动弹 IME，修复后再交付截图。

### S05 — feat: implement verified Olevod API adapter

- 完成时间签名、动态图片根地址、分类元数据、完整筛选位置、搜索特殊响应、详情与直播模型。
- API会话注入和媒体客户端分离；API不跟随跨站重定向；错误不回显凭据。
- 5项单元测试通过：签名向量、参数顺序、特殊搜索词编码、业务错误脱敏、跨站鉴权隔离。
- guest独立HTTP实测目录/搜索/普通详情/直播详情成功；登录/收藏写入仍待验收。
- UI修复与独立复测正在S04中进行。

### S04 — fix: refine remote focus and preview layouts

- 独立agent完成模拟器 D-pad UI 检查并记录报告。
- 修复搜索裁切、输入框吞上下键、全屏控制焦点边距。首页采用边缘滚动与分类组定位，并增加组底部安全间距。
- 构建与lint通过；真实API/播放、返回焦点恢复另由后续步骤验收。

### S06 — feat: load real home recommendations and categories

- 首页加载真实推荐、网站分类和每类最近更新10部，分区独立失败/重试。
- 预览入口仅debug可用；正常启动直接请求API。
- 模拟器在线实测推荐与短剧等分类加载，截图 artifacts/screenshots/05-live-home.png。
- assembleDebug通过；暂无Chromecast实机连接。

### S07 — feat: connect catalog filters and pagination

- 网站动态地区/年份/子类型，会员/首字母及更新、添加、热度、评分排序。
- 每页20部、明确上一页/下一页，便于遥控器定位；返回保留筛选和滚动状态。
- 模拟器真实电影目录显示成功；API参数顺序由单元测试验证。
- 独立API审查发现直播组字段为title，已修正；新增契约测试另随API检查提交。

### S08 — feat: connect remote search to grouped site results

- 热搜、400ms防抖、取消旧查询、12条分页、系统输入法与遥控器字母键盘。
- 实测发现data.data是按type分组，已按vod.list/total读取。
- 独立agent新增真实分组回归，12项协议单元测试全部通过；空结果和热门词加载已模拟器检查。
- 登录验证码已显示给用户，等待输入；其他工作继续。

### S09 — feat: play on-demand video with Media3

- 原生HLS、系统MediaSession、暂停/播放、±30秒、速度、选集、全屏、错误重试与后台暂停。
- 模拟器普通影片实际视频画面已显示；dumpsys media_session为PLAYING且进度递增。
- 已交给独立agent回归遥控器控制，报告待补；VIP随登录后验证。

### S10 — feat: add TV login and encrypted session storage

- 直接电视输入账号/密码/图片验证码，刷新验证码、登录状态与退出。
- Android Keystore AES-GCM加密token，仅会话落盘，不保存密码/验证码；禁止Android备份。
- assembleDebug与协议测试通过；真实账号登录待用户填写验证码，尚未验收VIP/收藏写入。

### S11 — feat: add native live channels and programme guide

- 央视/地方分类、频道分页、北京时间日期与节目单、独立MediaSession直播播放器。
- 采用服务端返回的detail.hls，不擅自选择会员候选地址。
- assembleDebug通过；等待模拟器交还后验证两频道播放。

### S12 — feat: add programme replay selection

- 仅过去且hasVod的节目显示回看操作；使用网站回看授权接口，支持返回直播。
- 回看支持±30秒，节目日期以Asia/Shanghai显示；不假设直播支持DVR。
- 回看请求字段/节目资格已有独立契约测试；真实媒体播放待模拟器实测。

### S13 — feat: persist complete device history and resume playback

- SQLite持久化完整点播历史，无18条截断；按账号哈希与访客隔离，不保存片源或token。
- 每10秒、后台与退出保存进度；重新打开优先本机续播；单条删除/确认清空。
- 加入超过18条、数据库重开、账户隔离的模拟器测试，APK测试包构建通过，待运行。
- 同步修复点播非全屏首键吞掉、全屏唤醒焦点与Media3 lint标记。

### S14 — feat: add account movie favorites browser

- 网站影视收藏分页列表、登录入口、播放页收藏/取消与失败反馈。
- 增删接口请求已通过契约测试；账号实际写入与频道收藏仍待登录后验证。

### S15 — docs: record cloud history protocol boundary

- 网站历史页读取本地记录并发同步；匿名history/list和watches/sync均code12。
- 未登录不能验证全部云记录或安全同步，因此没有把未知字段自动写回账号。
- 详细待验证步骤见 CLOUD_HISTORY_STATUS.md。

### S16 — fix: harden TV focus, sessions and playback lifecycle

- 输入框仅确定后进入编辑框；补全联想词分组解析、本地搜索历史、列表卡片返回焦点。
- 直播时间签名与布局修正；频道收藏使用网站明确channelId/favoriteType字段，影片收藏优先vodId。
- 独立审查驱动修复：异步详情返回不得后台起播、历史账户在播放开始捕获、暂停后台不刷新观看时间、切集先保存。
- 会话过期清除本地token，以网站userId分账户；媒体URL不进入数据类日志输出。
- 当前assembleDebug、lintDebug与13项协议测试通过；Android数据库集成测试通过；UI复测仍在独立agent中。

### S10补验 — fix: verify native CAPTCHA login across processes

- 首次简略测试OK不能证明实际登录，已纠正；重复旧码与电脑获取的新码都收到网站code15（验证码错误）。
- 改为同一个Android API客户端获取验证码并等待授权输入，raw测试状态0与成功文件确认登录完成。
- 后续独立进程读取Keystore会话成功，profile groupId3，VIP/收藏/云历史/直播详情均正常返回。
- 会话保存改为IO线程同步commit，新增真实磁盘密文测试；历史+密文共2项Android测试通过。
- 真实账号测试改为显式liveLogin参数加入，缺输入不再以普通OK误判；验证码/密码不入Git或APK。

### S11/S12补验 — 会员直播与回看

- 按实际账号会员等级选择网站返回的授权HLS，附公开时间签名；不向媒体主机发送账号JWT。
- 独立agent实测CCTV13、东方卫视重试恢复及午夜新闻回看均有实际画面和MediaSession PLAYING。
- 首页两行全部图文与分类标题完整，进入第二行影片再返回恢复原焦点/滚动。

### S15 — feat: read and sync authorized account watch history

- 用户明确授权新观看记录同步；云列表每页20条，正确解析影片ID、集数及秒单位。
- 本地和网络分队列按序处理，30秒节流及暂停/退出触发，账号冻结和切换检查，失败保留本地。
- 真实账号同步/回读测试与只读账号测试共2项通过；进度117秒/总时长1050秒匹配真实VIP观看。
- 云上传为运行时尽力完成，不承诺杀进程后的离线补传。

### S14 supplement - account favorites validation

- Movie favorite save/list/cancel passed; original account state restored.
- Channel cancellation uses the public website endpoint and fields. Business code 0 did not converge after a bounded 60-second read-back. This test remains FAILED.
- Buttons reject repeated clicks while pending; channel saves are idempotent and displayed channels are deduplicated.
- Final read-only check: exactly one original CCTV13 favorite, record ID 687; no additional channels.

### S17 - emulator validation checkpoint

- assembleDebug, assembleDebugAndroidTest, lintDebug and 17 unit tests passed.
- Native encrypted login, cross-process session, local history storage, actual VIP playback and cloud watch sync/read-back passed.
- Independent agent verified CCTV13, Dongfang after retry, programme replay, full two-row home and card focus restoration. Ordered cloud queue fix reviewed independently.
- Cloud history UI displays the actual account records. Chromecast audio, HDR/4K and sustained playback remain pending physical-device connection.

### S10/S17 - physical Chromecast installation and login

- User supplied a LAN Chromecast and explicitly enabled debugging. Standard port 5555 was closed; device advertised its wireless TLS connect/pair services through mDNS.
- Paired using the user-provided code, installed current debug APK, launched native home. Android 14, physical display 3840x2160 and logical override 1920x1080 reported.
- Fixed login helper to select ANDROID_SERIAL and atomically rename a completed private input file; Wi-Fi transfer previously exposed an empty file to the test reader.
- Native CAPTCHA login passed after correcting image recognition. A fresh instrumentation process read encrypted session and authenticated profile/VIP/history successfully.
- Independent agent now owns physical-device regression; no pairing code, password or token is stored in these notes.

### S07/S08 - live catalog coverage

- Read-only Android integration passed for all six homepage categories with 10-item retrieval.
- All four sort modes returned distinct first/second pages with 20 items each.
- Combined USA / 2025 / free / score filter returned matching results; Chinese search matched the requested title.
- Test was executed on emulator-5554 while the independent agent owned Chromecast, without interrupting physical playback.

### S16 - prevent stale request session invalidation

- Capture the token used to construct each API request. Only invalidate the session if the response still belongs to the active token.
- Regression covers old-token expiration after a new token is active, and expiration of the actual current session.
- assembleDebug, lintDebug and all 18 unit tests passed. Physical device will receive this API-only update after the independent UI run.

### S16 - fullscreen Back key

- Independent physical test exposed hidden controls consuming the first Back key.
- Back now bypasses the video wake-control handler and reaches the existing page handler.
- Emulator verified hidden-controls UI had no text, then one Back restored the header/details/fullscreen button. Build/lint/18 unit tests passed. Physical confirmation follows next install.

### S18 - user-requested login memory and navigation shortcuts

- Explicit user request replaces the earlier session-only retention choice: username/password now live in a separate Android Keystore AES-GCM vault. Logout retains it; a clear action removes it.
- Login form autofills remembered credentials and initially focuses the on-screen numeric CAPTCHA keypad. Digits can be selected directly with the remote, without obscuring the CAPTCHA with the system keyboard.
- Global header has accessible icon-only Home and Movie Directory shortcuts. Directory always selects the movie category.
- Encrypted synthetic credentials passed disk reopen/no-plaintext/session-logout/clear testing. User-provided test credentials were privately initialized on Chromecast and read back successfully.
- Build/lint/18 unit tests passed. Independent agent is validating the new controls on physical Chromecast; no new APK installation until that run finishes.

### S20 - ordered credentials and continuous result feed

- Credential save, clear and pre-login save share a single queue; login awaits acknowledgement. Immediate in-memory state prevents stale autofill after clear. Clear uses a checked synchronous disk commit on IO.
- Independent review confirmed the write/clear race is fixed.
- CatalogFeed appends and deduplicates batches, retains old results on error, retries the same page and stops at end. Bounded per-filter/per-query caches retain results when returning from a movie.
- New regression covers concurrent-load rejection, failed append, retry and end-of-list. Build/lint/all 19 unit tests passed.

### S21 - reference-inspired category layout and infinite scrolling

- Replaced the modal filter UI with seven directly navigable rows, using only verified site dimensions.
- Six portrait posters per row; no page buttons. The last visible row triggers the next batch after layout has caught up, avoiding duplicate initial prefetch.
- Filter-specific scroll/focus state and cached feeds retain loaded results on return. Emulator screenshot confirmed the initial 20 items and complete poster titles.
- Latest implementation is installed on Chromecast for independent validation.

### S22 - three-column search layout

- Left: input and 6-column alphabet/number remote keyboard, clear/backspace and Chinese/system input.
- Middle: vertical suggestions, popular and recent searches. Right: two portrait poster columns with continuous results.
- Query-specific cache, scroll and focus avoid applying old search-card focus to a different query. Search requests remain debounced.
- Build/lint/19 tests passed; emulator rendered the three-column layout. Physical focus verification is ongoing.

### S23 - keep episode selection visible

- Playback synopsis is capped to four lines / 90dp, title to two lines and cast/director to two lines.
- Reduced right-panel spacing so episode selection remains accessible on the first screen. Installed for physical confirmation.

### S24 - exact remote playback control order

- Controls now appear in the user-requested order: fullscreen, play/pause, -30s, +30s, -5min, +5min, speed, favorite.
- Default control focus is fullscreen. Five-minute jumps clamp at zero and media duration; unit coverage includes both boundaries.
- Physical Back is handled on key-up by the player, preserving menu/fullscreen/page hierarchy and avoiding a first press merely clearing focus.
- Build/lint/all 20 unit tests passed; latest physical revalidation pending installation after the current independent run.

### S25 - bottom-row visibility

- Catalog and search lists have 64dp bottom content padding.
- Poster focus requests visibility for the whole image/title/year container, not just the image; requests from cards that lost focus are ignored.
- Reuse existing category metadata when returning to the catalog to reduce transient empty-top layout before focus restoration.

### S26 - compact header with focused labels

- All six top navigation buttons show only their icon when unfocused; the focused button expands to show icon plus title.
- Accessible icon descriptions remain available. Build, lint and 20 unit tests passed; bundled with the next device installation.

### S27 - rich watch-history cards

- Commit `86eda01`: local and cloud history now display cover, available year/region/score, update note, episode, position/duration and progress bar.
- Missing metadata is fetched only for composed rows through a cached loader with three concurrent requests at most; failure retains the original record.
- Resume explicitly carries the selected record and enriched movie. Build/lint passed; physical review pending below.

### S28 - deterministic D-pad focus

- Category filters use explicit up/down links to adjacent rows; all horizontal chips are composed, so an off-screen target remains reachable and scrolls into view.
- Search keyboard has explicit four-direction links. The right edge enters suggestions/results, and Left returns to the remembered keyboard key; first result column can return to suggestions.
- Emulator D-pad checks passed: all seven category rows down/up; search header → input → clear → A, across to F, down L/R/X/4/0, right into a suggestion and left back to 0.
- Build/lint/all 20 unit tests passed. Independent Chromecast verification follows.
