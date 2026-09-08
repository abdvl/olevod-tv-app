# 欧乐影院网站与 API 调查

调查日期：2026-09-07（America/Los_Angeles；UTC 为 09-08）。
方法：使用用户已登录的 Chrome，浏览器 CDP 被动观察请求、响应及网站发布的 JavaScript；通过正常页面操作验证搜索、筛选、点播、直播、回看。没有 Android 客户端实测。
网站构建资源版本：`1787324346000`。接口为网站内部接口，不应当作稳定公开 SDK。

## 1. 结论与证据等级

| 功能 | 证据 | 结论 / 边界 |
| --- | --- | --- |
| 首页推荐及分类内容 | 页面、200 响应、JSON | 可实现；电影首页响应包含 `list`、`rank`，样本 list 为 12 条 |
| 每类最近更新 10 部 | 最新排序请求实测 | 使用分类列表全局 update 排序取前 10，不能把首页任意 12 条简单当作最新 |
| 分类、地区、年份、类型、首字母、会员筛选 | 页面可见；美国地区请求实测 | 可实现；全部参数位含义需逐项对照，避免猜值 |
| 最新、添加、最热、评分 | 页面及 update/score 请求实测；hot/desc 请求观察 | 服务端排序；不能仅排序当前页 |
| 中文搜索、分类限定 | 中文搜索实测返回 3 条结果 | 有分页和联想端点；拼音全拼、首字母缩写尚未实测 |
| 详情、海报、剧集、收藏状态 | 详情 JSON 实测 | 可实现；普通版与 VIP 同名内容须按 ID 区分 |
| VIP 点播 | 详情和 HLS 清单、分片成功 | 一个样本为 H.264/AAC；尚未验证 Android、全部蓝光格式及未登录行为 |
| 直播 | CCTV-13 页面、HLS 和分片成功 | 有独立频道目录与播放链路 |
| 节目单、回看 | 节目单页面；回看 POST code=0、HLS 200 | 按节目可用性开放；不保证全部频道、全部历史日期可回看 |
| 登录 | 已登录状态；前端登录实现 | 账号、密码、图片验证码；本次未退出账号或重做登录 |
| 收藏列表 | 页面及列表请求成功 | 添加/取消端点来自源码，本次未改收藏来验证写操作 |
| 全部云端历史 | 历史页面、源码和同步请求 | 尚不能承诺；当前网页影视/频道各截断到最近 18 条本地记录 |
| 播放进度 | 页面续播提示、详情 record 字段、同步源码 | 有相关字段；单位与跨端读写一致性需验证 |

## 2. 网站结构

- 首页 `/`；分类聚合页 `/tabs-{id}.html`。
- 已观察分类：直播、短剧、电影、连续剧、综艺、动漫、VIP蓝光影院。
- 点播分类 ID：短剧 14、电影 1、连续剧 2、综艺 3、动漫 4、VIP 6；直播页面入口 0。运行时仍应读取导航配置。
- 点播筛选路由示例：`/nav/电影/美国/全部年份/全部类型/全部/全部/最新.html`（实际 URL 为编码形式）。
- 直播筛选 `/nav/0/1?types=tv`；此样本为央视。
- 搜索 `/search?q=流浪地球`。
- 详情 `/details-6-80632.html`；点播 `/player/vod/6-80632-1.html`。
- 直播 `/player/live/tv-CCTV13HD-58.html`。
- 历史 `/user/back`；收藏 `/user/access`。
- 首页另有赛事信息和官方电视盒子端下载入口；赛事详情与赛事回放协议未在此次样本中验证，不将它们与电视频道协议混为一谈。

## 3. API 总览

API origin：`https://api.olelive.com`。以下仅记录无凭据路径。

标记：**R** 为真实请求或响应已观察；**S** 为网站源码存在、未独立验证。

| 方法与路径 | 等级 | 用途 / 已知参数 |
| --- | --- | --- |
| GET `/v1/pub/index/banners` | R | 首页横幅 |
| GET `/v1/pub/index/navs` | S | 导航与分类元数据 |
| GET `/v1/pub/index/data` | S | 全局配置，需确认响应中的图片根地址等 |
| GET `/v1/pub/vod/list/type` | S | 点播筛选元数据候选 |
| GET `/v1/pub/index/vod/data/{categoryId}` | R | 分类首页，list/rank |
| GET `/v1/pub/vod/list/{p1}/{p2}/{p3}/{area}/{category}/{p6}/{p7}/{sort}/{page}/{pageSize}` | R | 点播筛选；未验证的参数位保留中性名称 |
| GET `/v1/pub/index/vod/level/1/0/6` | R | 电影推荐样本 |
| GET `/v1/pub/vod/rank/week/1/0/true/10` | R | 排行榜样本；布尔位含义待核实 |
| GET `/v1/pub/index/search/hot/keywords` | S | 热门词 |
| GET `/v1/pub/index/search/keywords/{query}` | R | 搜索联想 |
| GET `/v1/pub/index/search/{query}/vod/{category}/{page}/{pageSize}` | R | 样本 category=0、page=1、pageSize=4 |
| GET `/v1/pub/vod/detail/{vodId}/{playMode}` | R | 同一影片 false 为详情，true 为播放页面请求；需验证布尔位完整语义 |
| GET `/v1/pub/vod/random/{category}/{type}/{count}` | R | 相关推荐 |
| POST `/pub/captcha` | S | 图片验证码 |
| POST `/pub/user/login` | S | 登录 |
| POST `/pub/user/info` | R | 当前账户信息 |
| POST `/pub/vod/favorite/list` | R | 影视收藏列表；分页请求体待验证 |
| POST `/pub/vod/favorite`、`/pub/vod/favorite/cancel` | S | 影视收藏 / 取消 |
| POST `/pub/vod/history/list` | S | 云端影视历史候选；是否仍可用待验证 |
| POST `/pub/vod/watch/record` | S | 影视进度写入候选 |
| POST `/pub/user/watches/sync` | R | 本地记录同步；不是已证实的全部历史读取接口 |
| POST `/pub/user/watches` | S | 用户观看记录候选，需要确认媒体范围 |
| GET `/v1/pub/live/conditions` | R | 直播筛选配置 |
| GET `/v1/pub/live/list/tv/0/0/3/1/1/16` | R | 央视列表样本，响应 total=5/page=1/pageSize=16 |
| GET `/v1/pub/live/info/tv/{channelId}/{streamId}/{date}` | R | 频道详情及节目单，日期时区待确认 |
| GET `/dis/live/{streamId}/hls.m3u8` | R | 直播分发，随后请求媒体 CDN |
| POST `/pub/tv/vod/url` | R | 回看地址；body 为 programmeId、stream_id |
| POST `/pub/user/play/token` | S | 其他播放授权候选，VIP 样本未观察到依赖它 |
| POST `/pub/user/favorites`、`/pub/user/favorite/save`、`/pub/user/favorite/cancel/channel` | S | 频道收藏候选，不能与点播接口混用 |

筛选实测对照：

```text
电影 + 美国 + 最新：/v1/pub/vod/list/true/3/0/美国/1/0/0/update/1/48
电影 + 美国 + 评分：/v1/pub/vod/list/true/3/0/美国/1/0/0/score/1/48
电影聚合页热播：   /v1/pub/vod/list/true/3/0/0/1/0/0/hot/1/4
电影聚合页新添加： /v1/pub/vod/list/true/3/0/0/1/0/0/desc/1/8
```

`update/score` 有直接筛选交互对照；`hot/desc` 的业务名称结合聚合页内容推断，实施前补齐单变量对照。年份、类型、首字母、会员以及直播数字枚举，不根据此表猜测。

## 4. 响应与数据模型证据

常见包裹：`{ code: 0, data: ..., msg: "ok" }`。HTTP 200 不代表业务成功。

影片卡片字段：`id/name/actor/area/year/typeId/typeId1/pic/picThumb/blurb/hits/score/remarks/version/vip/preFree/new/vodTime`。
详情补充：`content/director/lang/douBanScore/favorite/lock/copyright/total/urls` 和 `recordEpisode/recordCurrentTitle/recordWatchDuration/recordWatchPercent`。
剧集条目：`index/title/url/vip/new/seek_start/seek_end`。各标记、时间字段的含义须对照实际 UI，不按名称自动实现跳片头/尾。
样本详情 `urls[0].url` 是 HTTP URL；只读分析未发现该样本必须通过 WASM 解密地址。网站加载 `play.wasm` 不等于所有视频必须依赖 WASM。

图片有相对路径，网站使用 `image_base` 拼接。不能统一按 www.olevod.com 解析；绝对 CDN URL 保持原样。

直播列表字段：`id/title/streamId/currentTitle/currentImg/playTimes/liveHasVod/liveAlive`，部分有 `playCountRank`。
样本 `liveHasVod=false`、`liveAlive=false`，但同频道详情存在回看且真实回看成功。因此这些列表标记不能单独作为禁用播放或回看的依据，应以详情和实际授权结果为准。

## 5. 登录与请求封装

网站 Axios 拦截器给 GET 加 `_vv`，由当前秒级时间经前端函数生成；不是一个可长期复制的固定字符串。
登录 token 被拆为 `_he/_pl/_si` 附在 API query 中。它们及完整请求 URL 都属于凭据，不保存到代码、示例、日志、崩溃上报或 fixture。
API 与 CDN 使用独立 HTTP 客户端；绝不能把 API token 自动附到视频、图片或跳转后的其他域名。

登录前端输入字段：`username/password/captcha/captcha_id`，验证码响应使用 `captchaId/picPath`，登录成功消费 `data.user/data.token`。
网站有记住账号密码逻辑；Android 设计不复制明文密码持久化。保存加密会话，密码只在本次登录表单存活。
未证实 token refresh 或设备码协议。首版会话失效后重新登录，不假设有 refresh token。

## 6. 历史记录的真实限制

`tv-pc-back.1787324346000.js` 从本地记录读取影视和频道，分别执行 `length > 18` 时截断到 18，然后排序并展示。
进入历史页时，网页调用 `/pub/user/watches/sync`，发送记录数组。源码映射如下：

```text
duration  <- parseInt(local.currentTime)
episode   <- local.level
id        <- vod.id 或 tv.ids
percent   <- parseInt(local.duration)
title     <- local.name
saveTime  <- Unix 秒级时间
type      <- "vod" 或 "tv"
```

这里 `percent` 来自本地 duration，名称可能具有历史兼容语义，不能直接解释为 0..100 百分比。详情 record 字段是否与之同单位还需验证。
源码存在 `/pub/vod/history/list`，但当前页面没有调用它；本次未获得其全量响应和分页约定。不能宣称云端一定支持完整历史、双向同步或无限保留。

实现底线：App 自身完整保存实际播放记录；云端历史按已验证的接口能力展示。若云端只能返回部分数据，明确标注来源和范围，不能把本地记录伪装为网站全部历史。

## 7. 播放结果

- VIP 样本：`80632`，标准 HLS，主清单 1920×1040 / 24fps，CODECS 声明 H.264 + AAC，媒体清单未见 `EXT-X-KEY`；实际收到 TS 分片。仅代表此样本，不代表所有 VIP 片源、DRM 状态或码率兼容性。
- 直播样本：CCTV-13，分发路径转到 `newlive.olelive.com`，清单及多段 TS 成功，页面显示 Pause（正在播放）。
- 回看样本：POST `{programmeId:2269224, stream_id:"CCTV13HD"}` 返回 `code=0`，data 包含 `type/hls/flv/streamId`，随后 HLS 主清单与媒体清单 200。
- 回看 URL 路径含动态授权信息，不在文档保存实际值。
- 尚未测：原生 Android 独立请求、Referer/Origin 是否必需、长时播放、地址过期、异地/CDN 差异、HEVC/HDR/多音轨/字幕、多个普通影片和长剧选集。

## 8. 可重复验证清单

1. 使用正常 UI 逐次改变一个筛选，记录无凭据路径及选中状态。
2. 检查响应业务 code 和 JSON schema；加载中的零条结果不能认定无内容。
3. 对需要 body 的 POST，只保留字段名和合成样例；用户资料只保留类型。
4. 验证分页 total/page/pageSize、重复页、最后一页和异常页。
5. 用空会话测试公开目录，再使用用户本人新登录会话测试受限播放。
6. 测试云端历史读取，不将 sync 调用当读取协议；写入测试使用明确标记的测试记录。
7. Android 播放探针通过后，才能把浏览器可播放升级为原生兼容。

调查时正常播放和搜索会产生网站自己的历史/进度记录；本次未清空历史、未更改收藏、未退出登录。研究新开的两个播放标签已关闭，原标签已恢复初始详情页。

## 9. 参考来源

- [欧乐影院首页](https://www.olevod.com/)
- [网站主程序：端点、登录和拦截器](https://www.olevod.com/js/tv-pc-main.1787324346000.js)
- [历史页面实现：本地 18 条与同步](https://www.olevod.com/js/tv-pc-back.1787324346000.js)
- [VIP 详情样本](https://www.olevod.com/details-6-80632.html)
- [直播样本](https://www.olevod.com/player/live/tv-CCTV13HD-58.html)

脚本文件随网站升级可能失效；这里记录的是本次观察版本，不是永久契约。
