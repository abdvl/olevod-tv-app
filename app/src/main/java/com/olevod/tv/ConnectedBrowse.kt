package com.olevod.tv

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.olevod.tv.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    val category=categories.firstOrNull{it.name==categoryName || categoryLabel(it.id)==categoryName}
    val filter=Filter(category?.id?:1,area,year,type,initial,membership,sort)
    val feed=remember(filter,category!=null,vm.sessionVersion){if(category==null)null else vm.catalogFeed(filter)}
    DisposableEffect(feed){onDispose{feed?.cancel()}}
    LaunchedEffect(feed){if(feed!=null&&feed.state.nextPage==1&&feed.state.error==null)feed.loadNext()}
    CatalogPageContent(category?:Category(1,categoryName,emptyList(),emptyList(),emptyList()),categories,filter,
        feed?.state ?: CatalogFeedState(loading=error==null,error=error),open,
        changeFilter={next->if(next!=filter){vm.catalogFeed(next).reset();area=next.area;year=next.year;type=next.type;initial=next.initial;membership=next.membership;sort=next.sort}},
        chooseCategory={id->categories.firstOrNull{it.id==id}?.let{c->
            if(c.id!=filter.category){vm.catalogFeed(Filter(category=c.id)).reset();chooseCategory(c.name)}
        }},loadMore={if(error!=null)retry++ else feed?.loadNext()})
}

@Composable
internal fun CatalogPageContent(category:Category,categories:List<Category>,filter:Filter,result:CatalogFeedState,
                                open:(Movie)->Unit,changeFilter:(Filter)->Unit,chooseCategory:(Int)->Unit,loadMore:()->Unit){
    val list=rememberSaveable(filter,saver=LazyListState.Saver){LazyListState()}
    val triggers=remember{List(5){FocusRequester()}}
    val title=remember{FocusRequester()};val reset=remember{FocusRequester()};val firstRow=remember{List(6){FocusRequester()}}
    var lastTrigger by rememberSaveable{mutableIntStateOf(0)}
    var overlay by remember{mutableStateOf<String?>(null)}
    var overlaySource by remember{mutableIntStateOf(0)}
    var restoreTrigger by remember{mutableIntStateOf(0)}
    val page=LocalPageFocus.current
    val memory=LocalContentFocusMemory.current
    val scope=rememberCoroutineScope()
    val movies=result.items
    val enterResults:(Int)->Unit={column->if(movies.isNotEmpty())scope.launch{list.scrollToItem(0);withFrameNanos{};firstRow[minOf(column,movies.lastIndex)].requestFocus()}}
    DisposableEffect(page){page?.enter={triggers[0].requestFocus()};onDispose{page?.enter=null}}
    LaunchedEffect(Unit){if(memory?.anchor?.value==null){withFrameNanos{};triggers[0].requestFocus()}}
    LaunchedEffect(restoreTrigger){if(restoreTrigger>0){withFrameNanos{};(if(overlaySource<0)title else triggers[overlaySource]).requestFocus()}}
    val close:()->Unit={overlay=null;restoreTrigger++}
    // Updated callbacks/state are read in the scrolling observer; each query has a separate saved anchor.
    val currentResult by rememberUpdatedState(result)
    val currentLoad by rememberUpdatedState(loadMore)
    LaunchedEffect(list,filter,"tail") {snapshotFlow{
        val state=currentResult;val rows=(state.items.size+5)/6
        rows>0&&(list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1)>=rows-1&&!state.loading&&!state.endReached&&state.error==null
    }.distinctUntilChanged().collect{if(it)currentLoad()}}
    Column(Modifier.fillMaxSize().padding(horizontal=36.dp).testTag("catalog-page"),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){
            CatalogTitleButton("${categoryLabel(category.id)}目录",modifier=Modifier.focusRequester(title).restoreContentFocus("category")
                .testTag("catalog-category").focusProperties{up=page?.header?:FocusRequester.Default;down=triggers[0];right=reset}){overlaySource=-1;overlay="category"}
            Text(if(result.total>=0)"${result.total} 部"else"已加载 ${movies.size} 部",color=Muted,fontSize=13.sp,lineHeight=18.sp)
            Spacer(Modifier.weight(1f))
            TvAction("重置筛选",modifier=Modifier.focusRequester(reset).restoreContentFocus("reset").testTag("catalog-reset").focusProperties{left=title;down=triggers[4];up=page?.header?:FocusRequester.Default}){changeFilter(Filter(category=category.id))}
        }
        val labels=listOf("排序：${sortOptions.firstOrNull{it.value==filter.sort}?.label?:"最近更新"}",
            "类型：${category.types.firstOrNull{it.first==filter.type}?.second?:"全部"}","地区：${filter.area.takeUnless{it=="0"}?:"全部"}",
            "年份：${filter.year.takeUnless{it=="0"}?:"全部"}","更多筛选"+listOf(filter.membership!=3,filter.initial!="0").count{it}.takeIf{it>0}?.let{" · $it"}.orEmpty())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            labels.forEachIndexed { index,label->FilterTrigger(label,expanded=overlay!=null&&overlaySource==index,modifier=Modifier.weight(if(index==0)1.25f else 1f)
                .focusRequester(triggers[index]).restoreContentFocus("filter:$index").onFocusChanged{if(it.isFocused)lastTrigger=index}
                .testTag("filter:$index").focusProperties{up=if(index<3)title else reset;left=if(index>0)triggers[index-1]else FocusRequester.Cancel;right=if(index<4)triggers[index+1]else FocusRequester.Cancel}
                .onPreviewKeyEvent { e->if(e.key==androidx.compose.ui.input.key.Key.DirectionDown&&movies.isNotEmpty()){if(e.type==androidx.compose.ui.input.key.KeyEventType.KeyDown)enterResults(index);true}else false }){overlaySource=index;overlay=listOf("sort","type","area","year","more")[index]} }
        }
        val extraFilters=listOf(membershipOptions.firstOrNull{it.value==filter.membership.toString()}?.label?.takeIf{filter.membership!=3}.orEmpty(),filter.initial.takeUnless{it=="0"}.orEmpty()).filter(String::isNotBlank).joinToString(" · ")
        if(extraFilters.isNotBlank())Text(extraFilters,color=Muted,fontSize=13.sp,lineHeight=18.sp)
        PosterFocusGroup("catalog:$filter") {LazyColumn(Modifier.weight(1f).testTag("catalog-results"),state=list,contentPadding=PaddingValues(4.dp,4.dp,4.dp,64.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            itemsIndexed(movies.chunked(6),key={_,row->row.first().id}){rowIndex,row->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                row.forEachIndexed{column,movie->PosterCard(movie,Modifier.weight(1f)
                    .then(if(rowIndex==0)Modifier.focusRequester(firstRow[column])else Modifier)
                    .focusProperties{if(rowIndex==0)up=triggers[lastTrigger];if(column==0)left=FocusRequester.Cancel;if(column==row.lastIndex)right=FocusRequester.Cancel},
                    onFocused={if(rowIndex==(movies.size-1)/6&&!result.loading&&!result.endReached&&result.error==null)loadMore()}){open(movie)}}
                repeat(6-row.size){Spacer(Modifier.weight(1f))}
            }}
            item("state"){
                when{
                    result.error!=null->ErrorNotice(result.error,loadMore)
                    result.loading||result.nextPage==1->Text(if(movies.isEmpty())"正在加载影片…"else"正在加载更多…",color=Muted,fontSize=14.sp)
                    movies.isEmpty()->Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text("当前条件下没有影片",color=Muted);TvAction("重置筛选"){changeFilter(Filter(category=category.id))}}
                    result.endReached->Text("已显示全部影片",color=Muted,fontSize=13.sp)
                }
            }
        }}
    }
    when(overlay){
        "category"->OptionPopover("选择分类",categories.map{FilterOption(it.id.toString(),categoryLabel(it.id))},category.id.toString(),onDismiss=close){chooseCategory(it.toInt());close()}
        "sort"->OptionPopover("排序",sortOptions,filter.sort,onDismiss=close){changeFilter(filter.copy(sort=it));close()}
        "type"->OptionPopover("类型",listOf(FilterOption("0","全部"))+category.types.map{FilterOption(it.first.toString(),it.second)},filter.type.toString(),3,close){changeFilter(filter.copy(type=it.toInt()));close()}
        "area"->OptionPopover("地区",listOf(FilterOption("0","全部"))+category.areas.map{FilterOption(it,it)},filter.area,3,close){changeFilter(filter.copy(area=it));close()}
        "year"->OptionPopover("年份",listOf(FilterOption("0","全部"))+category.years.sortedByDescending{it.toIntOrNull()?:0}.map{FilterOption(it,it)},filter.year,4,close){changeFilter(filter.copy(year=it));close()}
        "more"->MoreFiltersPopover(filter.membership,filter.initial,close){membership,initial->changeFilter(filter.copy(membership=membership,initial=initial));close()}
    }
}

@Composable internal fun SectionTitle(title:String,subtitle:String=""){Row(verticalAlignment=Alignment.CenterVertically){Text(title,color=White,fontSize=28.sp,lineHeight=36.sp);Spacer(Modifier.width(12.dp));Text(subtitle,color=Muted,fontSize=13.sp,lineHeight=18.sp)}}
