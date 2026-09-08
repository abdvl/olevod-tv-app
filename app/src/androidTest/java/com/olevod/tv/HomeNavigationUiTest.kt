package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in real home regression, including a disposed recent-playback lazy item. */
@OptIn(ExperimentalTestApi::class)
class HomeNavigationUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun homeDownRemountsRecentAndUpReturnsToHome(){
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveHomeUi")=="true")
        val header=compose.onNodeWithContentDescription("首页")
        val navigation=compose.onNode(hasText("首页") and !hasContentDescription("首页"))
        fun key(code:Int)=InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        fun focused(node:SemanticsNodeInteraction){
            compose.waitUntil(5_000){node.fetchSemanticsNode().config[SemanticsProperties.Focused]}
            node.assertIsFocused()
        }
        fun recentRoundTrip(){
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            compose.waitUntil(5_000){!navigation.fetchSemanticsNode().config[SemanticsProperties.Focused]}
            compose.onNodeWithText("最近播放").assertIsDisplayed()
            header.assertIsNotFocused()
            key(KeyEvent.KEYCODE_DPAD_UP)
            focused(navigation)
            header.assertIsNotFocused()
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            focused(compose.onNodeWithText("直播"))
            key(KeyEvent.KEYCODE_DPAD_LEFT)
            focused(navigation)
        }
        compose.waitUntil(30_000){compose.onAllNodesWithText("最近播放").fetchSemanticsNodes().isNotEmpty()}
        header.performSemanticsAction(SemanticsActions.RequestFocus){it()}
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        focused(navigation)
        recentRoundTrip()
        key(KeyEvent.KEYCODE_DPAD_UP)
        focused(header)
        // Restore deeply scrolled home, then traverse header -> category row -> recent.
        compose.waitUntil(30_000){compose.onAllNodesWithText("正在为你寻找好故事…").fetchSemanticsNodes().isEmpty()}
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        compose.onNodeWithContentDescription("电影目录").performClick()
        header.performClick()
        header.performSemanticsAction(SemanticsActions.RequestFocus){it()}
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        focused(navigation)
        recentRoundTrip()
    }
}
