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

    @Test fun bothRankingsHaveTwelveDistinctMoviesInTwoRowsOfFiveAndRestoreBrowseAll() {
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

    @Test fun vipUsesExplicitAllYearsHeadingsWithTwelveMoviesPerRanking() {
        val vip=Category(6,"VIP蓝光影院",emptyList(),listOf("2026"),emptyList())
        showMini(hot to score,vip)
        focused("nav:vip")
        compose.onNode(hasText("全部年份",substring=true) and hasText("人气最高",substring=true)).assertExists()
        compose.onAllNodesWithText("2026 人气最高",substring=true).assertCountEquals(0)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("ranking:hot:1")
        traverseRanking("hot",1L)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("ranking:score:1")
        compose.onNode(hasText("全部年份",substring=true) and hasText("评分最高",substring=true)).assertExists()
        compose.onAllNodesWithText("2026 评分最高",substring=true).assertCountEquals(0)
        traverseRanking("score",101L)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("mini-browse-all")
    }

    private fun traverseRanking(sort: String, firstId: Long) {
        focused("ranking:$sort:1")
        val visited=mutableSetOf(firstId,firstId+1)
        // Featured films must not be repeated among the ten regular posters.
        compose.onNodeWithTag("poster:$firstId").assertDoesNotExist()
        compose.onNodeWithTag("poster:${firstId+1}").assertDoesNotExist()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("ranking:$sort:2")
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        focused("ranking:$sort:1")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.waitUntil(5_000) { currentTag().removePrefix("poster:").toLongOrNull()?.let { it in (firstId+2..firstId+6) }==true }
        repeat(4) { if(currentTag()!="poster:${firstId+2}")press(KeyEvent.KEYCODE_DPAD_LEFT) }
        focused("poster:${firstId+2}");visited+=firstId+2
        (3L..6L).forEach { offset -> press(KeyEvent.KEYCODE_DPAD_RIGHT);focused("poster:${firstId+offset}");visited+=firstId+offset }
        assertFiveColumnRow(firstId+2)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("poster:${firstId+11}");visited+=firstId+11
        (10L downTo 7L).forEach { offset -> press(KeyEvent.KEYCODE_DPAD_LEFT);focused("poster:${firstId+offset}");visited+=firstId+offset }
        assertFiveColumnRow(firstId+7)
        assertEquals("Two featured plus ten individually reachable regular films",(firstId..firstId+11).toSet(),visited)
        compose.onNodeWithTag("poster:${firstId+12}").assertDoesNotExist()
    }

    private fun assertFiveColumnRow(firstId:Long) {
        val bounds=(firstId..firstId+4).map { id ->
            compose.onNodeWithTag("poster:$id").assertIsDisplayed().getUnclippedBoundsInRoot()
        }
        bounds.forEach { rect -> assertEquals("Five intended movies share one row",bounds.first().top.value,rect.top.value,1f) }
        bounds.zipWithNext().forEach { (left,right) -> assertTrue("Five columns do not overlap",left.right<=right.left) }
        val viewport=compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue("Last poster and title remain inside viewport",bounds.last().bottom<=viewport.bottom)
    }

    private fun showMini(initial: Pair<List<Movie>, List<Movie>>, selectedCategory:Category=category) {
        val headerKey=if(selectedCategory.id==6)"vip"else"movie"
        compose.setContent {
            var fixture by remember { mutableStateOf(initial) }
            replaceFixture = { fixture=it }
            var catalog by remember { mutableStateOf(false) }
            val holder = rememberSaveableStateHolder()
            val header = remember { navigationItems.associate { it.key to FocusRequester() } }
            val body = remember { FocusRequester() }
            val page = remember { PageFocusController(header.getValue(headerKey), body) }
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        UnifiedHeader(headerKey, header, { page.enterContent() }) {}
                        Box(Modifier.weight(1f).fillMaxWidth().focusRequester(body).focusGroup()) {
                            if (!catalog) CompositionLocalProvider(LocalPageFocus provides page) {
                                holder.SaveableStateProvider("mini-movie") {
                                    ContentFocusScope {
                                        MiniCategoryHome(selectedCategory, isolated.vm, open={}, browse={browseOpens++;catalog=true},
                                            navigationFocus=header.getValue(headerKey), setEntry={page.enter=it},
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
            LaunchedEffect(Unit) { withFrameNanos {}; header.getValue(headerKey).requestFocus() }
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
