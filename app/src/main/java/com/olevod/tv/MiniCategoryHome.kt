package com.olevod.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.olevod.tv.data.Category
import com.olevod.tv.data.Filter
import kotlinx.coroutines.launch
import java.time.Year

@Composable
fun MiniCategoryHome(category:Category,vm:AppViewModel,open:(Movie)->Unit,browse:()->Unit,
                     navigationFocus:FocusRequester,setEntry:((()->Unit)?)->Unit){
    val year=remember{Year.now().value.toString()}
    val hot=remember(category.id,year){vm.catalogFeed(Filter(category=category.id,year=year,sort="hot"))}
    val score=remember(category.id,year){vm.catalogFeed(Filter(category=category.id,year=year,sort="score"))}
    LaunchedEffect(hot){if(hot.state.nextPage==1&&hot.state.error==null)hot.loadNext()}
    LaunchedEffect(score){if(score.state.nextPage==1&&score.state.error==null)score.loadNext()}
    val list=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val hotFocus=remember{FocusRequester()};val scoreFocus=remember{FocusRequester()};val allFocus=remember{FocusRequester()}
    val hotReady=hot.state.items.isNotEmpty()||hot.state.error!=null
    val scoreReady=score.state.items.isNotEmpty()||score.state.error!=null
    DisposableEffect(list,hotReady,scoreReady){
        setEntry{scope.launch{
            val index=if(hotReady)0 else if(scoreReady)1 else 2
            list.scrollToItem(index)
            withFrameNanos { }
            (if(index==0)hotFocus else if(index==1)scoreFocus else allFocus).requestFocus()
        }}
        onDispose{setEntry(null)}
    }
    LazyColumn(Modifier.fillMaxSize(),state=list,contentPadding=PaddingValues(40.dp,14.dp,40.dp,64.dp),verticalArrangement=Arrangement.spacedBy(28.dp)){
        item("hot"){
            MiniRanking(category.id,"hot","$year 年人气最高",hot.state,hotFocus,navigationFocus,open){hot.loadNext()}
        }
        item("score"){
            MiniRanking(category.id,"score","$year 年评分最高",score.state,scoreFocus,null,open){score.loadNext()}
        }
        item("browse-all"){
            TvAction("浏览全部",modifier=Modifier.fillMaxWidth().focusRequester(allFocus),onClick=browse)
        }
    }
}

@Composable
private fun MiniRanking(categoryId:Int,sort:String,title:String,state:CatalogFeedState,entry:FocusRequester,
                        up:FocusRequester?,open:(Movie)->Unit,retry:()->Unit){
    val movies=state.items.take(10)
    PosterFocusGroup("mini:$categoryId:$sort"){
        val remembered=LocalPosterFocus.current
        Column(Modifier.focusRequester(entry).focusGroup(),verticalArrangement=Arrangement.spacedBy(14.dp)){
            SectionHeading(title,"前 ${movies.size} 部")
            when {
                movies.isNotEmpty()->{
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
                        movies.take(2).forEachIndexed{index,movie->
                            key(movie.id){
                                val focus=remember{FocusRequester()}
                                LaunchedEffect(Unit){if(remembered?.value==movie.id)focus.requestFocus()}
                                HeroCard(Hero(movie.id,movie.title,movie.image,
                                    listOf("第 ${index+1} 名",movie.score.takeIf{it.isNotBlank()}?.let{"评分 $it"}.orEmpty(),movie.note).filter{it.isNotBlank()}.joinToString(" · ")),
                                    Modifier.weight(1f).focusRequester(focus).onFocusChanged{if(it.isFocused)remembered?.value=movie.id}
                                        .focusProperties{if(up!=null)this.up=up}){open(movie)}
                            }
                        }
                        if(movies.size==1)Spacer(Modifier.weight(1f))
                    }
                    movies.drop(2).chunked(4).forEach{row->
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
                            row.forEach{movie->key(movie.id){PosterCard(movie,Modifier.weight(1f),posterRatio=1.5f){open(movie)}}}
                            repeat(4-row.size){Spacer(Modifier.weight(1f))}
                        }
                    }
                }
                state.error!=null->ErrorNotice(state.error,retry)
                state.loading||state.nextPage==1->Text("正在加载榜单…",color=Muted)
                else->Text("今年暂无相关影片",color=Muted)
            }
        }
    }
}
