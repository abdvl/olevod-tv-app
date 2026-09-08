# UI v2 运行截图集

这里的24张图片来自Android TV模拟器上的原生App。页面使用正式生产组件，公开影片海报搭配合成观看记录和公开示例账号 `movie_fan`；没有展示真实用户的账号或历史。播放器画面是明确标注的静态布局预览，分辨率/码率/时间均为夹具值，真实媒体验证请看 [V2-10](../V2-10.md)。

[设计对照检查](../../../../design-qa.md) · [74条验收覆盖](../V2-10.md#74-条设计要求的覆盖) · [图片散列与设备/字体/状态清单](manifest.json)

全部运行图1920×1080、density320、逻辑960×540dp；默认fontScale1.0，最后两张1.3。捕获期间的App按各图manifest中的SHA确认，“更多筛选”在最后短标签留白修正后单独重拍。原图未裁剪或修饰；历史日期由设备当地日期生成。旧的 `home-focused.png` / `mini-movie.png` 属于早期阶段，不计入这24张最终图库。

重现入口（先构建并安装debug包）：

```sh
source scripts/android-env.sh
adb -s emulator-5554 shell am start -n com.olevod.tv/.MainActivity --ez preview true --es screen home
# screen可选：home / category / browse / search / player / history / favorites / account
python3 docs/verification/v2/capture-screenshots.py
```

捕获脚本只发送遥控键并截图，保持预览模式；会暂时设置fontScale1.3并恢复原值。图片加载/异步返回时间会变化，运行后应人工核对每张的焦点和状态，不将脚本结束当作测试通过。登录失败图用fixture主动拒绝提交，不向网站提交示例账号；验证码仅为公开页面请求的临时图片。

## 首页默认：五部＋全部历史

![首页默认：五部＋全部历史](home-default.png)

## 最近播放：同高展开

![最近播放：同高展开](home-recent-focused.png)

## 退出确认：默认继续观看

![退出确认：默认继续观看](exit-confirmation.png)

## 首页最后一行：完整焦点与元信息

![首页最后一行：完整焦点与元信息](home-latest-bottom.png)

## 电影小首页：当年人气榜前二

![电影小首页：当年人气榜前二](mini-category.png)

## 电影目录：紧凑筛选与完整竖图

![电影目录：紧凑筛选与完整竖图](catalog.png)

## 年份四列浮层

![年份四列浮层](catalog-year.png)

## 会员与首字母草稿

![会员与首字母草稿](catalog-more.png)

## MN字面输入与联想

![MN字面输入与联想](search-suggestion.png)

## 确认公共片名后的首结果焦点

![确认公共片名后的首结果焦点](search-results.png)

## 普通播放器：八按钮与分组选集

![普通播放器：八按钮与分组选集](player-normal.png)

## 视频区域可聚焦并确认全屏

![视频区域可聚焦并确认全屏](player-video-focus.png)

## 全屏控制覆盖层

![全屏控制覆盖层](player-full-visible.png)

## 隐藏控制层，同一虚拟视频

![隐藏控制层，同一虚拟视频](player-full-hidden.png)

## 21–30组，只预览未切播

![21–30组，只预览未切播](player-episodes.png)

## 速度浮层

![速度浮层](player-speed.png)

## 此设备：合成观看记录与删除入口

![此设备：合成观看记录与删除入口](history-device.png)

## 网站历史样式：无日期/删除

![网站历史样式：无日期/删除](history-cloud.png)

## 收藏：六列完整竖图

![收藏：六列完整竖图](favorites.png)

## 记住账号：默认数字1

![记住账号：默认数字1](login-remembered.png)

## 夹具提交失败：保留账号并刷新验证码

![夹具提交失败：保留账号并刷新验证码](login-error.png)

## 首次表单：默认用户名

![首次表单：默认用户名](login-first.png)

## 1.3字体：普通播放器

![1.3字体：普通播放器](large-font.png)

## 1.3字体：历史第二排聚焦

![1.3字体：历史第二排聚焦](history-large-font.png)
