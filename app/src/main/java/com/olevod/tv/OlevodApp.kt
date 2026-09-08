@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.olevod.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import org.json.JSONArray
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal val Bg = Color(0xFF101718)
internal val Panel = Color(0xFF1B2526)
internal val Green = Color(0xFF66E681)
internal val Muted = Color(0xFF99A7A8)
internal val White = Color(0xFFF3F6F5)
internal val Gold = Color(0xFFE2C593)

data class Movie(val id: Long, val title: String, val image: String, val note: String = "", val score: String = "", val category: Int = 1, val year: String = "", val area: String = "", val vip: Boolean = false)
data class Hero(val id: Long, val title: String, val image: String, val note: String)

@Composable
fun OlevodApp(initialScreen: String = "home", preview: Boolean = false, vm: AppViewModel = viewModel()) {
    val home by vm.home.collectAsStateWithLifecycle()
    LaunchedEffect(preview){ if(!preview)vm.loadHome() }
    val context = LocalContext.current
    val movies = remember { val a = JSONArray(context.assets.open("preview_movies.json").bufferedReader().readText()); (0 until a.length()).map { i -> val m=a.getJSONObject(i); Movie(m.getLong("id"),m.getString("name"),m.getString("image"),m.optString("remarks"),m.optString("score"),m.optInt("typeId1",1),m.optString("year"),m.optString("area"),m.optBoolean("vip")) } }
    val heroes = remember { val a = JSONArray(context.assets.open("preview_banners.json").bufferedReader().readText()); (0 until a.length()).map { i -> val m=a.getJSONObject(i);Hero(m.getLong("id"),m.getString("title"),m.getString("image"),m.getString("desc")) } }
    val pageStates=rememberSaveableStateHolder()
    var screen by rememberSaveable { mutableStateOf(initialScreen) }
    var category by rememberSaveable { mutableStateOf("电影") }
    var selected by remember { mutableStateOf(movies.first()) }
    var backScreen by rememberSaveable { mutableStateOf("home") }
    var full by rememberSaveable { mutableStateOf(false) }
    val openMovie: (Movie) -> Unit = { selected=it;backScreen=screen;screen="player" }
    BackHandler(screen!="home" || full) { if(full)full=false else screen=if(screen=="player")backScreen else "home" }
    CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Green,onPrimary=Bg,surface=Panel,onSurface=White,background=Bg)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF192425),Bg)))) {
            if(!full) Header(screen,if(preview || screen !in listOf("home","browse","search","player","account","live"))"界面预览" else "实时内容",{screen=it})
            if(!full && screen in listOf("home","browse","live")) Navigation(category=if(screen=="home")"首页" else if(screen=="live")"直播" else category) { label -> if(label=="首页") screen="home" else if(label=="直播") screen="live" else {category=label;screen="browse"} }
            pageStates.SaveableStateProvider(screen) { when(screen) {
                "home" -> if(preview) HomeScreen(movies,heroes,openMovie,{category=it;screen="browse"}) else ConnectedHome(home,vm,openMovie){category=it;screen="browse"}
                "browse" -> if(preview) BrowseScreen(category,movies,openMovie) else ConnectedBrowse(category,vm,openMovie)
                "search" -> if(preview) SearchScreen(movies,openMovie) else ConnectedSearch(vm,openMovie)
                "player" -> if(preview) PlayerPreview(selected,movies,full,{full=!full},openMovie) else NativePlayer(selected,vm,full){full=!full}
                "live" -> if(preview) LivePreview() else LiveScreen(vm,full){full=!full}
                "history" -> EmptyCollection("观看历史","从上次的精彩，继续看下去","开始播放后，此设备的观看记录会出现在这里",Icons.Rounded.History){screen="home"}
                "favorites" -> EmptyCollection("我的收藏","把喜欢的故事留在这里","登录后可同步欧乐账号的收藏",Icons.Rounded.BookmarkBorder){screen="account"}
                "account" -> if(preview) AccountPreview() else AccountScreen(vm)
            }}
        }
    }
}

}

internal object EdgeBringIntoViewSpec: BringIntoViewSpec {
    override fun calculateScrollDistance(offset:Float,size:Float,containerSize:Float):Float = when {
        offset<0f -> offset
        offset+size>containerSize -> offset+size-containerSize
        else -> 0f
    }
}

@Composable
internal fun TvAction(label: String, icon: ImageVector? = null, selected: Boolean=false, modifier: Modifier=Modifier, onClick:()->Unit) {
    val interaction=remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val color by animateColorAsState(if(focused)Green else if(selected)Color(0xFF263E31) else Color.Transparent,label="focus")
    Row(modifier.clip(RoundedCornerShape(50)).background(color).border(if(selected&&!focused)1.dp else 0.dp,if(selected&&!focused)Green.copy(alpha=.35f) else Color.Transparent,RoundedCornerShape(50)).clickable(interactionSource=interaction,indication=null,onClick=onClick).padding(horizontal=14.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        if(icon!=null)Icon(icon,null,Modifier.size(17.dp),tint=if(focused)Bg else if(selected)Green else White)
        Text(label,color=if(focused)Bg else if(selected)Green else White,fontSize=14.sp,fontWeight=if(focused||selected)FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun Header(screen:String,status:String,go:(String)->Unit) {
    Row(Modifier.fillMaxWidth().padding(start=38.dp,end=38.dp,top=19.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
        TvAction("搜索",Icons.Rounded.Search,screen=="search"){go("search")}
        TvAction("历史",Icons.Rounded.History,screen=="history"){go("history")}
        TvAction("收藏",Icons.Rounded.BookmarkBorder,screen=="favorites"){go("favorites")}
        Spacer(Modifier.weight(1f))
        Text(status,fontSize=10.sp,color=Muted,modifier=Modifier.border(1.dp,Muted.copy(alpha=.3f),RoundedCornerShape(4.dp)).padding(horizontal=6.dp,vertical=3.dp))
        Spacer(Modifier.width(18.dp))
        Text("OLE",color=White,fontSize=22.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)
        Text(" TV",color=Green,fontSize=22.sp,fontWeight=FontWeight.Black)
        Spacer(Modifier.width(18.dp))
        TvAction("登录",Icons.Rounded.AccountCircle,screen=="account"){go("account")}
    }
}

@Composable
private fun Navigation(category:String,onSelect:(String)->Unit) {
    LazyRow(Modifier.fillMaxWidth().padding(horizontal=38.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        items(listOf("首页","直播","短剧","电影","连续剧","综艺","动漫","VIP蓝光")) { tab -> TvAction(tab,selected=tab==category){onSelect(tab)} }
    }
}

@Composable
private fun HomeScreen(movies:List<Movie>,heroes:List<Hero>,open:(Movie)->Unit,more:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=40.dp,end=40.dp,top=14.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
        item { Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) { heroes.take(2).forEachIndexed { i,h -> HeroCard(h,Modifier.weight(if(i==0)1.6f else 1f)){open(Movie(h.id,h.title,h.image,h.note,category=2))} } } }
        item { HomeMovieGroup(movies.take(10),open){more("电影")} }
        item { SectionHeading("连续剧", "最新剧集，一眼找到",{more("连续剧")}) }
        item { Text("预览展示公开影片样本；实时分类与账号功能正在接入。",color=Muted,fontSize=13.sp) }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun HomeMovieGroup(movies:List<Movie>,open:(Movie)->Unit,title:String="电影",more:()->Unit) {
    val requester=remember { BringIntoViewRequester() }
    val scope=rememberCoroutineScope()
    Column(Modifier.bringIntoViewRequester(requester).onFocusChanged { if(it.hasFocus)scope.launch{requester.bringIntoView()} }.focusGroup().padding(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionHeading(title,"最近更新 · ${movies.size} 部",more)
        movies.chunked(5).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                row.forEach { movie ->PosterCard(movie,Modifier.weight(1f),posterRatio=1.5f,onFocused={scope.launch{kotlinx.coroutines.delay(200);requester.bringIntoView()}}){open(movie)} }
                repeat(5-row.size){Spacer(Modifier.weight(1f))}
            }
        }
    }
}

@Composable
internal fun HeroCard(hero:Hero,modifier:Modifier,onClick:()->Unit) {
    Card(onClick=onClick,modifier=modifier.height(190.dp),shape=CardDefaults.shape(RoundedCornerShape(12.dp)),scale=CardDefaults.scale(focusedScale=1.015f),border=CardDefaults.border(focusedBorder=Border(androidx.compose.foundation.BorderStroke(2.dp,Green)))) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(hero.image,hero.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.8f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text("精选推荐",color=Green,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                Text(hero.title,color=White,fontSize=27.sp,fontWeight=FontWeight.Bold)
                Text(hero.note+"   ·   进入观看  ›",color=White.copy(alpha=.8f),fontSize=12.sp)
            }
        }
    }
}

@Composable
internal fun SectionHeading(title:String,subtitle:String="",more:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(4.dp,21.dp).clip(RoundedCornerShape(4.dp)).background(Green))
        Spacer(Modifier.width(10.dp));Text(title,color=White,fontSize=21.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.width(12.dp));Text(subtitle,color=Muted,fontSize=12.sp)
        Spacer(Modifier.weight(1f));if(more!=null)TvAction("查看全部",Icons.Rounded.ChevronRight,onClick=more)
    }
}

@Composable
internal fun PosterCard(movie:Movie,modifier:Modifier=Modifier,posterRatio:Float=.72f,onFocused:()->Unit={},onClick:()->Unit) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Card(onClick=onClick,modifier=Modifier.onFocusChanged{if(it.isFocused)onFocused()}.fillMaxWidth().aspectRatio(posterRatio),shape=CardDefaults.shape(RoundedCornerShape(9.dp)),scale=CardDefaults.scale(focusedScale=1.035f),border=CardDefaults.border(focusedBorder=Border(androidx.compose.foundation.BorderStroke(2.dp,Green)))) {
            Box(Modifier.fillMaxSize().background(Panel)) {
                AsyncImage(movie.image,movie.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.8f)))))
                if(movie.vip)Text("VIP",fontSize=10.sp,color=Bg,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.TopEnd).padding(7.dp).clip(RoundedCornerShape(3.dp)).background(Gold).padding(horizontal=5.dp,vertical=2.dp))
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.Bottom) {
                    Text(movie.note,color=White,fontSize=11.sp,maxLines=1,modifier=Modifier.weight(1f),overflow=TextOverflow.Ellipsis)
                    if(movie.score.isNotBlank())Text(movie.score,color=Green,fontWeight=FontWeight.Bold,fontSize=15.sp)
                }
            }
        }
        Text(movie.title,color=White,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(listOf(movie.year,movie.area).filter{it.isNotBlank()}.joinToString(" · "),color=Muted,fontSize=11.sp,maxLines=1)
    }
}

@Composable
private fun BrowseScreen(category:String,movies:List<Movie>,open:(Movie)->Unit) {
    var area by rememberSaveable(category){mutableStateOf("全部地区")}
    var year by rememberSaveable(category){mutableStateOf("全部年份")}
    var type by rememberSaveable(category){mutableStateOf("全部类型")}
    var sort by rememberSaveable(category){mutableStateOf("最近更新")}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(40.dp,12.dp,40.dp,30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel.copy(alpha=.65f)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            FilterLine(listOf("全部地区","大陆","香港","台湾","美国","韩国","日本","更多"),area){area=it}
            FilterLine(listOf("全部年份","2026","2025","2024","2023","2022","更早"),year){year=it}
            FilterLine(listOf("全部类型","动作","喜剧","爱情","科幻","悬疑","纪录片"),type){type=it}
        } }
        item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(category,color=White,fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(12.dp));Text("筛选布局预览",color=Muted,fontSize=12.sp);Spacer(Modifier.weight(1f));listOf("最近更新","最热","评分").forEach { s ->TvAction(s,selected=s==sort){sort=s} } } }
        items(movies.take(10).chunked(5)){row ->Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){row.forEach{m->PosterCard(m,Modifier.weight(1f)){open(m)}};repeat(5-row.size){Spacer(Modifier.weight(1f))}}}
    }
}
@Composable private fun FilterLine(values:List<String>,selected:String,choose:(String)->Unit) { LazyRow(horizontalArrangement=Arrangement.spacedBy(3.dp)){items(values){TvAction(it,selected=it==selected){choose(it)}}} }

@Composable
private fun SearchScreen(movies:List<Movie>,open:(Movie)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val input=remember { FocusRequester() }
    val keyboard=LocalSoftwareKeyboardController.current
    Row(Modifier.fillMaxSize().padding(40.dp,12.dp,40.dp,20.dp),horizontalArrangement=Arrangement.spacedBy(30.dp)) {
        Column(Modifier.width(254.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("发现想看的故事",color=White,fontSize=22.sp,fontWeight=FontWeight.Bold)
            InputBox(query,{query=it},"输入片名 / 演员",Modifier.focusRequester(input))
            Row { TvAction("清空",Icons.Rounded.Close){query=""};TvAction("退格",Icons.Rounded.Backspace){query=query.dropLast(1)} }
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)) { "abcdefghijklmnopqrstuvwxyz1234567890".chunked(6).forEach { line ->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){line.forEach{c->KeyButton(c.toString(),Modifier.weight(1f)){query+=c}}} } }
            TvAction("中文 / 语音输入",Icons.Rounded.Keyboard){input.requestFocus();keyboard?.show()}
            Text("支持系统输入法与手机遥控输入",fontSize=11.sp,color=Muted)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(White.copy(alpha=.08f)))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            SectionHeading(if(query.isBlank())"大家都在看" else "搜索结果",if(query.isBlank())"热门推荐" else "预览样本匹配")
            if(query.isBlank()) {
                listOf("流浪地球" to "凡人修仙传","早春晴朗" to "斗破苍穹","披荆斩棘2026" to "花开锦绣").forEachIndexed { i,pair ->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)){TvAction("0${i*2+1}  ${pair.first}",modifier=Modifier.weight(1f)){query=pair.first};TvAction("0${i*2+2}  ${pair.second}",modifier=Modifier.weight(1f)){query=pair.second}} }
                Text("值得一看",color=Muted,fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){movies.take(3).forEach{m->PosterCard(m,Modifier.weight(1f),posterRatio=1.1f){open(m)}}}
            } else {
                val found=movies.filter{it.title.contains(query,true)}
                if(found.isEmpty())Text("预览样本中暂无匹配，正式搜索接入中",color=Muted,fontSize=15.sp)
                else Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){found.take(3).forEach{m->PosterCard(m,Modifier.weight(1f),posterRatio=1.1f){open(m)}};repeat(3-found.take(3).size){Spacer(Modifier.weight(1f))}}
            }
        }
    }
}
@Composable internal fun KeyButton(label:String,modifier:Modifier,onClick:()->Unit) { Button(onClick=onClick,modifier=modifier.height(30.dp),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.colors(containerColor=Panel,contentColor=White,focusedContainerColor=Green,focusedContentColor=Bg),shape=ButtonDefaults.shape(RoundedCornerShape(7.dp))){Text(label,fontSize=16.sp)} }
@Composable internal fun InputBox(value:String,change:(String)->Unit,hint:String,modifier:Modifier=Modifier,password:Boolean=false) {
    val interaction=remember{MutableInteractionSource()};val focused by interaction.collectIsFocusedAsState()
    val keyboard=LocalSoftwareKeyboardController.current
    val focus=LocalFocusManager.current
    val imeOpen=WindowInsets.ime.getBottom(LocalDensity.current)>0
    BasicTextField(value,change,modifier.onPreviewKeyEvent {
        when {
            !imeOpen && (it.key==Key.DirectionDown || it.key==Key.DirectionUp) -> {
                if(it.type==KeyEventType.KeyDown)focus.moveFocus(if(it.key==Key.DirectionDown)FocusDirection.Down else FocusDirection.Up)
                true
            }
            it.type==KeyEventType.KeyUp && (it.key==Key.DirectionCenter || it.key==Key.Enter)->{keyboard?.show();true}
            else->false
        }
    }.fillMaxWidth().border(1.dp,if(focused)Green else Muted.copy(alpha=.3f),RoundedCornerShape(8.dp)).background(Panel,RoundedCornerShape(8.dp)).padding(13.dp),textStyle=TextStyle(color=White,fontSize=15.sp),singleLine=true,keyboardOptions=KeyboardOptions(showKeyboardOnFocus=false),interactionSource=interaction,cursorBrush=androidx.compose.ui.graphics.SolidColor(Green),visualTransformation=if(password)PasswordVisualTransformation() else VisualTransformation.None,decorationBox={inner->Box{if(value.isEmpty())Text(hint,color=Muted,fontSize=15.sp);inner()}})
}

@Composable
private fun PlayerPreview(movie:Movie,movies:List<Movie>,full:Boolean,toggleFull:()->Unit,open:(Movie)->Unit) {
    var paused by remember{mutableStateOf(true)}
    var speed by remember{mutableStateOf("1.0×")}
    var position by remember{mutableIntStateOf(0)}
    Row(Modifier.fillMaxSize().padding(if(full)0.dp else 40.dp,if(full)0.dp else 18.dp,if(full)0.dp else 40.dp,if(full)24.dp else 25.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.fillMaxWidth().then(if(full)Modifier.weight(1f) else Modifier.aspectRatio(16f/9)).clip(RoundedCornerShape(if(full)0.dp else 10.dp)).background(Color.Black),contentAlignment=Alignment.Center) {
                AsyncImage(movie.image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alpha=.23f)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha=.35f),Color.Transparent,Color.Black.copy(alpha=.35f)))))
                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Rounded.PlayCircleOutline,null,Modifier.size(58.dp),tint=Green)
                    Text(movie.title,color=White,fontSize=24.sp,fontWeight=FontWeight.Bold)
                    Text("播放器布局预览 · 尚未加载视频",color=White.copy(alpha=.7f),fontSize=12.sp)
                }
                Text("OLE TV",color=White.copy(alpha=.7f),fontSize=11.sp,modifier=Modifier.align(Alignment.TopStart).padding(17.dp))
            }
            Column(Modifier.padding(horizontal=if(full)30.dp else 0.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(White.copy(alpha=.16f))){Box(Modifier.fillMaxWidth(.18f).fillMaxHeight().background(Green))}
                Row(Modifier.fillMaxWidth()){Text("${position/60}:${(position%60).toString().padStart(2,'0')}",color=Muted,fontSize=11.sp);Spacer(Modifier.weight(1f));Text("预览",color=Muted,fontSize=11.sp)}
                LazyRow(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                    item{TvAction("30秒",Icons.Rounded.Replay30){position=(position-30).coerceAtLeast(0)}}
                    item{TvAction(if(paused)"播放" else "暂停",if(paused)Icons.Rounded.PlayArrow else Icons.Rounded.Pause,selected=true){paused=!paused}}
                    item{TvAction("30秒",Icons.Rounded.Forward30){position+=30}}
                    item{TvAction(speed){speed=if(speed=="1.0×")"1.5×" else "1.0×"}}
                    item{TvAction(if(full)"退出全屏" else "全屏",Icons.Rounded.Fullscreen,onClick=toggleFull)}
                    item{TvAction("收藏",Icons.Rounded.BookmarkBorder){}}
                }
            }
        }
        if(!full)Column(Modifier.width(260.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(movie.title,color=White,fontSize=25.sp,fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Text(movie.score.ifBlank{"精选"},color=Green,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("${movie.year} · ${movie.area}",color=Muted,fontSize=12.sp)}
            Text("${movie.note}  ·  ${if(movie.vip)"VIP 蓝光" else "电影"}",color=Gold,fontSize=12.sp)
            Text("放慢脚步，沉浸在好故事里。影片简介、演职员与播放线路将在详情接口接入后显示。",color=Muted,fontSize=13.sp,lineHeight=22.sp)
            Text("选集",color=White,fontSize=17.sp,fontWeight=FontWeight.Bold)
            TvAction("正片",Icons.Rounded.PlayArrow,selected=true){}
            Text("你可能还喜欢",color=White,fontSize=17.sp,fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){movies.takeLast(2).forEach{m->PosterCard(m,Modifier.weight(1f)){open(m)}}}
        }
    }
}

@Composable private fun EmptyCollection(title:String,headline:String,subtitle:String,icon:ImageVector,go:()->Unit){Column(Modifier.fillMaxSize().padding(40.dp,25.dp)){SectionHeading(title);Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(icon,null,Modifier.size(60.dp),tint=Green.copy(alpha=.6f));Spacer(Modifier.height(20.dp));Text(headline,fontSize=24.sp,color=White);Spacer(Modifier.height(9.dp));Text(subtitle,fontSize=14.sp,color=Muted);Spacer(Modifier.height(25.dp));TvAction("去发现精彩",Icons.Rounded.ArrowForward,selected=true,onClick=go)}}}

@Composable private fun AccountPreview(){var user by remember{mutableStateOf("")};var pwd by remember{mutableStateOf("")};var captcha by remember{mutableStateOf("")};var message by remember{mutableStateOf("")};Row(Modifier.fillMaxSize().padding(65.dp,25.dp),horizontalArrangement=Arrangement.spacedBy(90.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(17.dp)){Text("欢迎回到",color=Muted,fontSize=24.sp);Text("你的私人影院",color=White,fontSize=39.sp,fontWeight=FontWeight.Bold);Box(Modifier.size(44.dp,4.dp).background(Green));Text("同步收藏，继续精彩。\n在大屏上，找到喜欢的每一个故事。",color=Muted,fontSize=16.sp,lineHeight=28.sp);Text("使用欧乐影院账号登录",color=Green,fontSize=13.sp)};Column(Modifier.width(320.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(25.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("账号登录",color=White,fontSize=23.sp,fontWeight=FontWeight.Bold);InputBox(user,{user=it},"账号 / 邮箱");InputBox(pwd,{pwd=it},"密码",password=true);InputBox(captcha,{captcha=it},"图片验证码");Text("验证码区域 · 登录接口接入中",color=Muted,fontSize=12.sp);TvAction("登录",Icons.Rounded.ArrowForward,selected=true,modifier=Modifier.fillMaxWidth()){message="当前为界面预览，尚未提交账号"};if(message.isNotEmpty())Text(message,color=Gold,fontSize=11.sp);Text("密码不会保存到本机",color=Muted,fontSize=11.sp)}}}

@Composable private fun LivePreview(){var channel by remember{mutableStateOf("央视")};Column(Modifier.fillMaxSize().padding(40.dp,18.dp)){SectionHeading("电视直播","此刻，正在发生");Spacer(Modifier.height(15.dp));FilterLine(listOf("全部频道","央视","地方"),channel){channel=it};Spacer(Modifier.height(25.dp));Row(horizontalArrangement=Arrangement.spacedBy(17.dp)){listOf("CCTV 13" to "新闻","CCTV 6" to "电影","CCTV 5" to "体育").forEach{(logo,name)->Card(onClick={},modifier=Modifier.weight(1f).height(170.dp),colors=CardDefaults.colors(containerColor=Panel),border=CardDefaults.border(focusedBorder=Border(androidx.compose.foundation.BorderStroke(2.dp,Green)))){Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.SpaceBetween){Row(Modifier.fillMaxWidth()){Text(logo,fontSize=26.sp,color=White,fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));Text("LIVE",color=Green,fontSize=10.sp)};Column{Text(name,color=White,fontSize=18.sp);Text("频道预览 · 实时节目待接入",color=Muted,fontSize=11.sp)}}}}}}

}
