package com.olevod.tv

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun NativePlayer(movie:Movie,vm:AppViewModel,full:Boolean,toggleFull:()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val scope=rememberCoroutineScope()
    val surfaceFocus=remember{FocusRequester()}
    val playFocus=remember{FocusRequester()}
    var interactionTick by remember{mutableIntStateOf(0)}
    val resume=remember(movie.id){vm.history.records.value.find{it.movie.id==movie.id}}
    var detail by remember(movie.id){mutableStateOf<Detail?>(null)}
    var episode by remember(movie.id){mutableIntStateOf(-1)}
    var error by remember(movie.id){mutableStateOf<String?>(null)}
    var retry by remember{mutableIntStateOf(0)}
    var controls by remember{mutableStateOf(true)}
    var speedMenu by remember{mutableStateOf(false)}
    var playing by remember{mutableStateOf(false)}
    var buffering by remember{mutableStateOf(true)}
    var ended by remember{mutableStateOf(false)}
    var position by remember{mutableLongStateOf(0)}
    var duration by remember{mutableLongStateOf(0)}
    var speed by remember{mutableFloatStateOf(1f)}
    var favorite by remember{mutableStateOf(false)}
    var note by remember{mutableStateOf("")}
    val player=remember {
        val http=DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OlevodTV/0.1").setDefaultRequestProperties(mapOf("Referer" to "https://www.olevod.com/"))
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(http)).setSeekBackIncrementMs(30000).setSeekForwardIncrementMs(30000).setAudioAttributes(AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build()
    }
    val currentDetail by rememberUpdatedState(detail)
    val currentEpisode by rememberUpdatedState(episode)
    fun saveProgress(){val d=currentDetail;val ep=d?.episodes?.getOrNull(currentEpisode);if(d!=null&&ep!=null)vm.record(d.movie,ep.index,player.currentPosition,player.duration.coerceAtLeast(0))}
    DisposableEffect(player,owner) {
        val session=MediaSession.Builder(context,player).build()
        val listener=object:Player.Listener {
            override fun onIsPlayingChanged(value:Boolean){playing=value}
            override fun onPlaybackStateChanged(state:Int){buffering=state==Player.STATE_BUFFERING;ended=state==Player.STATE_ENDED}
            override fun onPlayerError(e:PlaybackException){error="视频暂时无法播放，可重新加载或返回选择其他影片";buffering=false}
        }
        player.addListener(listener)
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP){saveProgress();player.pause()}}
        owner.lifecycle.addObserver(observer)
        onDispose{saveProgress();owner.lifecycle.removeObserver(observer);player.removeListener(listener);session.release();player.release()}
    }
    LaunchedEffect(movie.id,retry) {
        player.stop();detail=null;error=null;buffering=true
        try { val d=vm.api.detail(movie.id);detail=d;favorite=d.favorite;episode=d.episodes.indexOfFirst{it.index==(resume?.episode?:d.resumeEpisode)}.takeIf{it>=0}?:0;if(d.episodes.isEmpty()){error="该影片暂无可用播放源";buffering=false} }
        catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e);buffering=false}
    }
    LaunchedEffect(detail,episode) {
        val d=detail?:return@LaunchedEffect
        val item=d.episodes.getOrNull(episode)?:return@LaunchedEffect
        if(!item.uri.startsWith("https://")){error="此片源暂不支持原生播放";buffering=false;return@LaunchedEffect}
        player.setMediaItem(MediaItem.Builder().setUri(item.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(d.movie.title).build()).build())
        val start=if(item.index==resume?.episode)resume.positionMs.takeUnless{resume.durationMs>0&&it>=resume.durationMs-10000}?:0 else if(item.index==d.resumeEpisode)d.resumeSeconds*1000 else 0
        if(start>0)player.seekTo(start)
        player.prepare();player.play();ended=false
    }
    LaunchedEffect(player){while(true){position=player.currentPosition.coerceAtLeast(0);duration=player.duration.takeIf{it!=C.TIME_UNSET&&it>0}?:0;delay(500)}}
    LaunchedEffect(player){while(true){delay(10000);saveProgress()}}
    LaunchedEffect(controls,playing,speedMenu,full,interactionTick){if(full&&controls&&playing&&!speedMenu){delay(5000);controls=false}}
    LaunchedEffect(ended){if(ended&&episode+1<(detail?.episodes?.size?:0))episode++}
    LaunchedEffect(full,controls){if(full&&!controls)surfaceFocus.requestFocus()else playFocus.requestFocus()}
    BackHandler(speedMenu){speedMenu=false}
    Row(Modifier.fillMaxSize().background(Color.Black).focusRequester(surfaceFocus).focusable().onPreviewKeyEvent { event ->
        if(event.type!=KeyEventType.KeyDown)false else {interactionTick++;when(event.nativeKeyEvent.keyCode){
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE->{if(player.isPlaying)player.pause()else player.play();true}
            AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD->{player.seekForward();true}
            AndroidKeyEvent.KEYCODE_MEDIA_REWIND->{player.seekBack();true}
            else->{if(full&&!controls){when(event.nativeKeyEvent.keyCode){AndroidKeyEvent.KEYCODE_DPAD_LEFT->player.seekBack();AndroidKeyEvent.KEYCODE_DPAD_RIGHT->player.seekForward();else->controls=true};true}else false}
        }
    }}.padding(if(full)0.dp else 40.dp,if(full)0.dp else 12.dp,if(full)0.dp else 40.dp,if(full)24.dp else 24.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().then(if(full)Modifier.weight(1f)else Modifier.aspectRatio(16f/9)),contentAlignment=Alignment.Center) {
                AndroidView(factory={PlayerView(it).apply{this.player=player;useController=false;isFocusable=false;keepScreenOn=true}},modifier=Modifier.fillMaxSize(),update={it.player=player;it.keepScreenOn=playing})
                if(buffering)Text("正在缓冲…",color=White,modifier=Modifier.background(Bg).padding(12.dp))
                error?.let{Column(Modifier.background(Bg).padding(20.dp)){ErrorNotice(it){retry++}}}
            }
            if(controls || !full || !playing)Column(Modifier.padding(horizontal=if(full)30.dp else 0.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Panel)){Box(Modifier.fillMaxWidth(if(duration>0)(position.toFloat()/duration).coerceIn(0f,1f)else 0f).fillMaxHeight().background(Green))}
                Row(Modifier.fillMaxWidth()){Text(clock(position),color=Muted,fontSize=11.sp);Spacer(Modifier.weight(1f));Text(clock(duration),color=Muted,fontSize=11.sp)}
                LazyRow(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                    item{TvAction("30秒",Icons.Rounded.Replay30){player.seekBack()}}
                    item{TvAction(if(playing)"暂停" else "播放",if(playing)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,selected=true,modifier=Modifier.focusRequester(playFocus)){if(playing)player.pause() else player.play()}}
                    item{TvAction("30秒",Icons.Rounded.Forward30){player.seekForward()}}
                    item{TvAction("${speed}×"){speedMenu=true}}
                    item{TvAction(if(full)"退出全屏"else"全屏",Icons.Rounded.Fullscreen,onClick=toggleFull)}
                    item{TvAction(if(favorite)"已收藏"else"收藏",Icons.Rounded.BookmarkBorder){scope.launch{try{vm.api.favorite(movie.id,!favorite);favorite=!favorite;note=""}catch(e:Exception){if(e is CancellationException)throw e;note=safeError(e)}}}}
                }
                if(note.isNotBlank())Text(note,color=Gold,fontSize=11.sp)
            }
        }
        if(!full)Column(Modifier.width(260.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Text(detail?.movie?.title?:movie.title,color=White,fontSize=24.sp)
            Text(listOf(detail?.movie?.score?:movie.score,detail?.movie?.year?:movie.year,detail?.movie?.area?:movie.area).filter{it.isNotBlank()}.joinToString(" · "),color=Green,fontSize=14.sp)
            Text(detail?.description?:"正在加载影片信息…",color=Muted,fontSize=13.sp,lineHeight=21.sp)
            detail?.let{d->Text("导演：${d.director}\n主演：${d.actor}",color=Muted,fontSize=12.sp,lineHeight=20.sp)}
            Text("选集",color=White,fontSize=18.sp)
            detail?.episodes?.chunked(4)?.forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){row.forEach{ep->TvAction(if((detail?.episodes?.size?:0)>1)ep.index.toString() else ep.title,selected=detail?.episodes?.getOrNull(episode)?.index==ep.index){episode=detail!!.episodes.indexOf(ep)}}}}
        }
    }
    if(speedMenu)Dialog(onDismissRequest={speedMenu=false}){Column(Modifier.background(Panel).padding(25.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("播放速度",color=White,fontSize=22.sp);listOf(.5f,.75f,1f,1.25f,1.5f,1.75f,2f).forEach{value->TvAction("${value}×",selected=speed==value){speed=value;player.setPlaybackSpeed(value);speedMenu=false}}}}
}
internal fun clock(ms:Long):String {val seconds=ms/1000;return if(seconds>=3600)"%d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60)else "%02d:%02d".format(seconds/60,seconds%60)}
