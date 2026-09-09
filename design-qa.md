# UI v2 设计对照检查

2026-09-08 · Kotlin / Compose for TV / Media3 原生实现。设计来源：[DESIGN_SPEC](docs/design-v2/DESIGN_SPEC.md)、[十张认可的参考图](docs/design-v2/README.md)；运行结果：[24张截图](docs/verification/v2/screenshots/README.md)。

后续用户修订已经实施：VIP明确显示全部年份榜，每榜顶部2部＋下方10部五列两行，评分叠在海报右上角。对应 [最新独立复测](docs/verification/v2/V02_UI_FOLLOWUP.md) 与 [三张真实VIP运行图](docs/verification/v2/followup-screenshots/README.md)；下方原设计对照图库保留原始版本，不把旧四列／标题旁评分截图当成当前外观。

**当前结论：可运行的开发版已完成；所执行的视觉/交互检查通过，完整验收门槛尚未关闭。** 在本次明确检查范围内发现的布局和焦点问题均已修复并复验；不据此声明没有其他缺陷、74条设计要求全部通过或已适配全部 Chromecast 行为。

## 比较方法

- 参考图与对应运行截图在同一次比较输入中并列查看：首页、目录、年份浮层、搜索、普通/全屏播放器、历史、登录。两者均为16:9；运行图1920×1080，逻辑960×540 dp，density320。比较布局比例/组件边界时按逻辑视口判断，生成图中的影片、总数、字体栅格不当作产品事实。
- 官网原始 Logo 与现有公开海报直接用于原生组件；竖图 ContentScale.Fit，不从设计图裁出素材、不用 AI 重绘标识。当前图库不是完整产品数据库，所有进度/账号示例均为夹具。
- 截图以最终正式页面 Composable 的 debug preview 取得；播放器画面明确标注“布局预览”，静态视频、时长、格式值不能证明实际媒体。真实首帧/格式/进度落盘证据另见 [V2-10](docs/verification/v2/V2-10.md)。
- 焦点、返回、弹层、异常与状态恢复用实质遥控测试验证；没有把截图本身当作交互通过证明。

## 页面检查与设计差异

| 页面 | 参考 → 运行 | 结论 |
| --- | --- | --- |
| 首页 | [参考](docs/design-v2/assets/01-home-default.png) → [默认](docs/verification/v2/screenshots/home-default.png)、[展开](docs/verification/v2/screenshots/home-recent-focused.png) | 搜索在首页左、单行Header、五部＋历史成立。最近项默认显示进度，展开不改变推荐y；横幅保留叠加大标题。公开素材不同于生成图，首页每类真实接口仍按最近更新十部。 |
| mini 首页 | 规范第8节 → [运行](docs/verification/v2/screenshots/mini-category.png) | 当前年双榜分别请求，前二完整竖图＋旁侧标题，后八不重复；浏览全部保留来源。当前截图只展示首榜顶部，双榜/空榜由Mini测试验证。 |
| 目录 | [参考](docs/design-v2/assets/03-catalog.png) → [运行](docs/verification/v2/screenshots/catalog.png) | 分类标题与五入口、六列完整海报；图外元信息。目录Header高亮目录工具，分类导航用于mini首页，遵循正式路由语义。下排在未聚焦时允许部分进入视口，聚焦整卡时滚入安全范围。 |
| 筛选浮层 | [参考](docs/design-v2/assets/04-catalog-year-overlay.png) → [年份](docs/verification/v2/screenshots/catalog-year.png)、[更多](docs/verification/v2/screenshots/catalog-more.png) | 年份四列、390dp宽/330dp最大高，Dialog居中，符合规范允许安全区不足时居中。首项“全部”仍在列表上方；选值确认/取消不推移背景。更多的会员/首字母为独立草稿。 |
| 搜索 | [参考](docs/design-v2/assets/06-search-results.png) → [建议](docs/verification/v2/screenshots/search-suggestion.png)、[首结果](docs/verification/v2/screenshots/search-results.png) | 左254dp键盘、中170dp建议、右两列结果；示例公共片名不同。整张卡连同标题/元信息具有焦点边界，按规范优先于生成图仅围海报的示意。确认自动首结果、Back回输入已测；联想错误另有重试。 |
| 播放 | [参考](docs/design-v2/assets/07-player.png) → [普通](docs/verification/v2/screenshots/player-normal.png)、[视频焦点](docs/verification/v2/screenshots/player-video-focus.png) | 保留600/264dp分区、八按钮、四行简介、控制下分组。选集用真实索引分组；生成图中的视频内容用明确虚拟状态替代，不冒充播放截图。 |
| 全屏 | [参考](docs/design-v2/assets/08-player-fullscreen.png) → [显示](docs/verification/v2/screenshots/player-full-visible.png)、[隐藏](docs/verification/v2/screenshots/player-full-hidden.png) | 控件在渐变覆盖层中，视频外框和虚拟帧三轮显隐四边在±0.5dp内不变。上隐藏、下唤醒、分组层级和速度菜单回程通过。 |
| 历史/收藏 | [参考](docs/design-v2/assets/09-history.png) → [设备](docs/verification/v2/screenshots/history-device.png)、[网站](docs/verification/v2/screenshots/history-cloud.png)、[收藏](docs/verification/v2/screenshots/favorites.png) | 默认两列历史、完整海报/进度、独立删除。网站来源不造时间或删除按钮；收藏沿用六列。两来源分别保存深位置及操作角色，尾页重试可达。 |
| 登录 | [参考](docs/design-v2/assets/10-login-remembered.png) → [记住](docs/verification/v2/screenshots/login-remembered.png)、[首次](docs/verification/v2/screenshots/login-first.png)、[失败](docs/verification/v2/screenshots/login-error.png) | 账号区＋验证码键盘，数字1默认，四视觉格可容纳更多字符，显式登录。实际验证码用浅底保障可见，账号显示为公开合成movie_fan；错误截图是夹具拒绝提交，非真实账号错误。 |

图像保持完整时，非2:3原图可能出现留白，这是等比Fit的结果。未聚焦长标题可按规范省略，详情/聚焦信息继续提供完整片名。官方Logo、绿色焦点、背景/面板、圆角/边距以tokens和规范为准，没有加入新的主题或导航层。

## 检查中修复的问题

- 修正预览夹具日期、验证码透明底可读性、普通播放器控件边框/简介标题和大字体控件高度。
- 修复旧预览路线与真实组件不一致、根状态恢复重新抢首页焦点、mini目录返回来源漂移。
- 修复收藏被取消请求后的缓存失效、账号过期后精确收藏意图丢失、空/深收藏回程的未挂载焦点。
- 独立最终复核补齐历史尾错误重试、双来源滚动/操作角色保存、搜索联想错误与重试；标签加入Selected语义。最后45项集成全部通过，保留前一轮44项中的失败证据。
- 最后视觉复核发现“更多筛选”的首字母“全部”被选中勾号挤成两行。仅收紧该类短标签的内边距和勾号尺寸，保持14sp文字；目录4项复跑与截图另行留证。

## 未完成的验收

[V2-10逐条矩阵](docs/verification/v2/V2-10.md) 目前记录40项在所述范围通过、31项部分覆盖、3项待验收；这不是全量产品通过率。当前未关闭的主要门槛是：

- [后续Chromecast验证](docs/verification/v2/CHROMECAST_V2.md)：跨批次累计56个不同用例通过，最终修复包19项定向回归通过，实际搜索和深目录截图已归档。VIP旧会话、错误按钮焦点、排序复位问题已修复；会话修复基线的30分钟播放采样、真实Home暂停及后台进程重建已完成。现场听感／同步、4K/HDR及完整性能目标仍待验收。
- [1.3字体播放器](docs/verification/v2/screenshots/large-font.png)及[历史](docs/verification/v2/screenshots/history-large-font.png)中当前焦点/海报/控件可见；全部放大字体路径、屏幕阅读器/减少动效和实际对比度未验收。字体变大时导航可横向滚动，不能用一张默认态截图证明每个隐藏项可达。
- 真实账号切换并发、收藏写入、超过20条的云历史及指定时间续播、全部登录回程、100/1000集与自动下一集等，保留部分覆盖。
- 没有单独归档目录尾错误截图；该状态的保页/去重/重试有Feed与列表测试，不能声称完成每张建议截图。

交付物是开发分支和debug APK，README的v0.1 release链接保持原发布版本。本报告不构成新版本发布或完整实机验收。
