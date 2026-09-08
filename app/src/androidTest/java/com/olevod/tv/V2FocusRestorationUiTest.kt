@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test

class V2FocusRestorationUiTest {
    @get:Rule val compose=createComposeRule()
    private fun key(code:Int)=InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)

    @Test fun duplicateMovieReturnsToTheExactSectionAndDoesNotStealOnMetadataChange() {
        var updateMetadata:()->Unit={}
        compose.setContent {
            val holder=rememberSaveableStateHolder()
            var player by remember{mutableStateOf(false)}
            var title by remember{mutableStateOf("同一影片")}
            updateMetadata={title="同一影片 · 元数据已补全"}
            MaterialTheme {
                if(player){
                    val returnFocus=remember{FocusRequester()}
                    BackHandler{player=false}
                    TvAction("返回",modifier=Modifier.focusRequester(returnFocus)){player=false}
                    LaunchedEffect(Unit){returnFocus.requestFocus()}
                } else holder.SaveableStateProvider("home") {
                    ContentFocusScope {
                        val first=remember{FocusRequester()}
                        Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                            listOf("recent","latest").forEach { section ->
                                PosterFocusGroup(section) {
                                    PosterTile(Movie(1,title,"",year="2026"),Modifier.width(90.dp)
                                        .then(if(section=="recent")Modifier.focusRequester(first)else Modifier)){player=true}
                                }
                            }
                        }
                        val memory=LocalContentFocusMemory.current
                        LaunchedEffect(Unit){if(memory?.anchor?.value==null)first.requestFocus()}
                    }
                }
            }
        }
        compose.onAllNodesWithTag("poster:1").onFirst().assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onAllNodesWithTag("poster:1").onLast().assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("返回").assertIsFocused()
        key(KeyEvent.KEYCODE_BACK)
        compose.onAllNodesWithTag("poster:1").onLast().assertIsFocused()
        compose.runOnIdle{updateMetadata()}
        compose.onAllNodesWithTag("poster:1").onLast().assertIsFocused()
        compose.onAllNodesWithTag("poster:1").onFirst().assertIsNotFocused()
    }
}
