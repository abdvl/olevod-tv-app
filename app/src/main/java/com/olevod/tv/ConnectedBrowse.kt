package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.olevod.tv.data.*
import kotlinx.coroutines.CancellationException

@Composable
fun ConnectedBrowse(categoryName:String,vm:AppViewModel,open:(Movie)->Unit) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var error by remember{mutableStateOf<String?>(null)}
    var loading by remember{mutableStateOf(true)}
    var page by rememberSaveable(categoryName){mutableIntStateOf(1)}
    var area by rememberSaveable(categoryName){mutableStateOf("0")}
    var year by rememberSaveable(categoryName){mutableStateOf("0")}
    var type by rememberSaveable(categoryName){mutableIntStateOf(0)}
    var initial by rememberSaveable(categoryName){mutableStateOf("0")}
    var membership by rememberSaveable(categoryName){mutableIntStateOf(3)}
    var sort by rememberSaveable(categoryName){mutableStateOf("update")}
    var panel by rememberSaveable{mutableStateOf(false)}
    var retry by remember{mutableIntStateOf(0)}
    var movies by remember{mutableStateOf<List<Movie>>(emptyList())}
    var total by remember{mutableIntStateOf(0)}
    LaunchedEffect(retry){try{categories=vm.api.categories()}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}}
    val category=categories.firstOrNull{it.name==categoryName || (categoryName=="VIP蓝光"&&it.id==6)}
    val filter=Filter(category?.id?:1,area,year,type,initial,membership,sort)
    LaunchedEffect(filter,page,retry,category) {
        if(category==null)return@LaunchedEffect
        loading=true;error=null
        try {
            val result=vm.api.browse(filter,page)
            movies=result.items;total=result.total
        }catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}finally{loading=false}
    }
    Column(Modifier.fillMaxSize().padding(40.dp,12.dp,40.dp,20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            SectionTitle(categoryName,"共 $total 部")
            Spacer(Modifier.weight(1f))
            TvAction("筛选条件",selected=panel){panel=true}
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            item{Text(listOf(area.takeUnless{it=="0"}?:"全部地区",year.takeUnless{it=="0"}?:"全部年份",category?.types?.find{it.first==type}?.second?:"全部类型").joinToString(" · "),color=Muted,fontSize=12.sp,modifier=Modifier.padding(vertical=10.dp))}
            items(listOf("update" to "最近更新","desc" to "最近添加","hot" to "最热","score" to "评分")){(key,label)->TvAction(label,selected=sort==key){page=1;sort=key}}
        }
        when {
            error!=null->ErrorNotice(error!!){retry++}
            loading->Text("正在加载…",color=Muted)
            movies.isEmpty()->Text("当前条件下没有影片",color=Muted)
            else->LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(4.dp,5.dp,4.dp,15.dp)){
                items(movies.chunked(5),key={row->row.first().id}){row->Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){row.forEach{m->PosterCard(m,Modifier.weight(1f),posterRatio=1.15f){open(m)}};repeat(5-row.size){Spacer(Modifier.weight(1f))}}}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){if(page>1)TvAction("上一页"){page--};Text("第 $page / ${((total+19)/20).coerceAtLeast(1)} 页",color=Muted,fontSize=13.sp,modifier=Modifier.padding(12.dp));if(page*20<total&&!loading)TvAction("下一页"){page++}}
    }
    if(panel)Dialog(onDismissRequest={panel=false}){
        Column(Modifier.width(750.dp).heightIn(max=450.dp).background(Panel,RoundedCornerShape(14.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("筛选 · $categoryName",color=White,fontSize=23.sp)
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
                item{OptionRow("地区",listOf("0" to "全部地区")+(category?.areas?:emptyList()).map{it to it},area){page=1;area=it}}
                item{OptionRow("年份",listOf("0" to "全部年份")+(category?.years?:emptyList()).map{it to it},year){page=1;year=it}}
                item{OptionRow("类型",listOf("0" to "全部类型")+(category?.types?:emptyList()).map{it.first.toString() to it.second},type.toString()){page=1;type=it.toInt()}}
                item{OptionRow("首字母",listOf("0" to "全部")+('A'..'Z').map{it.toString() to it.toString()},initial){page=1;initial=it}}
                item{OptionRow("会员",listOf("3" to "全部","1" to "会员","2" to "免费"),membership.toString()){page=1;membership=it.toInt()}}
            }
            Row{TvAction("完成",selected=true){panel=false};TvAction("重置"){page=1;area="0";year="0";type=0;initial="0";membership=3}}
        }
    }
}
@Composable private fun OptionRow(title:String,options:List<Pair<String,String>>,value:String,choose:(String)->Unit){Column{Text(title,color=Muted,fontSize=12.sp);LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){items(options){(key,label)->TvAction(label,selected=key==value){choose(key)}}}}}
@Composable internal fun SectionTitle(title:String,subtitle:String=""){Row{Text(title,color=White,fontSize=23.sp);Spacer(Modifier.width(12.dp));Text(subtitle,color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=9.dp))}}
