@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.Category
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class V2MiniHomeUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var isolated: V2FixtureViewModel
    private var browseOpens = 0
    private var replaceFixture: (Pair<List<Movie>, List<Movie>>) -> Unit = {}
    private val category = Category(1, "电影", emptyList(), listOf("2026"), emptyList())
    private val hot = (1L..12L).map { Movie(it, "人气样本$it", "", year="2026") }
    private val score = (101L..112L).map { Movie(it, "评分样本$it", "", year="2026") }

    @Before fun setUp() { isolated = V2FixtureViewModel("mini") }
    @After fun tearDown() { if (::isolated.isInitialized) isolated.close() }

    @Test fun bothTopTensAreDistinctAndRemoteCanReturnToBrowseAll() {
        showMini(hot to score)
        focused("nav:movie")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("ranking:hot:1")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("nav:movie")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("ranking:hot:1")
        traverseRanking("hot", 1L)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("ranking:score:1")
        traverseRanking("score", 101L)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("mini-browse-all")
        compose.onNodeWithTag("mini-browse-all").assertIsDisplayed()
        val before = compose.onNodeWithTag("mini-browse-all").getUnclippedBoundsInRoot().top.value
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("mini-test:catalog")
        compose.runOnIdle { assertEquals(1, browseOpens) }
        press(KeyEvent.KEYCODE_BACK)
        focused("mini-browse-all")
        assertEquals("Returning from the destination preserves the source button position", before,
            compose.onNodeWithTag("mini-browse-all").getUnclippedBoundsInRoot().top.value, 1f)
    }

    @Test fun shortScoreAndEmptyRankingsStillOfferReachableBrowseAll() {
        showMini(emptyList<Movie>() to score.take(3))
        focused("nav:movie")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("ranking:score:1")
        compose.onNodeWithTag("ranking:hot:1").assertDoesNotExist()
        compose.onNodeWithTag("poster:104").assertDoesNotExist()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("ranking:score:2")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("poster:103")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("mini-browse-all")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("poster:103")
        press(KeyEvent.KEYCODE_DPAD_UP)
        compose.waitUntil(5_000) { currentTag().startsWith("ranking:score:") }
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("nav:movie")

        compose.runOnIdle { replaceFixture(emptyList<Movie>() to emptyList()) }
        compose.onAllNodesWithText("今年暂无相关影片").assertCountEquals(2)
        compose.waitForIdle()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("mini-browse-all")
        compose.onAllNodesWithText("今年暂无相关影片").assertCountEquals(2)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("mini-test:catalog")
        press(KeyEvent.KEYCODE_BACK)
        focused("mini-browse-all")
        compose.runOnIdle { assertEquals(1, browseOpens); assertTrue(isolated.vm.history.records.value.isEmpty()) }
    }

    private fun traverseRanking(sort: String, firstId: Long) {
        focused("ranking:$sort:1")
        compose.onNodeWithTag("poster:$firstId").assertDoesNotExist()
        compose.onNodeWithTag("poster:${firstId+1}").assertDoesNotExist()
        compose.onNodeWithTag("poster:${firstId+10}").assertDoesNotExist()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("ranking:$sort:2")
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        focused("ranking:$sort:1")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.waitUntil(5_000) { currentTag().removePrefix("poster:").toLongOrNull()?.let { it in (firstId+2..firstId+5) }==true }
        // Wide featured cards may enter either nearby column; then traverse each regular poster by keys.
        repeat(3) { if (currentTag()!="poster:${firstId+2}") press(KeyEvent.KEYCODE_DPAD_LEFT) }
        focused("poster:${firstId+2}")
        (3L..5L).forEach { offset -> press(KeyEvent.KEYCODE_DPAD_RIGHT); focused("poster:${firstId+offset}") }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("poster:${firstId+9}")
        (8L downTo 6L).forEach { offset -> press(KeyEvent.KEYCODE_DPAD_LEFT); focused("poster:${firstId+offset}") }
    }

    private fun showMini(initial: Pair<List<Movie>, List<Movie>>) {
        compose.setContent {
            var fixture by remember { mutableStateOf(initial) }
            replaceFixture = { fixture=it }
            var catalog by remember { mutableStateOf(false) }
            val holder = rememberSaveableStateHolder()
            val header = remember { navigationItems.associate { it.key to FocusRequester() } }
            val body = remember { FocusRequester() }
            val page = remember { PageFocusController(header.getValue("movie"), body) }
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        UnifiedHeader("movie", header, { page.enterContent() }) {}
                        Box(Modifier.weight(1f).fillMaxWidth().focusRequester(body).focusGroup()) {
                            if (!catalog) CompositionLocalProvider(LocalPageFocus provides page) {
                                holder.SaveableStateProvider("mini-movie") {
                                    ContentFocusScope {
                                        MiniCategoryHome(category, isolated.vm, open={}, browse={browseOpens++;catalog=true},
                                            navigationFocus=header.getValue("movie"), setEntry={page.enter=it},
                                            currentYear="2026", fixture=fixture)
                                    }
                                }
                            } else {
                                val entry = remember { FocusRequester() }
                                BackHandler { catalog=false }
                                TvAction("返回小首页", modifier=Modifier.focusRequester(entry).testTag("mini-test:catalog")) { catalog=false }
                                LaunchedEffect(Unit) { entry.requestFocus() }
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit) { withFrameNanos {}; header.getValue("movie").requestFocus() }
        }
    }

    private fun press(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.mainClock.advanceTimeBy(220)
        compose.waitForIdle()
    }
    private fun currentTag(): String = runCatching { compose.onNode(isFocused()).fetchSemanticsNode().config[SemanticsProperties.TestTag] }.getOrDefault("")
    private fun focused(tag: String) {
        try {
            compose.waitUntil(5_000) { runCatching { currentTag()==tag }.getOrDefault(false) }
        } catch (failure: Throwable) {
            throw AssertionError("Expected focus $tag, actual ${currentTag()}", failure)
        }
        compose.onNodeWithTag(tag).assertIsFocused()
    }
}
