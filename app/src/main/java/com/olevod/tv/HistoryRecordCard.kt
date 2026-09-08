package com.olevod.tv

import androidx.compose.foundation.BorderStroke
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
internal fun HistoryRecordCard(record:WatchRecord,metadata:HistoryMetadataLoader,modifier:Modifier=Modifier,onOpen:(Movie)->Unit) {
    var movie by remember(record.movie,metadata){mutableStateOf(record.movie)}
    LaunchedEffect(record.movie,metadata){
        try{movie=metadata.complete(record.movie)}catch(e:Exception){if(e is CancellationException)throw e}
    }
    Card(onClick={onOpen(movie)},modifier=modifier.fillMaxWidth(),
        shape=CardDefaults.shape(RoundedCornerShape(10.dp)),
        scale=CardDefaults.scale(focusedScale=1.015f),
        border=CardDefaults.border(focusedBorder=Border(BorderStroke(2.dp,Green)))) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(78.dp,112.dp).clip(RoundedCornerShape(6.dp)).background(Bg),contentAlignment=Alignment.Center){
                if(movie.image.isBlank())Text("暂无封面",color=Muted,fontSize=11.sp)
                else AsyncImage(movie.image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Text(movie.title,color=White,fontSize=21.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                val details=listOf(movie.year,movie.area,movie.score.takeIf{it.isNotBlank()}?.let{"评分 $it"}.orEmpty()).filter{it.isNotBlank()}
                if(details.isNotEmpty())Text(details.joinToString(" · "),color=Muted,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(movie.note.isNotBlank())Text(movie.note,color=Muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                val episode=if(record.episode>0)"第 ${record.episode} 集 · "else ""
                Text(episode+"已观看 ${clock(record.positionMs.coerceAtLeast(0))}"+if(record.durationMs>0)" / ${clock(record.durationMs)}"else "",color=Green,fontSize=13.sp)
                if(record.durationMs>0){
                    val progress=(record.positionMs.toDouble()/record.durationMs).coerceIn(0.0,1.0).toFloat()
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Muted.copy(alpha=.2f))){
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Green))
                    }
                }
            }
            Text("继续播放 ›",color=Green,fontSize=14.sp)
        }
    }
}
