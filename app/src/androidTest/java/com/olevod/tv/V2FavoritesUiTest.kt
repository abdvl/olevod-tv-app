@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.CatalogPage
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Exercises production FavoritesScreen and focus memory with isolated, in-memory paged responses. */
class V2FavoritesUiTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var isolated:V2FixtureViewModel
    private lateinit var activeFeed:MutableState<CatalogFeed>
    private lateinit var shown:MutableState<Boolean>
    private val opened=mutableListOf<Long>()
    private val movies=(1L..65L).map{Movie(it,"收藏样本$it","",year="2026",score="8.0")}
    private var browseCount=0

    @Before fun setUp(){isolated=V2FixtureViewModel("favorites")}
    @After fun tearDown(){if(::isolated.isInitialized)isolated.close()}

    private fun feed(data:List<Movie>,requested:MutableList<Int> = mutableListOf()):CatalogFeed =
        CatalogFeed(isolated.vm.viewModelScope){page->
            requested+=page
            CatalogPage(data.drop((page-1)*20).take(20),data.size,page,20)
        }

    @Test fun emptyCompletionUpdatesHeaderDownToBrowseAction(){
        val gate=CompletableDeferred<Unit>()
        val empty=CatalogFeed(isolated.vm.viewModelScope){page->gate.await();CatalogPage(emptyList(),0,page,20)}
        show(empty)
        focusedTag("nav:favorites")
        compose.onNodeWithText("正在加载收藏…").assertExists()
        compose.runOnIdle{gate.complete(Unit)}
        compose.waitUntil(5_000){empty.state.endReached}
        compose.waitForIdle()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedText("开始浏览")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle{assertEquals(1,browseCount)}
    }

    @Test fun removedSecondMovieRestoresSamePositionNeighbor(){
        show(feed(movies.take(4)))
        loaded()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedTag("poster:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focusedTag("poster:2")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focusedTag("return-favorites")
        compose.runOnIdle{assertEquals(listOf(2L),opened);activeFeed.value=feed(movies.take(4).filterNot{it.id==2L})}
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focusedTag("poster:3")
        compose.onNodeWithTag("poster:2").assertDoesNotExist()
    }

    @Test fun removedDeepMovieLoadsMissingPagesBeforeRestoringNeighbor(){
        val initial=feed(movies)
        show(initial);loaded()
        repeat(2){
            val before=initial.state.nextPage
            compose.runOnIdle{initial.loadNext()}
            compose.waitUntil(5_000){initial.state.nextPage>before}
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
        compose.onNodeWithTag("poster:50").performSemanticsAction(SemanticsActions.RequestFocus){it()}
        focusedTag("poster:50")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focusedTag("return-favorites")
        val requested=mutableListOf<Int>()
        compose.runOnIdle{activeFeed.value=feed(movies.filterNot{it.id==50L},requested)}
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focusedTag("poster:51")
        compose.onNodeWithTag("poster:51").assertIsDisplayed()
        compose.runOnIdle{assertTrue(requested.containsAll(listOf(1,2,3)))}
    }

    @Test fun tailFailureRetainsLoadedMoviesAndRetryAppendsWithoutDuplicates(){
        var fail=true
        val paging=CatalogFeed(isolated.vm.viewModelScope){page->
            if(page==2&&fail)throw IOException("fixture page failure")
            CatalogPage(movies.take(24).drop((page-1)*20).take(20),24,page,20)
        }
        show(paging);loaded()
        compose.runOnIdle{paging.loadNext()}
        compose.waitUntil(5_000){paging.state.error!=null}
        compose.runOnIdle{
            assertEquals((1L..20L).toList(),paging.state.items.map{it.id})
            assertEquals(2,paging.state.nextPage)
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        compose.onNodeWithText("重试").performSemanticsAction(SemanticsActions.RequestFocus){it()}
        focusedText("重试")
        compose.runOnIdle{fail=false}
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000){paging.state.endReached}
        compose.runOnIdle{
            assertEquals((1L..24L).toList(),paging.state.items.map{it.id})
            assertNull(paging.state.error)
        }
    }

    private fun show(initial:CatalogFeed){
        activeFeed=mutableStateOf(initial);shown=mutableStateOf(true)
        compose.setContent{
            val holder=rememberSaveableStateHolder()
            val headers=remember{navigationItems.associate{it.key to FocusRequester()}}
            val body=remember{FocusRequester()}
            val page=remember{PageFocusController(headers.getValue("favorites"),body)}
            MaterialTheme{CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec){
                Column(Modifier.fillMaxSize().background(Bg)){
                    UnifiedHeader("favorites",headers,{page.enterContent()}){}
                    Box(Modifier.weight(1f).fillMaxWidth().focusRequester(body).focusGroup()){
                        if(shown.value)holder.SaveableStateProvider("favorites"){
                            CompositionLocalProvider(LocalPageFocus provides page){ContentFocusScope{
                                FavoritesScreen(isolated.vm,open={opened+=it.id;shown.value=false},login={},browse={browseCount++},fixtureFeed=activeFeed.value)
                            }}
                        }else{
                            val back=remember{FocusRequester()}
                            TvAction("返回收藏",modifier=Modifier.focusRequester(back).testTag("return-favorites")){shown.value=true}
                            LaunchedEffect(Unit){withFrameNanos{};back.requestFocus()}
                        }
                    }
                }
            }}
            LaunchedEffect(Unit){withFrameNanos{};headers.getValue("favorites").requestFocus()}
        }
    }
    private fun loaded(){compose.waitUntil(5_000){activeFeed.value.state.nextPage>1};compose.waitForIdle()}
    private fun press(code:Int){InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code);compose.mainClock.advanceTimeBy(200);compose.waitForIdle()}
    private fun focusedTag(tag:String){focused(compose.onNodeWithTag(tag))}
    private fun focusedText(text:String){focused(compose.onNodeWithText(text))}
    private fun focused(node:SemanticsNodeInteraction){
        try{compose.waitUntil(5_000){runCatching{node.fetchSemanticsNode().config[SemanticsProperties.Focused]}.getOrDefault(false)}}
        catch(e:ComposeTimeoutException){throw AssertionError("Fixture focus did not settle:\n"+compose.onRoot(useUnmergedTree=true).printToString(),e)}
        node.assertIsFocused()
    }
}
