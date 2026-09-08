package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.Episode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class EpisodePickerTest {
    @get:Rule val compose=createComposeRule()
    @Test fun browsingAGroupDoesNotPlayUntilAnEpisodeIsChosenAndUpReturnsThroughRows(){
        val group=mutableIntStateOf(0)
        val playing=mutableIntStateOf(1)
        var selected:Int?=null
        compose.setContent{
            val controls=remember{FocusRequester()};val groupEntry=remember{FocusRequester()}
            MaterialTheme{Column(Modifier.width(800.dp)){
                TvAction("播放控制",modifier=Modifier.focusRequester(controls)){}
                EpisodePicker((1..31).map{Episode(it,"第 $it 集","",false)},playing.intValue,group.intValue,
                    chooseGroup={group.intValue=it},play={selected=it.index},
                    controlsFocus=controls,groupEntry=groupEntry,onFocusWithin={})
            }}
        }
        compose.onNodeWithText("21–30").performClick()
        compose.runOnIdle{assertNull(selected)}
        compose.onNodeWithText("21").assertExists()
        compose.onNodeWithText("30").assertExists()
        compose.onNodeWithText("11").assertDoesNotExist()
        compose.onNodeWithText("21–30").performSemanticsAction(SemanticsActions.RequestFocus){it()}
        compose.onNodeWithText("21–30").performKeyInput{pressKey(Key.DirectionDown)}
        compose.onNodeWithText("21").assertIsFocused()
        compose.onNodeWithText("21").performKeyInput{pressKey(Key.DirectionUp)}
        compose.onNodeWithText("21–30").assertIsFocused()
        compose.onNodeWithText("21–30").performKeyInput{pressKey(Key.DirectionUp)}
        compose.onNodeWithText("播放控制").assertIsFocused()
        compose.onNodeWithText("24").performClick()
        compose.runOnIdle{assertEquals(24,selected)}
        compose.onNodeWithText("24").performSemanticsAction(SemanticsActions.RequestFocus){it()}
        compose.runOnIdle{playing.intValue=31}
        compose.onNodeWithText("24").assertIsFocused()
        compose.onNodeWithText("21–30").assertExists()
    }
}
