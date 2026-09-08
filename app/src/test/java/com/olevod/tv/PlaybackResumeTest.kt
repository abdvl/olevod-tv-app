package com.olevod.tv

import com.olevod.tv.data.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackResumeTest {
    private val movie=Movie(9,"示例","")
    private val detail=Detail(movie,"","","",listOf(2,7,21).map{Episode(it,"第${it}集","",false)},false,7,65)
    @Test fun explicitRecordUsesRealIndexAndOutranksServer(){
        assertEquals(PlaybackStart(2,45000,false),playbackStart(detail,WatchRecord(movie,21,45000,120000,0)))
        assertEquals(PlaybackStart(1,65000,false),playbackStart(detail,null))
    }
    @Test fun missingEpisodeAndEmptySourceHaveSafeFallback(){
        assertEquals(PlaybackStart(0,0,true),playbackStart(detail,WatchRecord(movie,99,45000,120000,0)))
        assertEquals(PlaybackStart(-1,0,false),playbackStart(detail.copy(episodes=emptyList()),null))
    }
    @Test fun completedRestartsButUnknownDurationKeepsPosition(){
        assertEquals(0L,playbackStart(detail,WatchRecord(movie,7,110000,120000,0)).position)
        assertEquals(110000L,playbackStart(detail,WatchRecord(movie,7,110000,0,0)).position)
        assertEquals(0L,playbackStart(detail,WatchRecord(movie,7,-30,0,0)).position)
    }
}
