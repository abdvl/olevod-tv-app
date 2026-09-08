package com.olevod.tv

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent

/** The listener must wrap the focus target to receive keys when the surface itself has focus. */
internal fun Modifier.playerKeyInput(
    requester:FocusRequester,hiddenFullscreen:Boolean,onKey:(KeyEvent)->Boolean
):Modifier=focusRequester(requester).onPreviewKeyEvent(onKey).focusable(enabled=hiddenFullscreen)
