# Olevod Android TV App

面向个人使用的 Google Chromecast with Google TV 原生客户端，深色/绿色TV界面，支持遥控器操作。

当前可安装测试版：真实首页、分行分类筛选、三栏搜索、无限滚动、点播/VIP、电视登录、本机历史与云历史同步、影视收藏。账号密码可加密记住，下次直接输入验证码；播放器支持30秒和5分钟跳转。已安装到用户的Chromecast，实机结果见进度表。直播与回看入口保留，稳定性排查按用户要求暂缓。

- [当前检查点与恢复入口](docs/PROGRESS.md)
- [小步实施清单](docs/IMPLEMENTATION_STEPS.md)
- [构建与Android Studio调试](docs/BUILD_AND_TEST.md)
- [独立最终回归](docs/verification/FINAL_REGRESSION.md)
- [独立播放/搜索验证](docs/verification/PLAYBACK_SEARCH.md)
- [网站与API调查](docs/API_RESEARCH.md)
- [产品与代码设计](docs/DESIGN.md)
- [云历史支持边界](docs/CLOUD_HISTORY_STATUS.md)

首页按最近更新每类展示10部；首版目标包含直播和VIP蓝光，电视直接登录。未验证功能不会以预览或模拟数据冒充成功。
