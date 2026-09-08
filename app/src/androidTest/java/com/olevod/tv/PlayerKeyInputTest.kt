package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlayerKeyInputTest {
    @get:Rule val compose=createComposeRule()
    @Test fun hiddenSurfaceReceivesWakeKeysAndReturnsFocusToControls(){
        val hidden=mutableStateOf(true)
        compose.setContent{
            val surface=remember{FocusRequester()};val controls=remember{FocusRequester()}
            LaunchedEffect(hidden.value){if(hidden.value)surface.requestFocus()else controls.requestFocus()}
            MaterialTheme{Box(Modifier.size(800.dp,450.dp).testTag("surface").playerKeyInput(surface,hidden.value){event->
                if(event.type!=KeyEventType.KeyDown)false else when(event.key){
                    Key.DirectionDown->{hidden.value=false;true}
                    Key.DirectionUp->{hidden.value=true;true}
                    else->false
                }
            }){if(!hidden.value)TvAction("全屏控制",modifier=Modifier.focusRequester(controls)){} }}
        }
        compose.onNodeWithTag("surface").assertIsFocused()
        compose.onNodeWithTag("surface").performKeyInput{pressKey(Key.DirectionDown)}
        compose.onNodeWithText("全屏控制").assertIsFocused()
        compose.onNodeWithText("全屏控制").performKeyInput{pressKey(Key.DirectionUp)}
        compose.onNodeWithText("全屏控制").assertDoesNotExist()
        compose.onNodeWithTag("surface").assertIsFocused()
        compose.onNodeWithTag("surface").performKeyInput{pressKey(Key.DirectionDown)}
        compose.onNodeWithText("全屏控制").assertIsFocused()
    }
}
