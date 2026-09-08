package com.olevod.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.*
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.olevod.tv.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun LiveScreen(vm:AppViewModel,full:Boolean,toggleFull:()->Unit) {
    var selected by remember{mutableStateOf<Channel?>(vm.pendingChannel)}
    LaunchedEffect(Unit){vm.pendingChannel=null}
    BackHandler(selected!=null&&!full){selected=null}
    if(selected!=null){LiveWatch(selected!!,vm,full,toggleFull);return}
    var groups by remember{mutableStateOf<List<Pair<Int,String>>>(emptyList())}
    var group by rememberSaveable{mutableIntStateOf(0)}
    var page by rememberSaveable(group){mutableIntStateOf(1)}
    var channels by remember{mutableStateOf<List<Channel>>(emptyList())}
    var total by remember{mutableIntStateOf(0)}
    var loading by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf<String?>(null)}
    var retry by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){try{groups=vm.api.liveGroups()}catch(e:Exception){if(e is CancellationException)throw e}}
    LaunchedEffect(group,page,retry){loading=true;error=null;try{val result=vm.api.channels(group=group,page=page);channels=result.first;total=result.second}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}finally{loading=false}}
    Column(Modifier.fillMaxSize().padding(40.dp,12.dp,40.dp,25.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row{SectionTitle("电视直播");Spacer(Modifier.weight(1f));(listOf(0 to "全部")+groups).forEach{(id,name)->TvAction(name,selected=group==id){group=id}}}
        when{error!=null->ErrorNotice(error!!){retry++};loading->Text("正在加载频道…",color=Muted);channels.isEmpty()->Text("暂无频道",color=Muted);else->LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(18.dp)){items(channels.chunked(4)){row->Row(horizontalArrangement=Arrangement.spacedBy(15.dp)){row.forEach{c->PosterCard(Movie(c.id,c.title,c.image,c.programme),Modifier.weight(1f),posterRatio=1.7f){selected=c}};repeat(4-row.size){Spacer(Modifier.weight(1f))}}}}}
        Row{if(page>1)TvAction("上一页"){page--};Text("第 $page 页",color=Muted,modifier=Modifier.padding(12.dp));if(page*36<total)TvAction("下一页"){page++}}
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun LiveWatch(channel:Channel,vm:AppViewModel,full:Boolean,toggleFull:()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val scope=rememberCoroutineScope()
    val today=remember{LocalDate.now(ZoneId.of("Asia/Shanghai"))}
    var date by remember{mutableStateOf(today)}
    var detail by remember{mutableStateOf<LiveDetail?>(null)}
    var uri by remember{mutableStateOf("")}
    var favorite by remember{mutableStateOf(false)}
    var favoriteBusy by remember{mutableStateOf(false)}
    var replay by remember{mutableStateOf(false)}
    var title by remember{mutableStateOf(channel.title)}
    var error by remember{mutableStateOf<String?>(null)}
    var loading by remember{mutableStateOf(true)}
    var playing by remember{mutableStateOf(false)}
    var retry by remember{mutableIntStateOf(0)}
    val player=remember{ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OlevodTV/0.1").setDefaultRequestProperties(mapOf("Referer" to "https://www.olevod.com/")))).setSeekBackIncrementMs(30000).setSeekForwardIncrementMs(30000).setAudioAttributes(AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build()}
    DisposableEffect(player,owner){
        val session=MediaSession.Builder(context,player).build()
        val listener=object:Player.Listener{override fun onIsPlayingChanged(value:Boolean){playing=value};override fun onPlaybackStateChanged(state:Int){loading=state==Player.STATE_BUFFERING};override fun onPlayerError(e:PlaybackException){loading=false;error="频道暂时无法播放，请重试"}}
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)player.pause()}
        player.addListener(listener);owner.lifecycle.addObserver(observer)
        onDispose{owner.lifecycle.removeObserver(observer);player.removeListener(listener);session.release();player.release()}
    }
    LaunchedEffect(channel.id,date,retry,vm.sessionVersion){error=null;try{val d=vm.api.liveDetail(channel,date.toString());detail=d;favorite=d.favorite;if(uri.isBlank()||retry>0){replay=false;uri=d.uri;title=channel.title}}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e);loading=false}}
    LaunchedEffect(uri,retry){if(uri.isBlank())return@LaunchedEffect;if(!uri.startsWith("https://")){error="频道没有可用的安全播放源";loading=false;return@LaunchedEffect};player.setMediaItem(MediaItem.Builder().setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build());player.prepare();if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))player.play()}
    Row(Modifier.fillMaxSize().padding(start=if(full)24.dp else 40.dp,top=12.dp,end=if(full)24.dp else 40.dp,bottom=36.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)){
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.fillMaxWidth().weight(1f)){AndroidView(factory={PlayerView(it).apply{this.player=player;useController=false;isFocusable=false}},update={it.keepScreenOn=playing},modifier=Modifier.fillMaxSize());if(loading)Text("正在缓冲…",color=White,modifier=Modifier.background(Bg).padding(15.dp));error?.let{ErrorNotice(it){uri="";retry++}}}
            Text(if(replay)"回看 · $title" else "直播 · ${channel.title}",color=Green,fontSize=17.sp)
            LazyRow{item{TvAction(if(playing)"暂停"else"播放"){if(playing)player.pause()else player.play()}};if(replay){item{TvAction("后退30秒"){player.seekBack()}};item{TvAction("快进30秒"){player.seekForward()}}};item{TvAction(if(full)"退出全屏"else"全屏",onClick=toggleFull)};item{TvAction(if(favoriteBusy)"处理中…"else if(favorite)"已收藏"else"收藏"){if(!favoriteBusy){favoriteBusy=true;scope.launch{try{vm.api.favoriteChannel(channel.id,!favorite);favorite=!favorite}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}finally{favoriteBusy=false}}}}};if(replay)item{TvAction("返回直播"){replay=false;title=channel.title;uri=detail?.uri?:""}}}
        }
        if(!full)Column(Modifier.width(275.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            SectionTitle("节目单","北京时间")
            LazyRow{items((0..6).map{today.minusDays(it.toLong())}){d->TvAction(d.toString().substring(5),selected=date==d){date=d}}}
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
                items(detail?.programmes?:emptyList(),key={it.id}){p->
                    Column{Text(p.time,color=Muted,fontSize=11.sp)
                        if(p.hasReplay&&p.state==1)TvAction("回看 · ${p.title}"){scope.launch{loading=true;error=null;try{val source=vm.api.replay(channel,p);title=p.title;replay=true;uri=source}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e);loading=false}}}
                        else Text((if(p.state==2)"正在播 · "else"")+p.title,color=if(p.state==2)Green else White,fontSize=14.sp,modifier=Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}
