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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpRect
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.Detail
import com.olevod.tv.data.Episode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Player UI only. Virtual video and action callbacks never instantiate ExoPlayer or request a stream. */
class V2PlayerUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var state: MutableState<PlayerUiState>
    private lateinit var fullscreen: MutableState<Boolean>
    private val seeks = mutableListOf<Long>()
    private val speeds = mutableListOf<Float>()
    private val selectedGroups = mutableListOf<Int>()
    private val playedEpisodes = mutableListOf<Int>()
    private var playToggles = 0
    private var favoriteToggles = 0
    private var exits = 0
    private val movie = Movie(1, "遥控器播放器验证", "", year="2026", area="大陆", score="8.5")
    private val detail = Detail(movie, "这是一段很长的影片简介，用来检查文字不会挤掉控制和选集区域。".repeat(30),
        "演员甲、演员乙、演员丙", "导演", (1..32).map { Episode(it, "第${it}集", "", false) }, false, 1, 0)

    @Test fun eightControlsStayVisibleAndHeaderDownReturnsToVideo() {
        showPlayer()
        focused("player-action:0")
        assertEightControls(listOf("全屏", "播放", "−30秒", "+30秒", "−5分钟", "+5分钟", "速度 1.0×", "收藏"))
        compose.onNodeWithTag("episode-group:0").assertIsDisplayed()
        compose.onNodeWithTag("episode:1").assertIsDisplayed()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("player-action:1")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        (2..5).forEach { index ->
            press(KeyEvent.KEYCODE_DPAD_RIGHT)
            focused("player-action:$index")
            press(KeyEvent.KEYCODE_DPAD_CENTER)
        }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("player-action:6")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("player-action:7")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle {
            assertEquals(1, playToggles)
            assertEquals(listOf(-30_000L, 30_000L, -300_000L, 300_000L), seeks)
            assertEquals(1, favoriteToggles)
        }
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("player-video")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("nav:home")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("player-video")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("player-action:0")
        compose.runOnIdle { assertTrue(fullscreen.value) }
        press(KeyEvent.KEYCODE_BACK)
        focused("player-action:0")
        compose.runOnIdle { assertFalse(fullscreen.value); assertEquals(0, exits) }
    }

    @Test fun fullscreenUpDownKeepsVideoGeometryAndHiddenSeekDoesNotShowControls() {
        showPlayer(initialFull=true)
        focused("player-action:0")
        compose.onNodeWithText("1920 × 1080").assertIsDisplayed()
        compose.onNodeWithText("4.20 Mbps").assertIsDisplayed()
        val original = compose.onNodeWithTag("player-video").getUnclippedBoundsInRoot()
        val virtual = compose.onNodeWithTag("virtual-video").getUnclippedBoundsInRoot()
        repeat(3) {
            press(KeyEvent.KEYCODE_DPAD_UP)
            compose.onNodeWithTag("player-action:0").assertDoesNotExist()
            assertBounds(original, "player-video")
            assertBounds(virtual, "virtual-video")
            if (it==0) {
                press(KeyEvent.KEYCODE_DPAD_LEFT)
                press(KeyEvent.KEYCODE_DPAD_RIGHT)
                compose.onNodeWithTag("player-action:0").assertDoesNotExist()
                compose.runOnIdle { assertEquals(listOf(-30_000L, 30_000L), seeks) }
            }
            press(KeyEvent.KEYCODE_DPAD_DOWN)
            focused("player-action:0")
            assertBounds(original, "player-video")
            assertBounds(virtual, "virtual-video")
            (0..7).forEach { action -> compose.onNodeWithTag("player-action:$action").assertIsDisplayed() }
            compose.onNodeWithTag("episode-group:0").assertIsDisplayed()
        }
    }

    @Test fun episodeGroupsOnlyPreviewUntilConfirmedAndSpeedRestoresItsTrigger() {
        showPlayer()
        focused("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("episode-group:0")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("episode-group:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("episode-group:2")
        compose.onNodeWithTag("episode:1").assertExists()
        compose.onNodeWithTag("episode:21").assertDoesNotExist()
        compose.runOnIdle { assertTrue(selectedGroups.isEmpty()); assertTrue(playedEpisodes.isEmpty()) }
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("episode-group:2")
        compose.onNodeWithTag("episode:21").assertIsDisplayed()
        compose.onNodeWithTag("episode:30").assertIsDisplayed()
        compose.onNodeWithTag("episode:1").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(2), selectedGroups); assertEquals(0, state.value.episode); assertTrue(playedEpisodes.isEmpty()) }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("episode:21")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("episode:22")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("episode:22")
        compose.onNodeWithTag("episode:22").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(22), playedEpisodes) }
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("episode-group:2")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("player-action:0")
        repeat(6) { press(KeyEvent.KEYCODE_DPAD_RIGHT) }
        focused("player-action:6")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("option:1.0")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("option:0.75")
        compose.runOnIdle { assertTrue(speeds.isEmpty()); assertEquals(1f, state.value.speed) }
        press(KeyEvent.KEYCODE_BACK)
        focused("player-action:6")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("option:1.0")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:1.25")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("option:1.5")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("player-action:6")
        compose.runOnIdle { assertEquals(listOf(1.5f), speeds) }
        repeat(6) { press(KeyEvent.KEYCODE_DPAD_LEFT) }
        focused("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("episode-group:2")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("episode-group:3")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("episode-group:3")
        compose.onNodeWithTag("episode:31").assertIsDisplayed()
        compose.onNodeWithTag("episode:32").assertIsDisplayed()
        compose.onNodeWithTag("episode:33").assertDoesNotExist()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("episode:31")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("episode:32")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("episode:32")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(listOf(22, 32), playedEpisodes); assertEquals(1.5f, state.value.speed) }
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("episode-group:3")
        compose.onNodeWithTag("player-action:0").assertIsDisplayed()
        press(KeyEvent.KEYCODE_DPAD_UP)
        focused("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_UP)
        compose.onNodeWithTag("player-action:0").assertDoesNotExist()
    }

    @Test fun unseekableControlsRemainVisibleButRemoteSkipsAndIgnoresThem() {
        showPlayer(seekable=false)
        focused("player-action:0")
        (2..5).forEach { compose.onNodeWithTag("player-action:$it").assertIsDisplayed().assertIsNotEnabled() }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("player-action:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("player-action:6")
        press(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
        press(KeyEvent.KEYCODE_MEDIA_REWIND)
        compose.runOnIdle { assertTrue(seeks.isEmpty()) }
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        focused("player-action:1")
    }

    private fun showPlayer(initialFull: Boolean=false, seekable: Boolean=true) {
        state = mutableStateOf(PlayerUiState(movie, detail, episode=0, position=600_000, duration=5_400_000,
            seekable=seekable, trackInfo=videoTrackInfo(1920,1080,4_200_000,-1)))
        fullscreen = mutableStateOf(initialFull)
        compose.setContent {
            val header = remember { navigationItems.associate { it.key to FocusRequester() } }
            val body = remember { FocusRequester() }
            val page = remember { PageFocusController(header.getValue("home"),body) }
            val actions = PlayerActions(
                back={if(fullscreen.value)fullscreen.value=false else exits++}, toggleFull={fullscreen.value=!fullscreen.value},
                togglePlay={playToggles++;state.value=state.value.copy(playing=!state.value.playing,playRequested=!state.value.playRequested)},
                seek={delta->seeks+=delta;state.value=state.value.copy(position=jumpPosition(state.value.position,state.value.duration,delta))},
                setSpeed={speeds+=it;state.value=state.value.copy(speed=it)}, favorite={favoriteToggles++;state.value=state.value.copy(favorite=!state.value.favorite)},
                chooseGroup={selectedGroups+=it;state.value=state.value.copy(group=it)},
                playEpisode={ep->playedEpisodes+=ep.index;state.value=state.value.copy(episode=detail.episodes.indexOf(ep),position=0)},
                retry={},episodeFocus={})
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        if(!fullscreen.value)UnifiedHeader("home",header,{page.enterContent()}){}
                        Box(Modifier.weight(1f).fillMaxWidth().focusRequester(body).focusGroup()) {
                            CompositionLocalProvider(LocalPageFocus provides page) {
                                PlayerContent(state.value,fullscreen.value,actions) {
                                    Box(Modifier.fillMaxSize().background(Color(0xFF102030)).testTag("virtual-video"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun assertEightControls(labels: List<String>) {
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        var previousRight = 0f
        labels.forEachIndexed { index,label ->
            val control=compose.onNodeWithTag("player-action:$index")
            control.assertIsDisplayed().assertContentDescriptionEquals(label)
            val bounds=control.getUnclippedBoundsInRoot()
            assertTrue("Control $index must fit without clipping", bounds.left>=root.left && bounds.right<=root.right && bounds.top>=root.top && bounds.bottom<=root.bottom)
            assertTrue("Controls must keep visual order without overlap", bounds.left.value>=previousRight)
            previousRight=bounds.right.value
        }
    }
    private fun assertBounds(expected: DpRect, tag: String) {
        val actual=compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        assertEquals("$tag left",expected.left.value,actual.left.value,.5f)
        assertEquals("$tag top",expected.top.value,actual.top.value,.5f)
        assertEquals("$tag right",expected.right.value,actual.right.value,.5f)
        assertEquals("$tag bottom",expected.bottom.value,actual.bottom.value,.5f)
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
}
