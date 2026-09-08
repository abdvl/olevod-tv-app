package com.olevod.tv

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.olevod.tv.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ConnectedBrowse(categoryName:String,vm:AppViewModel,open:(Movie)->Unit,chooseCategory:(String)->Unit) {
    var categories by remember { mutableStateOf(vm.home.value.sections.map{it.category}) }
    var error by remember{mutableStateOf<String?>(null)}
    var area by rememberSaveable(categoryName){mutableStateOf("0")}
    var year by rememberSaveable(categoryName){mutableStateOf("0")}
    var type by rememberSaveable(categoryName){mutableIntStateOf(0)}
    var initial by rememberSaveable(categoryName){mutableStateOf("0")}
    var membership by rememberSaveable(categoryName){mutableIntStateOf(3)}
    var sort by rememberSaveable(categoryName){mutableStateOf("update")}
    var retry by remember{mutableIntStateOf(0)}
    LaunchedEffect(retry){error=null;try{if(categories.isEmpty()||retry>0)categories=vm.api.categories()}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}}
    val category=categories.firstOrNull{it.name==categoryName || (categoryName=="VIP蓝光"&&it.id==6)}
    val filter=Filter(category?.id?:1,area,year,type,initial,membership,sort)
    val feed=remember(filter,category!=null){if(category==null)null else vm.catalogFeed(filter)}
    val result=feed?.state?:CatalogFeedState(loading=true)
    val movies=result.items
    val total=result.total
    val loading=result.loading
    val listState=key(filter){rememberLazyListState()}
    LaunchedEffect(feed){if(feed!=null&&feed.state.items.isEmpty()&&feed.state.error==null)feed.loadNext()}
    LaunchedEffect(feed,listState){
        snapshotFlow {
            val layout=listState.layoutInfo
            val state=feed?.state
            (layout.visibleItemsInfo.lastOrNull()?.index?:-1)>=layout.totalItemsCount-2 &&
                state!=null && state.items.isNotEmpty() && layout.totalItemsCount>=((state.items.size+5)/6)+3 && !state.loading && !state.endReached && state.error==null
        }.distinctUntilChanged().collect{nearEnd->if(nearEnd)feed?.loadNext()}
    }
    PosterFocusGroup(filter.toString()){LazyColumn(Modifier.fillMaxSize().padding(horizontal=28.dp),state=listState,verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=8.dp,bottom=64.dp)) {
        item(key="filters"){
            Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
                val rows=listOf(
                    BrowseFilterRow("排序",listOf("desc" to "最新上传","update" to "最近更新","hot" to "人气最高","score" to "评分最高"),sort){sort=it},
                    BrowseFilterRow("分类",categories.map{it.id.toString() to it.name}.ifEmpty{listOf("1" to categoryName)},category?.id?.toString().orEmpty()){id->categories.firstOrNull{it.id.toString()==id}?.let{chooseCategory(it.name)}},
                    BrowseFilterRow("类型",listOf("0" to "全部类型")+(category?.types?:emptyList()).map{it.first.toString() to it.second},type.toString()){type=it.toInt()},
                    BrowseFilterRow("地区",listOf("0" to "全部地区")+(category?.areas?:emptyList()).map{it to it},area){area=it},
                    BrowseFilterRow("年份",listOf("0" to "全部年份")+(category?.years?:emptyList()).map{it to it},year){year=it},
                    BrowseFilterRow("范围",listOf("3" to "全部影片","1" to "会员","2" to "免费"),membership.toString()){membership=it.toInt()},
                    BrowseFilterRow("字母",listOf("0" to "全部字母")+('A'..'Z').map{it.toString() to it.toString()},initial){initial=it}
                )
                val targets=remember(rows.map{it.options.map{p->p.first}}){rows.map{r->r.options.map{FocusRequester()}}}
                rows.forEachIndexed { rowIndex,row ->
                    BrowseOptions(row,targets[rowIndex]) { column ->
                        val up=if(rowIndex>0)targets[rowIndex-1][column.coerceAtMost(targets[rowIndex-1].lastIndex)]else FocusRequester.Default
                        val down=if(rowIndex<rows.lastIndex)targets[rowIndex+1][column.coerceAtMost(targets[rowIndex+1].lastIndex)]else FocusRequester.Default
                        Modifier.focusProperties { this.up=up;this.down=down }
                    }
                }
            }
        }
        item(key="summary"){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("$categoryName · $total 部",color=White,fontSize=15.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("已加载 ${movies.size} 部",color=Muted,fontSize=12.sp)
                Spacer(Modifier.width(12.dp))
                BrowseChip("重置筛选",false){area="0";year="0";type=0;initial="0";membership=3;sort="update"}
            }
        }
        when {
            error!=null->item{ErrorNotice(error!!){retry++}}
            movies.isEmpty()&&loading->item{Text("正在加载…",color=Muted)}
            movies.isEmpty()&&result.error==null->item{Text("当前条件下没有影片",color=Muted)}
            else->items(movies.chunked(6),key={row->row.first().id}){row->
                Row(Modifier.padding(horizontal=3.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    row.forEach{m->PosterCard(m,Modifier.weight(1f),posterRatio=.78f){open(m)}}
                    repeat(6-row.size){Spacer(Modifier.weight(1f))}
                }
            }
        }
        item(key="load-more"){
            when {
                result.error!=null->ErrorNotice(result.error){feed?.loadNext()}
                loading&&movies.isNotEmpty()->Text("正在加载更多…",color=Muted,modifier=Modifier.padding(12.dp))
                result.endReached&&movies.isNotEmpty()->Text("已显示全部影片",color=Muted,modifier=Modifier.padding(12.dp))
            }
        }
    }}
}

private data class BrowseFilterRow(val title:String,val options:List<Pair<String,String>>,val value:String,val choose:(String)->Unit)

@Composable
private fun BrowseOptions(row:BrowseFilterRow,targets:List<FocusRequester>,directions:(Int)->Modifier){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Text(row.title,color=Muted,fontSize=12.sp,modifier=Modifier.width(48.dp))
        // Compose every chip so explicit vertical targets remain reachable even off screen.
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            row.options.forEachIndexed{index,(key,label)->
                BrowseChip(label,key==row.value,Modifier.focusRequester(targets[index]).then(directions(index))){row.choose(key)}
            }
        }
    }
}

@Composable
private fun BrowseChip(label:String,selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit){
    val interaction=remember{MutableInteractionSource()}
    val focused by interaction.collectIsFocusedAsState()
    Text(label,color=if(focused)Bg else if(selected)Green else White,fontSize=12.sp,lineHeight=16.sp,fontWeight=if(focused||selected)FontWeight.Bold else FontWeight.Normal,
        modifier=modifier.clip(RoundedCornerShape(30.dp)).background(if(focused)Green else if(selected)Green.copy(alpha=.1f)else androidx.compose.ui.graphics.Color.Transparent)
            .border(if(selected&&!focused)1.dp else 0.dp,if(selected&&!focused)Green.copy(alpha=.3f)else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(30.dp))
            .clickable(interactionSource=interaction,indication=null,onClick=onClick).padding(horizontal=11.dp,vertical=3.dp))
}

@Composable internal fun SectionTitle(title:String,subtitle:String=""){Row{Text(title,color=White,fontSize=23.sp);Spacer(Modifier.width(12.dp));Text(subtitle,color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=9.dp))}}
