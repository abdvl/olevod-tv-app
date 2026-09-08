package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Fullscreen controls are a sibling overlay and never participate in sizing the video. */
@Composable
internal fun PlayerVideoStage(
    full:Boolean,
    controlsVisible:Boolean,
    modifier:Modifier=Modifier,
    video:@Composable ()->Unit,
    controlContent:@Composable ()->Unit
) {
    if(full)Box(modifier.fillMaxHeight()){
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){video()}
        if(controlsVisible)Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.82f))))
                .padding(start=30.dp,top=28.dp,end=30.dp,bottom=24.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){controlContent()}
    }else Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)){
        Box(Modifier.fillMaxWidth().aspectRatio(16f/9),contentAlignment=Alignment.Center){video()}
        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(9.dp)){controlContent()}
    }
}
