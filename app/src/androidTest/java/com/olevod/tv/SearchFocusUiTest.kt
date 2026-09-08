package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in regression against actual suggestion/search responses. */
class SearchFocusUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun suggestionSelectsFirstResultAndBackReturnsToInput()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveSearchUi")=="true")
        val api=OlevodApi()
        val expected=api.search("魔女").items.first().title
        fun key(code:Int)=InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        fun focused(node:SemanticsNodeInteraction){
            compose.waitUntil(30_000){runCatching{node.fetchSemanticsNode().config[SemanticsProperties.Focused]}.getOrDefault(false)}
            node.assertIsFocused()
        }
        compose.onNodeWithContentDescription("搜索").performClick()
        compose.onNodeWithText("M").performSemanticsAction(SemanticsActions.RequestFocus){it()}
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("N").performSemanticsAction(SemanticsActions.RequestFocus){it()}
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.mainClock.advanceTimeBy(500)
        compose.waitUntil(30_000){compose.onAllNodesWithText("魔女").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("N").performSemanticsAction(SemanticsActions.RequestFocus){it()}
        repeat(5){key(KeyEvent.KEYCODE_DPAD_RIGHT)}
        // Select the requested suggestion; API can reorder the suggestions over time.
        compose.onNodeWithText("魔女").performClick()
        val first=compose.onNodeWithContentDescription(expected)
        focused(first)
        key(KeyEvent.KEYCODE_BACK)
        val input=compose.onNodeWithContentDescription("搜索输入框")
        focused(input)
        // A cached repeat of the same suggestion must still transfer focus once.
        compose.onNode(hasText("魔女") and !hasContentDescription("搜索输入框")).performClick()
        focused(first)
        key(KeyEvent.KEYCODE_BACK)
        focused(input)
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithContentDescription("搜索输入框").assertDoesNotExist()
    }
}
