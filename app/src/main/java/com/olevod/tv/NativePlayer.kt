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
fun NativePlayer(movie:Movie,vm:AppViewModel,full:Boolean,toggleFull:()->Unit,onLogin:(Boolean?)->Unit={},onBack:()->Unit) {
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val scope=rememberCoroutineScope()
    var episodeAreaFocused by remember{mutableStateOf(false)}
    var episodeGroup by remember(movie.id){mutableIntStateOf(0)}
    val accountAtStart=remember(movie.id){vm.sessions.accountKey}
    val resume=remember(movie.id){vm.pendingResume?.takeIf{it.movie.id==movie.id}?:vm.history.records.value.find{it.movie.id==movie.id}}
    LaunchedEffect(movie.id){vm.pendingResume=null}
    var detail by remember(movie.id){mutableStateOf<Detail?>(null)}
    var episode by remember(movie.id){mutableIntStateOf(-1)}
    var error by remember(movie.id){mutableStateOf<String?>(null)}
    var retry by remember{mutableIntStateOf(0)}
    var playRetry by remember{mutableIntStateOf(0)}
    var nextStart by remember(movie.id){mutableLongStateOf(0)}
    var seekable by remember{mutableStateOf(false)}
    var playing by remember{mutableStateOf(false)}
    var playRequested by remember{mutableStateOf(false)}
    var buffering by remember{mutableStateOf(true)}
    var ended by remember{mutableStateOf(false)}
    var renderedFrame by remember{mutableStateOf(false)}
    var position by remember{mutableLongStateOf(0)}
    var duration by remember{mutableLongStateOf(0)}
    var speed by remember{mutableFloatStateOf(1f)}
    var trackInfo by remember{mutableStateOf(videoTrackInfo(-1,-1,-1,-1))}
    var favorite by remember{mutableStateOf(false)}
    var favoriteBusy by remember{mutableStateOf(false)}
    var note by remember{mutableStateOf("")}
    var loginRequired by remember{mutableStateOf(false)}
    var loginDesired by remember{mutableStateOf<Boolean?>(null)}
    val player=remember {
        val http=DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OlevodTV/0.1").setDefaultRequestProperties(mapOf("Referer" to "https://www.olevod.com/"))
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(http)).setSeekBackIncrementMs(30000).setSeekForwardIncrementMs(30000).setAudioAttributes(AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build()
    }
    val currentDetail by rememberUpdatedState(detail)
    val currentEpisode by rememberUpdatedState(episode)
    fun saveProgress(forceSync:Boolean=true){val d=currentDetail;val ep=d?.episodes?.find{it.index==player.currentMediaItem?.mediaId?.toIntOrNull()};if(d!=null&&ep!=null)vm.record(d.movie,ep.index,player.currentPosition,player.duration.coerceAtLeast(0),accountAtStart,forceSync)}
    fun login(desired:Boolean?){
        saveProgress();detail?.episodes?.getOrNull(episode)?.let{ep->vm.pendingResume=WatchRecord(detail!!.movie,ep.index,player.currentPosition.coerceAtLeast(0),player.duration.coerceAtLeast(0),System.currentTimeMillis())}
        vm.pendingAuthentication=null
        onLogin(desired)
    }
    fun setFavorite(desired:Boolean){
        if(favoriteBusy)return
        if(vm.sessions.token==null){login(desired);return}
        val account=vm.sessions.accountKey;favoriteBusy=true
        // Cancellation can leave a successful server mutation without its response; always re-read.
        vm.invalidateFavorites()
        scope.launch{try{vm.api.favorite(movie.id,desired);if(vm.sessions.accountKey==account){favorite=desired;vm.invalidateFavorites();note=""}}
            catch(e:Exception){if(e is CancellationException)throw e;note=safeError(e);if(e is ApiException&&e.code in setOf(12,13,14,16)){loginRequired=true;loginDesired=desired;vm.pendingAuthentication=PendingAuthentication(movie.id,desired)}}
            finally{favoriteBusy=false}}
    }
    DisposableEffect(player,owner) {
        val session=MediaSession.Builder(context,player).build()
        val listener=object:Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady:Boolean,reason:Int){playRequested=playWhenReady}
            override fun onIsPlayingChanged(value:Boolean){playing=value;if(!value)saveProgress()}
            override fun onPlaybackStateChanged(state:Int){buffering=state==Player.STATE_BUFFERING;ended=state==Player.STATE_ENDED;seekable=player.isCurrentMediaItemSeekable}
            override fun onRenderedFirstFrame(){renderedFrame=true}
            override fun onPlayerError(e:PlaybackException){error=when(e.errorCode){
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT->"视频网络连接失败，请检查网络后重试"
                PlaybackException.ERROR_CODE_DECODING_FAILED,PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,PlaybackException.ERROR_CODE_DECODER_INIT_FAILED->"当前设备无法解码此片源，请选择其他影片"
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND->"视频资源暂不可用或已过期，请重新加载"
                else->"视频暂时无法播放，可重新加载或返回选择其他影片"
            };buffering=false}
        }
        val analytics=object:androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onVideoInputFormatChanged(eventTime:androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,format:Format,decoderReuseEvaluation:androidx.media3.exoplayer.DecoderReuseEvaluation?){
                trackInfo=videoTrackInfo(format.width,format.height,format.averageBitrate,format.peakBitrate)
            }
        }
        player.addAnalyticsListener(analytics)
        player.addListener(listener)
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP){saveProgress();player.pause()}}
        owner.lifecycle.addObserver(observer)
        onDispose{saveProgress();if(vm.pendingFavorite?.movieId==movie.id)vm.pendingFavorite=null;owner.lifecycle.removeObserver(observer);player.removeListener(listener);player.removeAnalyticsListener(analytics);session.release();player.release()}
    }
    LaunchedEffect(movie.id,retry) {
        saveProgress();player.stop();trackInfo=videoTrackInfo(-1,-1,-1,-1);detail=null;error=null;buffering=true;loginRequired=false
        try { val d=vm.api.detail(movie.id);val start=playbackStart(d,resume)
            detail=d;favorite=d.favorite;episode=start.arrayIndex;nextStart=start.position
            if(start.missingEpisode)note="原观看集数已不可用，从第一集开始"
            if(d.episodes.isEmpty()){error="该影片暂无可用播放源";buffering=false}
        }
        catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e);buffering=false;loginRequired=e is ApiException&&e.code in setOf(12,13,14,16)}
    }
    LaunchedEffect(detail,vm.sessionVersion){if(detail!=null){
        val intent=vm.consumeFavoriteIntent(movie.id);if(intent!=null){
            if(intent.desired!=favorite)setFavorite(intent.desired)
        }
    }}
    LaunchedEffect(detail,episode,playRetry) {
        val d=detail?:return@LaunchedEffect
        val item=d.episodes.getOrNull(episode)?:return@LaunchedEffect
        if(!item.uri.startsWith("https://")){error="此片源暂不支持原生播放";buffering=false;return@LaunchedEffect}
        // Auto-next must not replace the episode row the user is currently navigating.
        if(!episodeAreaFocused)episodeGroup=episode.coerceAtLeast(0)/10
        trackInfo=videoTrackInfo(-1,-1,-1,-1)
        renderedFrame=false
        player.setMediaItem(MediaItem.Builder().setMediaId(item.index.toString()).setUri(item.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(d.movie.title).build()).build())
        val start=nextStart;nextStart=0
        if(start>0)player.seekTo(start)
        player.setPlaybackSpeed(speed)

        error=null;player.prepare();if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))player.play();ended=false
    }
    LaunchedEffect(player){while(true){
        position=player.currentPosition.coerceAtLeast(0);duration=player.duration.takeIf{it!=C.TIME_UNSET&&it>0}?:0
        seekable=player.isCurrentMediaItemSeekable
        delay(500)
    }}
    LaunchedEffect(player){while(true){delay(10000);if(player.isPlaying&&owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))saveProgress(false)}}
    LaunchedEffect(ended){if(ended&&episode+1<(detail?.episodes?.size?:0)){saveProgress();nextStart=0;episode++}}
    val shownMovie=detail?.movie?:movie
    val authentication=vm.pendingAuthentication?.takeIf{it.movieId==movie.id}
    PlayerContent(PlayerUiState(shownMovie,detail,episode,episodeGroup,playing,buffering,ended,position,duration,speed,seekable,favorite,favoriteBusy,trackInfo,error,note,playRequested,loginRequired||authentication!=null,renderedFrame),full,
        PlayerActions(back=onBack,toggleFull=toggleFull,togglePlay={if(player.playWhenReady)player.pause()else player.play()},
            seek={delta->if(player.isCurrentMediaItemSeekable)player.seekTo(jumpPosition(player.currentPosition,player.duration,delta))},
            setSpeed={speed=it;player.setPlaybackSpeed(it)},favorite={if(authentication!=null)login(authentication.desired)else setFavorite(!favorite)},chooseGroup={episodeGroup=it},playEpisode={ep->val target=detail?.episodes?.indexOf(ep)?:-1;if(target>=0&&target!=episode){saveProgress();nextStart=0;episode=target}},
            retry={if(detail==null)retry++ else{nextStart=player.currentPosition.coerceAtLeast(0);playRetry++}},episodeFocus={episodeAreaFocused=it},login={login(authentication?.desired?:loginDesired)})){
        AndroidView(factory={PlayerView(it).apply{this.player=player;useController=false;isFocusable=false;keepScreenOn=true}},modifier=Modifier.fillMaxSize(),update={it.player=player;it.keepScreenOn=playing})
    }
}
internal fun clock(ms:Long):String {val seconds=ms/1000;return if(seconds>=3600)"%d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60)else "%02d:%02d".format(seconds/60,seconds%60)}

internal fun jumpPosition(position:Long,duration:Long,delta:Long):Long=(position+delta).coerceAtLeast(0).coerceAtMost(duration.takeIf{it>0}?:Long.MAX_VALUE)
