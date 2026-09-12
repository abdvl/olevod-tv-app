package com.olevod.tv

import android.app.Application
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsNavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun settingsIsLeftOfAvatarAndDpadReturnsToHeader() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val vm = SettingsViewModel(app)
        compose.setContent {
            val refs = remember { navigationItems.associate { it.key to FocusRequester() } }
            var selected by remember { mutableStateOf("settings") }
            val page = remember { PageFocusController(refs.getValue("settings"), FocusRequester()) }
            MaterialTheme {
                Column {
                    UnifiedHeader(selected, refs, { page.enterContent() }) { selected = it }
                    CompositionLocalProvider(LocalPageFocus provides page) { SettingsScreen(vm) }
                }
            }
        }
        assertEquals("settings", navigationItems[navigationItems.indexOfFirst { it.key == "account" } - 1].key)
        compose.onNodeWithTag("nav:settings").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("nav:account").assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithTag("nav:settings").assertIsFocused().assertContentDescriptionEquals("设置")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("check-update").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithText("当前版本  ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        key(KeyEvent.KEYCODE_DPAD_UP)
        compose.onNodeWithTag("nav:settings").assertIsFocused()
    }
    private fun key(code: Int) = InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
}
