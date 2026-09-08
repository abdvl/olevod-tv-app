package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun ConnectedSearch(vm:AppViewModel,open:(Movie)->Unit) {
    var query by rememberSaveable{mutableStateOf("")}
    var page by rememberSaveable(query){mutableIntStateOf(1)}
    var suggestions by remember{mutableStateOf<List<String>>(emptyList())}
    var hot by remember{mutableStateOf<List<String>>(emptyList())}
    var movies by remember{mutableStateOf<List<Movie>>(emptyList())}
    var total by remember{mutableIntStateOf(0)}
    var loading by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var retry by remember{mutableIntStateOf(0)}
    var editRequest by remember{mutableIntStateOf(0)}
    val input=remember{FocusRequester()}
    val keyboard=LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit){try{hot=vm.api.hotWords()}catch(e:Exception){if(e is CancellationException)throw e}}
    LaunchedEffect(query){suggestions=emptyList();if(query.isNotBlank())try{delay(350);suggestions=vm.api.suggestions(query.trim())}catch(e:Exception){if(e is CancellationException)throw e}}
    LaunchedEffect(query,page,retry){
        error=null;movies=emptyList();total=0
        if(query.isBlank()){loading=false;return@LaunchedEffect}
        loading=true
        try{delay(400);val result=vm.api.search(query.trim(),page=page,size=12);movies=result.items;total=result.total}
        catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}finally{loading=false}
    }
    Row(Modifier.fillMaxSize().padding(40.dp,12.dp,40.dp,20.dp),horizontalArrangement=Arrangement.spacedBy(30.dp)){
        Column(Modifier.width(254.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("发现想看的故事",color=White,fontSize=22.sp)
            InputBox(query,{query=it},"输入片名 / 演员",Modifier.focusRequester(input),editRequest=editRequest)
            Row{TvAction("清空",Icons.Rounded.Close){query=""};TvAction("退格",Icons.Rounded.Backspace){query=query.dropLast(1)}}
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)){"abcdefghijklmnopqrstuvwxyz1234567890".chunked(6).forEach{line->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){line.forEach{c->KeyButton(c.toString(),Modifier.weight(1f)){query+=c}}}}}
            TvAction("中文 / 语音输入",Icons.Rounded.Keyboard){editRequest++}
            Text("支持系统输入法与手机遥控输入",color=Muted,fontSize=11.sp)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(White.copy(alpha=.08f)))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
            SectionTitle(if(query.isBlank())"大家都在看" else "搜索结果",if(query.isBlank())"热门推荐" else "共 $total 部")
            if(query.isNotBlank()&&suggestions.isNotEmpty())LazyRow{items(suggestions){word->TvAction(word){vm.saveQuery(word);query=word}}}
            if(query.isBlank()&&vm.searchHistory.isNotEmpty()){Row{Text("最近搜索",color=Muted);TvAction("清除"){vm.clearSearchHistory()}};LazyRow{items(vm.searchHistory){word->TvAction(word){vm.saveQuery(word);query=word}}}}
            when {
                query.isBlank()->LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(hot.chunked(2)){row->Row{row.forEach{word->TvAction(word,modifier=Modifier.weight(1f)){vm.saveQuery(word);query=word}};repeat(2-row.size){Spacer(Modifier.weight(1f))}}}}
                error!=null->ErrorNotice(error!!){retry++}
                loading->Text("正在搜索…",color=Muted)
                movies.isEmpty()->Text("没有找到相关影片，请换个关键词",color=Muted)
                else->LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(4.dp,5.dp,4.dp,15.dp)){items(movies.chunked(3)){row->Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){row.forEach{m->PosterCard(m,Modifier.weight(1f),posterRatio=1.1f){vm.saveQuery(query);open(m)}};repeat(3-row.size){Spacer(Modifier.weight(1f))}}}}
            }
            if(query.isNotBlank()&&!loading&&error==null)Row{if(page>1)TvAction("上一页"){page--};Text("第 $page 页",color=Muted,fontSize=12.sp,modifier=Modifier.padding(12.dp));if(page*12<total)TvAction("下一页"){page++}}
        }
    }
}
