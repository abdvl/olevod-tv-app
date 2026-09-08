package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

@Composable
internal fun TvConfirmDialog(title:String,message:String,confirmLabel:String="确认",cancelLabel:String="取消",onCancel:()->Unit,onConfirm:()->Unit){
    val cancel=remember{FocusRequester()};val confirm=remember{FocusRequester()}
    Dialog(onDismissRequest=onCancel,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(Modifier.width(420.dp).heightIn(max=440.dp).background(Panel,RoundedCornerShape(14.dp)).padding(24.dp).testTag("confirm-dialog"),verticalArrangement=Arrangement.spacedBy(18.dp)){
            Text(title,color=White,fontSize=22.sp,lineHeight=28.sp)
            Text(message,color=Muted,fontSize=16.sp,lineHeight=24.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                TvAction(cancelLabel,modifier=Modifier.weight(1f).focusRequester(cancel).testTag("confirm-cancel").focusProperties{right=confirm;left=FocusRequester.Cancel;up=FocusRequester.Cancel;down=FocusRequester.Cancel},onClick=onCancel)
                TvAction(confirmLabel,modifier=Modifier.weight(1f).focusRequester(confirm).testTag("confirm-accept").focusProperties{left=cancel;right=FocusRequester.Cancel;up=FocusRequester.Cancel;down=FocusRequester.Cancel},onClick=onConfirm)
            }
        }
        LaunchedEffect(Unit){withFrameNanos{};cancel.requestFocus()}
    }
}
