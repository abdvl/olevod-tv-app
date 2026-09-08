package com.olevod.tv

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.Filter
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in: checks actual rendered results after each asynchronous sort response. */
class CatalogSortUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Test fun changingSortRendersNewServerResults()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveCatalogUi")=="true")
        val api=OlevodApi()
        val sorts=listOf("update" to "最近更新","desc" to "最新上传","hot" to "人气最高","score" to "评分最高")
        val expected=sorts.associate{(key,_)->key to api.browse(Filter(sort=key)).items.first().title}
        compose.onNodeWithContentDescription("电影目录").performClick()
        for((key,label) in sorts){
            compose.onNodeWithText(label).performClick()
            compose.waitUntil(30_000){compose.onAllNodesWithText(expected.getValue(key)).fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText(expected.getValue(key)).assertIsDisplayed()
            compose.onNodeWithText("当前条件下没有影片").assertDoesNotExist()
            compose.onNodeWithText("已加载 20 部").assertExists()
        }
        // Switching back to a cached sort must also render its own data.
        compose.onNodeWithText("最近更新").performClick()
        compose.waitUntil(30_000){compose.onAllNodesWithText(expected.getValue("update")).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText(expected.getValue("update")).assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        compose.waitForIdle()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.waitUntil(30_000){compose.onAllNodesWithText("已加载 40 部").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("已加载 40 部").assertExists()
        Unit
    }
}
