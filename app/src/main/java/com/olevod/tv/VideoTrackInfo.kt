package com.olevod.tv

import java.util.Locale

internal data class VideoTrackInfo(val resolution:String,val bitrate:String)

/** Uses the current renderer's format, never the network bandwidth estimate. */
internal fun videoTrackInfo(width:Int,height:Int,averageBitrate:Int,peakBitrate:Int):VideoTrackInfo {
    val resolution=if(width>0&&height>0)"$width × $height"else "分辨率未知"
    val rate=when{averageBitrate>0->averageBitrate;peakBitrate>0->peakBitrate;else->0}
    val value=when{
        rate>=1_000_000->String.format(Locale.ROOT,"%.2f Mbps",rate/1_000_000.0)
        rate>0->String.format(Locale.ROOT,"%.0f kbps",rate/1000.0)
        else->"码率未提供"
    }
    return VideoTrackInfo(resolution,if(averageBitrate<=0&&peakBitrate>0)"峰值 $value"else value)
}
