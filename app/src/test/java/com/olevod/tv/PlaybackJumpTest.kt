package com.olevod.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackJumpTest {
    @Test fun fiveMinuteJumpClampsToPlayableBounds(){
        assertEquals(90000L,jumpPosition(390000,900000,-300000))
        assertEquals(690000L,jumpPosition(390000,900000,300000))
        assertEquals(0L,jumpPosition(20000,900000,-300000))
        assertEquals(900000L,jumpPosition(850000,900000,300000))
    }
}
