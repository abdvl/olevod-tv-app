package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun CloudHistoryPanel(vm:AppViewModel,open:(Movie)->Unit,login:()->Unit,up:FocusRequester=FocusRequester.Default){
    val loggedIn=vm.sessionVersion.let{vm.sessions.token!=null}
    val feed=remember(vm.sessionVersion){if(loggedIn)vm.cloudHistoryFeed()else null}
    val metadata=rememberHistoryMetadata(vm)
    val entry=remember{FocusRequester()}
    val result=feed?.state
    DisposableEffect(feed){onDispose{feed?.cancel()}}
    LaunchedEffect(feed){if(feed!=null&&feed.state.nextPage==1&&feed.state.error==null)feed.loadNext()}
    val page=LocalPageFocus.current
    DisposableEffect(page,result?.items?.isNotEmpty(),loggedIn,result?.error){page?.enter={if(!loggedIn||result?.items?.isNotEmpty()==true||result?.error!=null)entry.requestFocus()else up.requestFocus()};onDispose{page?.enter=null}}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)){
        vm.historySyncError?.let{Text(it,color=Muted,fontSize=13.sp)}
        when{
            !loggedIn->TvAction("登录查看网站历史",modifier=Modifier.focusRequester(entry).restoreContentFocus("cloud-login"),onClick=login)
            result==null||result.items.isEmpty()->Column(Modifier.focusRequester(entry)){
                when{result?.error!=null->ErrorNotice(result.error){feed?.loadNext()};result?.loading==true||result?.nextPage==1->Text("正在加载网站历史…",color=Muted);else->Text("网站账号没有更多观看历史",color=Muted)}
            }
            else->HistoryGrid(result.items,metadata,"cloud:${vm.sessionVersion}",entry,up,{record,movie->vm.pendingResume=record.copy(movie=movie);open(movie)},
                loading=result.loading,endReached=result.endReached,error=result.error,loadMore={feed?.loadNext()})
        }
    }
}
