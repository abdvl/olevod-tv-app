package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.Filter
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.time.Year

class MiniCategoryHomeUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun categoryRankingsMatchCurrentYearAndBrowseReturnsToMiniHome()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveMiniUi")=="true")
        val year=Year.now().value.toString()
        val api=OlevodApi()
        val hot=api.browse(Filter(category=1,year=year,sort="hot")).items.take(10)
        val score=api.browse(Filter(category=1,year=year,sort="score")).items.take(10)
        fun key(code:Int)=InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.onNodeWithText("电影").performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithText("$year 年人气最高").fetchSemanticsNodes().isNotEmpty()}
        compose.waitUntil(30_000){compose.onAllNodesWithText(hot.first().title).fetchSemanticsNodes().isNotEmpty()}
        compose.onAllNodesWithText(hot.first().title).onFirst().assertIsDisplayed()
        compose.onAllNodesWithText(hot[1].title).onFirst().assertIsDisplayed()
        // Selected navigation goes down into the ranking, whose top hero goes back up.
        val category=compose.onNodeWithText("电影")
        key(KeyEvent.KEYCODE_DPAD_UP) // Switch from the earlier touch selection to remote input.
        category.performSemanticsAction(SemanticsActions.RequestFocus){it()}
        category.assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.waitUntil(5_000){!category.fetchSemanticsNode().config[SemanticsProperties.Focused]}
        key(KeyEvent.KEYCODE_DPAD_UP)
        compose.waitUntil(5_000){category.fetchSemanticsNode().config[SemanticsProperties.Focused]}
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        compose.waitUntil(30_000){compose.onAllNodesWithText(score.first().title).fetchSemanticsNodes().isNotEmpty()}
        compose.onAllNodesWithText(score.first().title).onLast().assertIsDisplayed()
        compose.onAllNodesWithText(score[1].title).onLast().assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithText("浏览全部").performClick()
        compose.onNodeWithText("最近更新").assertExists()
        compose.onNodeWithText("VIP蓝光影院").performClick()
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("浏览全部").assertExists()
        val vip=compose.onNodeWithText("VIP蓝光")
        vip.performSemanticsAction(SemanticsActions.RequestFocus){it()}
        key(KeyEvent.KEYCODE_DPAD_UP)
        val header=compose.onNodeWithContentDescription("首页")
        compose.waitUntil(5_000){header.fetchSemanticsNode().config[SemanticsProperties.Focused]}
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.waitUntil(5_000){vip.fetchSemanticsNode().config[SemanticsProperties.Focused]}
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("最近播放").assertExists()
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("退出应用？").assertIsDisplayed()
        compose.onNodeWithText("继续观看").assertIsFocused()
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("退出应用？").assertDoesNotExist()
    }
}
