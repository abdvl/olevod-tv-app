package com.olevod.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTrackInfoTest {
    @Test fun usesAverageRatherThanPeakWhenBothAreAvailable(){
        assertEquals(VideoTrackInfo("1920 × 1080","3.25 Mbps"),videoTrackInfo(1920,1080,3250000,6000000))
    }
    @Test fun marksPeakAndHandlesLowerBitrates(){
        assertEquals("峰值 850 kbps",videoTrackInfo(1280,720,-1,850000).bitrate)
    }
    @Test fun doesNotInventMissingResolutionOrBitrate(){
        assertEquals(VideoTrackInfo("分辨率未知","码率未提供"),videoTrackInfo(0,-1,-1,0))
        assertEquals("码率未提供",videoTrackInfo(1920,1040,0,-1).bitrate)
    }
}
