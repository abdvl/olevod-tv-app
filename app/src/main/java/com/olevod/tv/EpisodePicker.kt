package com.olevod.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.olevod.tv.data.Episode

@Composable
internal fun EpisodePicker(
    episodes:List<Episode>,playingIndex:Int,groupIndex:Int,
    chooseGroup:(Int)->Unit,play:(Episode)->Unit,
    controlsFocus:FocusRequester,groupEntry:FocusRequester,
    onFocusWithin:(Boolean)->Unit
){
    if(episodes.isEmpty())return
    val groups=episodes.chunked(10)
    val selectedGroup=groupIndex.coerceIn(groups.indices)
    val shown=groups[selectedGroup]
    val episodeEntry=remember{FocusRequester()}
    val entryIndex=shown.firstOrNull{it.index==playingIndex}?.index?:shown.first().index
    val currentFocusCallback by rememberUpdatedState(onFocusWithin)
    DisposableEffect(Unit){onDispose{currentFocusCallback(false)}}
    Column(Modifier.fillMaxWidth().onFocusChanged{onFocusWithin(it.hasFocus)}.focusGroup(),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Text("选集",color=Muted,fontSize=12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                groups.forEachIndexed{index,group->
                    TvAction("${group.first().index}–${group.last().index}",selected=index==selectedGroup,
                        modifier=Modifier.then(if(index==selectedGroup)Modifier.focusRequester(groupEntry)else Modifier)
                            .focusProperties{up=controlsFocus;down=episodeEntry}){chooseGroup(index)}
                }
            }
        }
        key(selectedGroup){
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                shown.forEach{episode->
                    TvAction(episode.index.toString(),selected=episode.index==playingIndex,
                        modifier=Modifier.widthIn(min=42.dp)
                            .then(if(episode.index==entryIndex)Modifier.focusRequester(episodeEntry)else Modifier)
                            .focusProperties{up=groupEntry;down=FocusRequester.Cancel}){play(episode)}
                }
            }
        }
    }
}
