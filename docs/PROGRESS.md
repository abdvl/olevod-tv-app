# 实施进度 / 恢复入口

最近更新：2026-09-07。

## 当前检查点

- 已完成：S01–S06（真实首页已接入）。
- 进行中：S07 分类浏览接入。
- 下一步：S06真实首页，随后浏览、搜索、播放。
- 已有设计基线提交：`4552cc8 Add design docs`。
- 用户目标设备：Google Chromecast with Google TV（4K/HD 待确认）。
- 用户已授权每步本地 commit、使用 Android Studio 调试、独立 agent 验证和 `.secrects` 测试账号。
- 当前不推送远端；不改测试账户密码或购买会员。

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
