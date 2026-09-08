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
        val home=compose.onNodeWithContentDescription("首页")
        compose.waitUntil(30_000){compose.onAllNodesWithText("最近播放").fetchSemanticsNodes().isNotEmpty()}
        home.performSemanticsAction(SemanticsActions.RequestFocus){it()}
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.waitForIdle()
        compose.waitUntil(5_000){home.fetchSemanticsNode().config[SemanticsProperties.Focused]==false}
        home.assertIsNotFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP)
        compose.waitForIdle()
        compose.waitUntil(5_000){home.fetchSemanticsNode().config[SemanticsProperties.Focused]}
        home.assertIsFocused()
        // Remove the first lazy item, leave home, and restore its saved deep scroll.
        compose.waitUntil(30_000){compose.onAllNodesWithText("正在为你寻找好故事…").fetchSemanticsNodes().isEmpty()}
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        compose.onNodeWithContentDescription("电影目录").performClick()
        home.performClick()
        home.performSemanticsAction(SemanticsActions.RequestFocus){it()}
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText("最近播放").assertIsDisplayed()
        compose.waitForIdle()
        compose.waitUntil(5_000){home.fetchSemanticsNode().config[SemanticsProperties.Focused]==false}
        home.assertIsNotFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP)
        compose.waitForIdle()
        compose.waitUntil(5_000){home.fetchSemanticsNode().config[SemanticsProperties.Focused]}
        home.assertIsFocused()
    }
}
