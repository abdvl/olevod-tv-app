package com.olevod.tv

import com.olevod.tv.data.Detail
import com.olevod.tv.data.WatchRecord

internal data class PlaybackStart(val arrayIndex:Int,val position:Long,val missingEpisode:Boolean)
internal fun playbackStart(detail:Detail,record:WatchRecord?):PlaybackStart {
    if(detail.episodes.isEmpty())return PlaybackStart(-1,0,false)
    val requested=record?.episode ?: detail.resumeEpisode
    val index=detail.episodes.indexOfFirst{it.index==requested}
    if(index<0)return PlaybackStart(0,0,requested>0)
    val position=if(record!=null)record.positionMs.takeUnless{record.durationMs>0&&it>=record.durationMs-10000}?:0 else detail.resumeSeconds*1000
    return PlaybackStart(index,position.coerceAtLeast(0),false)
}
