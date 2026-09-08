package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.olevod.tv.data.Detail
import com.olevod.tv.data.Episode

@Composable
internal fun PlayerPreviewFixture(movie:Movie,full:Boolean,toggleFull:()->Unit,back:()->Unit){
    val detail=remember(movie){Detail(movie,"这是一段用于核对排版的简介。正式播放会显示网站提供的影片介绍。长简介限制为四行，完整内容通过展开查看，不改变视频区域大小。", "", "",(1..32).map{Episode(it,"第${it}集","",false)},false,1,0)}
    var state by remember(movie){mutableStateOf(PlayerUiState(movie,detail,episode=0,position=384000,duration=5400000,seekable=true,trackInfo=videoTrackInfo(1920,1080,4200000,-1)))}
    PlayerContent(state,full,PlayerActions(back,toggleFull,{state=state.copy(playing=!state.playing,playRequested=!state.playing)},
        {state=state.copy(position=jumpPosition(state.position,state.duration,it))},{state=state.copy(speed=it)},{state=state.copy(favorite=!state.favorite)},
        {state=state.copy(group=it)},{state=state.copy(episode=detail.episodes.indexOf(it),position=0)}, {}, {})){
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black),contentAlignment=Alignment.Center){
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
                OfficialOlevodLogo(Modifier.width(140.dp).height(25.dp))
                Text("播放器布局预览 · 尚未加载视频",color=Muted,fontSize=14.sp)
            }
        }
    }
}
