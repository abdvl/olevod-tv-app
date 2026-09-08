package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerVideoStageTest {
    @get:Rule val compose=createComposeRule()
    @Test fun fullscreenControlsDoNotResizeOrMoveVideo(){
        val visible=mutableStateOf(true)
        compose.setContent{
            Box(Modifier.size(800.dp,450.dp)){
                PlayerVideoStage(true,visible.value,Modifier.fillMaxSize(),
                    video={Box(Modifier.fillMaxSize().testTag("video"))},
                    controlContent={Box(Modifier.fillMaxWidth().height(80.dp).testTag("controls"))})
            }
        }
        val shown=compose.onNodeWithTag("video").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("controls").assertExists()
        compose.runOnIdle{visible.value=false}
        compose.onNodeWithTag("controls").assertDoesNotExist()
        assertEquals(shown,compose.onNodeWithTag("video").getUnclippedBoundsInRoot())
        compose.runOnIdle{visible.value=true}
        compose.onNodeWithTag("controls").assertExists()
        assertEquals(shown,compose.onNodeWithTag("video").getUnclippedBoundsInRoot())
    }
}
