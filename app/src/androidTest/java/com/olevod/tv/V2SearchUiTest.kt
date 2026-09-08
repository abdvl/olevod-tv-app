@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.CatalogPage
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real search screen with controllable local transport, including a deliberately late canceled response. */
class V2SearchUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var isolated: V2FixtureViewModel
    private lateinit var jobs: CoroutineScope
    private val witchStarted = CompletableDeferred<Unit>()
    private val witchResponse = CompletableDeferred<Unit>()
    private val witchDelivered = CompletableDeferred<Unit>()
    private val feeds = mutableMapOf<String, CatalogFeed>()
    private val opened = mutableListOf<Long>()
    private val witch = Movie(501, "魔女", "", year="2026")
    private val newer = Movie(601, "新输入的匹配", "", year="2026")

    @Before fun setUp() {
        isolated = V2FixtureViewModel("search")
        jobs = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
    @After fun tearDown() {
        witchResponse.complete(Unit)
        if (::jobs.isInitialized) jobs.cancel()
        if (::isolated.isInitialized) isolated.close()
    }

    @Test fun mnSuggestionConfirmationWaitsForFirstResultAndBackReturnsToInput() {
        showSearch()
        enterMnAndConfirmWitch()
        focused("suggestion:魔女")
        compose.onNodeWithTag("poster:501").assertDoesNotExist()
        compose.runOnIdle { witchResponse.complete(Unit) }
        focused("poster:501")
        compose.onNodeWithTag("poster:501").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(opened.isEmpty())
            assertTrue("Preview fixture searches must not be persisted", isolated.vm.searchHistory.isEmpty())
        }
        press(KeyEvent.KEYCODE_BACK)
        focused("search-input")
        compose.onNodeWithTag("poster:501").assertExists()
        compose.runOnIdle { assertTrue(opened.isEmpty()) }
    }

    @Test fun backDuringConfirmedSearchCancelsLateFocusTransfer() {
        showSearch()
        enterMnAndConfirmWitch()
        press(KeyEvent.KEYCODE_BACK)
        focused("search-input")
        compose.runOnIdle { witchResponse.complete(Unit) }
        compose.waitUntil(5_000) { witchDelivered.isCompleted && compose.onAllNodesWithTag("poster:501").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        focused("search-input")
        compose.onNodeWithTag("poster:501").assertIsNotFocused()
    }

    @Test fun newerTypingCancelsOldFeedAndLateResultCannotReplaceOrStealFocus() {
        showSearch()
        enterMnAndConfirmWitch()
        // The suggestion remembers R, the last keyboard key used to enter the middle column.
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        focused("search-key:R")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("search-key:R")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("poster:601").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { witchResponse.complete(Unit) }
        compose.waitUntil(5_000) { witchDelivered.isCompleted }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        focused("search-key:R")
        compose.onNodeWithTag("poster:601").assertExists().assertIsNotFocused()
        compose.onNodeWithTag("poster:501").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(601L), feeds.getValue("魔女R").state.items.map { it.id })
            assertTrue("The obsolete request must not populate even its canceled feed", feeds.getValue("魔女").state.items.isEmpty())
            assertFalse(feeds.getValue("魔女").state.loading)
            assertTrue(opened.isEmpty())
        }
    }

    private fun enterMnAndConfirmWitch() {
        focused("search-input")
        press(KeyEvent.KEYCODE_DPAD_DOWN) // Clear
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("search-key:A")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("search-key:G")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("search-key:M")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("search-key:N")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("suggestion:魔女").fetchSemanticsNodes().isNotEmpty() }
        listOf("O", "P", "Q", "R").forEach { press(KeyEvent.KEYCODE_DPAD_RIGHT); focused("search-key:$it") }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("suggestion:魔女")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000) { witchStarted.isCompleted }
    }

    private fun showSearch() {
        val fixture = SearchFixture(hot=emptyList(), suggest={ query ->
            when(query) { "MN" -> listOf("魔女", "魔女2"); "魔女" -> listOf("魔女"); else -> emptyList() }
        }, feed={ query -> feeds.getOrPut(query) {
            CatalogFeed(jobs) { page ->
                val items = when(query) {
                    "魔女" -> {
                        witchStarted.complete(Unit)
                        // Deliberately non-cooperative transport: the page generation must reject it.
                        withContext(NonCancellable) { witchResponse.await() }
                        witchDelivered.complete(Unit)
                        listOf(witch)
                    }
                    "魔女R" -> listOf(newer)
                    else -> emptyList()
                }
                CatalogPage(items, items.size, page, 20)
            }
        } })
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        Spacer(Modifier.height(TvDesign.headerHeight))
                        Box(Modifier.weight(1f)) {
                            ContentFocusScope { ConnectedSearch(isolated.vm, open={opened+=it.id}, fixture=fixture) }
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
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused] }.getOrDefault(false)
        }
        compose.onNodeWithTag(tag).assertIsFocused()
    }
}
