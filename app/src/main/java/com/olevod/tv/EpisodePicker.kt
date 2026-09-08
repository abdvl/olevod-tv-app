package com.olevod.tv

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.olevod.tv.data.Episode

@Composable
internal fun EpisodePicker(episodes:List<Episode>,playingIndex:Int,groupIndex:Int,
    chooseGroup:(Int)->Unit,play:(Episode)->Unit,controlsFocus:FocusRequester,groupEntry:FocusRequester,onFocusWithin:(Boolean)->Unit){
    if(episodes.isEmpty())return
    val groups=episodes.chunked(10)
    val selected=groupIndex.coerceIn(groups.indices)
    val shown=groups[selected]
    val groupRefs=remember(episodes.map{it.index}){groups.map{FocusRequester()}}
    val episodeRefs=remember(shown.map{it.index}){shown.map{FocusRequester()}}
    var lastEpisodes by rememberSaveable{mutableStateOf(mapOf<Int,Int>())}
    val entryIndex=shown.indexOfFirst{it.index==playingIndex}.takeIf{it>=0}
        ?:shown.indexOfFirst{it.index==lastEpisodes[selected]}.takeIf{it>=0}?:0
    val focusCallback by rememberUpdatedState(onFocusWithin)
    DisposableEffect(Unit){onDispose{focusCallback(false)}}
    Column(Modifier.fillMaxWidth().onFocusChanged{onFocusWithin(it.hasFocus)}.focusGroup(),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            groups.forEachIndexed{index,group->EpisodeButton("${group.first().index}–${group.last().index}",index==selected,false,
                Modifier.widthIn(min=60.dp).focusRequester(groupRefs[index]).then(if(index==selected)Modifier.focusRequester(groupEntry)else Modifier)
                    .testTag("episode-group:$index").focusProperties{
                        up=controlsFocus;down=episodeRefs[entryIndex]
                        left=if(index>0)groupRefs[index-1]else FocusRequester.Cancel
                        right=if(index<groups.lastIndex)groupRefs[index+1]else FocusRequester.Cancel
                    }){chooseGroup(index)} }
        }
        key(selected){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            shown.forEachIndexed{index,episode->
                EpisodeButton(episodeLabel(episode),false,episode.index==playingIndex,Modifier.weight(1f).focusRequester(episodeRefs[index])
                    .testTag("episode:${episode.index}").semantics{contentDescription="${episode.title}，第 ${episode.index} 集"}
                    .onFocusChanged{if(it.isFocused)lastEpisodes=lastEpisodes+(selected to episode.index)}.focusProperties{
                        up=groupRefs[selected];down=FocusRequester.Cancel
                        left=if(index>0)episodeRefs[index-1]else FocusRequester.Cancel
                        right=if(index<shown.lastIndex)episodeRefs[index+1]else FocusRequester.Cancel
                    }){if(episode.index!=playingIndex)play(episode)}
            }
            repeat(10-shown.size){Spacer(Modifier.weight(1f))}
        }}
    }
}
internal fun episodeLabel(episode:Episode):String = episode.title.takeIf{it.isNotBlank()&&!it.matches(Regex("(?:第)?\\s*0*${episode.index}\\s*(?:集|期)?"))}?:episode.index.toString()
@Composable
private fun EpisodeButton(label:String,selected:Boolean,playing:Boolean,modifier:Modifier,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState()
    Column(modifier.height(36.dp).background(if(focused)Green else if(playing)Green.copy(alpha=.12f)else Color.Transparent,RoundedCornerShape(7.dp))
        .semantics{this.selected=selected||playing}.clickable(interactionSource=source,indication=null,onClick=onClick).padding(horizontal=6.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Text((if(playing)"▶ "else"")+label,color=if(focused)Bg else if(selected||playing)Green else White,fontSize=13.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=if(selected||playing)FontWeight.Bold else FontWeight.Normal)
        Box(Modifier.width(18.dp).height(2.dp).background(if(selected&&!focused)Green else Color.Transparent))
    }
}
