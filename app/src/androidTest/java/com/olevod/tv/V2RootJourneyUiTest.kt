@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Actual root routes and D-pad input; only data/transport use the app's public preview fixtures. */
class V2RootJourneyUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var isolated: V2FixtureViewModel
    private lateinit var movies: List<Pair<Long, String>>
    private var exitCalls = 0

    @Before fun setUp() {
        isolated = V2FixtureViewModel("root-journey")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val data = JSONArray(context.assets.open("preview_movies.json").bufferedReader().use { it.readText() })
        movies = (0 until data.length()).map { index ->
            data.getJSONObject(index).let { it.getLong("id") to it.getString("name") }
        }
        assertTrue("The real preview asset must supply at least three recent movies", movies.size >= 3)
    }

    @After fun tearDown() { if (::isolated.isInitialized) isolated.close() }

    @Test fun thirdRecentOpensActualPlayerAndBackRestoresExactHomeSource() {
        showRoot()
        focused("nav:home")
        compose.onNodeWithTag("nav:home").assertIsSelected()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("recent:${movies[0].first}")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("recent:${movies[1].first}")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        val source = "recent:${movies[2].first}"
        focused(source)
        val sourceY = compose.onNodeWithTag(source).getUnclippedBoundsInRoot().top.value
        press(KeyEvent.KEYCODE_DPAD_CENTER)

        // This is PlayerPreviewFixture -> actual PlayerContent, not a placeholder destination.
        focused("player-action:0")
        compose.onNodeWithTag("player-video").assertIsDisplayed()
        compose.onNodeWithText("播放器布局预览 · 尚未加载视频").assertIsDisplayed()
        compose.onNodeWithText(movies[2].second).assertIsDisplayed()
        compose.onNodeWithTag(source).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(movies[2].first, isolated.vm.pendingResume?.movie?.id)
            assertTrue("Preview must not write synthetic watching history", isolated.vm.history.records.value.isEmpty())
        }

        press(KeyEvent.KEYCODE_BACK)
        focused(source)
        compose.onNodeWithTag("player-video").assertDoesNotExist()
        compose.onNodeWithTag("nav:home").assertIsSelected()
        assertEquals("Root Back must preserve the recent row's vertical position", sourceY,
            compose.onNodeWithTag(source).getUnclippedBoundsInRoot().top.value, 1f)
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("nav:home")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused(source)
        compose.runOnIdle { assertEquals(0, exitCalls) }
    }

    @Test fun miniBrowseAllReturnsToMovieCategoryAndSameScrolledButton() {
        showRoot()
        focused("nav:home")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("nav:movie")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("nav:movie").assertIsSelected()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("ranking:hot:1")
        moveDownTo("mini-browse-all")
        compose.onNodeWithTag("mini-browse-all").assertIsDisplayed()
        val sourceY = compose.onNodeWithTag("mini-browse-all").getUnclippedBoundsInRoot().top.value
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("filter:0")
        compose.onNodeWithTag("catalog-page").assertIsDisplayed()
        compose.onNodeWithText("电影目录").assertExists()

        // The preview now uses the root category callback too, so this changes the directory route key.
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("catalog-category")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("option:1")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:2")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("电视剧目录").assertExists()
        compose.onNodeWithTag("option-popover").assertDoesNotExist()
        compose.onNode(isFocused()).assertIsDisplayed()
        press(KeyEvent.KEYCODE_BACK)

        focused("mini-browse-all")
        compose.onNodeWithTag("catalog-page").assertDoesNotExist()
        compose.onNodeWithTag("nav:movie").assertIsSelected()
        compose.onNodeWithTag("nav:series").assertIsNotSelected()
        assertEquals("Root catalog Back must preserve the mini-home scroll anchor", sourceY,
            compose.onNodeWithTag("mini-browse-all").getUnclippedBoundsInRoot().top.value, 1f)
        press(KeyEvent.KEYCODE_DPAD_UP)
        assertTrue("Restored browse-all must still navigate into the score posters: ${currentTag()}",
            currentTag().startsWith("poster:"))
        compose.onNode(isFocused()).assertIsDisplayed()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("mini-browse-all")
        compose.runOnIdle { assertEquals(0, exitCalls) }
    }

    @Test fun restoredSubmittedSearchDoesNotReplayFocusOrPlaybackAndExitRequiresExplicitChoice() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { OlevodApp(preview=true, vm=isolated.vm, onExit={exitCalls++}) }
        focused("nav:home")
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        focused("nav:search")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("search-input")
        compose.onNodeWithTag("nav:search").assertIsSelected()
        press(KeyEvent.KEYCODE_DPAD_DOWN) // Clear, then the first on-screen keyboard key.
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("search-key:A")
        listOf("B", "C", "D", "E", "F").forEach {
            press(KeyEvent.KEYCODE_DPAD_RIGHT)
            focused("search-key:$it")
        }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        val title = movies.first().second
        focused("suggestion:$title")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        val result = "poster:${movies.first().first}"
        focused(result)
        compose.onNodeWithText("「$title」搜索结果", substring=true).assertExists()
        press(KeyEvent.KEYCODE_BACK)
        focused("search-input")

        // Save after the explicit focus-to-first-result intent was consumed and Back returned to input.
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag(result).fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        focused("search-input")
        compose.onNodeWithTag("nav:search").assertIsSelected()
        compose.onNodeWithTag("nav:home").assertIsNotSelected()
        compose.onNodeWithTag(result).assertIsNotFocused()
        compose.onNodeWithText("「$title」搜索结果", substring=true).assertExists()
        compose.onNodeWithTag("player-video").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, exitCalls)
            assertTrue("Preview confirmation must not persist query history", isolated.vm.searchHistory.isEmpty())
            assertTrue(isolated.vm.history.records.value.isEmpty())
        }

        press(KeyEvent.KEYCODE_BACK) // Input -> root home, without invoking exit.
        compose.onNodeWithTag("nav:home").assertIsSelected()
        compose.onNodeWithTag("search-page").assertDoesNotExist()
        press(KeyEvent.KEYCODE_BACK)
        focusedText("继续观看")
        compose.onNodeWithText("退出应用？").assertIsDisplayed()
        press(KeyEvent.KEYCODE_DPAD_CENTER) // Safe default does not exit.
        compose.onNodeWithText("退出应用？").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, exitCalls) }

        press(KeyEvent.KEYCODE_BACK)
        focusedText("继续观看")
        press(KeyEvent.KEYCODE_BACK) // Back dismisses the dialog too.
        compose.onNodeWithText("退出应用？").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, exitCalls) }

        press(KeyEvent.KEYCODE_BACK)
        focusedText("继续观看")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focusedText("退出应用")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("退出应用？").assertDoesNotExist()
        compose.runOnIdle { assertEquals("Only the explicitly selected exit action invokes onExit", 1, exitCalls) }
    }

    private fun showRoot() {
        compose.setContent { OlevodApp(preview=true, vm=isolated.vm, onExit={exitCalls++}) }
    }

    private fun moveDownTo(tag: String) {
        val path = mutableListOf(currentTag())
        repeat(12) {
            if (currentTag()==tag) return
            press(KeyEvent.KEYCODE_DPAD_DOWN)
            path += currentTag()
        }
        assertEquals("Bounded D-pad traversal must reach $tag; path=$path", tag, currentTag())
    }

    private fun press(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
    }

    private fun currentTag(): String = runCatching {
        compose.onNode(isFocused()).fetchSemanticsNode().config[SemanticsProperties.TestTag]
    }.getOrDefault("")

    private fun focused(tag: String) {
        try {
            // An Android Dialog and its underlying Compose window can each retain a focused node.
            // Inspect the target directly rather than requiring a single focus node across windows.
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused] }.getOrDefault(false)
            }
        } catch (failure: Throwable) {
            val actual = compose.onAllNodes(isFocused()).fetchSemanticsNodes().map {
                runCatching { it.config[SemanticsProperties.TestTag] }.getOrDefault("<untagged>")
            }
            throw AssertionError("Expected root focus $tag, focused nodes $actual", failure)
        }
        compose.onNodeWithTag(tag).assertIsFocused()
    }

    private fun focusedText(label: String) {
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText(label).fetchSemanticsNode().config[SemanticsProperties.Focused] }.getOrDefault(false)
        }
        compose.onNodeWithText(label).assertIsFocused()
    }
}
