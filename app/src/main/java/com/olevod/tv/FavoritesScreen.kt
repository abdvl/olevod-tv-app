package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(vm:AppViewModel,open:(Movie)->Unit,login:()->Unit) {
    var movies by remember{mutableStateOf<List<Movie>>(emptyList())}
    var total by remember{mutableIntStateOf(0)}
    var page by rememberSaveable{mutableIntStateOf(1)}
    var loading by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf<String?>(null)}
    var retry by remember{mutableIntStateOf(0)}
    val loggedIn=vm.sessionVersion.let{vm.sessions.token!=null}
    LaunchedEffect(page,retry,vm.sessionVersion){if(!loggedIn)return@LaunchedEffect;loading=true;error=null;try{val result=vm.api.favorites(page);movies=result.items;total=result.total}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}finally{loading=false}}
    Column(Modifier.fillMaxSize().padding(40.dp,15.dp,40.dp,25.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
        SectionTitle("我的收藏","网站账号收藏")
        when{!loggedIn->TvAction("登录查看收藏",onClick=login);error!=null->ErrorNotice(error!!){retry++};loading->Text("正在加载…",color=Muted);movies.isEmpty()->Text("还没有收藏的影片",color=Muted);else->LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(18.dp)){items(movies.chunked(5)){row->Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){row.forEach{m->PosterCard(m,Modifier.weight(1f),posterRatio=1.15f){open(m)}};repeat(5-row.size){Spacer(Modifier.weight(1f))}}}}}
        if(loggedIn)Row{if(page>1)TvAction("上一页"){page--};Text("第 $page 页",color=Muted,modifier=Modifier.padding(12.dp));if(page*20<total)TvAction("下一页"){page++}}
    }
}
