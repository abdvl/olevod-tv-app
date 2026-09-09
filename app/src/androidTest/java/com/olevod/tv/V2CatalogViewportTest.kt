@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.graphics.Bitmap
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.Category
import com.olevod.tv.data.Filter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real catalog/popover and saveable route, controlled results only; no server or account data. */
class V2CatalogViewportTest {
    @get:Rule val compose = createComposeRule()
    private val category = Category(1, "电影", emptyList(), emptyList(), emptyList())
    private lateinit var filter: MutableState<Filter>
    private lateinit var feed: MutableState<CatalogFeedState>
    private lateinit var shown: MutableState<Boolean>
    private var selected: Long? = null
    private val commits = mutableListOf<Filter>()
    private fun movies(ids: Iterable<Long>) = ids.map { Movie(it, "滚动验证$it", "", year="2026") }

    @Test fun changedSortResetsDeepViewportButAppendAndReturnPreserveIt() {
        showCatalog()
        focused("filter:0")
        // Set the independently observed condition: fixed sort trigger focused, grid at fifth row.
        // Sort selection, entering the new result and leaving/returning below use actual D-pad keys.
        compose.onNodeWithTag("catalog-results").performScrollToIndex(4)
        compose.onNodeWithTag("poster:25").assertIsDisplayed()
        focused("filter:0")
        snapshot("before-sort")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("option:update")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:hot")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:score")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("filter:0")
        compose.onNodeWithText("正在加载影片…").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(Filter(sort="score")), commits)
            // Some old row keys reappear at different positions after the asynchronous response.
            feed.value = CatalogFeedState(movies((61L..72L) + (25L..48L) + (1L..12L)), total=60, nextPage=2)
        }
        compose.waitForIdle()
        snapshot("applied-sort")
        focused("filter:0")
        compose.onNodeWithTag("poster:61").assertIsDisplayed()
        val gridTop = compose.onNodeWithTag("catalog-results").getUnclippedBoundsInRoot().top.value
        val firstTop = compose.onNodeWithTag("poster:61").getUnclippedBoundsInRoot().top.value
        assertEquals("First sorted row must be visible immediately without pressing Down", gridTop+4f, firstTop, 1f)

        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("poster:61")
        listOf(67L,25L,31L,37L).forEach { id -> press(KeyEvent.KEYCODE_DPAD_DOWN); focused("poster:$id") }
        val beforeAppend = compose.onNodeWithTag("poster:37").getUnclippedBoundsInRoot().top.value
        compose.runOnIdle { feed.value = feed.value.copy(loading=true) }
        compose.waitForIdle()
        focused("poster:37")
        compose.runOnIdle { feed.value = feed.value.copy(items=feed.value.items+movies(49L..60L), nextPage=3, loading=false, endReached=true) }
        compose.waitForIdle()
        focused("poster:37")
        assertEquals("Appending a page must not reset the current row", beforeAppend,
            compose.onNodeWithTag("poster:37").getUnclippedBoundsInRoot().top.value, 1f)
        compose.onNodeWithTag("poster:61").assertIsNotDisplayed()
        snapshot("appended-deep")

        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("viewport-fixture-destination")
        compose.runOnIdle { assertEquals(37L, selected); assertFalse(shown.value) }
        press(KeyEvent.KEYCODE_BACK)
        focused("poster:37")
        compose.runOnIdle { assertEquals(Filter(sort="score"), filter.value); assertEquals(1, commits.size) }
        assertEquals("Returning to the same query must restore its deep viewport", beforeAppend,
            compose.onNodeWithTag("poster:37").getUnclippedBoundsInRoot().top.value, 1f)
        compose.onNodeWithTag("poster:61").assertIsNotDisplayed()
        snapshot("returned-deep")
    }

    private fun showCatalog() {
        filter = mutableStateOf(Filter())
        feed = mutableStateOf(CatalogFeedState(movies(1L..48L), total=60, nextPage=2))
        shown = mutableStateOf(true)
        compose.setContent {
            val holder = rememberSaveableStateHolder()
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        Spacer(Modifier.height(TvDesign.headerHeight))
                        Box(Modifier.weight(1f)) {
                            if (shown.value) holder.SaveableStateProvider("catalog-route") {
                                ContentFocusScope {
                                    CatalogPageContent(category, listOf(category), filter.value, feed.value,
                                        open={ selected=it.id; shown.value=false },
                                        changeFilter={ commits+=it; filter.value=it; feed.value=CatalogFeedState(loading=true) },
                                        chooseCategory={}, loadMore={})
                                }
                            } else {
                                val entry = remember { FocusRequester() }
                                BackHandler { shown.value=true }
                                TvAction("返回目录", modifier=Modifier.focusRequester(entry).testTag("viewport-fixture-destination")) { shown.value=true }
                                LaunchedEffect(Unit) { withFrameNanos {}; entry.requestFocus() }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun press(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }

    private fun focused(tag: String) {
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused] }.getOrDefault(false) }
        compose.onNodeWithTag(tag).assertIsFocused()
    }

    private fun snapshot(name: String) {
        val tags = compose.onAllNodes(SemanticsMatcher("poster tags") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("poster:") == true
        }).fetchSemanticsNodes().map { "${it.config.getOrNull(SemanticsProperties.TestTag)}@${it.boundsInRoot}" }
        Log.i("V2CatalogViewport", "$name posters=$tags")
        if (InstrumentationRegistry.getArguments().getString("viewportSnapshots") == "true") {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.cacheDir, "v2-catalog-viewport-$name.png").outputStream().use {
                assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        }
    }
}
