# 集成版本独立实机回归

本轮安装版本0.1.0（versionCode 1），对应历史卡片86eda01与分类/搜索焦点1c41b55集成代码，包含整海报BringIntoView、底部64dp、播放器新顺序与5分钟按钮。安装前assembleDebug/lintDebug成功；主agent另报告集成20项单元测试通过。本轮只操作Chromecast sabrina，不重复模拟器，不进行直播测试，不退出账号。已交还实机，停在搜索页，无播放。此后新增全屏Up隐藏控制栏不属于本轮已装实测版本，由主agent单独验收。

## 实机已通过

- 分类：从header连续Down七次到字母行，完整经过七行；右移26次到Z，长行自动横向滚动保证Z焦点可见；从Z向上逐行返回header。聚焦header收藏图标时仅该图标展开文字，未执行收藏操作。
- 搜索：F→建议区→右侧《小气鬼》结果可达；左移返回建议再返回F，回到上一个键。最末两张竖海报的绿框、片名及年份地区均完整位于可见区域。截图chromecast-search-bottom.png、chromecast-search-keyboard-return.png。
- 新播放器：入页默认焦点全屏，顺序为全屏、暂停、-30秒、+30秒、-5分钟、+5分钟、速度、收藏。窄控制区水平滚动，未点收藏。截图chromecast-player-order.png。
- 5分钟：暂停位置113216ms，按+5分钟后413226ms（解码定位有10ms量化），再-5分钟为113226ms，返回量精确300000ms。
- 隐藏控制后单Back：全屏播放等待7秒，发一次Back回非全屏，保持进度，chromecast-single-back.png。
- 非全屏单Back：从搜索进入影片后仅发一次Back，返回原搜索结果卡焦点，chromecast-nonfull-back.png。本轮通过，覆盖此前版本吞Back疑点。
- 本机历史丰富卡片：封面、片名、2026/地区/评分、清晰度备注、集数、已看/总长及进度条均正常；删除独立于播放卡。截图chromecast-history-rich.png。
- 历史实际续播：点击《小气鬼》00:36记录后，MediaSession初始BUFFERING位置36823ms，随后PLAYING，证明确实从保存位置准备媒体，不只是显示历史文字。

## 只读审查

ConnectedBrowse七行均完整组合到横向Row，显式上下FocusRequester目标按列裁剪到相邻行范围，避免LazyRow未组合的目标；长行实机可达Z。ConnectedSearch左右桥接指向建议/结果focusGroup，反向记住lastKey；实机跨区双向回归通过。当前未发现新的焦点阻塞。历史整张卡的图文均在同一Card内，视觉缩放边缘留有padding；元数据补全不替换记录键，保持集数进度。

CredentialsStore与credentialQueue顺序审查见CREDENTIALS_REVIEW.md；本轮账号验证码UI登录由主agent接手，不称本agent已复测。云历史卡复用相同组件及有限并发详情补全，已编译/lint，但本轮只截取本机历史，未额外切云历史验收。

## 本轮未重复或未覆盖

目录20→60加载已在前一集成轮观察，本轮没有再滚到60；搜索多页真实关键词追加未补测，HI主结果为0不能充当该证据。分类深处返回的最新稳定截图未重复，搜索返回已通过。30分钟稳定性、4K/HDR源、现场环绕听感未测。ADB视频层黑色是捕获限制；用户此前已确认普通片实际声音画面正常，不从黑色截图推断播放故障。

主agent提供的模拟器补证：七行上下往返以及0→建议→两张结果→建议→0完整通过；这些是主agent实测，不冒充本轮实机重复。

## 主agent后续实测：上键隐藏与首页最近播放

- `9344127`：全屏单次Up后UI树不再包含退出全屏/播放/快进退按钮，Down后全部恢复；独立agent只读审查通过。此项当时片源处于BUFFERING，结论仅覆盖控制栏行为。
- `d649ebe`：首页最近播放位于推荐之前，五张横版卡完整显示，包含实际集数/进度。遥控器从导航Down到最近播放，再Down到推荐可达。
- 从最近播放进入《眼眸》01:01记录，实际MediaSession准备位置61568ms；一次Back返回首页同影片焦点，即使最新观看重新排序也能恢复。
- 最后修正卡宽含左右留白以及缺失集数文案；assembleDebug、lintDebug和20单元测试全部通过，最终APK已install-r到Chromecast并冷启动留在首页。未退出账号。
