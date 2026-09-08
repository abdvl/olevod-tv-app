# 持久化与生命周期独立代码审查

2026-09-07。只读实现代码，未操作模拟器、未读取凭据。动态实现由主 agent 修改；下列区分发现时问题和已观察到的修复。

## 具体发现

1. **异步解析在后台起播**：原实现 ON_STOP 时 pause，但详情请求稍后返回会无条件 play。点影片立即Home可触发。主 agent 已在 prepare 后检查生命周期至少 STARTED；代码复核已见修复，仍需模拟器延迟网络复验。
2. **切集前未保存**：原实现手动/自动下一集只改 episode，会丢最近周期内进度。主 agent 已添加切集前 saveProgress。数据库目前仍以 `(scope,id)` 为主键，只保留影片最近一集，不是设计中每集进度表；从其他集切回较早集无法恢复该集独立进度。
3. **后台暂停仍刷新历史时间**：周期 LaunchedEffect 每10秒写入，不检查 playing 或前台。暂停后台保留Activity时仍将updatedAt不断改为现在，误报最近观看并无意义读全表。建议周期仅在前台正在播放时写，pause/stop/切集各做一次事件保存。已通知主 agent。
4. **账户切换与异步写入竞态**：原 scope 在IO执行阶段才取当前session，旧账户待执行记录可能写入新账户。主 agent 已在播放器入口捕获accountAtStart并传到record/save；代码复核已见修复。
5. **账户标识取输入值**：SessionStore 使用输入用户名的规范化hash隔离历史；网站若同账号可用用户名和邮箱登录，会形成两个分区。应优先用登录响应user稳定ID；当前已加密会话且allowBackup=false，不把该标识问题误称为明文密码存储。

## 搜索输入修复复核

新InputBox默认是可聚焦显示Box，只有确定/中文入口才进入Dialog内BasicTextField。这从结构上消除了“聚焦即弹IME”和编辑框吞方向键的问题；未在本轮操作模拟器，最终输入法Back/完成及焦点恢复仍以实机复验为准。

## 直播授权回归

新增 liveDistributionGetsFreshTimeSignatureWithoutAccountToken：源URL原query保留，附当前时间签名token，两次不同秒得到不同签名，不将账户_he/_pl/_si附到媒体URL。测试使用合成example.test地址，无真实媒体授权。

独立执行testDebugUnitTest成功。此测试验证客户端构造，不代替直播CDN与原生解码验收。
