# API Adapter 独立检查

2026-09-07。只读检查 `OlevodApi.kt`、站点公开构建 JS 与 `.tools/v1_pub_*.json` 公开样本；不读取 `.secrects`，不发送真实账户写请求，不操作模拟器。

## 确认缺陷

1. **直播分类标签字段不匹配（已修复）**：`liveGroups()` 读取 `name`，实际 `v1_pub_live_conditions.json` 为 `groups:[{id:1,title:"央视",type:"tv"},{id:2,title:"地方",type:"tv"}]`。结果 ID 正常、标签为空。新增回归测试已复现。

## 已对照通过

- browse 路径与既有单变量对照一致，搜索词采用独立 URL path segment 编码；普通/VIP ID 不合并。
- 修正初轮遗漏：search 的 data.data 是媒体分组，影片在 type=vod 组的 list，total 也取该组；主 agent 已修复，新增回归通过。browse 为 data.list。
- 点播收藏添加 `{id}`，取消 `{ids:[id]}` 与 player JS 一致。
- 回看 POST `{programmeId,stream_id}`、返回 `data.hls` 与公开记录一致。
- 直播详情 `detail` + `programs`、`showTime/start/end/liveType/hasVod` 字段匹配。保留 ISO `+08:00` 时区文本，未把频道 `liveHasVod=false` 当节目回看禁用条件。
- token 拆分 `_he/_pl/_si` 与网站拦截器一致；GET 附动态 `_vv`；POST不附 `_vv`。
- 默认 API 客户端不跟随重定向，既有测试可阻止跨 origin 泄漏 token。
- 当前公开配置 `encrypt=false`；不要求本次实现网站可选加密数据模式，但协议变更应明确报错。

## 登录 / 媒体后续风险

- 此次审查版本没有 `login/captcha/userInfo` 具体方法，不能称登录已验收。源码表单为 username/password/captcha/captcha_id，验证码返回 captchaId/picPath，成功为 data.user/data.token。
- 网站 code12 是打开登录框，13/14/16 执行登出；当前全部显示“登录已失效”，首次未登录也会误称失效。错误分型可区分 LoginRequired 与 SessionExpired。
- `image()` 未保留 `http://` 绝对 URL，会拼成 `imageBase/http://...`。本次公开图片样本多为相对地址，属于待兼容分支，不据此宣称当前首页图片失败。
- `Episode` 为 data class，默认 toString 包含媒体 uri；`Detail` 默认 toString 也递归包含这些地址。后续调试/日志应避免打印，最好为媒体地址包装器提供脱敏 toString。
- `resumeSeconds` 把 `recordWatchDuration` 直接命名为秒；字段单位仍需跨端播放验证，当前名称不是证据。
- 直播 groups、节目状态和流 uri 的原生播放可用性是不同验收；此次 MockWebServer 回归不能证明实际媒体解码。

## 测试与结果

新增 `OlevodAdapterContractTest.kt`，5 项合成响应合同测试，不包含账户资料和带签名媒体链接：

- 直播 groups.title 字段。
- 回看 programme 身份字段、POST及API token拆分。
- 节目时区与单节目 hasVod 独立于频道 liveHasVod。
- 无 Content-Encoding 头的 gzip 响应。
- 取消收藏 ids 数组。

独立执行 `./gradlew testDebugUnitTest`：10项共9过1失败，唯一失败为直播 title/name 缺陷。实现由主 agent 修复后需复跑，测试不应为了绿色改成错误预期。


## 修复复验

主 agent 已修复直播 title 字段及搜索分组解析。新增两项搜索测试：只取 vod 组影片和该组 total；没有 vod 组不能解析出假影片。独立执行 testDebugUnitTest，共12项全部通过。初轮9过1失败记录保留为缺陷复现历史。
