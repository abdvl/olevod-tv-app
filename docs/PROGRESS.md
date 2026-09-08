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
| S19–S28 | 账号记忆、导航展开、分类/搜索重设计、无限滚动、播放控制、底部留白、历史卡片与明确方向键焦点已实现；目录/搜索跨区焦点、末行可见性、历史卡片/续播与播放器控制均已独立实机复核 |

- 登录依赖已解除：用户授权本次登录调试代填验证码，Android同客户端验证码登录已成功；Chrome因Mac锁定暂不可用。
- 待补：记住凭据后通过实际验证码表单再次登录、多结果搜索追加，以及30分钟播放稳定性。导航、历史续播与新增首行最近播放已实机验证。用户已明确暂缓直播稳定性排查，保留现有直播功能及问题记录。
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

### S29 - fullscreen Up hides controls

- Fullscreen Up hides the controls immediately; repeated Up keeps them hidden. Down/OK can show them again, and manual hide also works when paused.
- Chromecast UI tree confirmed no fullscreen/play/seek buttons after one Up, and all controls returned after Down. The sampled VIP source was buffering during this UI check; this is input/visibility verification, not a new playback-stability claim.
- Independent code review, build, lint and 20 unit tests passed. Installed on Chromecast.

### S30 - recent playback first on home

- Home starts with the current account/device's latest 10 watch records in a horizontal row (five cards visible), followed by recommendations and category sections. Empty history has an explicit placeholder; View all opens history.
- Cards show saved episode/position and pass the full record into resume. Missing movie metadata uses the same bounded loader as history.
- Recent cards have distinct focus identities from category copies of the same movie. Independent review caught and fixed the row's 8dp width excess and absent-episode wording.
- Chromecast confirmed recent playback above recommendations. Opening 01:01 record prepared media at 61568ms; one Back restored the same recent card after record reordering. Screenshot: chromecast-home-recent.png / chromecast-recent-return.png.
- Build, lint and 20 unit tests passed. Latest APK includes the above fullscreen Up gesture.

### S31 - current video resolution and bitrate

- Fullscreen controls reserve a non-focusable information area on the right, refreshed from ExoPlayer's current video format every 500ms.
- Shows actual format width/height and the supplied average bitrate; peak-only values are explicitly marked. Missing metadata is shown as unavailable, never replaced by network download speed.
- Retry/episode changes clear the prior display. Three new tests cover average precedence, peak labeling and missing data; build/lint/all 23 tests passed. Integrated device check follows the official-logo update.

### S32 - stable fullscreen video with translucent controls

- Fullscreen video occupies a fixed full-size Box. The controls are a bottom-aligned sibling overlay with a transparent-to-82%-black gradient, rather than a row that consumes video height.
- Resolution/bitrate remain on the overlay's right. Up hide, Down/OK show and automatic hide use the same overlay visibility without resizing the video.
- Added a real Compose Android regression test: video bounds are identical with controls shown, hidden, then shown again. Passed on TV emulator (1 test); build/lint/all 23 unit tests passed.
- Integrated Chromecast verification will follow the logo and reported catalog-sort fix.

### S33 - original official brand assets

- Commit `a72e5a8`: Header and preview use the website's original white transparent logo; launcher uses the official favicon, and TV banner centers the unmodified logo with proportional XML sizing.
- Current public website configuration and raw asset hashes are recorded in BRAND_ASSETS.md. No runtime image download is required for branding.
- Integrated build/lint passed. Final device installation awaits the newly reported catalog-sort fix, reproduced on emulator despite healthy direct public API results.

### S34 - catalog sorting stale UI fix

- Commit `48c17c9`: direct API returned20 items while switching filters left the LazyColumn rendering an old empty snapshot. Replaced keyed rememberLazyListState groups with input-keyed rememberSaveable/LazyListState.Saver in browse and search.
- Independent real API/UI test matched first movies for all four sorts, switched back to cached results and loaded20→40 at the end. Real D-pad sorting kept focus and displayed4123/20 on emulator.
- Source, regression and screenshot evidence are in verification/CATALOG_SORT_REGRESSION.md.

### S35 - visible video focus and grouped episode selection

- Nonfullscreen control Up now focuses a visibly outlined video area; OK enters fullscreen and Down returns to the first control. Root is focusable only while fullscreen controls are hidden, eliminating the invisible nonfullscreen focus trap.
- All eight main buttons remain composed in a horizontal scroll row, so returning to the first control never targets an unloaded lazy item.
- Episode selection moved below controls: choose a group of10, then a concrete episode; original API episode numbers are preserved. Fullscreen uses the same picker inside the translucent overlay.
- Episode/group Up navigates toward controls instead of hiding them. Automatic hiding pauses while browsing episodes, and auto-next retains the browsed group to avoid destroying its focus.
- Compose Android tests passed for fixed video bounds, selecting21–30 without playback,21/30 visibility, group/episode/control D-pad navigation, explicit episode24 selection and retaining focus when playback advances outside the group. Build/lint/all23 unit tests passed. Integrated Chromecast check follows.

### S36 - fullscreen root key routing regression

- Physical QA exposed that a preview-key listener placed after focusable misses keys when the hidden fullscreen root itself owns focus. Extracted PlayerKeyInput with the listener before the focus target.
- Root stays disabled in normal playback; controls Up targets video, video can continue Up to navigation, and navigation Down can return to a visible focus target.
- Added an Android regression asserting root focus, Down wake to controls, Up hide back to root, then Down wake again. All3 player Android UI regressions and23 unit tests/build/lint passed.

### S36 physical verification completion

- Chromecast confirmed video/header/control navigation, fullscreen group21–30, Up hide/Down wake and identical1920×1080 video bounds with overlay shown/hidden.
- Current stream1280×720 and peak2.58Mbps displayed correctly; original logo visible. Same film/episode resumed and retained.
- Final evidence and capture limitations: verification/PLAYER_FINAL_DEVICE_CHECK.md.

### S37 - home focus return and requested category order

- Recent-playback cards Up explicitly returns to the header Home icon. Header Home and navigation Home Down return to the last focused recent card (first card initially); empty history targets View all.
- Recent cards and navigation use fully composed horizontal rows. Home Down first scrolls the outer lazy list to item0, waits for a frame, then requests focus. Page disposal clears the callback and cancels pending work, avoiding unloaded targets after deep scrolling.
- Recommendation content is followed by category IDs1,2,3,6,14: 电影、电视剧、综艺、VIP、短剧. Display aliases retain original API names for directory navigation; anime remains available in navigation/directory.
- Build/lint passed. Independent agent reviewed source and found the outer-lazy-item lifecycle issue, resolved before delivery.
- Chromecast installed and verified Home→牧神记→Home, horizontal end/View all→Home→last recent movie. Screenshot: verification/screenshots/chromecast-home-focus-fixed.png.
- Emulator rendered headings in exact requested order. HomeNavigationUiTest passed using real Android D-pad events and waiting for asynchronous focus completion: normal round trip plus deep scroll→directory→Home→Down remount→Up return. Initial test assertions ran before async focus settled; the final test waits for actual focus state.

### S38 - corrected meaning of Home navigation

- User clarified that recent-playback Up must return to the second category row (首页、直播、短剧、电影…), not the top icon bar. This supersedes the S37 focus target.
- Added a separate category Home focus anchor. Recent cards/View all Up target category Home; category Home Up reaches top Home, and top Home Down returns to category Home. Category Home Down retains safe asynchronous recent-row remount/restoration.
- Build/lint, independent source review and HomeNavigationUiTest passed. The test now asserts both navigation levels, Right to Live/Left to Home, and the deeply scrolled leave/return path.
- Installed on Chromecast. Fresh physical UI dumps confirmed top Home bounds[76,30][231,126], category Home[76,132][191,226], Down to recent film, Up back to category Home, Right Live and Left Home. Device left on category Home. Screenshot: verification/screenshots/chromecast-home-category-focus.png.

### S39 - search suggestion focus and Back to query

- Selecting a suggestion now records its keyword as a pending focus request. On matching results, the right list scrolls to index0, waits for composition, and focuses its first movie. Cached repeated selection also works; clearing pending after completion prevents pagination from stealing focus.
- Back while outside the query box cancels pending transfer and focuses the query box. Back again uses the existing page return. Letter input, edits, clear/delete and opening Chinese/voice input also cancel pending transfer. Empty/error completion focuses the query box.
- Build/lint and independent source review passed. SearchFocusUiTest passed on emulator against actual API responses: remote M/N input, Right to suggestion column, select魔女, first result focus, Back to input, cached same-query repeat, and second Back exits search.
- Test keyboard activation uses native Android DPAD_CENTER; touch-clicking TV key buttons did not enter letters in the initial test harness. Real MN suggestions confirmed魔女 as first entry.
- Installed debug APK on Chromecast successfully. Physical follow-through was interrupted when the foreground changed to system UI; no new app crash was found in the crash buffer. Do not claim full physical search-path verification. No further device navigation was sent after that mismatch.

### S40a - reusable exit confirmation dialog

- Added ExitConfirmationDialog with caller-owned visibility and separate dismiss/confirm callbacks. It performs no Activity or navigation operations itself.
- Continue watching is the default focus; explicit horizontal targets connect Continue and Exit, with outer/up/down edges stopped. Remote Back uses Dialog dismissal and never invokes exit confirmation.
- Added two Android Compose tests for initial focus, Left/Right navigation and boundaries, both OK actions, and Back cancellation from the Exit button. Tests use native Android remote key events.
- Independent component implementation complete; build and emulator execution are pending the main agent's combined mini-home/root-routing integration. No device was controlled during this step.

### S40b - category mini homes, exit integration, short dramas last

- Middle VOD category tabs now enter a mini home. Actual API Filter(category, year=current calendar year, sort=hot/score) supplies each ranking; display takes the first10, top2 large recommendation cards plus the remaining8 in four-column rows. Empty/error states do not silently substitute other years.
- Bottom Browse all opens full category directory; Back returns to the mini home. Player return retains the originating screen. Category tabs remain present and selected; entry callbacks remount ranking rows before focus. VIP蓝光影院 from directory is normalized to the VIP蓝光 navigation tab, fixing an independently identified unloaded focus-target risk.
- Short dramas moved to the end of the middle navigation. Main home section order continues to end with short dramas.
- Integrated ExitConfirmationDialog with root-home Back and MainActivity.finish. Independent agent implemented and tested the component in2766f76. Touch-to-remote verification exposed absent initial focus; keyboard input mode + frame delay now ensures Continue watching is visibly focused.
- Final build/lint passed. Three Android tests passed: real API MiniCategoryHomeUiTest (movie hot/score top2, navigation focus, directory→VIP→mini alias path, root exit cancel) and two ExitConfirmationDialog tests (default, boundaries, both actions and Back).
- Actual emulator touch→Back→exit showed Continue watching focus and direct OK dismissed; screenshots inspected. Latest APK installed successfully on Chromecast. No claim of complete physical mini-home validation.
- Screenshots: verification/screenshots/emulator-mini-movie-hot.png, emulator-mini-movie-score.png, emulator-exit-confirmation.png. All show the final navigation with short dramas last.

### S41 - v0.1 release preparation

- Prepared dedicated signed, non-debuggable v0.1 APK (app version 0.1.0/code 1); private key/config remain ignored and local. APK SHA256: 72291aa02800eb599f9d9261e94a39586eb77880be88b15373772f1c5d845e3d.
- Release build, 23 unit tests, lint and signature verification passed; fresh TV emulator install and real guest playback succeeded. Existing Chromecast debug install and data retained.
- Added ten actual release screenshots, detailed README features/remote navigation, installation and debug-to-release migration instructions, signing/build instructions, release notes and verification report. Independent agent reviewed documentation and release validation.
- GitHub publication follows this commit; APK and checksum are under ignored artifacts/releases/v0.1.

### S42 - v0.1 published

- Published https://github.com/abdvl/olevod-tv-app/releases/tag/v0.1 after explicit authorization to publish source, documentation and history. Tag points to 3853dd7d6da4a825c6220e7575ece87d0ef8a6e2.
- Public release is neither draft nor prerelease; APK (14,210,642 bytes) and SHA256SUMS.txt are downloadable. Downloaded both public assets and verified exact checksum/file equality with local release artifacts. APK SHA256: 72291aa02800eb599f9d9261e94a39586eb77880be88b15373772f1c5d845e3d.
- Source, README with ten screenshots and installation instructions are available at the release tag. Signing keys and test credentials remain local and excluded. Existing Chromecast debug app/data retained.

### S43 - MIT license

- Added the standard MIT License with copyright 2026 Aikepaer Abuduweili and linked it from README. Clarified that third-party dependencies, branding and website media retain their respective rights/licenses.
- Documentation-only change; checked formatting and license link before committing and pushing.

### S44 - release token history

- Added README Token history with the previously reported v0.1 development snapshot, model, main/subagent totals, cached/uncached input, output and Standard API equivalent cost ($202.00).
- Documented snapshot boundaries (includes publication, MIT License and initial usage inquiry), pricing date, assumptions, excluded fees and future per-release incremental accounting. No raw task logs or credentials published.
- Verified arithmetic and Markdown formatting; documentation-only change.

### S45 - approved UI v2 design specification

- User approved the image-based redesign and requested a complete development specification. Added `docs/design-v2/DESIGN_SPEC.md` covering 18 hard requirements, the single navigation row, visual units/tokens, complete portrait artwork, every main page, precise remote focus/Back rules, async focus intent, continuous loading, playback/episode/account states, API mappings and current limitations.
- Preserved ten final approved ImageGen references in `docs/design-v2/assets/` (15,535,990 bytes total), copied without modification; manifest records source filenames, dimensions and SHA-256. The gallery explicitly identifies them as design references, flags the missing search icon in the focused-home draft, and distinguishes mock data from runtime facts.
- Added `tokens.json` and `ACCEPTANCE.md`: 72 pending implementation acceptance cases, screenshot requirements, and ten resumable development checkpoints. Covered additional states without standalone mocks, including category mini homes, favorites, first login, errors and confirmations.
- Independent read-only review completed in two passes. Corrected lifecycle wording, focus-triggered pagination wording, filter apply versus cancel scroll behavior, and minimum focus-target dimensions. Reconciled ordinary-player height arithmetic with the 36 dp minimum target size.
- Documentation checks passed: 66 local links, ten PNG hashes/dimensions, JSON parsing, Markdown tables/fences/whitespace, layout arithmetic, minimum target dimensions, action/header order, and coverage of all 18 requirements. Base palette contrast checked; actual rendered contrast and all Android acceptance cases remain for implementation.
- Linked the new specification from README and marked the early `docs/DESIGN.md` as historical where superseded. This checkpoint contains documentation and design assets only; no APK/UI implementation or new release. Resume future development from V2-01 in the acceptance document after implementation is requested.

### S46 - UI v2 design token history

- Paused the newly authorized UI implementation at the user's request to account for the design phase first. Added an incremental UI v2 design milestone to README Token history, from the original v0.1 snapshot through the completed design-spec push: 15,226,201 input (14,570,880 cached), 92,184 output and 15,318,385 total tokens; Standard API equivalent $25.73.
- Reconciled 100 unique request records across the main task and design review agent; the previous two agents have no additional usage. Included one context-compaction request omitted from the UI snapshot delta, without rewriting the historical v0.1 row. Verified current official GPT-6 Astra prices, zero cache writes and maximum input below the long-context threshold.
- Recorded 20 ImageGen calls/results separately. No image-model identity or billable usage is exposed, so additional image-generation fees remain uncounted. Documented exact timestamps, scope exclusions and cumulative recorded cost ($227.73); this accounting turn and all implementation are outside the fixed cutoff.
- Added `docs/accounting/ui-v2-design-2026-09-08.json` with aggregate counters and reconciliation data for the next checkpoint. No prompts, raw logs, credentials or image payloads are included. Documentation-only change; validation covers arithmetic, metadata consistency, local links and whitespace.
- Independent read-only review found no discrepancies in the four sessions, compaction reconciliation, Decimal costs, timestamps or unknown image fees. This review is after the fixed cutoff and excluded from the milestone.

### S47 / V2-01 - native UI v2 foundations

- Started the user-authorized implementation on `codex/ui-v2`, following the approved native Android spec and its ten resumable checkpoints. Added `docs/verification/v2/IMPLEMENTATION_STATUS.md` and an independently authored validation plan.
- Added `TvDesign.kt` with spec colors/dimensions, official logo, one Header, search before Home, persistent category labels, focus-expanded utility labels, and no live navigation item. Cold launch focuses Home; only confirmation changes routes. Existing native player/API/storage remain in use.
- Replaced shared `PosterCard` rendering with a whole-card `PosterTile`: 2:3 container, full Fit artwork, outside-artwork title/score/metadata, fixed title area, no focus scaling, one semantic click target, and 4 dp scroll outset. Removed competing whole-section bringIntoView in home groups.
- Verification: app/test builds, existing unit tests and lint passed. Independent `V2FoundationUiTest` final rerun passed 3/3, zero skips: header key traversal/confirm-only route selection, unclipped last-row card bounds/no scaling, and pixel checks for all four corners of 2:3, 3:4 and very tall source fixtures.
- Earlier validation exposed an unmounted test focus target, a real missing scroll outset, and an incorrect image-load probe in letterboxing; all corrected and rerun. Full histories and per-page behavior remain in their later checkpoints; this is not whole-v2 or Chromecast acceptance.
- Next: V2-02 source focus identity and shared cumulative feed. Its work may exist uncommitted; use the status document and Git diff to resume rather than repeat this checkpoint.

### S48 / V2-02 - source focus and cumulative feeds

- Added a saved route/section/entity focus identity, keeping duplicate titles in different home sections independent. Header revisits reset only the destination; returning from playback preserves its source route and scroll state. Mounted-target restoration is one-shot and metadata changes do not trigger it again; deep list and missing-source recovery remain page-level acceptance work.
- Generalized cumulative feeds with one in-flight request, deduplicated appends, cancellation/generation protection, preserved retry page/items, and a bounded error for wholly repeated pages. Unknown totals use -1 internally and are omitted in UI labels.
- Account transitions cancel cached feeds; active catalog/search compositions include sessionVersion to obtain fresh feeds. Disposal cancels outstanding requests while preserving completed pages.
- App/test builds, unit tests and lint passed. Independent Android regression: 4/4 passed, zero skips; new duplicate-section restoration test plus the three foundation tests. PagedFeed unit cases passed 3/3 for repeat pages, non-cooperative late responses and canceled-request preservation.
- Independent review caught unknown totals leaking into text and active feed keys missing sessionVersion. Both corrected; final build/unit/lint log is `.tools/v2-02-fixes-build.log`. See `docs/verification/v2/V2-02.md` for frozen artifacts and exact verification boundaries.
- Next: V2-03 home layout. Its ConnectedHome draft is intentionally outside this checkpoint commit.

### S49 / V2-03 - home recent viewing and recommendations

- Replaced the connected home with current-account latest five records plus a final all-history card. Progress is always overlaid at the bottom of full Fit artwork. Focus expands only the active card horizontally, preserves every other poster width and recommendation Y, and collapses/reset scroll after leaving the row.
- Expanded cards show title, real metadata, episode/time, progress and a single continuation action. Rows reserve scaled text height; normal default uses six equal slots. Down chooses the recommendation on the same visible side; Up returns to the unified Home item.
- Homepage all-history creates a fresh device-history entry, avoiding restoration into a formerly selected cloud tab. Recommendations use two equally weighted real banners with overlaid large titles; section order is movie, series, variety, VIP, short drama.
- Reused the actual ConnectedHome in explicit preview mode with public poster/banner fixtures and synthetic progress, without inserting fixture records into history. Saved emulator default/focus screenshots under `docs/verification/v2/screenshots/`; compared each against the approved matching reference. Removed theme-colored banner letterboxing and duplicate selected-card title.
- Independent V2HomeUiTest passed 3/3, zero skips: header/recent/history and same-side recommendation navigation, expansion geometry, exact source/resume restoration and no fixture persistence. Initial run found a Header subcomposition timing race; production cold-start and fixture now await a layout frame before requesting Home.
- Latest app/test build, JVM suite and lint passed in `.tools/v2-03-06-build.log`. Full large-font/Chromecast acceptance remains V2-10. Subsequent page drafts may be uncommitted; retain them when resuming.

### S50 / V2-04 - category mini homes

- Added two current-year rankings per category, using the API hot/score filters. Each ranking has two wide cards with complete portrait artwork and adjacent rank/title/metadata, followed by the remaining eight items in four columns. Empty/short lists preserve their true counts and the all-years browse action.
- Category entry prioritizes hot, score, then browse-all. The browse action has its own saved source identity. Public-image preview uses the same implementation and a fixed fixture, documented separately from API ranking results.
- Independent review found stale catalog filters on reentry and origin-category loss after changing catalog category. Added openCatalog with a fresh destination epoch/default feed and saved source category, keeping the originating mini state intact.
- Independent Mini tests passed 2/2, zero skips: both long rankings and browse-all return, sparse/empty ranking entry. First empty-fixture test raced recomposition; synchronized the test and reran the original app. Actual root route fixes independently reviewed; full real-route execution remains integration work.
- Details and screenshot: `docs/verification/v2/V2-04.md`, `screenshots/mini-movie.png`.

### S51 / V2-05 - compact catalog filters

- Replaced seven persistent filter rows with a category title, five compact triggers and a six-column poster grid. Filters remain fixed while results scroll; unknown totals show loaded count, not -1 or a fabricated zero.
- Added modal option lists/grids with current-value checks and explicit directional neighbors. Years scroll internally in four columns. Simple confirmation applies once and returns to its trigger; cancel preserves the grid and query. More filters keeps membership/initial drafts until Apply; cancel discards both.
- Query changes reset the matching feed and scroll anchor; repeated same-value confirmations preserve them. Header and triggers stay operable during loading, empty results or errors. Last-row visibility/focus triggers cumulative loading with bounded retry behavior.
- Independent catalog tests passed 4/4, plus Foundation/Home 6/6 in the same frozen build (10/10, zero skips). Covered year boundaries/confirmation, sort cancellation with preserved deep-grid setup, combined draft apply/cancel, and unknown total text. See `docs/verification/v2/V2-05.md` for limits.
- Compared runtime catalog and year dialog against approved references, refined title/trigger typography and modal size, and removed a redundant loaded-count row. Latest runtime images are in `docs/verification/v2/screenshots/`; final minor visual refinements will receive the integrated UI rerun.

### S52 / V2-06 - three-column search and cancellable focus intent

- Replaced search with a 6×6 keyboard, separate suggestions column and two-column portrait results. Draft and confirmed queries are distinct; selecting a suggestion waits for its results and focuses the first result once. Back returns to the input without opening the IME; further typing or directional navigation cancels pending focus transfer.
- Added explicit cross-column neighbors and remembered keyboard/result targets, cumulative result loading, independent error/empty guidance, and Unicode code-point deletion. API debouncing remains 350 ms for suggestions and 400 ms for typed results.
- Independent Search 3 + Catalog 4 tests passed 7/7, zero skips, including MN → 魔女 → first result, Back during a pending request, and a non-cooperative old response after newer typing. See `V2-06.md`; API/IME/device integration and final runtime screenshots remain in V2-10.
- App/test builds, JVM tests and lint also passed after subsequent player/account drafts. Those uncommitted drafts are intentionally outside this checkpoint.

### S53 / V2-07 - native playback and stable fullscreen overlay

- Separated testable PlayerContent from the existing Media3 lifecycle wrapper. Ordinary layout reserves all eight controls and ten-episode groups below the video; description is limited to four lines and expands in a modal. Up targets the video, then Header; Header Down returns to the video.
- Fullscreen shows a translucent gradient over the unchanged video rectangle. Up hides controls, Down/Confirm reveals them, and hidden Left/Right seek 30 seconds. Episode navigation and paused/buffering/error/menu/accessibility states prevent idle hiding.
- Retained MediaSession/audio-focus handling, background pause, release on route departure, local progress and throttled cloud synchronization. Media3 Format supplies resolution and average/peak bitrate. True episode array indices drive resume and manual/automatic next episode; manual selection starts at zero and speed persists through the playback session.
- Independent Player UI tests passed 4/4, zero skips, covering all button deltas/bounds, Header/video round trips, three fullscreen visibility cycles with identical video bounds, groups/short final group, speed confirmation/cancel, and disabled seek navigation. See V2-07.md for frozen artifacts and the exclusion of actual media playback/lifecycle acceptance.
- Added focused JVM coverage for explicit resume precedence, non-contiguous indices, missing episodes, completed restart and unknown duration. Added shared favorite/cloud feed caches required to invalidate returned collections after a confirmed favorite change; collection layouts remain in V2-08.

### S54 / V2-08 - history and favorites collections

- Rebuilt device history as two columns of complete portrait/details/progress cards. Resume and delete are separate focus targets with explicit row/column/role navigation. Delete and clear require confirmation, default to Cancel, and restore the trigger or a surviving neighbor; the empty state opens the catalog.
- Cloud history and movie favorites use cached cumulative 20-item feeds, retain items on tail errors, omit unknown totals, and cancel on disposal/account changes. Cloud cards omit untrusted dates and deletion controls. The favorites page no longer offers the deferred live-channel collection; existing channel data is untouched.
- Device deletion captures the account before IO, and publishing a loaded history snapshot checks its scope again. Metadata enrichment remains limited to visible cards, cached, with at most three parallel requests; failures keep available title/artwork.
- Independent History UI tests passed 5/5, zero skips: five-card main/delete navigation and short final row, cancellation to original delete trigger, confirmed deletion to a neighbor, clear/empty browsing, final deletion, and read-only cloud card behavior. See V2-08.md. These tests use isolated fixture records; real DB/account and service integration remain separate checks.
- App/test builds, unit tests and lint passed after account/visual drafts. Collection commit contains only this checkpoint and the required root collection callbacks; remaining drafts are retained.

### S55 / V2-09 - remembered login and account-safe return intents

- Rebuilt first/remembered/signed-in account states with a three-column numeric keypad, a single atomic CAPTCHA image/id pair, expandable image on a readable light background, four display slots that accept up to eight Unicode characters, and explicit submission. Fourth-digit input never submits. Busy, refresh, failure and clear-credentials confirmation retain legal focus targets.
- Credential state is published only after encrypted persistence succeeds. Storage failure preserves current input and permits the login attempt without claiming the credentials were remembered. The injectable CredentialVault preserves the production Keystore implementation and allows deterministic failure/timing verification.
- Network login is cancellable; once its disk commit begins, the corresponding cache clearing/sessionVersion/home transition finishes atomically. History loads in ViewModel scope. Cancellation after commit prevents replay of a source action. Login sources retain movie/resume/cloud-history intent; favorites record an exact desired value, are consumed once for the current account, and are revoked on route departure.
- Independent account UI 6 and state 4 tests passed. Search 3 passed on rerun after removing an obsolete expectation that preview fixtures write search history. See V2-09.md for both rounds; no real credentials or login service were used in these tests.
- Additional independent review fixed cancelled favorite requests leaving stale cache, expired-auth desired state being lost during account rekeying, empty favorites Header entry, and deleted/deep favorite source restoration. Favorites final rerun passed 4/4 after retaining two initial failures as evidence; details in FAVORITES_LIFECYCLE_REVIEW.md.
- Connected every debug preview to the same production composables with public artwork and isolated synthetic progress, removed obsolete duplicate preview layouts, improved scaled controls/description scrolling, and added real rendered-first-frame accessibility state. Process-restored routes no longer rerun cold-Home focus. Final screenshots, root journeys, actual media/account checks and device limits are V2-10 work.

### S56 / V2-10 - integrated developer handoff; device acceptance pending

- Completed independent root journeys and final fixes: cloud history tail retry is reachable and returns to a stable card, device/cloud source states retain deep scroll and action role, suggestions report their own errors and retry without losing input or stealing focus, and selected actions expose accessibility semantics. The final visual pass tightened only initial-filter chip padding/check size to keep “全部” on one line.
- Final integration run passed 45/45 Android UI/state tests, zero skips, 85.355 seconds. The preceding 44-case run had one failure for missing Selected semantics; retained the assertion and fixed production. The final padding-only change then passed the affected Catalog 4/4 (9.634 seconds). Build, 30 JVM tests and lint passed. Detailed versions, hashes, prior failures and scope are in `docs/verification/v2/V2-10.md` and `results.json`.
- Separately enabled live checks passed real catalog categories/four sorts/two pages/filter/Chinese search, native CAPTCHA login and encrypted session persistence, saved-account VIP/favorites/cloud-history page1 reads, and ordinary NativePlayer first frame/actual format/fullscreen controls/local progress on departure. Do not expand these into VIP decoding, exact seek continuity, service writes or background lifecycle acceptance.
- Archived 24 running UI states with public artwork and synthetic private data, including home/recent, mini, compact filters, search, player controls/fullscreen/episodes/speed, device/cloud history, favorites, first/remembered/error login, exit and fontScale1.3 examples. `design-qa.md` links reference/runtime comparisons; README separates the v2 development preview from the existing v0.1 release/gallery.
- Requirement audit has 74 cases (66 P0 + 8 P1): 40 passed within explicitly stated scope, 31 partially covered, 3 awaiting evidence. This is a resumable developer delivery, not completion of all V2-10 acceptance gates. Chromecast connection timed out at 192.168.128.86:5555 and no wireless service was discovered; await its current connection port. Target audio/video, long playback, performance, full accessibility/process recovery and private-service journeys remain listed in V2-10.
- Final debug App SHA-256: `e14387a733c072d46a78b7af722336893115dd7409ba8deaf549cbed725587ce`. Development branch remains `codex/ui-v2`; no version bump, release/signing changes, publication or push in this implementation turn. Resume from the remaining checks, not by repeating all completed stages.
