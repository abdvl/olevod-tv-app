package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text

/** The caller owns visibility and performs the actual application exit. */
@Composable
internal fun ExitConfirmationDialog(onDismiss:()->Unit,onConfirm:()->Unit) {
    val continueFocus=remember{FocusRequester()}
    val exitFocus=remember{FocusRequester()}
    Dialog(onDismissRequest=onDismiss) {
        val inputMode=LocalInputModeManager.current
        Column(
            Modifier.width(420.dp).background(Panel,RoundedCornerShape(16.dp)).padding(28.dp),
            verticalArrangement=Arrangement.spacedBy(20.dp)
        ) {
            Text("退出应用？",color=White,fontSize=24.sp)
            Text("选择继续观看可返回当前页面。",color=Muted,fontSize=15.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                TvAction("继续观看",modifier=Modifier.weight(1f).focusRequester(continueFocus)
                    .focusProperties{left=FocusRequester.Cancel;right=exitFocus;up=FocusRequester.Cancel;down=FocusRequester.Cancel},onClick=onDismiss)
                TvAction("退出应用",modifier=Modifier.weight(1f).focusRequester(exitFocus)
                    .focusProperties{left=continueFocus;right=FocusRequester.Cancel;up=FocusRequester.Cancel;down=FocusRequester.Cancel},onClick=onConfirm)
            }
        }
        LaunchedEffect(Unit){inputMode.requestInputMode(InputMode.Keyboard);withFrameNanos{};continueFocus.requestFocus()}
    }
}
