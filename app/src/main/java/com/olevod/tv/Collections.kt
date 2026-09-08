package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.olevod.tv.data.WatchRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class HistoryDelete(val id:Long?,val title:String,val account:String)
@Composable
fun HistoryScreen(vm:AppViewModel,open:(Movie)->Unit,browse:()->Unit={},initialCloud:Boolean=false,
                  fixtureRecords:List<WatchRecord>?=null,fixtureDelete:(suspend (Long?)->Unit)?=null,login:()->Unit) {
    val savedRecords by vm.history.records.collectAsStateWithLifecycle()
    val records=fixtureRecords?:savedRecords
    var cloud by rememberSaveable{mutableStateOf(initialCloud)}
    var removal by remember{mutableStateOf<HistoryDelete?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    var restoreId by remember{mutableStateOf<Long?>(null)}
    var restoreDelete by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val metadata=if(fixtureRecords==null)rememberHistoryMetadata(vm)else null
    val tabs=remember{List(2){FocusRequester()}};val clear=remember{FocusRequester()}
    val entry=remember{FocusRequester()}
    val page=LocalPageFocus.current
    LaunchedEffect(vm.sessionVersion){removal=null;restoreId=null}
    DisposableEffect(page,cloud,records.isNotEmpty()){
        page?.enter={if(!cloud&&records.isNotEmpty())entry.requestFocus()else tabs[if(cloud)1 else 0].requestFocus()}
        onDispose{page?.enter=null}
    }
    Column(Modifier.fillMaxSize().padding(horizontal=36.dp).testTag("history-page"),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(Modifier.padding(top=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
            SectionTitle("观看历史",if(cloud)"网站账号"else"此设备 · ${records.size} 部")
            Spacer(Modifier.weight(1f))
            if(!cloud&&records.isNotEmpty())TvAction("清空历史",Icons.Rounded.DeleteOutline,modifier=Modifier.focusRequester(clear).restoreContentFocus("clear-history").focusProperties{down=tabs[0];up=page?.header?:FocusRequester.Default}){removal=HistoryDelete(null,"",vm.sessions.accountKey)}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("此设备","网站账号").forEachIndexed{index,label->TvAction(label,selected=cloud==(index==1),modifier=Modifier.focusRequester(tabs[index]).restoreContentFocus("history-source:$index").testTag("history-source:$index")
                .focusProperties{up=page?.header?:FocusRequester.Default;left=if(index==1)tabs[0]else FocusRequester.Cancel;right=if(index==0)tabs[1]else if(!cloud&&records.isNotEmpty())clear else FocusRequester.Cancel}){cloud=index==1} }
        }
        error?.let{Text(it,color=TvDesign.error,fontSize=13.sp)}
        if(cloud)CloudHistoryPanel(vm,open,login,tabs[1])else {
            if(records.isEmpty())Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text("还没有观看记录",color=Muted);TvAction("开始浏览",onClick=browse)}
            else HistoryGrid(records,metadata,"device:${vm.sessionVersion}",entry,tabs[0],open={record,movie->vm.pendingResume=record.copy(movie=movie);open(movie)},
                remove={removal=HistoryDelete(it.movie.id,it.movie.title,vm.sessions.accountKey)},restoreId=restoreId,restoreDelete=restoreDelete,onRestored={restoreId=null;restoreDelete=false})
        }
    }
    removal?.let{target->TvConfirmDialog(if(target.id==null)"清空观看历史？"else"删除《${target.title}》的记录？",
        "仅清除此设备当前账号的${if(target.id==null)"全部观看历史"else"这条观看记录"}，不影响网站账号。",if(target.id==null)"清空"else"删除",
        onCancel={removal=null;scope.launch{withFrameNanos{};if(target.id==null)clear.requestFocus()else{restoreDelete=true;restoreId=target.id}}},onConfirm={
            removal=null
            restoreDelete=false
            if(target.account==vm.sessions.accountKey)scope.launch{
                val index=records.indexOfFirst{it.movie.id==target.id}
                val neighbor=records.getOrNull(index+1)?.movie?.id?:records.getOrNull(index-1)?.movie?.id
                try{if(fixtureDelete!=null)fixtureDelete(target.id)else vm.history.remove(target.id,target.account);error=null;if(target.id==null||neighbor==null){withFrameNanos{};tabs[0].requestFocus()}else restoreId=neighbor}
                catch(e:Exception){if(e is CancellationException)throw e;error="无法删除记录，请重试";restoreId=target.id}
            }
        })}
}

@Composable
internal fun HistoryGrid(records:List<WatchRecord>,metadata:HistoryMetadataLoader?,identity:String,entry:FocusRequester,up:FocusRequester,
                         open:(WatchRecord,Movie)->Unit,remove:((WatchRecord)->Unit)?=null,restoreId:Long?=null,restoreDelete:Boolean=false,onRestored:()->Unit={},
                         loading:Boolean=false,endReached:Boolean=true,error:String?=null,loadMore:()->Unit={}){
    val list=rememberSaveable(identity,saver=LazyListState.Saver){LazyListState()}
    val scope=rememberCoroutineScope()
    val refs=remember(records.map{it.movie.id}){records.associate{it.movie.id to List(2){FocusRequester()}}}
    val currentRecords by rememberUpdatedState(records);val currentLoad by rememberUpdatedState(loadMore)
    val canLoad by rememberUpdatedState(!loading&&!endReached&&error==null)
    LaunchedEffect(list){snapshotFlow{val rows=(currentRecords.size+1)/2;rows>0&&(list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1)>=rows-1&&canLoad}.distinctUntilChanged().collect{if(it)currentLoad()}}
    LaunchedEffect(restoreId,records.map{it.movie.id}){if(restoreId!=null){val i=records.indexOfFirst{it.movie.id==restoreId};if(i>=0){list.scrollToItem(i/2);withFrameNanos{};refs[restoreId]?.get(if(restoreDelete&&remove!=null)1 else 0)?.requestFocus();onRestored()}}}
    PosterFocusGroup(identity){LazyColumn(Modifier.fillMaxSize().testTag("history-grid"),state=list,contentPadding=PaddingValues(4.dp,4.dp,4.dp,64.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        itemsIndexed(records.chunked(2),key={_,row->row.first().movie.id}){rowIndex,row->Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
            row.forEachIndexed{column,record->val i=rowIndex*2+column
                fun navigation(role:Int):Modifier = Modifier.focusRequester(refs.getValue(record.movie.id)[role]).focusProperties{
                    if(rowIndex==0)this.up=up
                    left=when{role==1->refs.getValue(record.movie.id)[0];column==1->refs.getValue(row[0].movie.id)[if(remove!=null)1 else 0];else->FocusRequester.Cancel}
                    right=when{role==0&&remove!=null->refs.getValue(record.movie.id)[1];column==0&&row.size==2->refs.getValue(row[1].movie.id)[0];else->FocusRequester.Cancel}
                }.onPreviewKeyEvent{event->
                    val delta=when(event.key){Key.DirectionDown->2;Key.DirectionUp->-2;else->0}
                    if(delta==0)false else if(event.type==KeyEventType.KeyUp)true else{
                        val index=if(delta>0&&i+delta>=records.size&&rowIndex<(records.size-1)/2)records.lastIndex else i+delta
                        if(index<0)up.requestFocus()else if(index<records.size)scope.launch{list.scrollToItem(index/2);withFrameNanos{};refs[records[index].movie.id]?.get(role)?.requestFocus()}
                        true
                    }
                }
                HistoryRecordCard(record,metadata,Modifier.weight(1f),resumeModifier=navigation(0).then(if(i==0)Modifier.focusRequester(entry)else Modifier),
                    deleteModifier=navigation(1),onDelete=remove?.let{{it(record)}},showUpdated=remove!=null){open(record,it)}
            };repeat(2-row.size){Spacer(Modifier.weight(1f))}
        }}
        item("state"){when{error!=null->ErrorNotice(error,loadMore);loading->Text("正在加载更多…",color=Muted);endReached&&records.isNotEmpty()->Text("已显示全部记录",color=Muted,fontSize=13.sp)}}
    }}
}
