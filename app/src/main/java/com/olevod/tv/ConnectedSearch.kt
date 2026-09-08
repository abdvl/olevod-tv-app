package com.olevod.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ConnectedSearch(vm:AppViewModel,open:(Movie)->Unit) {
    var query by rememberSaveable{mutableStateOf("")}
    var suggestions by remember{mutableStateOf<List<String>>(emptyList())}
    var hot by remember{mutableStateOf<List<String>>(emptyList())}
    var editRequest by remember{mutableIntStateOf(0)}
    val input=remember{FocusRequester()}
    val keys=remember{List(36){FocusRequester()}}
    val clear=remember{FocusRequester()}
    val delete=remember{FocusRequester()}
    val chinese=remember{FocusRequester()}
    val suggestionsFocus=remember{FocusRequester()}
    val resultsFocus=remember{FocusRequester()}
    var lastKey by remember{mutableIntStateOf(5)}
    val home by vm.home.collectAsStateWithLifecycle()
    val term=query.trim()
    val feed=remember(term){if(term.isBlank())null else vm.searchFeed(term)}
    val result=feed?.state?:CatalogFeedState()
    val movies=if(term.isBlank())home.sections.firstOrNull{it.category.id==1}?.movies.orEmpty()else result.items
    val words=if(term.isBlank())(vm.searchHistory+hot).distinct().take(20)else(suggestions+movies.map{it.title}).distinct().take(20)
    val listState=key(term){rememberLazyListState()}
    LaunchedEffect(Unit){try{hot=vm.api.hotWords()}catch(e:Exception){if(e is CancellationException)throw e}}
    LaunchedEffect(term){suggestions=emptyList();if(term.isNotBlank())try{delay(350);suggestions=vm.api.suggestions(term)}catch(e:Exception){if(e is CancellationException)throw e}}
    LaunchedEffect(feed){if(feed!=null&&feed.state.items.isEmpty()&&feed.state.error==null){delay(400);feed.loadNext()}}
    LaunchedEffect(feed,listState){
        snapshotFlow {
            val layout=listState.layoutInfo;val state=feed?.state
            state!=null && state.items.isNotEmpty() && layout.totalItemsCount>=(state.items.size+1)/2+1 &&
                (layout.visibleItemsInfo.lastOrNull()?.index?:-1)>=layout.totalItemsCount-2 &&
                !state.loading && !state.endReached && state.error==null
        }.distinctUntilChanged().collect{nearEnd->if(nearEnd)feed?.loadNext()}
    }
    Row(Modifier.fillMaxSize().padding(36.dp,18.dp,36.dp,20.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)){
        Column(Modifier.width(254.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            InputBox(query,{query=it},"输入片名 / 演员",Modifier.focusRequester(input).focusProperties{down=clear},editRequest=editRequest)
            Row{TvAction("清空",Icons.Rounded.Close,modifier=Modifier.focusRequester(clear).focusProperties{up=input;down=keys[0];right=delete}){query=""};Spacer(Modifier.weight(1f));TvAction("退格",Icons.Rounded.Backspace,modifier=Modifier.focusRequester(delete).focusProperties{up=input;down=keys[5];left=clear}){query=query.dropLast(1)}}
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)){"ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".chunked(6).forEachIndexed{row,line->
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){line.forEachIndexed{column,c->
                    val index=row*6+column
                    KeyButton(c.toString(),Modifier.weight(1f).focusRequester(keys[index]).onFocusChanged{if(it.isFocused)lastKey=index}.focusProperties{
                        up=if(row>0)keys[index-6]else if(column<3)clear else delete
                        down=if(row<5)keys[index+6]else chinese
                        left=if(column>0)keys[index-1]else FocusRequester.Cancel
                        right=if(column<5)keys[index+1]else if(words.isNotEmpty())suggestionsFocus else if(movies.isNotEmpty())resultsFocus else FocusRequester.Cancel
                    }){query+=c}
                }}
            }}
            TvAction("中文 / 语音输入",Icons.Rounded.Keyboard,modifier=Modifier.focusRequester(chinese).focusProperties{up=keys[30]}){editRequest++}
            Text("支持系统输入法与手机遥控输入",color=Muted,fontSize=11.sp)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(White.copy(alpha=.08f)))
        Column(Modifier.width(170.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Text(if(term.isBlank())"热门与最近搜索"else"猜你想搜",color=White,fontSize=18.sp)
            LazyColumn(Modifier.weight(1f).focusRequester(suggestionsFocus).focusGroup(),verticalArrangement=Arrangement.spacedBy(5.dp)){
                items(words,key={it}){word->TvAction(word,modifier=Modifier.fillMaxWidth().focusProperties{left=keys[lastKey];right=if(movies.isNotEmpty())resultsFocus else FocusRequester.Cancel}){vm.saveQuery(word);query=word}}
            }
            if(term.isBlank()&&vm.searchHistory.isNotEmpty())TvAction("清除搜索记录"){vm.clearSearchHistory()}
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(term.isBlank())"最近更新"else"包含「$term」的影片 · ${result.total} 部",color=White,fontSize=17.sp)
            PosterFocusGroup(term){LazyColumn(Modifier.weight(1f).focusRequester(resultsFocus).focusGroup(),state=listState,verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(4.dp,5.dp,4.dp,64.dp)){
                items(movies.chunked(2),key={it.first().id}){row->Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    row.forEachIndexed{column,m->PosterCard(m,Modifier.weight(1f).focusProperties{if(column==0)left=if(words.isNotEmpty())suggestionsFocus else keys[lastKey]},posterRatio=.74f){if(term.isNotBlank())vm.saveQuery(term);open(m)}}
                    repeat(2-row.size){Spacer(Modifier.weight(1f))}
                }}
                item(key="load-more"){
                    when {
                        result.error!=null->ErrorNotice(result.error){feed?.loadNext()}
                        term.isNotBlank()&&(result.loading||result.nextPage==1)->Text("正在搜索…",color=Muted)
                        term.isNotBlank()&&movies.isEmpty()->Text("没有找到相关影片",color=Muted)
                        result.endReached&&movies.isNotEmpty()->Text("已显示全部结果",color=Muted,fontSize=12.sp)
                    }
                }
            }}
        }
    }
}
