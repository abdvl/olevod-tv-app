package com.olevod.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class SearchFixture(val hot:List<String>,val suggest:suspend(String)->List<String>,val feed:(String)->CatalogFeed,val initialQuery:String="")
internal fun removeLastCodePoint(value:String):String=if(value.isEmpty())value else value.substring(0,value.offsetByCodePoints(value.length,-1))

@Composable
internal fun ConnectedSearch(vm:AppViewModel,open:(Movie)->Unit,fixture:SearchFixture?=null) {
    var draftQuery by rememberSaveable{mutableStateOf(fixture?.initialQuery.orEmpty())}
    var submittedQuery by rememberSaveable{mutableStateOf<String?>(null)}
    var suggestions by remember{mutableStateOf<List<String>>(emptyList())}
    var suggestionError by remember{mutableStateOf(false)}
    var suggestionsLoading by remember{mutableStateOf(false)}
    var suggestionRetry by remember{mutableIntStateOf(0)}
    val retrySuggestion=remember{FocusRequester()}
    var hot by remember{mutableStateOf(fixture?.hot.orEmpty())}
    var editRequest by remember{mutableIntStateOf(0)}
    val input=remember{FocusRequester()};val firstResult=remember{FocusRequester()}
    val retryResults=remember{FocusRequester()}
    var inputFocused by remember{mutableStateOf(false)}
    var focusIntent by remember{mutableStateOf<String?>(null)}
    var resultEntryJob by remember{mutableStateOf<Job?>(null)}
    fun cancelResultEntry(){resultEntryJob?.cancel();resultEntryJob=null}
    val changeQuery:(String)->Unit={cancelResultEntry();focusIntent=null;submittedQuery=null;draftQuery=it}
    BackHandler(enabled=!inputFocused){cancelResultEntry();focusIntent=null;input.requestFocus()}
    val keys=remember{List(36){FocusRequester()}}
    val clear=remember{FocusRequester()};val delete=remember{FocusRequester()};val chinese=remember{FocusRequester()}
    val suggestionsFocus=remember{FocusRequester()};val resultsFocus=remember{FocusRequester()}
    var lastKey by rememberSaveable{mutableIntStateOf(5)}
    var lastWord by rememberSaveable{mutableStateOf<String?>(null)}
    var lastResult by rememberSaveable{mutableLongStateOf(-1)}
    val page=LocalPageFocus.current
    val memory=LocalContentFocusMemory.current
    val scope=rememberCoroutineScope()
    val term=draftQuery.trim()
    val feed=remember(term,vm.sessionVersion,fixture){if(term.isBlank())null else fixture?.feed?.invoke(term)?:vm.searchFeed(term)}
    DisposableEffect(feed){onDispose{cancelResultEntry();feed?.cancel()}}
    val result=feed?.state?:CatalogFeedState(nextPage=2,endReached=true)
    val latestResult by rememberUpdatedState(result);val latestFeed by rememberUpdatedState(feed)
    val latestTerm by rememberUpdatedState(term)
    val movies=result.items
    val words=if(term.isBlank())(vm.searchHistory+hot).distinct().take(20)else suggestions.distinct().take(20)
    val list=rememberSaveable(term,saver=LazyListState.Saver){LazyListState()}
    val wordRefs=remember(words){words.associateWith{FocusRequester()}}
    val wordList=rememberSaveable(saver=LazyListState.Saver){LazyListState()}
    val resultRefs=remember(movies.map{it.id}){movies.associate{it.id to FocusRequester()}}
    fun enterWords(){if(suggestionError)retrySuggestion.requestFocus()else if(words.isNotEmpty()){val i=words.indexOf(lastWord).coerceAtLeast(0)
        if(wordList.layoutInfo.visibleItemsInfo.any{it.index==i})wordRefs[words[i]]?.requestFocus()
        else scope.launch{wordList.scrollToItem(i);withFrameNanos{};wordRefs[words[i]]?.requestFocus()}
    }}
    val enterResults:()->Unit={
        cancelResultEntry()
        if(movies.isNotEmpty())resultEntryJob=scope.launch{
            val index=movies.indexOfFirst{it.id==lastResult}.coerceAtLeast(0)
            list.scrollToItem(index/2);withFrameNanos{};resultRefs[movies[index].id]?.requestFocus()
        }else if(result.error!=null)resultEntryJob=scope.launch{
            list.scrollToItem(0);withFrameNanos{}
            if(latestTerm==term&&latestResult.items.isEmpty()&&latestResult.error!=null)retryResults.requestFocus()
        }
    }
    DisposableEffect(page){page?.enter={input.requestFocus()};onDispose{page?.enter=null}}
    LaunchedEffect(Unit){if(memory?.anchor?.value==null){withFrameNanos{};input.requestFocus()}}
    LaunchedEffect(term,focusIntent,result.items.firstOrNull()?.id,result.loading,result.error,result.nextPage){
        if(focusIntent!=term)return@LaunchedEffect
        if(result.items.isNotEmpty()){
            list.scrollToItem(0);withFrameNanos{}
            if(focusIntent==term){firstResult.requestFocus();focusIntent=null}
        }else if(result.error!=null){focusIntent=null}
        else if(!result.loading&&result.nextPage>1){focusIntent=null;input.requestFocus()}
    }
    LaunchedEffect(fixture){if(fixture==null)try{hot=vm.api.hotWords()}catch(e:Exception){if(e is CancellationException)throw e}}
    LaunchedEffect(term,fixture,suggestionRetry){
        // Keep the confirmed suggestion visible while its full query is loading.
        if(submittedQuery!=term)suggestions=emptyList()
        suggestionError=false;suggestionsLoading=term.isNotBlank()
        if(term.isNotBlank())try{
            delay(350)
            val fetched=fixture?.suggest?.invoke(term)?:vm.api.suggestions(term)
            currentCoroutineContext().ensureActive()
            suggestions=if(submittedQuery==term)(listOf(term)+fetched).distinct()else fetched
            suggestionsLoading=false
        }catch(e:Exception){
            if(e is CancellationException)throw e
            currentCoroutineContext().ensureActive()
            suggestionError=true;suggestionsLoading=false
        }
    }
    LaunchedEffect(feed){if(feed!=null&&feed.state.nextPage==1&&feed.state.error==null){delay(if(focusIntent==term)0 else 400);feed.loadNext()}}
    LaunchedEffect(list,term){snapshotFlow{
        val state=latestResult;val rows=(state.items.size+1)/2
        rows>0&&(list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1)>=rows-1&&!state.loading&&!state.endReached&&state.error==null
    }.distinctUntilChanged().collect{if(it)latestFeed?.loadNext()}}
    Row(Modifier.fillMaxSize().padding(36.dp,12.dp,36.dp,0.dp).testTag("search-page").onPreviewKeyEvent{event->
        if(event.type==KeyEventType.KeyDown){
            cancelResultEntry()
            if(event.key in listOf(Key.DirectionLeft,Key.DirectionRight,Key.DirectionUp,Key.DirectionDown))focusIntent=null
        }
        false
    },horizontalArrangement=Arrangement.spacedBy(18.dp)){
        Column(Modifier.width(254.dp).verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            InputBox(draftQuery,changeQuery,"输入片名 / 演员",Modifier.focusRequester(input).restoreContentFocus("input").testTag("search-input")
                .onFocusChanged{inputFocused=it.isFocused}.semantics{contentDescription="搜索输入框"}
                .focusProperties{down=clear;up=page?.header?:FocusRequester.Default},editRequest=editRequest,onEditingFinished={input.requestFocus()})
            Row{TvAction("清空",Icons.Rounded.Close,modifier=Modifier.focusRequester(clear).restoreContentFocus("clear").focusProperties{up=input;down=keys[0];right=delete;left=FocusRequester.Cancel}){changeQuery("")}
                Spacer(Modifier.weight(1f));TvAction("退格",Icons.Rounded.Backspace,modifier=Modifier.focusRequester(delete).restoreContentFocus("delete").focusProperties{up=input;down=keys[5];left=clear}){changeQuery(removeLastCodePoint(draftQuery))}}
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){"ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".chunked(6).forEachIndexed{row,line->
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){line.forEachIndexed{column,c->
                    val index=row*6+column
                    KeyButton(c.toString(),Modifier.weight(1f).focusRequester(keys[index]).restoreContentFocus("key:$c").testTag("search-key:$c").onFocusChanged{if(it.isFocused)lastKey=index}.focusProperties{
                        up=if(row>0)keys[index-6]else if(column<3)clear else delete
                        down=if(row<5)keys[index+6]else chinese
                        left=if(column>0)keys[index-1]else FocusRequester.Cancel
                        right=if(column<5)keys[index+1]else FocusRequester.Cancel
                    }.onPreviewKeyEvent{event->if(column==5&&event.key==Key.DirectionRight){if(event.type==KeyEventType.KeyDown){if(words.isNotEmpty()||suggestionError)enterWords()else enterResults()};true}else false}){changeQuery(draftQuery+c)}
                }}
            }}
            TvAction("中文 / 语音输入",Icons.Rounded.Keyboard,modifier=Modifier.focusRequester(chinese).restoreContentFocus("ime").focusProperties{up=keys[30+(lastKey%6)];down=FocusRequester.Cancel}){focusIntent=null;editRequest++}
            Text("支持系统输入法与手机遥控输入",color=Muted,fontSize=13.sp,lineHeight=18.sp)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(TvDesign.border))
        Column(Modifier.width(170.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(term.isBlank())"热门与最近搜索"else"猜你想搜",color=White,fontSize=18.sp,lineHeight=24.sp)
            if(suggestionError){
                Text("联想暂时不可用，仍可搜索完整片名",color=TvDesign.error,fontSize=13.sp,lineHeight=18.sp)
                TvAction("重试联想",modifier=Modifier.focusRequester(retrySuggestion).testTag("suggestion-retry").focusProperties{
                    left=keys[lastKey];right=FocusRequester.Cancel;up=page?.header?:FocusRequester.Default
                    down=words.firstOrNull()?.let{wordRefs[it]}?:FocusRequester.Cancel
                }.onPreviewKeyEvent{event->if(event.key==Key.DirectionRight){if(event.type==KeyEventType.KeyDown)enterResults();true}else false}){
                    keys[lastKey].requestFocus();suggestionRetry++
                }
            }else if(suggestionsLoading)Text("正在查找联想…",color=Muted,fontSize=13.sp)
            LazyColumn(Modifier.weight(1f).focusRequester(suggestionsFocus).focusGroup(),state=wordList,contentPadding=PaddingValues(bottom=64.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                itemsIndexed(words,key={_,word->word}){index,word->TvAction(word,selected=submittedQuery==word,modifier=Modifier.fillMaxWidth().focusRequester(wordRefs.getValue(word))
                    .restoreContentFocus("suggestion:$word").testTag("suggestion:$word").onFocusChanged{if(it.isFocused)lastWord=word}
                    .focusProperties{left=keys[lastKey];right=FocusRequester.Cancel;if(index==0)up=if(suggestionError)retrySuggestion else page?.header?:FocusRequester.Default;if(index==words.lastIndex)down=FocusRequester.Cancel}
                    .onPreviewKeyEvent{event->if(event.key==Key.DirectionRight){if(event.type==KeyEventType.KeyDown)enterResults();true}else false}){
                        cancelResultEntry()
                        if(fixture==null)vm.saveQuery(word);submittedQuery=word.trim();focusIntent=word.trim();draftQuery=word
                        if(word.trim()==term&&result.error!=null)feed?.loadNext()
                    }}
                if(term.isBlank()&&vm.searchHistory.isNotEmpty())item{TvAction("清除搜索记录"){vm.clearSearchHistory()}}
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(term.isBlank())"发现想看的故事"else(if(submittedQuery==term)"「$term」搜索结果"else"包含「$term」的影片")+(if(result.total>=0)" · ${result.total} 部"else""),color=White,fontSize=17.sp,lineHeight=24.sp)
            PosterFocusGroup("search:$term") {LazyColumn(Modifier.weight(1f).focusRequester(resultsFocus).focusGroup().testTag("search-results"),state=list,verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(4.dp,4.dp,4.dp,64.dp)){
                itemsIndexed(movies.chunked(2),key={_,row->row.first().id}){rowIndex,row->Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    row.forEachIndexed{column,m->PosterCard(m,Modifier.weight(1f).focusRequester(resultRefs.getValue(m.id))
                        .then(if(rowIndex==0&&column==0)Modifier.focusRequester(firstResult)else Modifier)
                        .focusProperties{if(column==0)left=FocusRequester.Cancel;else right=FocusRequester.Cancel;if(rowIndex==0)up=page?.header?:FocusRequester.Default}
                        .onPreviewKeyEvent{event->if(column==0&&event.key==Key.DirectionLeft){if(event.type==KeyEventType.KeyDown){if(words.isNotEmpty()||suggestionError)enterWords()else keys[lastKey].requestFocus()};true}else false},
                        onFocused={lastResult=m.id;if(rowIndex==(movies.size-1)/2&&!result.loading&&!result.endReached&&result.error==null)feed?.loadNext()}){if(fixture==null)vm.saveQuery(term);open(m)}}
                    repeat(2-row.size){Spacer(Modifier.weight(1f))}
                }}
                item(key="state"){
                    when {
                        term.isBlank()->Text("输入片名，或选择中间的词条",color=Muted,fontSize=14.sp,lineHeight=22.sp)
                        result.error!=null->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                            Text(result.error,color=TvDesign.error,fontSize=15.sp)
                            TvAction("重试",modifier=Modifier.focusRequester(retryResults).restoreContentFocus("result-retry")
                                .testTag("search-results-retry").focusProperties{
                                    left=FocusRequester.Cancel;right=FocusRequester.Cancel;down=FocusRequester.Cancel
                                    if(movies.isEmpty())up=page?.header?:input
                                }.onPreviewKeyEvent{event->
                                    if(event.key==Key.DirectionLeft){
                                        if(event.type==KeyEventType.KeyDown){if(words.isNotEmpty()||suggestionError)enterWords()else keys[lastKey].requestFocus()}
                                        true
                                    }else false
                                }){
                                // Keep focus on an attached target while retry removes its button.
                                input.requestFocus()
                                focusIntent=if(movies.isEmpty())term else null
                                feed?.loadNext()
                            }
                        }
                        result.loading||result.nextPage==1->Text("正在搜索…",color=Muted)
                        movies.isEmpty()->Text(if(submittedQuery!=term&&words.isNotEmpty())"选择联想词，查找完整片名"else"没有找到相关影片",color=Muted,fontSize=14.sp,lineHeight=22.sp)
                        result.endReached->Text("已显示全部结果",color=Muted,fontSize=13.sp)
                    }
                }
            }}
        }
    }
}
