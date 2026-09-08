# 官网品牌素材来源

2026-09-07重新读取官网当前HTML、当前前端JS与公开配置，采用网站实际Header标识，原始文件直接存入drawable-nodpi，不重绘、不重新压缩或改色。

| 本地资源 | 原始来源 | 原始格式 |
|---|---|---|
| official_olevod_logo.png | https://static.olelive.com/uploads/file/2ba1599ea6adbd319aeb2d9d77577ce2_20221229161103.png | 364×64 RGBA PNG，白色透明底标识 |
| official_olevod_icon.png | https://www.olevod.com/favicon.ico | 实际内容为72×72 RGBA PNG，官网黄色图标 |

核对链：[官网](https://www.olevod.com/)当前HTML加载`/js/tv-pc-main.1787324346000.js`；该JS的Header使用`indexData.image_base + "/" + indexData.logo`。实时公开`/v1/pub/index/data`返回上述CDN图片路径。`/css/tv-pc-logo.1787324346000.png`是另一处广告页标识，未用作本App主标识。

使用位置：Header及播放器布局预览替换自制OLE TV字样；应用图标使用官网favicon；TV launcher banner在深色320×180背景居中放置完整白色官网标识，按原比例273×48显示。XML仅设置背景、留白和等比缩放，不修改原始位图。

SHA-256：

- logo：`7e7c09eab96974688a84cfc23f8c2106fa0850b9f658f30c86dded3d318b5c86`
- favicon：`679295ad1de54cfc0b6177226391e76583e9a91410fd48cf2a6022cd44aab55c`
