@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.graphics.Bitmap
import android.util.Log
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Actual PlayerContent + D-pad; synthetic error states, no account, server, ExoPlayer or storage. */
class V2PlayerErrorFocusTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var state: MutableState<PlayerUiState>
    private lateinit var full: MutableState<Boolean>
    private var retries = 0
    private var logins = 0
    private var exits = 0

    @Test fun loginErrorHasRemoteEntryFromControlsAndHeaderAndActivatesOnlyOnce() {
        showPlayer(loginRequired = false, initialError = false)
        focusedTag("player-action:0")
        showError(loginRequired = true)
        focusedText("登录后继续")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focusedTag("nav:home")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedText("登录后继续")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedText("重试")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedTag("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focusedText("登录后继续")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, logins); assertEquals(0, retries); assertEquals(0, exits); assertFalse(full.value) }
    }

    @Test fun serviceErrorRetryHasRemoteEntryAndReturnsToVisibleControls() {
        showPlayer(loginRequired = false, initialError = false)
        focusedTag("player-action:0")
        showError(loginRequired = false)
        compose.onNodeWithText("登录后继续").assertDoesNotExist()
        focusedText("重试")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focusedTag("nav:home")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedText("重试")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, retries); assertEquals(0, logins) }
        focusedTag("player-action:0")
    }

    @Test fun fullscreenErrorDoesNotHideItsRecoveryActionAndBackReturnsToNormal() {
        showPlayer(loginRequired = false, initialError = false)
        focusedTag("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertTrue(full.value) }
        focusedTag("player-action:0")
        showError(loginRequired = true)
        focusedText("登录后继续")
        compose.onNodeWithTag("player-action:0").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(6_000)
        focusedText("登录后继续")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedText("重试")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedTag("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focusedText("登录后继续")
        press(KeyEvent.KEYCODE_BACK)
        compose.runOnIdle { assertFalse(full.value); assertEquals(0, exits) }
        focusedText("登录后继续")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, logins); assertEquals(0, retries) }
    }

    @Test fun lateLoginErrorDoesNotStealHeaderFocusButHeaderDownEntersRecovery() {
        showPlayer(loginRequired = false, initialError = false)
        focusedTag("player-action:0")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focusedTag("player-video")
        press(KeyEvent.KEYCODE_DPAD_UP)
        focusedTag("nav:home")
        showError(loginRequired = true)
        focusedTag("nav:home")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focusedText("登录后继续")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, logins); assertEquals(0, retries) }
    }

    private fun showPlayer(loginRequired: Boolean, initialError: Boolean = true) {
        state = mutableStateOf(PlayerUiState(Movie(80632, "错误焦点合成样本", "", vip = true),
            error = if (!initialError) null else if (loginRequired) "登录已失效，请重新登录" else "网络请求失败，请稍后重试",
            loginRequired = loginRequired, buffering = !initialError))
        full = mutableStateOf(false)
        compose.setContent {
            val header = remember { navigationItems.associate { it.key to FocusRequester() } }
            val body = remember { FocusRequester() }
            val page = remember { PageFocusController(header.getValue("home"), body) }
            val actions = PlayerActions(
                back = { if (full.value) full.value = false else exits++ },
                toggleFull = { full.value = !full.value }, togglePlay = {}, seek = {}, setSpeed = {}, favorite = {},
                chooseGroup = {}, playEpisode = {}, retry = {
                    retries++
                    state.value = state.value.copy(error = null, loginRequired = false, buffering = true)
                }, episodeFocus = {}, login = { logins++ })
            MaterialTheme {
                CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                    Column(Modifier.fillMaxSize().background(Bg)) {
                        if (!full.value) UnifiedHeader("home", header, { page.enterContent() }) {}
                        Box(Modifier.weight(1f).fillMaxWidth().focusRequester(body).focusGroup()) {
                            CompositionLocalProvider(LocalPageFocus provides page) {
                                PlayerContent(state.value, full.value, actions) {
                                    Box(Modifier.fillMaxSize().background(Color(0xFF102030)).testTag("error-fixture-video"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showError(loginRequired: Boolean) {
        compose.runOnIdle { state.value = state.value.copy(
            error = if (loginRequired) "登录已失效，请重新登录" else "网络请求失败，请稍后重试",
            loginRequired = loginRequired, buffering = false) }
        compose.waitForIdle()
    }

    private fun press(key: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(key)
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        val focused = compose.onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes()
        val labels = focused.map { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)
                ?: node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                ?: "focused-container"
        }
        Log.i("V2PlayerErrorFocus", "key=$key focused=$labels")
    }

    private fun focusedTag(tag: String) {
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused] }.getOrDefault(false) }
        compose.onNodeWithTag(tag).assertIsFocused()
    }

    private fun focusedText(label: String) {
        val action = compose.onNodeWithText(label)
        action.assertIsDisplayed()
        action.assertIsFocused()
        val bounds = action.getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val controls = compose.onNodeWithTag("player-action:0").getUnclippedBoundsInRoot()
        assertTrue("Recovery action must fit inside root: $label, $bounds / $root",
            bounds.left >= root.left && bounds.top >= root.top && bounds.right <= root.right && bounds.bottom <= root.bottom)
        assertTrue("Recovery action must stay above controls: $label, $bounds / $controls", bounds.bottom <= controls.top)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fontScale = context.resources.configuration.fontScale
        InstrumentationRegistry.getArguments().getString("expectedFontScale")?.toFloat()?.let {
            assertEquals("Configured font scale must actually be applied", it, fontScale, 0.01f)
        }
        Log.i("V2PlayerErrorFocus", "action=$label full=${full.value} fontScale=$fontScale bounds=$bounds controlsTop=${controls.top} root=$root")
        if (InstrumentationRegistry.getArguments().getString("errorFocusSnapshots") == "true") {
            val filename = "v2-error-focus-${if (full.value) "full" else "window"}-${if (label == "登录后继续") "login" else "retry"}.png"
            File(context.cacheDir, filename).outputStream().use {
                assertTrue("Synthetic screenshot must be saved", compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        }
    }
}
