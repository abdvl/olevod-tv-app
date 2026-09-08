package com.olevod.tv

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
fun NativePlayer(movie:Movie,vm:AppViewModel,full:Boolean,toggleFull:()->Unit,onBack:()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val scope=rememberCoroutineScope()
    val surfaceFocus=remember{FocusRequester()}
    val fullFocus=remember{FocusRequester()}
    val videoFocus=remember{FocusRequester()}
    val groupEntry=remember{FocusRequester()}
    var videoFocused by remember{mutableStateOf(false)}
    var episodeAreaFocused by remember{mutableStateOf(false)}
    var episodeGroup by remember(movie.id){mutableIntStateOf(0)}
    var interactionTick by remember{mutableIntStateOf(0)}
    val accountAtStart=remember(movie.id){vm.sessions.accountKey}
    val resume=remember(movie.id){vm.pendingResume?.takeIf{it.movie.id==movie.id}?:vm.history.records.value.find{it.movie.id==movie.id}}
    LaunchedEffect(movie.id){vm.pendingResume=null}
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
    var trackInfo by remember{mutableStateOf(videoTrackInfo(-1,-1,-1,-1))}
    var favorite by remember{mutableStateOf(false)}
    var favoriteBusy by remember{mutableStateOf(false)}
    var note by remember{mutableStateOf("")}
    val player=remember {
        val http=DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OlevodTV/0.1").setDefaultRequestProperties(mapOf("Referer" to "https://www.olevod.com/"))
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(http)).setSeekBackIncrementMs(30000).setSeekForwardIncrementMs(30000).setAudioAttributes(AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build()
    }
    val currentDetail by rememberUpdatedState(detail)
    val currentEpisode by rememberUpdatedState(episode)
    fun saveProgress(forceSync:Boolean=true){val d=currentDetail;val ep=d?.episodes?.find{it.index==player.currentMediaItem?.mediaId?.toIntOrNull()};if(d!=null&&ep!=null)vm.record(d.movie,ep.index,player.currentPosition,player.duration.coerceAtLeast(0),accountAtStart,forceSync)}
    DisposableEffect(player,owner) {
        val session=MediaSession.Builder(context,player).build()
        val listener=object:Player.Listener {
            override fun onIsPlayingChanged(value:Boolean){playing=value;if(!value)saveProgress()}
            override fun onPlaybackStateChanged(state:Int){buffering=state==Player.STATE_BUFFERING;ended=state==Player.STATE_ENDED}
            override fun onPlayerError(e:PlaybackException){error="视频暂时无法播放，可重新加载或返回选择其他影片";buffering=false}
        }
        player.addListener(listener)
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP){saveProgress();player.pause()}}
        owner.lifecycle.addObserver(observer)
        onDispose{saveProgress();owner.lifecycle.removeObserver(observer);player.removeListener(listener);session.release();player.release()}
    }
    LaunchedEffect(movie.id,retry) {
        saveProgress();player.stop();trackInfo=videoTrackInfo(-1,-1,-1,-1);detail=null;error=null;buffering=true
        try { val d=vm.api.detail(movie.id);detail=d;favorite=d.favorite;episode=d.episodes.indexOfFirst{it.index==(resume?.episode?:d.resumeEpisode)}.takeIf{it>=0}?:0;if(d.episodes.isEmpty()){error="该影片暂无可用播放源";buffering=false} }
        catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e);buffering=false}
    }
    LaunchedEffect(detail,episode) {
        val d=detail?:return@LaunchedEffect
        val item=d.episodes.getOrNull(episode)?:return@LaunchedEffect
        if(!item.uri.startsWith("https://")){error="此片源暂不支持原生播放";buffering=false;return@LaunchedEffect}
        // Auto-next must not replace the episode row the user is currently navigating.
        if(!episodeAreaFocused)episodeGroup=episode.coerceAtLeast(0)/10
        trackInfo=videoTrackInfo(-1,-1,-1,-1)
        player.setMediaItem(MediaItem.Builder().setMediaId(item.index.toString()).setUri(item.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(d.movie.title).build()).build())
        val start=if(item.index==resume?.episode)resume.positionMs.takeUnless{resume.durationMs>0&&it>=resume.durationMs-10000}?:0 else if(item.index==d.resumeEpisode)d.resumeSeconds*1000 else 0
        if(start>0)player.seekTo(start)
        error=null;player.prepare();if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))player.play();ended=false
    }
    LaunchedEffect(player){while(true){
        position=player.currentPosition.coerceAtLeast(0);duration=player.duration.takeIf{it!=C.TIME_UNSET&&it>0}?:0
        val format=player.videoFormat
        trackInfo=videoTrackInfo(format?.width?:-1,format?.height?:-1,format?.averageBitrate?:-1,format?.peakBitrate?:-1)
        delay(500)
    }}
    LaunchedEffect(player){while(true){delay(10000);if(player.isPlaying&&owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))saveProgress(false)}}
    LaunchedEffect(controls,playing,speedMenu,full,interactionTick,episodeAreaFocused){if(full&&controls&&playing&&!speedMenu&&!episodeAreaFocused){delay(5000);controls=false}}
    LaunchedEffect(ended){if(ended&&episode+1<(detail?.episodes?.size?:0)){saveProgress();episode++}}
    LaunchedEffect(full,controls){if(full&&!controls)surfaceFocus.requestFocus()else fullFocus.requestFocus()}
    BackHandler{if(speedMenu)speedMenu=false else onBack()}
    Row(Modifier.fillMaxSize().background(Color.Black).focusRequester(surfaceFocus).focusable(enabled=full&&!controls).onPreviewKeyEvent { event ->
        if(event.nativeKeyEvent.keyCode==AndroidKeyEvent.KEYCODE_BACK){
            if(event.type==KeyEventType.KeyUp){if(speedMenu)speedMenu=false else onBack()}
            true
        }else if(event.type!=KeyEventType.KeyDown)false else {interactionTick++;when(event.nativeKeyEvent.keyCode){
            AndroidKeyEvent.KEYCODE_DPAD_UP->{if(episodeAreaFocused)false else if(full){controls=false;true}else{videoFocus.requestFocus();true}}
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE->{if(player.isPlaying)player.pause()else player.play();true}
            AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD->{player.seekForward();true}
            AndroidKeyEvent.KEYCODE_MEDIA_REWIND->{player.seekBack();true}
            else->{if(full&&!controls){when(event.nativeKeyEvent.keyCode){AndroidKeyEvent.KEYCODE_DPAD_LEFT->player.seekBack();AndroidKeyEvent.KEYCODE_DPAD_RIGHT->player.seekForward();else->controls=true};true}else false}
        }
    }}.padding(if(full)0.dp else 40.dp,if(full)0.dp else 12.dp,if(full)0.dp else 40.dp,if(full)0.dp else 24.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
        PlayerVideoStage(full,controls,Modifier.weight(1f),
            videoModifier=if(full)Modifier else Modifier.focusRequester(videoFocus)
                .focusProperties{up=FocusRequester.Cancel;down=fullFocus}
                .onFocusChanged{videoFocused=it.isFocused}
                .border(if(videoFocused)2.dp else 0.dp,if(videoFocused)Green else Color.Transparent)
                .semantics{contentDescription="视频画面，按确认键全屏"}.clickable{toggleFull()},video={
                AndroidView(factory={PlayerView(it).apply{this.player=player;useController=false;isFocusable=false;keepScreenOn=true}},modifier=Modifier.fillMaxSize(),update={it.player=player;it.keepScreenOn=playing})
                if(buffering)Text("正在缓冲…",color=White,modifier=Modifier.background(Bg).padding(12.dp))
                error?.let{Column(Modifier.background(Bg).padding(20.dp)){ErrorNotice(it){retry++}}}
        },controlContent={
                Box(Modifier.fillMaxWidth().height(3.dp).background(Panel)){Box(Modifier.fillMaxWidth(if(duration>0)(position.toFloat()/duration).coerceIn(0f,1f)else 0f).fillMaxHeight().background(Green))}
                Row(Modifier.fillMaxWidth()){Text(clock(position),color=Muted,fontSize=11.sp);Spacer(Modifier.weight(1f));Text(clock(duration),color=Muted,fontSize=11.sp)}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).focusProperties{if(!full)up=videoFocus;if(!detail?.episodes.isNullOrEmpty())down=groupEntry},horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                    TvAction(if(full)"退出全屏"else"全屏",Icons.Rounded.Fullscreen,selected=true,modifier=Modifier.focusRequester(fullFocus),onClick=toggleFull)
                    TvAction(if(playing)"暂停" else "播放",if(playing)Icons.Rounded.Pause else Icons.Rounded.PlayArrow){if(playing)player.pause() else player.play()}
                    TvAction("30秒",Icons.Rounded.Replay30){player.seekBack()}
                    TvAction("30秒",Icons.Rounded.Forward30){player.seekForward()}
                    TvAction("5分钟",Icons.Rounded.FastRewind){player.seekTo(jumpPosition(player.currentPosition,player.duration,-300000))}
                    TvAction("5分钟",Icons.Rounded.FastForward){player.seekTo(jumpPosition(player.currentPosition,player.duration,300000))}
                    TvAction("${speed}×"){speedMenu=true}
                    TvAction(if(favoriteBusy)"处理中…"else if(favorite)"已收藏"else"收藏",Icons.Rounded.BookmarkBorder){if(!favoriteBusy){favoriteBusy=true;scope.launch{try{vm.api.favorite(movie.id,!favorite);favorite=!favorite;note=""}catch(e:Exception){if(e is CancellationException)throw e;note=safeError(e)}finally{favoriteBusy=false}}}}
                }
                if(full)Column(Modifier.width(145.dp),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(3.dp)){
                    Text(trackInfo.resolution,color=White,fontSize=12.sp,maxLines=1)
                    Text(trackInfo.bitrate,color=Muted,fontSize=11.sp,maxLines=1)
                }
                }
                if(note.isNotBlank())Text(note,color=Gold,fontSize=11.sp)
                detail?.let{d->EpisodePicker(d.episodes,d.episodes.getOrNull(episode)?.index?:-1,episodeGroup,
                    chooseGroup={episodeGroup=it},play={ep->saveProgress();episode=d.episodes.indexOf(ep)},
                    controlsFocus=fullFocus,groupEntry=groupEntry,onFocusWithin={episodeAreaFocused=it})}
        })
        if(!full)Column(Modifier.width(260.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(detail?.movie?.title?:movie.title,color=White,fontSize=24.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(listOf(detail?.movie?.score?:movie.score,detail?.movie?.year?:movie.year,detail?.movie?.area?:movie.area).filter{it.isNotBlank()}.joinToString(" · "),color=Green,fontSize=14.sp)
            Text(detail?.description?:"正在加载影片信息…",color=Muted,fontSize=13.sp,lineHeight=21.sp,maxLines=4,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.heightIn(max=90.dp))
            detail?.let{d->Text("导演：${d.director}\n主演：${d.actor}",color=Muted,fontSize=12.sp,lineHeight=20.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
        }
    }
    if(speedMenu)Dialog(onDismissRequest={speedMenu=false}){Column(Modifier.background(Panel).padding(25.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("播放速度",color=White,fontSize=22.sp);listOf(.5f,.75f,1f,1.25f,1.5f,1.75f,2f).forEach{value->TvAction("${value}×",selected=speed==value){speed=value;player.setPlaybackSpeed(value);speedMenu=false}}}}
}
internal fun clock(ms:Long):String {val seconds=ms/1000;return if(seconds>=3600)"%d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60)else "%02d:%02d".format(seconds/60,seconds%60)}

internal fun jumpPosition(position:Long,duration:Long,delta:Long):Long=(position+delta).coerceAtLeast(0).coerceAtMost(duration.takeIf{it>0}?:Long.MAX_VALUE)
