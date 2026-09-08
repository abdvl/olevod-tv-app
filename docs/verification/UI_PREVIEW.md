# 独立 UI 预览验收

2026-09-07，Android TV API34 ARM64，1920×1080 / 320dpi，ADB D-pad/确定/返回。此轮只验 UI 预览，不验登录、真实播放或历史同步；未读取 `.secrects`。

## 构建

- 首轮 `assembleDebug lintDebug`：成功，31 秒。
- 搜索布局修复版 `assembleDebug lintDebug`：成功，5 秒；最终输入焦点版 `assembleDebug`：成功。
- lint 有依赖版本更新建议和 TV banner vector 尺寸建议，未导致构建失败；不为建议任意升级已锁版本。
- 中间 API 文件 `List<T>=` 语法造成一次构建失败，主实现已修复，后续构建通过。

## 实测结果

| 项目 | 结果 |
| --- | --- |
| 首页推荐与影片导航 | D-pad 可从顶部进入推荐，继续到两行影片，焦点边框明显、可滚动 |
| 首页完整分类组 | 仍需修正滚动定位：第二行聚焦时第一行顶部与分类标题被固定导航裁掉；见 home-group-fixed.png |
| 搜索首屏裁切 | 修复后中文/语音按钮、右侧推荐标题均完整显示；见 search-fixed.png |
| 搜索输入焦点 | 原版输入框吞上下键；最终版关闭 IME 后按下可到退格，再下进入自绘键盘，确定可操作。首次聚焦/IME显示仍需后续冷启动精确回归；本轮复测过程中曾进入系统IME |
| 播放器布局 | 左视频/右信息、控制条清楚可读，当前明确标注未加载视频 |
| 全屏 | D-pad 可选全屏，Back 可退出；24dp底部修复后焦点完整显示，见 player-fullscreen-fixed.png |
| 登录表单 | 账号/密码/验证码/登录可见，1080p无裁切；仅静态表单，未提交凭据 |
| 浏览 | 筛选和排序布局可见；预览样本未测试真实筛选 |

## 后续必须验证

- 首页分类整组 BringIntoView 与子项默认焦点滚动的竞争；进入第二行时应保留标题和2×5组完整可见。
- 搜索：冷启动只下移到输入框时不要弹IME；上下移动可离开；确定打开IME；Back先收IME；自绘字母、清空、退格、中文入口完整闭环。
- 返回影片列表需恢复原卡片和滚动位置，目前路由只是字符串、selected非保存状态，不可视为已实现焦点恢复。
- 真实登录、VIP/直播/回看、MediaSession、进度恢复和Chromecast真机性能均未在此轮验收。

## 截图

截图在 `docs/verification/screenshots/`。`home.png`、`browse.png`、`account.png`为初版预览；`search-fixed.png`、`player-fullscreen-fixed.png`为已修复布局；`home-group-fixed.png`保留仍需修正的滚动问题。其余搜索截图用于前后验证，不作为最终产品展示。
