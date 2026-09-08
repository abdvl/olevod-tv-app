package com.olevod.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.material.icons.rounded.DeleteOutline
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.olevod.tv.data.OlevodApi
import com.olevod.tv.data.WatchRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** A visible-row cache: incomplete cloud records do not cause a request for every history page. */
internal class HistoryMetadataLoader(private val api:OlevodApi) {
    private val permits=Semaphore(3)
    private val cache=mutableMapOf<Long,Movie>()
    suspend fun complete(movie:Movie):Movie {
        if(movie.image.isNotBlank() && movie.year.isNotBlank() && movie.area.isNotBlank() && movie.score.isNotBlank())return movie
        val detail=cache[movie.id]?:permits.withPermit {
            cache[movie.id]?:api.detail(movie.id).movie.also{cache[movie.id]=it}
        }
        return movie.copy(
            title=movie.title.takeUnless{it.isBlank()||it=="未知影片"}?:detail.title,
            image=movie.image.ifBlank{detail.image},year=movie.year.ifBlank{detail.year},
            area=movie.area.ifBlank{detail.area},score=movie.score.ifBlank{detail.score},
            note=movie.note.ifBlank{detail.note})
    }
}

@Composable
internal fun rememberHistoryMetadata(vm:AppViewModel):HistoryMetadataLoader {
    val token=vm.sessions.token
    return remember(vm.sessionVersion){HistoryMetadataLoader(OlevodApi(token={token}))}
}

@Composable
internal fun HistoryRecordCard(record:WatchRecord,metadata:HistoryMetadataLoader?,modifier:Modifier=Modifier,
                               resumeModifier:Modifier=Modifier,deleteModifier:Modifier=Modifier,onDelete:(()->Unit)?=null,
                               showUpdated:Boolean=true,onOpen:(Movie)->Unit) {
    var movie by remember(record.movie,metadata){mutableStateOf(record.movie)}
    LaunchedEffect(record.movie,metadata){if(metadata!=null)try{movie=metadata.complete(record.movie)}catch(e:Exception){if(e is CancellationException)throw e}}
    val source=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}
    val focused by source.collectIsFocusedAsState()
    val height=164.dp*maxOf(1f,androidx.compose.ui.platform.LocalDensity.current.fontScale)
    val bring=remember{androidx.compose.foundation.relocation.BringIntoViewRequester()}
    val scope=rememberCoroutineScope()
    Row(modifier.fillMaxWidth().height(height).bringIntoViewRequester(bring).background(Panel,RoundedCornerShape(10.dp))
        .border(2.dp,if(focused)Green else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(10.dp)).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Row(resumeModifier.weight(1f).fillMaxHeight().restoreContentFocus("${record.movie.id}:resume").testTag("history-resume:${record.movie.id}")
            .onFocusChanged{if(it.isFocused)scope.launch{withFrameNanos{};bring.bringIntoView()}}
            .semantics(mergeDescendants=true){contentDescription="${movie.title}，${resumeLabel(record)}，${resumeActionLabel(record)}"}
            .clickable(interactionSource=source,indication=null){onOpen(movie)},horizontalArrangement=Arrangement.spacedBy(12.dp)){
            PosterArtwork(movie,Modifier.width(96.dp).fillMaxHeight())
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(movie.title,color=White,fontSize=18.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(listOf(movie.year,movie.area,movie.score.takeIf(String::isNotBlank)?.let{"评分 $it"}.orEmpty()).filter(String::isNotBlank).joinToString(" · "),color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(movie.note.isNotBlank())Text(movie.note,color=Muted,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(resumeLabel(record),color=Green,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                WatchProgress(record)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(resumeActionLabel(record),color=Green,fontSize=13.sp,lineHeight=18.sp)
                    if(showUpdated&&record.updatedAt>0){Spacer(Modifier.weight(1f));Text(watchDateLabel(record.updatedAt),color=Muted,fontSize=12.sp,lineHeight=16.sp)}
                }
            }
        }
        if(onDelete!=null)Column(Modifier.fillMaxHeight(),verticalArrangement=Arrangement.Bottom){
            HistoryDeleteButton(modifier=deleteModifier.width(36.dp).restoreContentFocus("${record.movie.id}:delete")
                .testTag("history-delete:${record.movie.id}").semantics{contentDescription="删除 ${movie.title} 的此设备记录"}
                .onFocusChanged{if(it.isFocused)scope.launch{withFrameNanos{};bring.bringIntoView()}},onClick=onDelete)
        }
    }
}

internal fun watchDateLabel(updatedAt:Long,now:Long=System.currentTimeMillis(),zone:java.time.ZoneId=java.time.ZoneId.systemDefault()):String {
    val today=java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val date=java.time.Instant.ofEpochMilli(updatedAt).atZone(zone).toLocalDate()
    return when{now>=updatedAt&&now-updatedAt<60000->"刚刚";date==today->"今天";date==today.minusDays(1)->"昨天";else->date.toString()}
}

@Composable
private fun HistoryDeleteButton(modifier:Modifier,onClick:()->Unit){
    val source=remember{androidx.compose.foundation.interaction.MutableInteractionSource()};val focused by source.collectIsFocusedAsState()
    Box(modifier.size(36.dp).background(if(focused)Green else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(8.dp))
        .clickable(interactionSource=source,indication=null,onClick=onClick),contentAlignment=Alignment.Center){
        androidx.tv.material3.Icon(androidx.compose.material.icons.Icons.Rounded.DeleteOutline,null,Modifier.size(20.dp),tint=if(focused)Bg else Muted)
    }
}
