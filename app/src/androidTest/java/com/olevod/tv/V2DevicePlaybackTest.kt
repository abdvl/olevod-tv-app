package com.olevod.tv

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Opt-in real media on the target device, with isolated history and no account/favorite writes. */
class V2DevicePlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realSeekSpeedFullscreenBackgroundAndRelease(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveDevicePlayback") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sessions = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val detail = OlevodApi().detail(83927)
        assertTrue("The ordinary movie must have a playable episode", detail.episodes.isNotEmpty())
        instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.MEDIA_CONTENT_CONTROL")
        try {
            V2FixtureViewModel("device-media-controls").use { fixture ->
                var full by mutableStateOf(false)
                var shown by mutableStateOf(true)
                compose.setContent { MaterialTheme { Box(Modifier.fillMaxSize()) {
                    if (shown) NativePlayer(detail.movie, fixture.vm, full, { full = !full },
                        onBack = { if (full) full = false else shown = false })
                } } }
                compose.waitUntil(60_000) {
                    runCatching { compose.onNodeWithTag("player-video").fetchSemanticsNode()
                        .config[SemanticsProperties.StateDescription] == "视频已开始显示" }.getOrDefault(false)
                }
                var controller: MediaController? = null
                waitFor("Active platform media session") {
                    controller = sessions.getActiveSessions(null).firstOrNull { it.packageName == context.packageName }
                    controller != null
                }
                val media = controller!!
                waitFor("Known duration longer than five minutes") {
                    (media.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0) > 360_000
                }
                compose.onNodeWithTag("player-action:0").assertIsFocused()
                press(KeyEvent.KEYCODE_DPAD_RIGHT)
                press(KeyEvent.KEYCODE_DPAD_CENTER)
                waitFor("Pause from the visible control") { media.playbackState?.state == PlaybackState.STATE_PAUSED }
                val initiallyPaused = position(media)
                SystemClock.sleep(750)
                assertEquals("Paused position stays fixed", initiallyPaused, position(media))

                // These are real D-pad confirmations of all four controls, checked at the player session.
                press(KeyEvent.KEYCODE_DPAD_RIGHT) // -30 seconds near the beginning clamps to zero.
                press(KeyEvent.KEYCODE_DPAD_CENTER)
                assertPosition(media, (initiallyPaused - 30_000).coerceAtLeast(0), "minus30")
                val beginning = position(media)
                repeat(3) { press(KeyEvent.KEYCODE_DPAD_RIGHT) }
                press(KeyEvent.KEYCODE_DPAD_CENTER) // +5 minutes
                assertPosition(media, beginning + 300_000, "plus300")
                repeat(3) { press(KeyEvent.KEYCODE_DPAD_LEFT) }
                press(KeyEvent.KEYCODE_DPAD_CENTER) // -30 seconds away from the boundary
                assertPosition(media, beginning + 270_000, "minus30Interior")
                press(KeyEvent.KEYCODE_DPAD_RIGHT)
                press(KeyEvent.KEYCODE_DPAD_CENTER) // +30 seconds
                assertPosition(media, beginning + 300_000, "plus30")
                press(KeyEvent.KEYCODE_DPAD_RIGHT)
                press(KeyEvent.KEYCODE_DPAD_CENTER) // -5 minutes
                assertPosition(media, beginning, "minus300")
                press(KeyEvent.KEYCODE_DPAD_RIGHT)
                press(KeyEvent.KEYCODE_DPAD_CENTER)
                assertPosition(media, beginning + 300_000, "restore300")

                press(KeyEvent.KEYCODE_DPAD_RIGHT) // Speed
                press(KeyEvent.KEYCODE_DPAD_CENTER)
                compose.onNodeWithTag("option:1.0").assertIsFocused()
                repeat(2) { press(KeyEvent.KEYCODE_DPAD_DOWN) }
                compose.onNodeWithTag("option:1.5").assertIsFocused()
                press(KeyEvent.KEYCODE_DPAD_CENTER)
                compose.onNodeWithTag("player-action:6").assertIsFocused()
                // Platform PlaybackState reports speed 0 while paused, regardless of the chosen rate.
                compose.onNodeWithContentDescription("速度 1.5×").assertExists()

                val stable = position(media)
                repeat(6) { press(KeyEvent.KEYCODE_DPAD_LEFT) }
                press(KeyEvent.KEYCODE_DPAD_CENTER)
                compose.runOnIdle { assertTrue(full) }
                val bounds = compose.onNodeWithTag("player-video").getUnclippedBoundsInRoot()
                repeat(2) {
                    press(KeyEvent.KEYCODE_DPAD_UP)
                    compose.onNodeWithTag("player-action:0").assertDoesNotExist()
                    assertEquals(bounds, compose.onNodeWithTag("player-video").getUnclippedBoundsInRoot())
                    press(KeyEvent.KEYCODE_DPAD_DOWN)
                    compose.onNodeWithTag("player-action:0").assertIsFocused()
                    assertEquals(bounds, compose.onNodeWithTag("player-video").getUnclippedBoundsInRoot())
                    assertPosition(media, stable, "fullscreenPaused")
                    assertTrue("Fullscreen keeps the platform session", sessions.getActiveSessions(null)
                        .any { it.sessionToken == media.sessionToken })
                }
                press(KeyEvent.KEYCODE_DPAD_RIGHT)
                press(KeyEvent.KEYCODE_DPAD_CENTER)
                waitFor("Resume from visible control") { media.playbackState?.state == PlaybackState.STATE_PLAYING }
                waitFor("Actual media speed is 1.5 while playing") { media.playbackState?.playbackSpeed == 1.5f }
                compose.waitUntil(15_000) {
                    runCatching { compose.onNodeWithTag("player-position").fetchSemanticsNode()
                        .config[SemanticsProperties.Text].first().text != clock(stable) }.getOrDefault(false)
                }

                compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
                waitFor("ON_STOP pauses actual media") { media.playbackState?.state == PlaybackState.STATE_PAUSED }
                val background = position(media)
                SystemClock.sleep(1_500)
                assertEquals("Playback does not progress in background", background, position(media))
                compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
                compose.waitForIdle()
                assertPosition(media, background, "backgroundReturn")
                assertEquals(PlaybackState.STATE_PAUSED, media.playbackState?.state)

                press(KeyEvent.KEYCODE_BACK)
                compose.runOnIdle { assertFalse(full) }
                press(KeyEvent.KEYCODE_BACK)
                compose.runOnIdle { assertFalse(shown) }
                compose.waitUntil(5_000) { fixture.vm.history.records.value.any {
                    it.movie.id == detail.movie.id && abs(it.positionMs - background) <= 1_000
                } }
                waitFor("Leaving releases the platform session") {
                    sessions.getActiveSessions(null).none { it.sessionToken == media.sessionToken }
                }
                Log.i("V2DevicePlayback", "passed seek=30/300 speed=1.5 fullscreenStable=true backgroundPaused=true release=true")
            }
        } finally {
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }

    private fun press(key: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(key)
        compose.waitForIdle()
    }

    private fun position(media: MediaController) = requireNotNull(media.playbackState).position

    private fun assertPosition(media: MediaController, expected: Long, label: String) {
        waitFor("$label position=$expected") {
            val actual = media.playbackState
            actual?.state == PlaybackState.STATE_PAUSED && abs(actual.position - expected) <= 1_000
        }
        Log.i("V2DevicePlayback", "$label expected=$expected actual=${position(media)}")
    }

    private fun waitFor(message: String, timeoutMs: Long = 10_000, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (!condition()) {
            if (SystemClock.elapsedRealtime() >= deadline) fail(message)
            SystemClock.sleep(100)
        }
    }
}
