package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExitConfirmationDialogTest {
    @get:Rule val compose=createComposeRule()
    private fun press(key:Int){
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(key)
        compose.waitForIdle()
    }
    @Test fun defaultContinueAndRemoteLeftRightConfirm() {
        var dismissed=0
        var confirmed=0
        compose.setContent{MaterialTheme{ExitConfirmationDialog({dismissed++},{confirmed++})}}
        compose.onNodeWithText("继续观看").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithText("继续观看").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("退出应用").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("退出应用").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithText("继续观看").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle{assertEquals(1,dismissed);assertEquals(0,confirmed)}
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle{assertEquals(1,dismissed);assertEquals(1,confirmed)}
    }
    @Test fun backDismissesWithoutConfirmingExit() {
        var dismissed=0
        var confirmed=0
        compose.setContent{MaterialTheme{ExitConfirmationDialog({dismissed++},{confirmed++})}}
        compose.onNodeWithText("继续观看").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_BACK)
        compose.runOnIdle{assertEquals(1,dismissed);assertEquals(0,confirmed)}
    }
}
