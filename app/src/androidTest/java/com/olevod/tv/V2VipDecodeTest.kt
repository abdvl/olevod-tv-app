package com.olevod.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.OlevodApi
import com.olevod.tv.data.SessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Authorized VIP media decoding only. Reads the saved session; never writes account/history. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class V2VipDecodeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savedVipSourceRendersVideoAndProcessesAudio(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogin") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val session = SessionStore(context)
        assertNotNull("A current authorized login is required", session.token)
        val detail = OlevodApi(token = { session.token }).detail(80632)
        val source = detail.episodes.firstOrNull { it.uri.startsWith("https://") }
        assertNotNull("VIP sample needs an authorized HTTPS source", source)
        val rendered = AtomicBoolean(false)
        val errorCode = AtomicInteger(0)
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            val http = DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OlevodTV/0.1")
                .setDefaultRequestProperties(mapOf("Referer" to "https://www.olevod.com/"))
            player = ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(http))
                .setAudioAttributes(AudioAttributes.DEFAULT, true).build()
            player.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() { rendered.set(true) }
                override fun onPlayerError(error: PlaybackException) { errorCode.set(error.errorCode) }
            })
        }
        try {
            compose.setContent {
                AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false } },
                    modifier = Modifier.fillMaxSize())
            }
            compose.runOnIdle {
                player.setMediaItem(MediaItem.fromUri(source!!.uri))
                player.prepare()
                player.play()
            }
            compose.waitUntil(60_000) { rendered.get() || errorCode.get() != 0 }
            assertEquals("VIP decoder error code (URL omitted)", 0, errorCode.get())
            assertTrue("VIP first frame must be rendered", rendered.get())
            var start = 0L
            compose.runOnIdle { start = player.currentPosition }
            Thread.sleep(5_000)
            compose.runOnIdle {
                assertTrue("VIP playback must remain error-free", player.playerError == null)
                assertTrue(player.isPlaying)
                val format = player.videoFormat
                assertNotNull(format)
                assertTrue(format!!.width > 0 && format.height > 0)
                assertTrue("VIP position must advance through actual decoding", player.currentPosition >= start + 3_000)
                val video = player.videoDecoderCounters
                val audio = player.audioDecoderCounters
                video?.ensureUpdated(); audio?.ensureUpdated()
                assertTrue("Decoded video output is required", (video?.renderedOutputBufferCount ?: 0) > 0)
                assertTrue("Processed audio output is required", (audio?.renderedOutputBufferCount ?: 0) > 0)
                android.util.Log.i("V2VipDecode", "firstFrame=true width=${format.width} height=${format.height} " +
                    "startMs=$start endMs=${player.currentPosition} videoBuffers=${video?.renderedOutputBufferCount} " +
                    "audioBuffers=${audio?.renderedOutputBufferCount}")
            }
        } finally {
            instrumentation.runOnMainSync { player.release() }
        }
    }
}
