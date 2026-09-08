# 观看进度云同步独立静态审查

2026-09-07。审查 AppViewModel.record、OlevodApi.syncWatch、NativePlayer.saveProgress；不操作模拟器，不读取凭据，不修改实现。真实云端回读由主 agent 执行，不算本报告实测。

## 具体风险

审查时 record 每次启动独立 viewModelScope 协程，先执行 history.save（切换 IO），再进入 syncMutex。IO完成顺序不等于 record 调用顺序，所以较旧进度可能在较新进度后进入 mutex；forceSync会跳过时间节流，旧记录可覆盖云端较新记录。本地 INSERT 同样没有版本防旧覆盖。已直接通知主 agent：将本地写入和远端请求放在按调用顺序消费的队列内，或用单调版本拒绝旧任务；仅网络 mutex 不保证先后。

暂停/退出任务依附 viewModelScope，没有持久补传队列；页面退出且 ViewModel仍存活时可继续同步，Activity销毁、系统杀进程不能保证网络请求完成。这是当前 best-effort 限制，不应承诺每次关闭App都上传成功。

## 已确认代码路径

- record调用时冻结当前token，且上传前比对account与当前token；API实例使用冻结token，因此账号切换后旧任务不会借新账号凭证发送。
- 本地保存先于网络失败，错误状态明确告知本机已存；取消异常向上传播。
- 暂停、ON_STOP、播放器dispose均请求forceSync；10秒采样且普通上传节流30秒，重复相同位置略过。
- saveProgress按播放器实际MediaItem.mediaId定位集数，减少切集状态变化时错误关联新集的风险。
- 与公开JS研究的字段映射一致：duration是已看秒数，percent是总时长秒数，saveTime是Unix秒。客户端使用小数秒，官网使用parseInt整数秒，兼容性须以主agent真实回读结果确认。

此报告为审查瞬间代码状态；主agent后续修复需要另行记录收口。

## 顺序修复复查

已再次只读检查最新实现。原先的顺序风险已修复：record同步trySend进入watchQueue，唯一消费者按接收顺序等待history.save完成后送入cloudQueue；另一个唯一消费者顺序等待每次网络请求。不存在多个IO保存完成顺序竞争，也不会因为网络慢而阻塞本地写入。普通播放器的主线程连续record调用顺序得到保持；云端失败由每项catch吸收，后续项仍可执行。

账号和token在入队时捕获、上传前再次比对，固定token API实例保留；暂停/退出force标记经过两级队列传递，没有被丢弃。duration、percent现已改为整数秒，与公开网站parseInt的语义一致。

主agent报告真实云回读2项测试通过：影片80632，观看117秒、总长1050秒；这是主agent实测结果，本复查未重复执行网络或模拟器测试。

本复查未发现新的顺序或跨账号发送缺陷。进程结束时内存队列仍可能取消，前述best-effort生命周期限制继续成立。
