@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.WatchRecord
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Exercises the actual ConnectedHome with local fixture data and D-pad events only. */
class V2HomeUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var namespace: String
    private lateinit var models: ViewModelStore
    private lateinit var vm: AppViewModel
    private var openedMovieId: Long? = null
    private var historyOpens = 0

    private val records = (1L..8L).map { id ->
        WatchRecord(Movie(id, "本地影片$id", "", year="2026", area="大陆", score="8.0"),
            (id * 3).toInt(), id * 54_000L, 5_400_000L, 10_000L - id)
    }
    private val state = HomeState(
        heroes=listOf(Hero(101, "左侧推荐", "", "本地样本"), Hero(102, "右侧推荐", "", "本地样本")),
        sections=emptyList(), loading=false)

    @Before fun createIsolatedViewModel() {
        namespace = "v2-home-${System.nanoTime()}"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FixtureApplication(context, namespace)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            models = ViewModelStore()
            vm = ViewModelProvider(models, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(app) as T
            })[AppViewModel::class.java]
        }
    }

    @After fun releaseIsolatedViewModel() {
        if (::models.isInitialized) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { models.clear() }
        }
        if (::vm.isInitialized) vm.history.close()
        if (::namespace.isInitialized) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.deleteDatabase("$namespace-history.db")
            listOf("search", "session", "remembered-login").forEach {
                context.deleteSharedPreferences("$namespace-$it")
            }
        }
    }

    @Test fun headerRecentRoundTripAndHistoryShortcutAreReachableWithRemote() {
        showFixtureHome()
        focused("nav:home")
        (1L..5L).forEach { compose.onNodeWithTag("recent:$it").assertIsDisplayed() }
        compose.onNodeWithTag("recent:6").assertDoesNotExist()
        compose.onNodeWithTag("all-history").assertIsDisplayed()

        repeat(3) {
            press(KeyEvent.KEYCODE_DPAD_DOWN)
            focused("recent:1")
            press(KeyEvent.KEYCODE_DPAD_UP)
            focused("nav:home")
        }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("recent:1")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithContentDescription("左侧推荐").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("recent:1")
        (2L..5L).forEach { id ->
            press(KeyEvent.KEYCODE_DPAD_RIGHT)
            focused("recent:$id")
        }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("all-history")
        compose.onNodeWithTag("all-history").assertIsDisplayed()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithContentDescription("右侧推荐").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("recent:5")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("all-history")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("test:history")
        compose.runOnIdle { assertEquals(1, historyOpens) }
    }

    @Test fun expandingAndCollapsingRecentKeepsRecommendationYAndAllDefaultSlots() {
        showFixtureHome()
        focused("nav:home")
        val recommendationY = compose.onNodeWithTag("recommendations").getUnclippedBoundsInRoot().top.value
        val defaultWidths = (1L..5L).associateWith {
            cardWidth(it)
        }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("recent:1")
        assertTrue("Focused recent item expands horizontally",
            cardWidth(1L) > defaultWidths.getValue(1L) * 2)
        assertRecommendationY(recommendationY)

        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("recent:2")
        assertRecommendationY(recommendationY)
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("nav:home")
        assertRecommendationY(recommendationY)
        (1L..5L).forEach { id ->
            val card = compose.onNodeWithTag("recent:$id")
            card.assertIsDisplayed()
            assertEquals("Default poster width restored for $id", defaultWidths.getValue(id),
                cardWidth(id), 1f)
        }
        compose.onNodeWithTag("all-history").assertIsDisplayed()
        val historyBounds = compose.onNodeWithTag("all-history").getUnclippedBoundsInRoot()
        val rootBounds = compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue("Default history shortcut is fully inside the viewport", historyBounds.right <= rootBounds.right)
    }

    @Test fun returningFromMovieRestoresExactRecentAndDoesNotPersistFixtureHistory() {
        showFixtureHome()
        focused("nav:home")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("recent:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("recent:2")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("recent:3")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("test:player")
        compose.runOnIdle {
            assertEquals(3L, openedMovieId)
            val resume = requireNotNull(vm.pendingResume)
            assertEquals(records[2].movie.id, resume.movie.id)
            assertEquals(records[2].episode, resume.episode)
            assertEquals(records[2].positionMs, resume.positionMs)
            assertTrue("Fixture records must not be saved to local history", vm.history.records.value.isEmpty())
        }
        press(KeyEvent.KEYCODE_BACK)
        focused("recent:3")
        compose.onNodeWithTag("recent:1").assertIsNotFocused()
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("nav:home")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("recent:3")
    }

    private fun showFixtureHome() {
        compose.setContent {
            val pageStates = rememberSaveableStateHolder()
            var route by remember { mutableStateOf("home") }
            val header = remember { navigationItems.associate { it.key to FocusRequester() } }
            val body = remember { FocusRequester() }
            val recent = remember { FocusRequester() }
            val page = remember { PageFocusController(header.getValue("home"), body) }
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        UnifiedHeader(if (route=="home") "home" else route, header, { page.enterContent() }) { destination ->
                            if (destination=="home") route="home"
                        }
                        Box(Modifier.weight(1f).fillMaxWidth().focusRequester(body).focusGroup()) {
                            if (route=="home") {
                                CompositionLocalProvider(LocalPageFocus provides page) {
                                    pageStates.SaveableStateProvider("fixture-home") {
                                        ContentFocusScope {
                                            ConnectedHome(state, vm,
                                                open={ openedMovieId=it.id; route="player" },
                                                history={ historyOpens++; route="history" },
                                                navigationHomeFocus=header.getValue("home"), recentFocus=recent,
                                                setRecentEntry={ page.enter=it }, fixtureRecords=records, browse={})
                                        }
                                    }
                                }
                            } else {
                                val returnFocus = remember(route) { FocusRequester() }
                                BackHandler { route="home" }
                                TvAction("返回首页", modifier=Modifier.focusRequester(returnFocus).testTag("test:$route")) { route="home" }
                                LaunchedEffect(route) { returnFocus.requestFocus() }
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit) { withFrameNanos {}; header.getValue("home").requestFocus() }
        }
    }

    private fun press(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
    }

    private fun focused(tag: String) {
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused] }.getOrDefault(false)
        }
        compose.onNodeWithTag(tag).assertIsFocused()
    }

    private fun assertRecommendationY(expected: Float) {
        assertEquals("Recent focus must not move the recommendation section", expected,
            compose.onNodeWithTag("recommendations").getUnclippedBoundsInRoot().top.value, 1f)
    }

    private fun cardWidth(id: Long): Float {
        val bounds = compose.onNodeWithTag("recent:$id").getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value
    }

    /** Prefixes all stores used by AppViewModel, so constructor reads cannot open real account/history data. */
    private class FixtureApplication(base: Context, private val prefix: String) : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences("$prefix-$name", mode)
        override fun getDatabasePath(name: String): File = super.getDatabasePath("$prefix-$name")
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            super.openOrCreateDatabase("$prefix-$name", mode, factory)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            super.openOrCreateDatabase("$prefix-$name", mode, factory, errorHandler)
    }
}
