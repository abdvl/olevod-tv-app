@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.olevod.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/** Native layout values from docs/design-v2/tokens.json. All dimensions are dp/sp. */
internal object TvDesign {
    val safeHorizontal = 36.dp
    val headerHeight = 64.dp
    val listBottom = 64.dp
    val border = Color(0xFF344143)
    val error = Color(0xFFFF9C96)
    val warning = Color(0xFFE8BC68)
    const val posterRatio = 2f / 3f
}
internal val Bg = Color(0xFF101718)
internal val Panel = Color(0xFF1B2526)
internal val Green = Color(0xFF66E681)
internal val Muted = Color(0xFF99A7A8)
internal val White = Color(0xFFF3F6F5)
internal val Gold = Color(0xFFE2C593)

internal data class NavigationItem(val key: String, val label: String, val icon: ImageVector? = null)
internal val navigationItems = listOf(
    NavigationItem("search", "搜索", Icons.Rounded.Search),
    NavigationItem("home", "首页"), NavigationItem("movie", "电影"),
    NavigationItem("series", "电视剧"), NavigationItem("variety", "综艺"),
    NavigationItem("anime", "动漫"), NavigationItem("vip", "VIP"),
    NavigationItem("short", "短剧"), NavigationItem("browse", "目录", Icons.Rounded.GridView),
    NavigationItem("history", "历史", Icons.Rounded.History),
    NavigationItem("favorites", "收藏", Icons.Rounded.BookmarkBorder),
    NavigationItem("settings", "设置", Icons.Rounded.Settings),
    NavigationItem("account", "账号", Icons.Rounded.AccountCircle)
)
internal fun categoryLabel(id: Int) = when(id) { 1 -> "电影"; 2 -> "电视剧"; 3 -> "综艺"; 4 -> "动漫"; 6 -> "VIP"; 14 -> "短剧"; else -> "影片" }
internal fun categoryNavigationKey(id: Int) = when(id) { 1 -> "movie"; 2 -> "series"; 3 -> "variety"; 4 -> "anime"; 6 -> "vip"; 14 -> "short"; else -> "home" }
internal fun navigationCategoryId(key: String): Int? = when(key) { "movie" -> 1; "series" -> 2; "variety" -> 3; "anime" -> 4; "vip" -> 6; "short" -> 14; else -> null }

/** A page explicitly registers its entry target; the header never points at a disposed row. */
internal class PageFocusController(val header: FocusRequester, val fallback: FocusRequester) {
    var enter: (() -> Unit)? = null
    var restoreBody: (() -> Boolean)? = null
    var forceEntry=false
    fun enterContent() {
        if(!forceEntry && restoreBody?.invoke()==true)return
        enter?.invoke() ?: fallback.requestFocus()
    }
}
internal val LocalPageFocus = compositionLocalOf<PageFocusController?> { null }

@Composable
internal fun UnifiedHeader(selected: String, requesters: Map<String, FocusRequester>,
                           onDown: () -> Unit, onSelect: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(TvDesign.headerHeight)
        .padding(start=TvDesign.safeHorizontal,end=TvDesign.safeHorizontal,top=12.dp).testTag("unified-header")) {
    val availableWidth=maxWidth
    Row(Modifier.horizontalScroll(rememberScrollState())) {
    Row(Modifier.width(maxOf(availableWidth, 785.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale + 84.dp)).height(52.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OfficialOlevodLogo(Modifier.width(120.dp).aspectRatio(364f / 64f))
        Spacer(Modifier.width(8.dp))
        navigationItems.forEachIndexed { index, item ->
            if(item.key=="browse")Spacer(Modifier.weight(1f))
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val width by animateDpAsState(if(item.icon != null && focused && item.key != "settings") 76.dp else 36.dp, tween(120), label="header-width")
            val tint = if(focused) Bg else if(selected == item.key) Green else White
            Column(Modifier.then(if(item.icon != null) Modifier.width(width) else Modifier)
                .height(40.dp).focusRequester(requesters.getValue(item.key))
                .focusProperties {
                    left = if(index > 0) requesters.getValue(navigationItems[index-1].key) else FocusRequester.Cancel
                    right = if(index < navigationItems.lastIndex) requesters.getValue(navigationItems[index+1].key) else FocusRequester.Cancel
                    up = FocusRequester.Cancel
                }
                .onPreviewKeyEvent { event ->
                    if(event.key == Key.DirectionDown) { if(event.type == KeyEventType.KeyDown) onDown(); true } else false
                }
                .testTag("nav:${item.key}").semantics { contentDescription=item.label; this.selected=selected==item.key }
                .clip(RoundedCornerShape(24.dp)).background(if(focused)Green else Color.Transparent)
                .clickable(interactionSource=interaction, indication=null) { onSelect(item.key) }
                .padding(horizontal=if(item.icon == null) 8.dp else 4.dp),
                horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    item.icon?.let { Icon(it,null,Modifier.size(22.dp),tint=tint) }
                    if(item.icon == null || (focused && item.key != "settings")) Text(item.label,color=tint,fontSize=16.sp,lineHeight=22.sp,fontWeight=FontWeight.Medium,maxLines=1)
                }
                Box(Modifier.padding(top=2.dp).width(20.dp).height(2.dp)
                    .background(if(selected==item.key && !focused)Green else Color.Transparent))
            }
        }
    }
}
}
}

@Composable
internal fun PosterArtwork(movie: Movie, modifier: Modifier = Modifier) {
    var failed by remember(movie.image) { mutableStateOf(movie.image.isBlank()) }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Panel), contentAlignment=Alignment.Center) {
        AsyncImage(movie.image, null, Modifier.fillMaxSize().testTag("artwork:${movie.id}"),
            contentScale=ContentScale.Fit, onSuccess={failed=false}, onError={failed=true})
        if(failed) Icon(Icons.Rounded.ImageNotSupported, null, Modifier.size(28.dp), tint=Muted)
        if(movie.score.isNotBlank()) Text(movie.score,color=Green,fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.align(Alignment.TopEnd).padding(6.dp).testTag("poster-score:${movie.id}")
                .clearAndSetSemantics{contentDescription="评分 ${movie.score}"}
                .background(Color.Black.copy(alpha=.82f),RoundedCornerShape(6.dp)).padding(horizontal=6.dp,vertical=3.dp))
    }
}

@Composable
internal fun PosterTile(movie: Movie, modifier: Modifier=Modifier, onFocused:()->Unit={},
                        focusIdentity:Long=movie.id, subtitle:String?=null, onClick:()->Unit) {
    val memory = LocalPosterFocus.current
    val focus = remember { FocusRequester() }
    val bring = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val titleHeight = with(LocalDensity.current) { 38.sp.toDp() }
    val focusOutset = with(LocalDensity.current) { 4.dp.toPx() }
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    Column(modifier.bringIntoViewRequester(bring).onSizeChanged{measuredSize=it}.restoreContentFocus(focusIdentity.toString(),focus)
        .onFocusChanged { if(it.isFocused) { memory?.value=focusIdentity; onFocused(); scope.launch { withFrameNanos{}; bring.bringIntoView(Rect(-focusOutset,-focusOutset,
                measuredSize.width+focusOutset,measuredSize.height+focusOutset)) } } }
        .testTag("poster:${movie.id}").semantics(mergeDescendants=true) { contentDescription=movie.title }
        .border(2.dp,if(focused)Green else Color.Transparent,RoundedCornerShape(10.dp))
        .clickable(interactionSource=interaction,indication=null,onClick=onClick).padding(4.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)) {
        PosterArtwork(movie,Modifier.fillMaxWidth().aspectRatio(TvDesign.posterRatio))
        Box(Modifier.fillMaxWidth().height(titleHeight),contentAlignment=Alignment.CenterStart) {
            Text(movie.title,color=White,fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Medium,
                maxLines=if(focused)1 else 2,overflow=TextOverflow.Ellipsis,
                modifier=if(focused)Modifier.basicMarquee(iterations=1,initialDelayMillis=800)else Modifier)
        }
        Text(subtitle ?: listOf(movie.year,movie.area,movie.note).filter(String::isNotBlank).joinToString(" · "),
            color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
