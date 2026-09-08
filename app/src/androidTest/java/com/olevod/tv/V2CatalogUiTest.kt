@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.Category
import com.olevod.tv.data.Filter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** No AppViewModel or network: actual catalog page/popovers with a controlled loaded result. */
class V2CatalogUiTest {
    @get:Rule val compose = createComposeRule()
    private val movieCategory = Category(1, "电影", listOf("大陆", "香港", "日本"),
        (2026 downTo 2015).map(Int::toString), listOf(1 to "喜剧", 2 to "动作", 3 to "剧情"))
    private val movies = (1L..36L).map { Movie(it, "目录样本$it", "", year="2026", area="大陆") }
    private var observed = Filter()
    private val commits = mutableListOf<Filter>()
    private val categoryChanges = mutableListOf<Int>()
    private var loadRequests = 0

    @Test fun yearGridHasFourColumnsAndOnlyConfirmationChangesFilter() {
        showCatalog()
        moveToTrigger(3)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("option:0")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("option:0")
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        focused("option:0")
        listOf("2026", "2025", "2024").forEach {
            press(KeyEvent.KEYCODE_DPAD_RIGHT)
            focused("option:$it")
        }
        // The fourth column must not wrap horizontally to the next row.
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("option:2024")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:2020")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:2016")
        // The partial final row has only 2015: down clamps to its actual item.
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:2015")
        compose.onNodeWithTag("option:2015").assertIsDisplayed()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:2015")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("option:2015")
        compose.runOnIdle {
            assertEquals(Filter(), observed)
            assertTrue("Moving inside the year popup must not commit", commits.isEmpty())
        }
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("filter:3")
        compose.onNodeWithTag("option-popover").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(Filter(year="2015")), commits)
            assertEquals("2015", observed.year)
        }
        // Reopening must focus the selected value, including a value in the last row.
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("option:2015")
        compose.onNodeWithTag("option:2015").assertIsSelected().assertIsDisplayed()
        press(KeyEvent.KEYCODE_BACK)
        focused("filter:3")
        compose.runOnIdle { assertEquals(1, commits.size) }
    }

    @Test fun cancelingSortRetainsAppliedFilterAndGridPosition() {
        showCatalog()
        focused("filter:0")
        // Controlled scroll precondition: keep the fixed filter focused while positioning the grid.
        // Popup navigation/dismissal below still uses only remote keys.
        compose.onNodeWithTag("catalog-results").performScrollToIndex(3)
        compose.onNodeWithTag("poster:19").assertIsDisplayed()
        val before = compose.onNodeWithTag("poster:19").getUnclippedBoundsInRoot().top.value
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("option:update")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:hot")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:score")
        compose.onNodeWithTag("option:update").assertIsSelected()
        compose.onNodeWithTag("option:score").assertIsNotSelected()
        compose.runOnIdle { assertTrue(commits.isEmpty()) }
        press(KeyEvent.KEYCODE_BACK)
        focused("filter:0")
        compose.onNodeWithTag("option-popover").assertDoesNotExist()
        compose.onNodeWithTag("poster:19").assertIsDisplayed()
        assertEquals("Dismissal must preserve the existing result scroll", before,
            compose.onNodeWithTag("poster:19").getUnclippedBoundsInRoot().top.value, 1f)
        compose.runOnIdle {
            assertEquals(Filter(), observed)
            assertTrue(commits.isEmpty())
        }
    }

    @Test fun moreFiltersAreDraftedCanceledAndAppliedAsOneChange() {
        showCatalog()
        moveToTrigger(4)
        openMoreAndChooseMemberA()
        compose.runOnIdle { assertEquals(Filter(), observed); assertTrue(commits.isEmpty()) }
        press(KeyEvent.KEYCODE_BACK)
        focused("filter:4")
        compose.onNodeWithTag("more-filters").assertDoesNotExist()
        compose.runOnIdle { assertEquals(Filter(), observed); assertTrue(commits.isEmpty()) }

        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("全部影片").assertIsFocused().assertIsSelected()
        compose.onNodeWithTag("draft-letter:0").assertIsSelected()
        // Dismissed drafts must not reappear on reopen.
        compose.onNodeWithText("会员").assertIsNotSelected()
        compose.onNodeWithTag("draft-letter:A").assertIsNotSelected()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("会员").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("draft-letter:A")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        listOf("H", "O", "V").forEach {
            press(KeyEvent.KEYCODE_DPAD_DOWN)
            focused("draft-letter:$it")
        }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("more-action:0")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("more-action:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("more-action:2")
        compose.runOnIdle { assertTrue("Draft changes must remain local until Apply", commits.isEmpty()) }
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("filter:4")
        compose.onNodeWithTag("more-filters").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(Filter(membership=1, initial="A")), commits)
            assertEquals(Filter(membership=1, initial="A"), observed)
        }
        compose.onNodeWithText("更多筛选 · 2").assertExists()

        // Reset changes only the draft until Apply; Cancel must preserve the committed pair.
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("会员").assertIsFocused().assertIsSelected()
        moveFromMemberToReset()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("全部影片").assertIsSelected()
        compose.onNodeWithTag("draft-letter:0").assertIsSelected()
        compose.runOnIdle { assertEquals(Filter(membership=1, initial="A"), observed); assertEquals(1, commits.size) }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("more-action:1")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("filter:4")
        compose.runOnIdle { assertEquals(Filter(membership=1, initial="A"), observed); assertEquals(1, commits.size) }

        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("会员").assertIsFocused().assertIsSelected()
        moveFromMemberToReset()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        repeat(2) { press(KeyEvent.KEYCODE_DPAD_RIGHT) }
        focused("more-action:2")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("filter:4")
        compose.runOnIdle {
            assertEquals(listOf(Filter(membership=1, initial="A"), Filter()), commits)
            assertEquals(Filter(), observed)
        }
        compose.onNodeWithText("更多筛选 · 2").assertDoesNotExist()
    }

    @Test fun unknownTotalShowsLoadedCountAndNeverNegativeTotal() {
        showCatalog(total=-1)
        focused("filter:0")
        compose.onNodeWithText("已加载 36 部").assertIsDisplayed()
        compose.onNodeWithText("-1", substring=true).assertDoesNotExist()
        compose.onNodeWithText("当前条件下没有影片").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, loadRequests)
            assertTrue(categoryChanges.isEmpty())
            assertTrue(commits.isEmpty())
        }
    }

    private fun openMoreAndChooseMemberA() {
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("全部影片").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("会员").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("draft-letter:A")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("会员").assertIsSelected()
        compose.onNodeWithTag("draft-letter:A").assertIsSelected()
    }

    private fun moveFromMemberToReset() {
        listOf("A", "H", "O", "V").forEach {
            press(KeyEvent.KEYCODE_DPAD_DOWN)
            focused("draft-letter:$it")
        }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("more-action:0")
    }

    private fun showCatalog(total: Int = 36) {
        compose.setContent {
            var filter by remember { mutableStateOf(Filter()) }
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        Spacer(Modifier.height(TvDesign.headerHeight))
                        Box(Modifier.weight(1f)) {
                            ContentFocusScope {
                                CatalogPageContent(movieCategory, listOf(movieCategory), filter,
                                    CatalogFeedState(items=movies, total=total, nextPage=3, endReached=true),
                                    open={}, changeFilter={ commits+=it; observed=it; filter=it },
                                    chooseCategory={categoryChanges+=it}, loadMore={loadRequests++})
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveToTrigger(index: Int) {
        focused("filter:0")
        repeat(index) { press(KeyEvent.KEYCODE_DPAD_RIGHT); focused("filter:${it+1}") }
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
