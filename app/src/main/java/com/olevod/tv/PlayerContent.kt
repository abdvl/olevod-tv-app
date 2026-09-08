package com.olevod.tv

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.olevod.tv.data.Detail
import com.olevod.tv.data.Episode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class PlayerUiState(val movie:Movie,val detail:Detail?=null,val episode:Int=-1,val group:Int=0,
    val playing:Boolean=false,val buffering:Boolean=false,val ended:Boolean=false,val position:Long=0,val duration:Long=0,
    val speed:Float=1f,val seekable:Boolean=false,val favorite:Boolean=false,val favoriteBusy:Boolean=false,
    val trackInfo:VideoTrackInfo=videoTrackInfo(-1,-1,-1,-1),val error:String?=null,val note:String="",val playRequested:Boolean=playing,val loginRequired:Boolean=false,val renderedFrame:Boolean=false)
internal data class PlayerActions(val back:()->Unit,val toggleFull:()->Unit,val togglePlay:()->Unit,val seek:(Long)->Unit,
    val setSpeed:(Float)->Unit,val favorite:()->Unit,val chooseGroup:(Int)->Unit,val playEpisode:(Episode)->Unit,
    val retry:()->Unit,val episodeFocus:(Boolean)->Unit,val login:()->Unit={})

@Composable
internal fun PlayerContent(state:PlayerUiState,full:Boolean,actions:PlayerActions,video:@Composable ()->Unit){
    val refs=remember{List(8){FocusRequester()}}
    val surface=remember{FocusRequester()};val stage=remember{FocusRequester()};val description=remember{FocusRequester()};val groups=remember{FocusRequester()}
    var stageFocused by remember{mutableStateOf(false)}
    var controls by remember{mutableStateOf(true)}
    var speedMenu by remember{mutableStateOf(false)}
    var descriptionMenu by remember{mutableStateOf(false)}
    var episodeFocused by remember{mutableStateOf(false)}
    var lastAction by remember{mutableIntStateOf(0)}
    var tick by remember{mutableIntStateOf(0)}
    var restoreSpeed by remember{mutableIntStateOf(0)}
    var restoreDescription by remember{mutableIntStateOf(0)}
    val page=LocalPageFocus.current
    val accessibility=LocalContext.current.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
    var exploring by remember{mutableStateOf(accessibility.isTouchExplorationEnabled)}
    DisposableEffect(accessibility){val listener=android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener{exploring=it};accessibility.addTouchExplorationStateChangeListener(listener);onDispose{accessibility.removeTouchExplorationStateChangeListener(listener)}}
    DisposableEffect(page,full){page?.forceEntry=true;page?.enter={stage.requestFocus()};onDispose{page?.forceEntry=false;page?.enter=null}}
    LaunchedEffect(full){controls=true}
    LaunchedEffect(full,controls){withFrameNanos{};if(full&&!controls)surface.requestFocus()else refs[0].requestFocus()}
    LaunchedEffect(full,controls,state.playing,state.buffering,state.error,speedMenu,descriptionMenu,episodeFocused,exploring,tick){
        if(full&&controls&&state.playing&&!state.buffering&&state.error==null&&!speedMenu&&!descriptionMenu&&!episodeFocused&&!exploring){delay(5000);controls=false}
    }
    LaunchedEffect(restoreSpeed){if(restoreSpeed>0){withFrameNanos{};refs[6].requestFocus()}}
    LaunchedEffect(restoreDescription){if(restoreDescription>0){withFrameNanos{};description.requestFocus()}}
    BackHandler{when{speedMenu->{speedMenu=false;restoreSpeed++};descriptionMenu->{descriptionMenu=false;restoreDescription++};else->actions.back()}}
    val episodes=state.detail?.episodes.orEmpty()
    val actualEpisode=episodes.getOrNull(state.episode)
    val enabled=List(8){it !in 2..5||state.seekable}
    val mediaKeys=setOf(AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD,AndroidKeyEvent.KEYCODE_MEDIA_REWIND)
    Row(Modifier.fillMaxSize().background(Bg).playerKeyInput(surface,full&&!controls){event->
        val key=event.nativeKeyEvent.keyCode
        if(key in mediaKeys){if(event.type==KeyEventType.KeyDown&&event.nativeKeyEvent.repeatCount==0){tick++;when(key){AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE->actions.togglePlay();AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD->if(state.seekable)actions.seek(30000);else->if(state.seekable)actions.seek(-30000)}};true}
        else if(event.type!=KeyEventType.KeyDown)false
        else{tick++;when{
            full&&!controls->{when(key){AndroidKeyEvent.KEYCODE_DPAD_DOWN,AndroidKeyEvent.KEYCODE_DPAD_CENTER,AndroidKeyEvent.KEYCODE_ENTER->controls=true;AndroidKeyEvent.KEYCODE_DPAD_LEFT->if(state.seekable)actions.seek(-30000);AndroidKeyEvent.KEYCODE_DPAD_RIGHT->if(state.seekable)actions.seek(30000)};key in setOf(19,20,21,22,23,66)}
            full&&key==AndroidKeyEvent.KEYCODE_DPAD_UP&&!episodeFocused->{controls=false;true}
            else->false
        }}
    }.padding(if(full)0.dp else 36.dp,if(full)0.dp else 4.dp,if(full)0.dp else 36.dp,if(full)0.dp else 24.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)){
        PlayerVideoStage(full,controls,Modifier.weight(1f),
            videoModifier=(if(full)Modifier.testTag("player-video")else Modifier.focusRequester(stage).testTag("player-video")
                .focusProperties{down=refs[0];up=page?.header?:FocusRequester.Default;right=if(state.detail!=null)description else FocusRequester.Cancel}
                .onFocusChanged{stageFocused=it.isFocused}.border(2.dp,if(stageFocused)Green else Color.Transparent)
                .semantics{contentDescription="视频画面，按确认键全屏"}.clickable(onClick=actions.toggleFull)).semantics{stateDescription=if(state.renderedFrame)"视频已开始显示"else"等待视频画面"},
            video={
                video()
                if(state.buffering)Text("正在缓冲…",color=White,fontSize=14.sp,modifier=Modifier.background(Bg).padding(12.dp))
                state.error?.let{Column(Modifier.background(Bg).padding(16.dp)){ErrorNotice(it,actions.retry);if(state.loginRequired)TvAction("登录后继续",onClick=actions.login)}}
            },controlContent={
                if(full)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(state.movie.title+actualEpisode?.let{" · ${it.title}"}.orEmpty(),color=White,fontSize=18.sp,lineHeight=24.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                    Text("上键隐藏 · 返回退出全屏",color=Muted,fontSize=13.sp,lineHeight=18.sp)
                }
                Column(Modifier.fillMaxWidth().height(24.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
                    Box(Modifier.fillMaxWidth().height(3.dp).background(TvDesign.border)){Box(Modifier.fillMaxWidth(if(state.duration>0)(state.position.toFloat()/state.duration).coerceIn(0f,1f)else 0f).fillMaxHeight().background(Green))}
                    Row(Modifier.fillMaxWidth()){Text(clock(state.position),color=Muted,fontSize=13.sp,lineHeight=18.sp);Spacer(Modifier.weight(1f));Text(if(state.duration>0)clock(state.duration)else"--:--",color=Muted,fontSize=13.sp,lineHeight=18.sp)}
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    val labels=listOf(if(full)"退出全屏"else"全屏",if(state.playRequested)"暂停"else"播放","−30秒","+30秒","−5分钟","+5分钟","速度",if(state.favoriteBusy)"处理中"else if(state.favorite)"已收藏"else"收藏")
                    val icons=listOf(Icons.Rounded.Fullscreen,if(state.playRequested)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,Icons.Rounded.Replay30,Icons.Rounded.Forward30,Icons.Rounded.FastRewind,Icons.Rounded.FastForward,null,if(state.favorite)Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder)
                    Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        labels.forEachIndexed{index,label->PlayerAction(label,icons[index],if(index==6)"${state.speed}×"else null,enabled[index],Modifier.weight(1f).focusRequester(refs[index]).testTag("player-action:$index")
                            .onFocusChanged{if(it.isFocused)lastAction=index}.focusProperties{
                                up=if(full)FocusRequester.Cancel else stage;down=if(episodes.isNotEmpty())groups else FocusRequester.Cancel
                                left=(index-1 downTo 0).firstOrNull{enabled[it]}?.let{refs[it]}?:FocusRequester.Cancel
                                right=(index+1..7).firstOrNull{enabled[it]}?.let{refs[it]}?:FocusRequester.Cancel
                            }){when(index){0->actions.toggleFull();1->actions.togglePlay();2->actions.seek(-30000);3->actions.seek(30000);4->actions.seek(-300000);5->actions.seek(300000);6->speedMenu=true;7->if(!state.favoriteBusy)actions.favorite()}}
                        }
                    }
                    if(full)Column(Modifier.width(140.dp),horizontalAlignment=Alignment.End){Text(state.trackInfo.resolution,color=White,fontSize=13.sp,lineHeight=18.sp,maxLines=1);Text(state.trackInfo.bitrate,color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=1)}
                }
                if(episodes.isNotEmpty())EpisodePicker(episodes,actualEpisode?.index?:-1,state.group,actions.chooseGroup,actions.playEpisode,refs[lastAction],groups){episodeFocused=it;actions.episodeFocus(it)}
                if(full&&state.note.isNotBlank())Text(state.note,color=TvDesign.warning,fontSize=13.sp,lineHeight=18.sp,maxLines=1)
            })
        if(!full)Column(Modifier.width(264.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text(state.movie.title,color=White,fontSize=24.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(listOf(state.movie.score.takeIf(String::isNotBlank)?.let{"评分 $it"}.orEmpty(),state.movie.year,state.movie.area,categoryLabel(state.movie.category)).filter(String::isNotBlank).joinToString(" · "),color=Green,fontSize=13.sp,lineHeight=18.sp)
            actualEpisode?.let{Text("正在播放：${it.title}",color=White,fontSize=14.sp,lineHeight=19.sp)}
            if(state.movie.note.isNotBlank())Text(state.movie.note,color=Muted,fontSize=13.sp,lineHeight=18.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(TvDesign.border))
            Text("剧情简介",color=White,fontSize=16.sp,lineHeight=22.sp)
            Text(state.detail?.description?:"正在加载影片信息…",color=Muted,fontSize=14.sp,lineHeight=22.sp,maxLines=4,overflow=TextOverflow.Ellipsis)
            if(state.detail!=null)TvAction("展开简介",Icons.Rounded.ExpandMore,modifier=Modifier.focusRequester(description).testTag("player-description").focusProperties{left=stage;up=page?.header?:FocusRequester.Default;down=refs[0];right=FocusRequester.Cancel}){descriptionMenu=true}
            state.detail?.let{d->if(d.director.isNotBlank())Text("导演：${d.director}",color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis);if(d.actor.isNotBlank())Text("主演：${d.actor}",color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}
            if(!state.seekable)Text("此片源暂不支持跳转",color=Muted,fontSize=13.sp,lineHeight=18.sp)
            if(state.ended&&state.episode==episodes.lastIndex)Text("已播放完",color=Green,fontSize=14.sp)
            if(state.note.isNotBlank())Text(state.note,color=TvDesign.warning,fontSize=13.sp,lineHeight=18.sp)
            if(state.loginRequired&&state.error==null)TvAction("重新登录",onClick=actions.login)
        }
    }
    if(speedMenu)OptionPopover("播放速度",listOf(.5f,.75f,1f,1.25f,1.5f,1.75f,2f).map{FilterOption(it.toString(),"${it}×")},state.speed.toString(),onDismiss={speedMenu=false;restoreSpeed++}){actions.setSpeed(it.toFloat());speedMenu=false;restoreSpeed++}
    if(descriptionMenu)Dialog(onDismissRequest={descriptionMenu=false;restoreDescription++},properties=DialogProperties(usePlatformDefaultWidth=false)){
        val close=remember{FocusRequester()}
        val button=remember{FocusRequester()};val scroll=rememberScrollState();val scope=rememberCoroutineScope()
        Column(Modifier.width(600.dp).heightIn(max=430.dp).background(Panel,RoundedCornerShape(14.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text(state.movie.title,color=White,fontSize=22.sp,lineHeight=28.sp)
            Column(Modifier.weight(1f,false).verticalScroll(scroll).focusRequester(close).focusProperties{down=button;up=FocusRequester.Cancel}
                .semantics{contentDescription="完整简介，上下键滚动阅读"}.onPreviewKeyEvent{event->
                    val delta=when(event.key){Key.DirectionDown->120;Key.DirectionUp->-120;else->0}
                    if(delta==0)false else if(delta>0&&scroll.value>=scroll.maxValue)false else{
                        if(event.type==KeyEventType.KeyDown)scope.launch{scroll.scrollTo((scroll.value+delta).coerceIn(0,scroll.maxValue))};true
                    }
                }.focusable(),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text(state.detail?.description.orEmpty(),color=White,fontSize=16.sp,lineHeight=24.sp)
                state.detail?.let{Text("导演：${it.director}\n主演：${it.actor}",color=Muted,fontSize=14.sp,lineHeight=22.sp)}
            }
            TvAction("返回播放",modifier=Modifier.focusRequester(button).focusProperties{up=close},onClick={descriptionMenu=false;restoreDescription++})
        }
        LaunchedEffect(Unit){withFrameNanos{};close.requestFocus()}
    }
}

@Composable
internal fun PlayerAction(label:String,icon:ImageVector?,value:String?,enabled:Boolean,modifier:Modifier,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState()
    Column(modifier.height(48.dp*maxOf(1f,androidx.compose.ui.platform.LocalDensity.current.fontScale)).background(if(focused)Green else Panel,RoundedCornerShape(8.dp)).border(1.dp,if(focused)Green else TvDesign.border,RoundedCornerShape(8.dp))
        .semantics{contentDescription=label+(value?.let{" $it"}.orEmpty());if(!enabled)disabled()}
        .clickable(enabled=enabled,interactionSource=source,indication=null,onClick=onClick),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        val tint=if(!enabled)Muted.copy(alpha=.4f)else if(focused)Bg else White
        if(icon!=null)Icon(icon,null,Modifier.size(22.dp),tint=tint)else Text(value.orEmpty(),color=tint,fontSize=16.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold)
        Text(label,color=tint,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
