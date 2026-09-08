# 登录测试未持久会话：离线诊断

2026-09-07。依据主 agent 描述的仪器测试输出与当前源码；未操作模拟器，未读取用户名/密码/验证码文件。

## 结论

现有证据不能确认首次登录真实成功。应优先排除 assumption skip，其次核查旧 apply 的落盘窗口。不能仅凭简略 `OK (1 test)` 宣称账号已登录，也不能把第二次 code15 直接归因为服务器“已经登录”。

## 逐项审查

- LoginIntegrationTest 和 AuthenticatedContentTest 都使用 InstrumentationRegistry.targetContext，目标是应用目录，未发现误写 test APK prefs 的代码。
- SessionStore 初始化顺序为 prefs、alias、cached=read；引用均已初始化。默认alias从固定olevod-session-v1变为模板后仍相同，不会仅因storeName参数引入就丢失原密钥。
- LoginIntegrationTest 对输入文件存在性使用 assumeTrue。文件缺失会报告假设未满足/跳过，而不是断言失败；简略成功摘要不足以证明走到api.login和session.save。
- 输入文件在解析后finally立即删除，后续API异常仍无文件。仅“文件不见了”不能证明登录成功。
- 旧测试的新SessionStore仍在同一进程，SharedPreferences可能共享已apply但未刷盘的内存；assertNotNull并未跨进程验证。
- 新save用commit检查返回值、新测试读取XML、login-confirmed标记加强了实际成功证据；标记应仅在API成功和持久化验证后创建。
- read异常执行clear().apply()，不显式删除session.xml；若磁盘只有search.xml，较符合没有写入、未落盘便进程结束或应用数据曾清理，而非单纯解密失败。此为推断，不是已复现根因。
- 安装测试APK本身不应按代码逻辑删除目标会话；若脚本包含目标应用卸载/pm clear/测试编排清理，需额外核查调用历史，本轮未获得该日志。

## 最小排查路径

1. 对显式调用的登录测试将缺输入文件视为失败，或required参数开启assert；常规全套可保留optional跳过。
2. 保留原始instrumentation -r结果中的状态与假设失败字段，不只保留末尾OK。
3. 登录后必须观察login-confirmed标记、session.xml存在及独立下一进程SessionStore.token非空；不输出token正文。
4. 合成session-test跨进程持久测试可排除Keystore/权限问题，无需重复消耗真实验证码。当前合成测试末尾clear，需要拆成写/读两个有序运行才能证明进程边界。
5. 新验证码只在确认输入文件已正确注入目标目录后提交一次；code15须结合网站错误语义，不自动猜测成功或凭据错误。
