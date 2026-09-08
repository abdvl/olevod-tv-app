@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.olevod.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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

internal val LocalPosterFocus=compositionLocalOf<MutableState<Long>?>{null}

@Composable
internal fun PosterFocusGroup(identity:String,content:@Composable ()->Unit){
    val lastPoster=rememberSaveable(identity){mutableLongStateOf(-1)}
    CompositionLocalProvider(LocalPosterFocus provides lastPoster, LocalFocusSection provides identity){content()}
}

data class Movie(val id: Long, val title: String, val image: String, val note: String = "", val score: String = "", val category: Int = 1, val year: String = "", val area: String = "", val vip: Boolean = false)
data class Hero(val id: Long, val title: String, val image: String, val note: String)

@Composable
fun OlevodApp(initialScreen: String = "home", preview: Boolean = false, vm: AppViewModel = viewModel(), initialMovieId:Long?=null,onExit:()->Unit={}) {
    val home by vm.home.collectAsStateWithLifecycle()
    LaunchedEffect(preview){ if(!preview)vm.loadHome() }
    val context = LocalContext.current
    val movies = remember { val a = JSONArray(context.assets.open("preview_movies.json").bufferedReader().readText()); (0 until a.length()).map { i -> val m=a.getJSONObject(i); Movie(m.getLong("id"),m.getString("name"),m.getString("image"),m.optString("remarks"),m.optString("score"),m.optInt("typeId1",1),m.optString("year"),m.optString("area"),m.optBoolean("vip")) } }
    val heroes = remember { val a = JSONArray(context.assets.open("preview_banners.json").bufferedReader().readText()); (0 until a.length()).map { i -> val m=a.getJSONObject(i);Hero(m.getLong("id"),m.getString("title"),m.getString("image"),m.getString("desc")) } }
    val pageStates=rememberSaveableStateHolder()
    var screen by rememberSaveable { mutableStateOf(initialScreen) }
    var category by rememberSaveable { mutableStateOf("电影") }
    var selected by rememberSaveable(stateSaver=androidx.compose.runtime.saveable.Saver<Movie,String>(save={com.olevod.tv.data.MovieJson.encode(it)},restore={com.olevod.tv.data.MovieJson.decode(it)})) { mutableStateOf(initialMovieId?.let{Movie(it,"正在加载影片…","")}?:movies.first()) }
    var backScreen by rememberSaveable { mutableStateOf("home") }
    var browseBack by rememberSaveable{mutableStateOf("home")}
    var browseOriginCategory by rememberSaveable{mutableStateOf("电影")}
    var confirmExit by rememberSaveable{mutableStateOf(false)}
    var full by rememberSaveable { mutableStateOf(false) }
    var routeEpochs by rememberSaveable { mutableStateOf(mapOf<String,Int>()) }
    var launched by rememberSaveable{mutableStateOf(false)}
    var loginOrigin by remember{mutableStateOf<String?>(null)}
    var loginFavorite by remember{mutableStateOf<Boolean?>(null)}
    var historyCloudReturn by rememberSaveable{mutableStateOf(false)}
    LaunchedEffect(screen){if(screen!="player")vm.pendingAuthentication=null}
    val headerTargets=remember { navigationItems.associate { it.key to FocusRequester() } }
    val selectedCategoryId=when(category){"连续剧","电视剧"->2;"综艺"->3;"动漫"->4;"VIP蓝光","VIP蓝光影院","VIP"->6;"短剧"->14;else->1}
    val selectedNavigation=if(screen=="category")categoryNavigationKey(selectedCategoryId)else screen
    val headerFocus=headerTargets[selectedNavigation]?:headerTargets.getValue("home")
    val contentFocus=remember(screen,category){FocusRequester()}
    val pageFocus=remember(screen,category){PageFocusController(headerFocus,contentFocus)}
    val recentFocus=remember{FocusRequester()}
    LaunchedEffect(Unit){if(!launched){launched=true;if(screen=="home"){withFrameNanos{};headerTargets.getValue("home").requestFocus()}}}
    val openCatalog:(String,String)->Unit={origin,targetCategory->
        browseBack=origin;browseOriginCategory=category;category=targetCategory
        val key="browse:$targetCategory";routeEpochs=routeEpochs+(key to ((routeEpochs[key]?:0)+1))
        val id=home.sections.firstOrNull{it.category.name==targetCategory||categoryLabel(it.category.id)==targetCategory}?.category?.id?:selectedCategoryId
        vm.catalogFeed(com.olevod.tv.data.Filter(category=id)).reset();screen="browse"
    }
    val openMovie: (Movie) -> Unit = { selected=it;backScreen=screen;screen="player" }
    val requestLogin:(Boolean?)->Unit={desired->loginOrigin=screen;loginFavorite=desired;full=false;screen="account"}
    BackHandler { if(full)full=false else when(screen){"home"->confirmExit=true;"player"->screen=backScreen;"browse"->{screen=browseBack;if(browseBack=="category")category=browseOriginCategory};"account"->{screen=loginOrigin?:"home";loginOrigin=null;loginFavorite=null};else->screen="home"} }
    CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Green,onPrimary=Bg,surface=Panel,onSurface=White,background=Bg)) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            if(!full) UnifiedHeader(selectedNavigation,headerTargets,{pageFocus.enterContent()}) { target ->
                pageFocus.restoreBody=null
                loginOrigin=null;loginFavorite=null
                if(target=="history")historyCloudReturn=false
                val id=navigationCategoryId(target)
                if(id!=null){category=home.sections.firstOrNull{it.category.id==id}?.category?.name ?: when(id){2->"连续剧";6->"VIP蓝光影院";else->categoryLabel(id)};screen="category"}
                else if(target=="browse"){category="电影";browseBack="home";screen="browse"}
                else screen=target
                val destination=if(screen in listOf("category","browse"))"$screen:$category"else screen
                routeEpochs=routeEpochs+(destination to ((routeEpochs[destination]?:0)+1))
            }
            Box(Modifier.weight(1f).fillMaxWidth().focusRequester(contentFocus).focusGroup()) {
            CompositionLocalProvider(LocalPageFocus provides pageFocus) {
            val routeKey=when(screen){"category","browse"->"$screen:$category";"player"->"player:${selected.id}";else->screen}
            pageStates.SaveableStateProvider("$routeKey:${routeEpochs[routeKey]?:0}:${if(screen in listOf("home","history","favorites","account"))vm.sessionVersion else 0}") {
                ContentFocusScope {
                val lastPoster=rememberSaveable{mutableLongStateOf(-1)}
                CompositionLocalProvider(LocalPosterFocus provides lastPoster){ when(screen) {
                "home" -> ConnectedHome(if(preview)previewHome(movies,heroes)else home,vm,openMovie,{historyCloudReturn=false;routeEpochs=routeEpochs+("history" to ((routeEpochs["history"]?:0)+1));screen="history"},headerTargets.getValue("home"),recentFocus,{pageFocus.enter=it},
                    fixtureRecords=if(preview)previewWatchRecords(movies)else null){openCatalog("home",it)}
                "category" -> {
                    val selectedCategory=(if(preview)previewHome(movies,heroes)else home).sections.firstOrNull{it.category.name==category||it.category.id==selectedCategoryId}?.category
                    if(selectedCategory!=null)key(selectedCategory.id){MiniCategoryHome(selectedCategory,vm,openMovie,{openCatalog("category",category)},headerFocus,{pageFocus.enter=it},fixture=if(preview)movies.take(10) to movies.takeLast(10)else null)}
                    else if(home.error!=null)ErrorNotice(home.error!!){vm.loadHome()}
                    else Text("正在加载分类…",color=Muted)
                }
                "browse" -> if(preview) CatalogPreview(movies,openMovie,selectedCategoryId){category=categoryLabel(it)} else ConnectedBrowse(category,vm,openMovie){category=it}
                "search" -> if(preview) SearchPreviewFixture(vm,movies,openMovie) else ConnectedSearch(vm,openMovie)
                "player" -> if(preview) PlayerPreviewFixture(selected,full,{full=!full}){if(full)full=false else screen=backScreen}
                    else key(vm.sessionVersion){NativePlayer(selected,vm,full,{full=!full},onLogin={requestLogin(it)}){if(full)full=false else screen=backScreen}}
                "live" -> if(preview) LivePreview() else LiveScreen(vm,full){full=!full}
                "history" -> if(!preview) HistoryScreen(vm,openMovie,browse={openCatalog("history","电影")},initialCloud=historyCloudReturn){historyCloudReturn=true;requestLogin(null)}
                    else HistoryPreviewFixture(vm,movies,openMovie,{openCatalog("history","电影")}){requestLogin(null)}
                "favorites" -> if(!preview) FavoritesScreen(vm,openMovie,{requestLogin(null)},browse={openCatalog("favorites","电影")})else FavoritesPreviewFixture(vm,movies,openMovie){openCatalog("favorites","电影")}
                "account" -> if(preview) AccountPreviewFixture(vm) else AccountScreen(vm,onLoggedIn={
                    loginFavorite?.let{desired->if(loginOrigin=="player")vm.pendingFavorite=PendingFavorite(selected.id,desired,vm.sessions.accountKey)}
                    screen=loginOrigin?:"account";loginOrigin=null;loginFavorite=null
                })
            }}}
                }
            }}
        }
        if(confirmExit)ExitConfirmationDialog(onDismiss={confirmExit=false},onConfirm={confirmExit=false;onExit()})
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
internal fun TvAction(label: String, icon: ImageVector? = null, selected: Boolean=false, modifier: Modifier=Modifier, enabled:Boolean=true,onClick:()->Unit) {
    val interaction=remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val color by animateColorAsState(if(focused)Green else if(selected)Color(0xFF263E31) else Color.Transparent,label="focus")
    Row(modifier.semantics{this.selected=selected}.clip(RoundedCornerShape(50)).background(color).border(if(selected&&!focused)1.dp else 0.dp,if(selected&&!focused)Green.copy(alpha=.35f) else Color.Transparent,RoundedCornerShape(50)).clickable(enabled=enabled,interactionSource=interaction,indication=null,onClick=onClick).padding(horizontal=14.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        val tint=if(!enabled)Muted.copy(alpha=.55f)else if(focused)Bg else if(selected)Green else White
        if(icon!=null)Icon(icon,null,Modifier.size(17.dp),tint=tint)
        Text(label,color=tint,fontSize=14.sp,fontWeight=if(focused||selected)FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
internal fun OfficialOlevodLogo(modifier:Modifier=Modifier) {
    androidx.compose.foundation.Image(
        painter=androidx.compose.ui.res.painterResource(R.drawable.official_olevod_logo),
        contentDescription="欧乐影院",modifier=modifier,contentScale=ContentScale.Fit)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun HomeMovieGroup(movies:List<Movie>,open:(Movie)->Unit,title:String="电影",more:()->Unit) {
    PosterFocusGroup("latest:$title") { Column(Modifier.focusGroup().padding(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionHeading(title,"最近更新 · ${movies.size} 部",more)
        movies.chunked(5).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                row.forEach { movie ->PosterCard(movie,Modifier.weight(1f),posterRatio=TvDesign.posterRatio){open(movie)} }
                repeat(5-row.size){Spacer(Modifier.weight(1f))}
            }
        }
    }
}}

@Composable
internal fun HeroCard(hero:Hero,modifier:Modifier,onClick:()->Unit) {
    Card(onClick=onClick,modifier=modifier.restoreContentFocus("hero:${hero.id}").height(138.dp),shape=CardDefaults.shape(RoundedCornerShape(12.dp)),scale=CardDefaults.scale(focusedScale=1f),border=CardDefaults.border(focusedBorder=Border(androidx.compose.foundation.BorderStroke(2.dp,Green)))) {
        Box(Modifier.fillMaxSize().background(Panel)) {
            AsyncImage(hero.image,hero.title,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.8f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(hero.title,color=White,fontSize=28.sp,lineHeight=34.sp,maxLines=2,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Bold)
                Text(hero.note,color=White.copy(alpha=.85f),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun SectionHeading(title:String,subtitle:String="",more:(()->Unit)?=null,moreModifier:Modifier=Modifier) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(4.dp,21.dp).clip(RoundedCornerShape(4.dp)).background(Green))
        Spacer(Modifier.width(10.dp));Text(title,color=White,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.width(12.dp));Text(subtitle,color=Muted,fontSize=13.sp,lineHeight=18.sp)
        Spacer(Modifier.weight(1f));if(more!=null)TvAction("查看全部",Icons.Rounded.ChevronRight,modifier=moreModifier,onClick=more)
    }
}

@Composable
internal fun PosterCard(movie:Movie,modifier:Modifier=Modifier,posterRatio:Float=TvDesign.posterRatio,onFocused:()->Unit={},focusIdentity:Long=movie.id,subtitle:String?=null,onClick:()->Unit) {
    // Legacy call sites keep compiling while pages migrate; artwork always uses the v2 portrait ratio.
    PosterTile(movie,modifier,onFocused,focusIdentity,subtitle,onClick)
}

@Composable private fun FilterLine(values:List<String>,selected:String,choose:(String)->Unit) { LazyRow(horizontalArrangement=Arrangement.spacedBy(3.dp)){items(values){TvAction(it,selected=it==selected){choose(it)}}} }

@Composable internal fun KeyButton(label:String,modifier:Modifier,onClick:()->Unit) { Button(onClick=onClick,modifier=modifier.height(36.dp*maxOf(1f,LocalDensity.current.fontScale)),scale=ButtonDefaults.scale(focusedScale=1f),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.colors(containerColor=Panel,contentColor=White,focusedContainerColor=Green,focusedContentColor=Bg),shape=ButtonDefaults.shape(RoundedCornerShape(7.dp))){Text(label,fontSize=16.sp)} }
@Composable internal fun InputBox(value:String,change:(String)->Unit,hint:String,modifier:Modifier=Modifier,password:Boolean=false,editRequest:Int=0,onEditingFinished:()->Unit={},digitSlots:Boolean=false) {
    val interaction=remember{MutableInteractionSource()}
    val focused by interaction.collectIsFocusedAsState()
    var editing by remember{mutableStateOf(false)}
    val keyboard=LocalSoftwareKeyboardController.current
    LaunchedEffect(editRequest){if(editRequest>0)editing=true}
    Box(modifier.fillMaxWidth().border(1.dp,if(focused)Green else Muted.copy(alpha=.3f),RoundedCornerShape(8.dp)).background(Panel,RoundedCornerShape(8.dp)).clickable(interactionSource=interaction,indication=null){editing=true}.padding(13.dp)) {
        if(digitSlots&&value.codePointCount(0,value.length)<=4)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            val digits=value.codePoints().toArray()
            repeat(4){index->Box(Modifier.weight(1f).height(36.dp).background(Bg,RoundedCornerShape(6.dp)),contentAlignment=Alignment.Center){
                Text(digits.getOrNull(index)?.let{String(Character.toChars(it))}?:"",color=White,fontSize=20.sp,lineHeight=26.sp)
            }}
        }else Text(if(value.isBlank())hint else if(password)"•".repeat(value.length)else value,color=if(value.isBlank())Muted else White,fontSize=15.sp,maxLines=1)
    }
    if(editing)androidx.compose.ui.window.Dialog(onDismissRequest={keyboard?.hide();editing=false;onEditingFinished()}) {
        val input=remember{FocusRequester()}
        Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(12.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(hint,color=White,fontSize=20.sp)
            BasicTextField(value,change,Modifier.fillMaxWidth().focusRequester(input).border(1.dp,Green,RoundedCornerShape(6.dp)).padding(12.dp),textStyle=TextStyle(color=White,fontSize=18.sp),singleLine=true,keyboardOptions=KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Done,keyboardType=if(password)androidx.compose.ui.text.input.KeyboardType.Password else androidx.compose.ui.text.input.KeyboardType.Text),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onDone={keyboard?.hide();editing=false;onEditingFinished()}),cursorBrush=androidx.compose.ui.graphics.SolidColor(Green),visualTransformation=if(password)PasswordVisualTransformation()else VisualTransformation.None)
            TvAction("完成"){keyboard?.hide();editing=false;onEditingFinished()}
        }
        LaunchedEffect(Unit){input.requestFocus();keyboard?.show()}
    }
}

@Composable private fun LivePreview(){var channel by remember{mutableStateOf("央视")};Column(Modifier.fillMaxSize().padding(40.dp,18.dp)){SectionHeading("电视直播","此刻，正在发生");Spacer(Modifier.height(15.dp));FilterLine(listOf("全部频道","央视","地方"),channel){channel=it};Spacer(Modifier.height(25.dp));Row(horizontalArrangement=Arrangement.spacedBy(17.dp)){listOf("CCTV 13" to "新闻","CCTV 6" to "电影","CCTV 5" to "体育").forEach{(logo,name)->Card(onClick={},modifier=Modifier.weight(1f).height(170.dp),colors=CardDefaults.colors(containerColor=Panel),border=CardDefaults.border(focusedBorder=Border(androidx.compose.foundation.BorderStroke(2.dp,Green)))){Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.SpaceBetween){Row(Modifier.fillMaxWidth()){Text(logo,fontSize=26.sp,color=White,fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));Text("LIVE",color=Green,fontSize=10.sp)};Column{Text(name,color=White,fontSize=18.sp);Text("频道预览 · 实时节目待接入",color=Muted,fontSize=11.sp)}}}}}}

}
