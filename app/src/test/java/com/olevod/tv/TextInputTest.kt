package com.olevod.tv
import org.junit.Assert.*
import org.junit.Test
class TextInputTest {
    @Test fun backspaceAndCaptchaLimitPreserveSurrogatePairs(){
        assertEquals("魔女",removeLastCodePoint("魔女\uD83D\uDE00"))
        assertEquals("",removeLastCodePoint("\uD83D\uDE00"))
        assertEquals("",removeLastCodePoint(""))
        assertEquals("1234\uD83D\uDE00567","1234\uD83D\uDE005678".takeCodePoints(8))
    }
}
