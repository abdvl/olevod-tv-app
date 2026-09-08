# S09 独立播放 / 搜索 / 浏览验证

2026-09-07。测试仅使用主 agent 已安装 S09 APK、API34 TV ARM64 1080p 模拟器。未构建/安装 APK，未读账号凭据，未修改实现。输入均为 adb D-pad、确定、返回、媒体键和系统 Home。

## 已实测通过

- 普通片《海洋奇缘：启航》原生播放成功，有实际画面及 MediaSession PLAYING；播放器总时长1:55:39。
- 媒体播放/暂停键可以暂停。D-pad移动至播放按钮再确定可从 PAUSED 切 PLAYING。
- 暂停时 D-pad 前进30秒：14754ms → 44754ms；后退30秒：44754ms → 14754ms，精确符合设计。
- 速度菜单可通过D-pad选择1.5倍，MediaSession实际报告speed=1.5。
- 全屏进入成功，控制条自动隐藏，Back恢复分栏，媒体继续推进而非归零。
- 播放中按系统Home，MediaSession转PAUSED，位置145466ms；回到应用仍停在2:25，未后台继续播放。
- 搜索热词“早春晴朗”实际返回1部，海报、标题、7.1评分、更新至19集、年份地区正常显示，无分组解析造成的假卡片。
- 浏览电影筛选面板可用D-pad打开、选择美国、完成；结果从4123部变1212部，摘要显示美国，首屏影片地区全为美国。
- 从筛选结果用D-pad打开《逃亡者》，实际进入原生播放，总时长1:25:50；Back可返回浏览。评分排序本轮未完成，不作已测声明。

## 发现并已通知主实现的问题（当前S09）

1. 非全屏控制条始终可见，但controls计时5秒后false，父层把首个普通键当唤醒消耗。用户会感觉明确可见的按钮需要按两次。主 agent 已安排修复，需新APK回归。
2. 冷启动搜索，顶部搜索按Down聚焦输入框便自动打开系统IME；后续Right/OK被键盘处理成字母w。Back收起后可移动清空/自绘键盘/右侧热词。主 agent 正修改输入模式，需新APK验证。
3. 筛选确认后焦点仍在筛选按钮，随后Down直接进入第五张影片；没有按设计直接聚焦首卡。属导航体验后续项。

## 证据

`docs/verification/screenshots/`：

- native-control.png：实际画面与暂停。
- native-speed-menu.png：倍速菜单。
- native-fullscreen.png：真实视频全屏。
- native-back-from-home.png：后台返回仍停2:25、1.5倍设置保留。
- search-real-results.png：热词真实结果。
- browse-usa-results.png：美国筛选及1212部结果。
- browse-selected-playback.png：筛选结果打开第二部原生片源。

不涵盖：登录、VIP会员、直播/回看、历史落盘、字幕轨道、30分钟稳定性、Chromecast真机解码/音频/HDR。模拟器以no-audio启动，不能据画面与MediaSession宣称已听测音质/同步。
