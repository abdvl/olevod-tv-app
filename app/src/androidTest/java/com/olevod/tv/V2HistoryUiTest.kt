@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.WatchRecord
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Local mutable records and nullable metadata loader avoid all website/account requests. */
class V2HistoryUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var isolated: V2FixtureViewModel
    private lateinit var records: MutableState<List<WatchRecord>>
    private val deletions = mutableListOf<Long?>()
    private val opened = mutableListOf<Long>()
    private var browseOpens = 0
    private val fixture = (1L..5L).map { id ->
        WatchRecord(Movie(id, "历史样本$id", "", year="2026", area="大陆", score="8.0"),
            id.toInt()*3, id*65_000L, 5_400_000L, 1_600_000_000_000L+id*1000L)
    }

    @Before fun setUp() { isolated=V2FixtureViewModel("history") }
    @After fun tearDown() { if(::isolated.isInitialized)isolated.close() }

    @Test fun resumeDeleteRolesNavigateAcrossRowsAndClampTheShortLastRow() {
        showDeviceHistory(fixture)
        enterHistory()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-delete:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-resume:2")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-delete:2")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("history-delete:4")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("history-delete:5")
        compose.onNodeWithTag("history-delete:5").assertIsDisplayed()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("history-delete:5")
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        focused("history-resume:5")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle {
            assertEquals(listOf(5L), opened)
            assertEquals(fixture[4].episode, isolated.vm.pendingResume?.episode)
            assertEquals(fixture[4].positionMs, isolated.vm.pendingResume?.positionMs)
            assertTrue(deletions.isEmpty())
        }
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("history-resume:3")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("history-resume:1")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("history-source:0")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("nav:history")
    }

    @Test fun cancelReturnsToDeleteAndConfirmationRemovesOnlyTargetThenFocusesNeighbor() {
        showDeviceHistory(fixture)
        enterHistory()
        repeat(2) { press(KeyEvent.KEYCODE_DPAD_DOWN) }
        focused("history-resume:5")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-delete:5")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("confirm-cancel")
        compose.runOnIdle { assertTrue(deletions.isEmpty()) }
        press(KeyEvent.KEYCODE_BACK)
        focused("history-delete:5")
        compose.runOnIdle { assertEquals(5, records.value.size); assertTrue(deletions.isEmpty()) }
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("confirm-cancel")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("confirm-accept")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("history-resume:4")
        compose.onNodeWithTag("history-delete:5").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(5L), deletions)
            assertEquals(listOf(1L,2L,3L,4L),records.value.map{it.movie.id})
        }
    }

    @Test fun clearCancelPreservesDataAndConfirmReturnsToValidEmptyState() {
        showDeviceHistory(fixture)
        enterHistory()
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("history-source:0")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-source:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("清空历史").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("confirm-cancel")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("清空历史").assertIsFocused()
        compose.runOnIdle { assertEquals(5,records.value.size); assertTrue(deletions.isEmpty()) }
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("confirm-cancel")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("confirm-accept")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("history-source:0")
        compose.onNodeWithText("还没有观看记录").assertIsDisplayed()
        compose.onNodeWithText("清空历史").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf<Long?>(null),deletions); assertTrue(records.value.isEmpty()) }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText("开始浏览").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1,browseOpens) }
    }

    @Test fun deletingTheOnlyRecordReturnsToTheDeviceSource() {
        showDeviceHistory(fixture.take(1))
        enterHistory()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-delete:1")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("confirm-cancel")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("history-source:0")
        compose.onNodeWithText("还没有观看记录").assertIsDisplayed()
        compose.onNodeWithTag("history-resume:1").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(1L),deletions); assertTrue(records.value.isEmpty()) }
    }

    @Test fun cloudStyleGridHasOnlyResumeTargetsAndNeverDisplaysLocalDates() {
        val cloudRecords=fixture.take(3).map{it.copy(durationMs=0)}
        compose.setContent {
            val header=remember{FocusRequester()};val entry=remember{FocusRequester()}
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        TvAction("网站历史",modifier=Modifier.focusRequester(header).testTag("cloud-source")){}
                        Box(Modifier.weight(1f)) {
                            ContentFocusScope {
                                HistoryGrid(cloudRecords,metadata=null,identity="cloud-fixture",entry=entry,up=header,
                                    open={_,movie->opened+=movie.id})
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit){withFrameNanos{};entry.requestFocus()}
        }
        focused("history-resume:1")
        (1L..3L).forEach { compose.onNodeWithTag("history-delete:$it").assertDoesNotExist() }
        cloudRecords.forEach { compose.onAllNodesWithText(watchDateLabel(it.updatedAt)).assertCountEquals(0) }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-resume:2")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("history-resume:3")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("history-resume:3")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("history-resume:1")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("cloud-source")
    }

    private fun showDeviceHistory(initial: List<WatchRecord>) {
        records=mutableStateOf(initial)
        compose.setContent {
            val header=remember{navigationItems.associate{it.key to FocusRequester()}}
            val body=remember{FocusRequester()}
            val page=remember{PageFocusController(header.getValue("history"),body)}
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        UnifiedHeader("history",header,{page.enterContent()}){}
                        Box(Modifier.weight(1f).fillMaxWidth().focusRequester(body).focusGroup()) {
                            CompositionLocalProvider(LocalPageFocus provides page) {
                                ContentFocusScope {
                                    HistoryScreen(isolated.vm,open={opened+=it.id},browse={browseOpens++},
                                        fixtureRecords=records.value,fixtureDelete={id->
                                            deletions+=id;records.value=if(id==null)emptyList()else records.value.filterNot{it.movie.id==id}
                                        },login={})
                                }
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit){withFrameNanos{};header.getValue("history").requestFocus()}
        }
    }
    private fun enterHistory() {
        focused("nav:history")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("history-resume:1")
    }
    private fun press(code:Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }
    private fun focused(tag:String) {
        compose.waitUntil(5_000){runCatching{compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused]}.getOrDefault(false)}
        compose.onNodeWithTag(tag).assertIsFocused()
    }
}
