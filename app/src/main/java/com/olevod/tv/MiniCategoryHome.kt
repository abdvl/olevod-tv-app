@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.olevod.tv

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.olevod.tv.data.Category
import com.olevod.tv.data.Filter
import kotlinx.coroutines.launch
import java.time.Year

@Composable
fun MiniCategoryHome(category:Category,vm:AppViewModel,open:(Movie)->Unit,browse:()->Unit,
                     navigationFocus:FocusRequester,setEntry:((()->Unit)?)->Unit,
                     currentYear:String=Year.now().value.toString(),fixture:Pair<List<Movie>,List<Movie>>?=null){
    val rankingScope=miniRankingScope(category.id,currentYear)
    val year=rankingScope.yearFilter
    val hot=remember(category.id,year,vm.sessionVersion,fixture){if(fixture==null)vm.catalogFeed(Filter(category=category.id,year=year,sort="hot"))else null}
    val score=remember(category.id,year,vm.sessionVersion,fixture){if(fixture==null)vm.catalogFeed(Filter(category=category.id,year=year,sort="score"))else null}
    DisposableEffect(hot,score){onDispose{hot?.cancel();score?.cancel()}}
    LaunchedEffect(hot){if(hot?.state?.nextPage==1&&hot.state.error==null)hot.loadNext()}
    LaunchedEffect(score){if(score?.state?.nextPage==1&&score.state.error==null)score.loadNext()}
    val hotState=hot?.state ?: CatalogFeedState(items=fixture?.first.orEmpty(),nextPage=2,endReached=true)
    val scoreState=score?.state ?: CatalogFeedState(items=fixture?.second.orEmpty(),nextPage=2,endReached=true)
    val list=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val hotFocus=remember{FocusRequester()};val scoreFocus=remember{FocusRequester()};val allFocus=remember{FocusRequester()}
    val hotReady=hotState.items.isNotEmpty()||hotState.error!=null
    val scoreReady=scoreState.items.isNotEmpty()||scoreState.error!=null
    DisposableEffect(list,hotReady,scoreReady){
        setEntry{scope.launch{
            val index=if(hotReady)0 else if(scoreReady)1 else 2
            list.scrollToItem(index)
            withFrameNanos { }
            (if(index==0)hotFocus else if(index==1)scoreFocus else allFocus).requestFocus()
        }}
        onDispose{setEntry(null)}
    }
    LazyColumn(Modifier.fillMaxSize().testTag("mini-home"),state=list,contentPadding=PaddingValues(36.dp,8.dp,36.dp,64.dp),verticalArrangement=Arrangement.spacedBy(24.dp)){
        item("hot"){
            MiniRanking(category.id,"hot","${categoryLabel(category.id)} · ${rankingScope.label} 人气最高",hotState,hotFocus,navigationFocus,open){hot?.loadNext()}
        }
        item("score"){
            MiniRanking(category.id,"score","${rankingScope.label} 评分最高",scoreState,scoreFocus,if(!hotReady)navigationFocus else null,open){score?.loadNext()}
        }
        item("browse-all"){
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                TvAction("浏览全部${categoryLabel(category.id)}",modifier=Modifier.fillMaxWidth().focusRequester(allFocus)
                    .restoreContentFocus("browse-all").testTag("mini-browse-all"),onClick=browse)
                Text("浏览所有年份，按最近更新排序",color=Muted,fontSize=13.sp,lineHeight=18.sp,modifier=Modifier.padding(horizontal=14.dp))
            }
        }
    }
}

@Composable
private fun MiniRanking(categoryId:Int,sort:String,title:String,state:CatalogFeedState,entry:FocusRequester,
                        up:FocusRequester?,open:(Movie)->Unit,retry:()->Unit){
    val movies=state.items.take(12)
    PosterFocusGroup("mini:$categoryId:$sort"){
        Column(Modifier.focusRequester(entry).focusGroup(),verticalArrangement=Arrangement.spacedBy(14.dp)){
            SectionHeading(title,if(movies.isEmpty())""else"前 ${movies.size} 部")
            when {
                movies.isNotEmpty()->{
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
                        movies.take(2).forEachIndexed{index,movie->
                            key(movie.id){ RankingFeature(movie,index+1,sort,Modifier.weight(1f)
                                .focusProperties{if(up!=null)this.up=up}){open(movie)} }
                        }
                        if(movies.size==1)Spacer(Modifier.weight(1f))
                    }
                    movies.drop(2).chunked(5).forEachIndexed{rowIndex,row->
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
                            row.forEachIndexed{column,movie->key(movie.id){PosterCard(movie,Modifier.weight(1f),subtitle="第 ${rowIndex*5+column+3} 名 · "+listOf(movie.year,movie.area,movie.note).filter(String::isNotBlank).joinToString(" · ")){open(movie)}}}
                            repeat(5-row.size){Spacer(Modifier.weight(1f))}
                        }
                    }
                }
                state.error!=null->ErrorNotice(state.error,retry)
                state.loading||state.nextPage==1->Text("正在加载榜单…",color=Muted)
                else->Text(if(categoryId==6)"暂无相关影片"else"今年暂无相关影片",color=Muted)
            }
        }
    }
}

@Composable
internal fun RankingFeature(movie:Movie,rank:Int,sort:String,modifier:Modifier=Modifier,onClick:()->Unit){
    val interaction=remember{MutableInteractionSource()};val focused by interaction.collectIsFocusedAsState()
    val bring=remember{BringIntoViewRequester()}
    val height=202.dp*maxOf(1f,LocalDensity.current.fontScale)
    LaunchedEffect(focused){if(focused){withFrameNanos{};bring.bringIntoView()}}
    val context=LocalContext.current
    val backdrop=remember(context,movie.image){ImageRequest.Builder(context).data(movie.image)
        .size(160,160).allowHardware(false).transformations(RankingBackdropBlur).build()}
    val shape=RoundedCornerShape(10.dp)
    Box(modifier.height(height).bringIntoViewRequester(bring).restoreContentFocus("rank:$rank:${movie.id}")
        .testTag("ranking:$sort:$rank").semantics(mergeDescendants=true){contentDescription="第 $rank 名，${movie.title}"}
        .clip(shape).background(Panel).border(2.dp,if(focused)Green else Color.Transparent,shape)
        .clickable(interactionSource=interaction,indication=null,onClick=onClick)) {
        AsyncImage(backdrop,null,Modifier.matchParentSize(),contentScale=ContentScale.FillBounds)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to Bg.copy(alpha=.14f),.30f to Bg.copy(alpha=.72f),1f to Bg.copy(alpha=.82f))))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
            Color.Transparent,Bg.copy(alpha=.30f)))))
        Row(Modifier.fillMaxSize().padding(6.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            PosterArtwork(movie,Modifier.width(126.dp).fillMaxHeight())
            Column(Modifier.weight(1f).padding(top=6.dp,end=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("TOP $rank",color=Green,fontSize=18.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold)
                Text(movie.title,color=White,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(listOf(movie.year,movie.area).filter(String::isNotBlank).joinToString(" · "),color=White.copy(alpha=.80f),fontSize=13.sp,lineHeight=18.sp,maxLines=2)
                if(movie.note.isNotBlank())Text(movie.note,color=White,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
