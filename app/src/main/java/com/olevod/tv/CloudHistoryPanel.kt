package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.olevod.tv.data.WatchRecord
import kotlinx.coroutines.CancellationException

@Composable
fun CloudHistoryPanel(vm:AppViewModel,open:(Movie)->Unit,login:()->Unit) {
    var page by rememberSaveable{mutableIntStateOf(1)}
    var records by remember{mutableStateOf<List<WatchRecord>>(emptyList())}
    var total by remember{mutableIntStateOf(0)}
    var error by remember{mutableStateOf<String?>(null)}
    var loading by remember{mutableStateOf(true)}
    var retry by remember{mutableIntStateOf(0)}
    val loggedIn=vm.sessionVersion.let{vm.sessions.token!=null}
    LaunchedEffect(page,retry,vm.sessionVersion){if(!loggedIn)return@LaunchedEffect;loading=true;error=null;try{val result=vm.api.cloudHistory(page);records=result.items;total=result.total}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}finally{loading=false}}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)){
        vm.historySyncError?.let{Text(it,color=Muted,fontSize=12.sp)}
        when{!loggedIn->TvAction("登录查看网站历史",onClick=login);error!=null->ErrorNotice(error!!){retry++};loading->Text("正在加载网站历史…",color=Muted);records.isEmpty()->Text("网站账号没有更多观看历史",color=Muted);else->LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp)){items(records,key={it.movie.id}){r->Column{TvAction(r.movie.title){vm.pendingResume=r;open(r.movie)};Text("第 ${r.episode} 集 · 已观看 ${clock(r.positionMs)}",color=Muted,fontSize=12.sp,modifier=Modifier.padding(start=14.dp))}}}}
        if(loggedIn&&!loading)Row{if(page>1)TvAction("上一页"){page--};Text("第 $page 页 · 共 $total 部",color=Muted,modifier=Modifier.padding(12.dp));if(page*20<total)TvAction("下一页"){page++}}
    }
}
