package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun ConnectedHome(state:HomeState,vm:AppViewModel,open:(Movie)->Unit,history:()->Unit,browse:(String)->Unit) {
    val records by vm.history.records.collectAsStateWithLifecycle()
    val recent=records.take(10)
    val metadata=rememberHistoryMetadata(vm)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(40.dp,14.dp,40.dp,30.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item(key="recent-playback"){
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                SectionHeading("最近播放","接着上次看",history)
                if(recent.isEmpty())Text("播放影片后会显示在这里",color=Muted,fontSize=13.sp)
                else BoxWithConstraints(Modifier.fillMaxWidth()){
                    val cardWidth=(maxWidth-72.dp)/5
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)){
                        items(recent,key={it.movie.id}){record->
                            var movie by remember(record.movie,metadata){mutableStateOf(record.movie)}
                            LaunchedEffect(record.movie,metadata){try{movie=metadata.complete(record.movie)}catch(e:Exception){if(e is CancellationException)throw e}}
                            // A different focus ID distinguishes this copy from the same movie in a category below.
                            PosterCard(movie,Modifier.width(cardWidth),posterRatio=1.5f,focusIdentity=-movie.id,
                                subtitle=(if(record.episode>0)"第 ${record.episode} 集 · "else "")+"已看 ${clock(record.positionMs)}"){
                                vm.pendingResume=record.copy(movie=movie);open(movie)
                            }
                        }
                    }
                }
            }
        }
        if(state.loading)item{Text("正在为你寻找好故事…",color=Muted,fontSize=18.sp)}
        state.error?.let{item{ErrorNotice(it){vm.loadHome()}}}
        if(state.heroes.isNotEmpty())item{Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){state.heroes.take(2).forEachIndexed{i,h->HeroCard(h,Modifier.weight(if(i==0)1.6f else 1f)){open(Movie(h.id,h.title,h.image,h.note))}}}}
        items(state.sections,key={it.category.id}) { section ->
            when {
                section.error!=null->Column{SectionHeading(section.category.name);ErrorNotice(section.error){vm.retrySection(section.category)}}
                section.loading->Column{SectionHeading(section.category.name);Text("正在加载…",color=Muted)}
                section.movies.isEmpty()->Column{SectionHeading(section.category.name);Text("暂时没有影片",color=Muted)}
                else->HomeMovieGroup(section.movies,open,section.category.name){browse(section.category.name)}
            }
        }
    }
}
@Composable internal fun ErrorNotice(message:String,retry:()->Unit){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(message,color=Muted,fontSize=15.sp);TvAction("重试",selected=true,onClick=retry)}}
