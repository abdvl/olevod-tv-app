@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.olevod.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.olevod.tv.data.WatchRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConnectedHome(state:HomeState,vm:AppViewModel,open:(Movie)->Unit,history:()->Unit,
                  navigationHomeFocus:FocusRequester,recentFocus:FocusRequester,setRecentEntry:((()->Unit)?)->Unit,
                  fixtureRecords:List<WatchRecord>?=null,browse:(String)->Unit) {
    val list=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val stored by vm.history.records.collectAsStateWithLifecycle()
    val recent=(fixtureRecords ?: stored).take(5)
    val metadata=rememberHistoryMetadata(vm)
    val heroEntry=remember{FocusRequester()}
    val heroRight=remember{FocusRequester()}
    val historyEntry=remember{FocusRequester()}
    var lastRecent by rememberSaveable { mutableLongStateOf(-1) }
    val entryId=recent.firstOrNull{it.movie.id==lastRecent}?.movie?.id ?: recent.firstOrNull()?.movie?.id
    val orderedSections=listOf(1,2,3,6,14).mapNotNull{id->state.sections.firstOrNull{it.category.id==id}}
    val fallback=if(state.heroes.isEmpty()&&!state.loading)orderedSections.flatMap{it.movies}.distinctBy{it.id}.take(2)else emptyList()
    val hasRecommendation=state.heroes.isNotEmpty()||fallback.isNotEmpty()
    DisposableEffect(list,entryId,hasRecommendation) {
        setRecentEntry { scope.launch {
            list.scrollToItem(if(recent.isEmpty() && hasRecommendation)1 else 0)
            withFrameNanos{}
            when {recent.isNotEmpty()->recentFocus.requestFocus();hasRecommendation->heroEntry.requestFocus();else->historyEntry.requestFocus()}
        } }
        onDispose{setRecentEntry(null)}
    }
    LazyColumn(Modifier.fillMaxSize().testTag("home-content"),state=list,
        contentPadding=PaddingValues(36.dp,4.dp,36.dp,64.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item(key="recent") {
            PosterFocusGroup("recent:${vm.sessionVersion}") {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    SectionHeading("最近播放")
                    if(recent.isEmpty()) Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("还没有观看记录，先发现一个好故事",color=Muted,fontSize=13.sp,modifier=Modifier.weight(1f))
                        TvAction("全部历史",Icons.Rounded.History,modifier=Modifier.focusRequester(historyEntry).focusProperties{up=navigationHomeFocus},onClick=history)
                    } else BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val tileWidth=(maxWidth-90.dp)/6
                        val fontScale=LocalDensity.current.fontScale
                        val posterHeight=maxOf(tileWidth*1.5f,(199.5.dp*fontScale))
                        var rowCenter by remember{mutableFloatStateOf(0f)}
                        var within by remember{mutableStateOf(false)}
                        val scroll=rememberScrollState()
                        LaunchedEffect(within){if(!within)scroll.scrollTo(0)}
                        Row(Modifier.fillMaxWidth().onGloballyPositioned{rowCenter=it.positionInRoot().x+it.size.width/2f}.onFocusChanged{within=it.hasFocus}.focusGroup()
                            .horizontalScroll(scroll).padding(vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                            recent.forEach { record -> key(record.movie.id) {
                                var movie by remember(record.movie,metadata){mutableStateOf(record.movie)}
                                LaunchedEffect(record.movie,metadata){if(fixtureRecords==null)try{movie=metadata.complete(record.movie)}catch(e:Exception){if(e is CancellationException)throw e}}
                                var cardCenter by remember{mutableFloatStateOf(0f)}
                                val expanded=within && lastRecent==movie.id
                                val width by animateDpAsState(if(expanded)396.dp else tileWidth,tween(180),label="recent-width")
                                RecentTile(record.copy(movie=movie),Modifier.width(width).onGloballyPositioned{cardCenter=it.positionInRoot().x+it.size.width/2f}
                                    .then(if(movie.id==entryId)Modifier.focusRequester(recentFocus)else Modifier)
                                    .focusProperties{up=navigationHomeFocus;if(hasRecommendation)down=if(cardCenter>rowCenter&&(state.heroes.size+fallback.size)>1)heroRight else heroEntry},
                                    tileWidth,posterHeight,expanded,{lastRecent=movie.id}) {
                                    vm.pendingResume=record.copy(movie=movie);open(movie)
                                }
                            } }
                            var historyCenter by remember{mutableFloatStateOf(0f)}
                            HistoryShortcut(Modifier.onGloballyPositioned{historyCenter=it.positionInRoot().x+it.size.width/2f}.width(tileWidth).height(posterHeight+with(LocalDensity.current){22.sp.toDp()}).focusRequester(historyEntry)
                                .focusProperties{up=navigationHomeFocus;if(hasRecommendation)down=if(historyCenter>rowCenter&&(state.heroes.size+fallback.size)>1)heroRight else heroEntry},posterHeight,history)
                        }
                    }
                }
            }
        }
        item(key="recommendations") {
            PosterFocusGroup("recommendations") {
                Column(Modifier.testTag("recommendations"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(state.heroes.isNotEmpty()) {
                        SectionHeading("精选推荐")
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            state.heroes.take(2).forEachIndexed { index,hero ->
                                HeroCard(hero,Modifier.weight(1f).focusRequester(if(index==0)heroEntry else heroRight)
                                    .focusProperties{up=if(recent.isNotEmpty())recentFocus else navigationHomeFocus}) {
                                    open(Movie(hero.id,hero.title,hero.image,hero.note))
                                }
                            }
                            if(state.heroes.size==1)Spacer(Modifier.weight(1f))
                        }
                    } else if(fallback.isNotEmpty()){
                        SectionHeading("最近更新")
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
                            fallback.forEachIndexed{index,movie->FallbackRecommendation(movie,Modifier.weight(1f).focusRequester(if(index==0)heroEntry else heroRight).focusProperties{up=if(recent.isNotEmpty())recentFocus else navigationHomeFocus}){open(movie)}}
                            if(fallback.size==1)Spacer(Modifier.weight(1f))
                        }
                    } else if(state.loading)Text("正在加载精选推荐…",color=Muted,fontSize=13.sp)
                    state.error?.let{ErrorNotice(it){vm.loadHome()}}
                }
            }
        }
        items(orderedSections,key={it.category.id}) { section ->
            val title=categoryLabel(section.category.id)
            when {
                section.error!=null->Column{SectionHeading(title);ErrorNotice(section.error){vm.retrySection(section.category)}}
                section.loading->Column{SectionHeading(title);Text("正在加载…",color=Muted)}
                section.movies.isEmpty()->Column{SectionHeading(title);Text("暂时没有影片",color=Muted)}
                else->HomeMovieGroup(section.movies.take(10),open,title){browse(section.category.name)}
            }
        }
    }
}

@Composable
private fun RecentTile(record:WatchRecord,modifier:Modifier,posterWidth:androidx.compose.ui.unit.Dp,
                       posterHeight:androidx.compose.ui.unit.Dp,expanded:Boolean,onFocused:()->Unit,onClick:()->Unit) {
    val source=remember{MutableInteractionSource()}
    val focused by source.collectIsFocusedAsState()
    val bring=remember{BringIntoViewRequester()}
    LaunchedEffect(focused){if(focused){withFrameNanos{};bring.bringIntoView();delay(200);bring.bringIntoView()}}
    Column(modifier.height(posterHeight+with(LocalDensity.current){22.sp.toDp()}).bringIntoViewRequester(bring).restoreContentFocus("${record.movie.id}:resume")
        .onFocusChanged{if(it.isFocused)onFocused()}.testTag("recent:${record.movie.id}")
        .semantics(mergeDescendants=true){contentDescription="${record.movie.title}，${resumeLabel(record)}，${resumeActionLabel(record)}"}
        .border(2.dp,if(focused)Green else Color.Transparent,RoundedCornerShape(8.dp))
        .clickable(interactionSource=source,indication=null,onClick=onClick)) {
        Row(Modifier.height(posterHeight),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.width(posterWidth).fillMaxHeight()) {
                PosterArtwork(record.movie,Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.25f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.9f)))))
                Text(resumeLabel(record),color=White,fontSize=13.sp,lineHeight=18.sp,maxLines=1,
                    modifier=Modifier.align(Alignment.BottomCenter).padding(horizontal=4.dp,vertical=8.dp))
            }
            if(expanded)Column(Modifier.weight(1f).padding(top=8.dp,end=14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(record.movie.title,color=White,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(listOf(record.movie.year,record.movie.area,record.movie.score.takeIf(String::isNotBlank)?.let{"评分 $it"}.orEmpty())
                    .filter(String::isNotBlank).joinToString(" · "),color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text((if(record.episode>0)"第 ${record.episode} 集 · "else"")+"已看 ${clock(record.positionMs)}"+(if(record.durationMs>0)" / ${clock(record.durationMs)}"else""),color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=2)
                WatchProgress(record)
                Row(Modifier.background(Green,RoundedCornerShape(8.dp)).padding(horizontal=14.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.PlayArrow,null,Modifier.size(22.dp),tint=Bg);Text(resumeActionLabel(record),color=Bg,fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold)}
            }
        }
        Text(if(expanded)""else record.movie.title,color=White,fontSize=14.sp,lineHeight=22.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=3.dp))
    }
}

@Composable
private fun HistoryShortcut(modifier:Modifier,height:androidx.compose.ui.unit.Dp,onClick:()->Unit) {
    val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState()
    Column(modifier.restoreContentFocus("all-history").testTag("all-history")
        .clickable(interactionSource=source,indication=null,onClick=onClick)) {
        Column(Modifier.fillMaxWidth().height(height).border(if(focused)2.dp else 1.dp,if(focused)Green else TvDesign.border,RoundedCornerShape(8.dp))
            .background(Panel,RoundedCornerShape(8.dp)),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Icon(Icons.Rounded.History,null,Modifier.size(42.dp),tint=if(focused)Green else White)
            Spacer(Modifier.height(16.dp));Text("全部历史",color=White,fontSize=18.sp,fontWeight=FontWeight.Medium)
            Spacer(Modifier.height(6.dp));Text("查看观看记录",color=Muted,fontSize=13.sp)
        }
    }
}
internal fun resumeLabel(record:WatchRecord):String = if(record.episode>0)"第${record.episode}集 · ${clock(record.positionMs)}"else"已看${clock(record.positionMs)}"
internal fun resumeActionLabel(record:WatchRecord):String=if(record.durationMs>0&&record.positionMs>=record.durationMs-10000)"重新播放"else"继续播放"
@Composable internal fun WatchProgress(record:WatchRecord,modifier:Modifier=Modifier) {
    if(record.durationMs>0)Box(modifier.fillMaxWidth().height(3.dp).background(Muted.copy(alpha=.25f))) {
        Box(Modifier.fillMaxWidth((record.positionMs.toDouble()/record.durationMs).coerceIn(0.0,1.0).toFloat()).fillMaxHeight().background(Green))
    }
}
@Composable internal fun ErrorNotice(message:String,retry:()->Unit){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(message,color=TvDesign.error,fontSize=15.sp);TvAction("重试",onClick=retry)}}

@Composable
private fun FallbackRecommendation(movie:Movie,modifier:Modifier,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState()
    Row(modifier.height(138.dp*maxOf(1f,LocalDensity.current.fontScale)).restoreContentFocus("fallback:${movie.id}")
        .background(Panel,RoundedCornerShape(10.dp)).border(2.dp,if(focused)Green else Color.Transparent,RoundedCornerShape(10.dp))
        .clickable(interactionSource=source,indication=null,onClick=onClick).padding(8.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)){
        PosterArtwork(movie,Modifier.width(81.dp).fillMaxHeight())
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(movie.title,color=White,fontSize=22.sp,lineHeight=28.sp,maxLines=2,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Bold)
            Text(listOf(movie.year,movie.area,movie.note).filter(String::isNotBlank).joinToString(" · "),color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}
