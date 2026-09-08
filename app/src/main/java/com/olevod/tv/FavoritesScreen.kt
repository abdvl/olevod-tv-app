package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(vm:AppViewModel,open:(Movie)->Unit,login:()->Unit,browse:()->Unit={},fixtureFeed:CatalogFeed?=null){
    val loggedIn=fixtureFeed!=null||vm.sessionVersion.let{vm.sessions.token!=null}
    val feed=remember(vm.sessionVersion,vm.favoritesVersion,fixtureFeed){fixtureFeed?:if(loggedIn)vm.favoritesFeed()else null}
    DisposableEffect(feed){onDispose{feed?.cancel()}}
    LaunchedEffect(feed){if(feed!=null&&feed.state.nextPage==1&&feed.state.error==null)feed.loadNext()}
    val result=feed?.state?:CatalogFeedState()
    val latest by rememberUpdatedState(result)
    val list=rememberSaveable(vm.sessionVersion,saver=LazyListState.Saver){LazyListState()}
    val entry=remember{FocusRequester()}
    val page=LocalPageFocus.current
    val scope=rememberCoroutineScope()
    val memory=LocalContentFocusMemory.current
    var lastIndex by rememberSaveable{mutableIntStateOf(0)}
    val itemRefs=remember{mutableMapOf<Long,FocusRequester>()}
    result.items.forEach{itemRefs.getOrPut(it.id){FocusRequester()}}
    LaunchedEffect(result.items.map{it.id},result.loading,result.nextPage,result.endReached,result.error){
        if(memory?.restorePending==true&&!result.loading&&result.nextPage>1){
            if(result.error!=null)return@LaunchedEffect
            val savedId=memory.anchor.value?.substringAfterLast(':')?.toLongOrNull()
            val exact=result.items.indexOfFirst{it.id==savedId}
            if(exact<0&&lastIndex>=result.items.size&&!result.endReached){feed?.loadNext();return@LaunchedEffect}
            val index=if(exact>=0)exact else lastIndex.coerceAtMost(result.items.lastIndex)
            if(index>=0){list.scrollToItem(index/6);withFrameNanos{};itemRefs[result.items[index].id]?.requestWhenAttached{memory.restorePending}}
            else{withFrameNanos{};entry.requestWhenAttached{memory.restorePending}}
            memory.restorePending=false
        }
    }
    DisposableEffect(page,result.items.isEmpty(),loggedIn,result.error,result.endReached,result.nextPage){
        page?.enter={scope.launch{list.scrollToItem(0);withFrameNanos{};if(latest.items.isNotEmpty()||!loggedIn||latest.error!=null||latest.endReached)entry.requestWhenAttached()else page.header.requestFocus()}}
        onDispose{page?.enter=null}
    }
    LaunchedEffect(list,feed){snapshotFlow{val state=latest;val rows=(state.items.size+5)/6;rows>0&&(list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1)>=rows-1&&!state.loading&&!state.endReached&&state.error==null}.distinctUntilChanged().collect{if(it)feed?.loadNext()}}
    Column(Modifier.fillMaxSize().padding(horizontal=36.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row(Modifier.padding(top=8.dp)){SectionTitle("我的收藏","网站账号收藏")}
        PosterFocusGroup("favorites:${vm.sessionVersion}"){LazyColumn(Modifier.weight(1f),state=list,contentPadding=PaddingValues(4.dp,4.dp,4.dp,64.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            if(!loggedIn)item{TvAction("登录查看收藏",modifier=Modifier.focusRequester(entry).restoreContentFocus("login"),onClick=login)}
            else{
                itemsIndexed(result.items.chunked(6),key={_,row->row.first().id}){rowIndex,row->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    row.forEachIndexed{column,m->PosterCard(m,Modifier.weight(1f).focusRequester(itemRefs.getValue(m.id)).then(if(rowIndex==0&&column==0)Modifier.focusRequester(entry)else Modifier)
                        .focusProperties{if(rowIndex==0)up=page?.header?:FocusRequester.Default;if(column==0)left=FocusRequester.Cancel;if(column==row.lastIndex)right=FocusRequester.Cancel},
                        onFocused={lastIndex=rowIndex*6+column;if(rowIndex==(result.items.size-1)/6&&!result.loading&&!result.endReached&&result.error==null)feed?.loadNext()}){open(m)}}
                    repeat(6-row.size){Spacer(Modifier.weight(1f))}
                }}
                item("state"){Column{
                    when{result.error!=null->Column(Modifier.then(if(result.items.isEmpty())Modifier.focusRequester(entry)else Modifier)){ErrorNotice(result.error){feed?.loadNext()}};result.loading||result.nextPage==1->Text("正在加载收藏…",color=Muted);result.items.isEmpty()->{Text("还没有收藏的影片",color=Muted);TvAction("开始浏览",modifier=Modifier.focusRequester(entry),onClick=browse)};result.endReached->Text("已显示全部收藏",color=Muted,fontSize=13.sp)}
                }}
            }
        }}
    }
}
