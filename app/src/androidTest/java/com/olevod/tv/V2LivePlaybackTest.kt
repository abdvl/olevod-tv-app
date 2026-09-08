package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Explicitly opted-in ordinary on-demand playback. No live channels, account or favorites writes. */
class V2LivePlaybackTest {
    @get:Rule val compose=createComposeRule()
    @Test fun ordinarySourceRendersAndFullscreenKeepsItsPosition()=runBlocking{
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveV2")=="true")
        val detail=OlevodApi().detail(83927)
        assertTrue(detail.episodes.isNotEmpty())
        V2FixtureViewModel("ordinary-media").use{fixture->
            var full by mutableStateOf(false)
            var visible by mutableStateOf(true)
            compose.setContent{MaterialTheme{Box(Modifier.fillMaxSize()){
                if(visible)NativePlayer(detail.movie,fixture.vm,full,{full=!full},onBack={if(full)full=false else visible=false})
            }}}
            fun key(code:Int){InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code);compose.waitForIdle()}
            compose.waitUntil(60_000){runCatching{compose.onNodeWithTag("player-video").fetchSemanticsNode().config[SemanticsProperties.StateDescription]=="视频已开始显示"}.getOrDefault(false)}
            compose.onNodeWithText("正在缓冲…").assertDoesNotExist()
            compose.waitUntil(10_000){runCatching{val time=compose.onNodeWithTag("player-position").fetchSemanticsNode().config[SemanticsProperties.Text].first().text;time!="00:00"&&time!="00:01"}.getOrDefault(false)}
            compose.onNodeWithTag("player-action:0").assertIsFocused()
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            compose.runOnIdle{assertTrue(full)}
            compose.waitUntil(10_000){compose.onAllNodes(SemanticsMatcher("reported video format"){node->node.config.getOrNull(SemanticsProperties.Text)?.any{it.text.matches(Regex("[1-9][0-9]* × [1-9][0-9]*"))}==true}).fetchSemanticsNodes().isNotEmpty()}
            key(KeyEvent.KEYCODE_DPAD_UP)
            compose.onNodeWithTag("player-action:0").assertDoesNotExist()
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            compose.onNodeWithTag("player-action:0").assertIsFocused()
            key(KeyEvent.KEYCODE_BACK)
            compose.runOnIdle{assertFalse(full)}
            key(KeyEvent.KEYCODE_BACK)
            compose.waitUntil(5000){fixture.vm.history.records.value.any{it.movie.id==detail.movie.id&&it.positionMs>=1000}}
            compose.onNodeWithTag("player-video").assertDoesNotExist()
            compose.runOnIdle{assertFalse(visible)}
        }
    }
}
