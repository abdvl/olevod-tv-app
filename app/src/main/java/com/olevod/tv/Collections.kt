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
fun HistoryScreen(vm:AppViewModel,open:(Movie)->Unit) {
    val records by vm.history.records.collectAsStateWithLifecycle()
    var clear by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(40.dp,15.dp,40.dp,25.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row{SectionTitle("观看历史","此设备 · ${records.size} 部");Spacer(Modifier.weight(1f));if(records.isNotEmpty())TvAction("清空历史"){clear=true}}
        if(records.isEmpty())Text("开始播放后，这里会保留所有观看记录",color=Muted)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(16.dp)){items(records,key={it.movie.id}){r->Row(horizontalArrangement=Arrangement.spacedBy(20.dp)){Column(Modifier.weight(1f)){TvAction(r.movie.title){open(r.movie)};Text("第 ${r.episode} 集 · ${clock(r.positionMs)} / ${clock(r.durationMs)}",color=Muted,fontSize=13.sp,modifier=Modifier.padding(start=14.dp))};TvAction("删除"){scope.launch{vm.history.remove(r.movie.id)}}}}}
    }
    if(clear)Dialog(onDismissRequest={clear=false}){Column(Modifier.background(Panel).padding(25.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){Text("清空此设备全部观看历史？",color=White);Row{TvAction("取消"){clear=false};TvAction("清空"){scope.launch{vm.history.remove(null)};clear=false}}}}
}
