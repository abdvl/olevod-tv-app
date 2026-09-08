package com.olevod.tv

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

internal data class FilterOption(val value:String,val label:String)
internal val sortOptions=listOf(FilterOption("desc","最新上传"),FilterOption("update","最近更新"),FilterOption("hot","人气最高"),FilterOption("score","评分最高"))
internal val membershipOptions=listOf(FilterOption("3","全部影片"),FilterOption("1","会员"),FilterOption("2","免费"))
internal val initialOptions=listOf(FilterOption("0","全部"))+('A'..'Z').map{FilterOption(it.toString(),it.toString())}

@Composable
internal fun OptionPopover(title:String,options:List<FilterOption>,selected:String,columns:Int=1,
                           onDismiss:()->Unit,onSelect:(String)->Unit) {
    val refs=remember(options){options.map{FocusRequester()}}
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.width(if(columns==1)360.dp else 390.dp).heightIn(max=330.dp).background(Panel,RoundedCornerShape(16.dp)).padding(20.dp)
            .testTag("option-popover"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("选择$title",color=White,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold)
            Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                options.chunked(columns).forEachIndexed { row,values ->
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        values.forEachIndexed { col,opt -> val i=row*columns+col
                            OptionChip(opt.label,opt.value==selected,Modifier.weight(1f).focusRequester(refs[i])
                                .testTag("option:${opt.value}").focusProperties {
                                    left=if(col>0)refs[i-1]else FocusRequester.Cancel
                                    right=if(col<values.lastIndex)refs[i+1]else FocusRequester.Cancel
                                    up=if(row>0)refs[i-columns]else FocusRequester.Cancel
                                    down=if((row+1)*columns<options.size)refs[minOf(i+columns,options.lastIndex)]else FocusRequester.Cancel
                                }){onSelect(opt.value)}
                        }
                        repeat(columns-values.size){Spacer(Modifier.weight(1f))}
                    }
                }
            }
            Text("确认应用 · 返回取消",color=Muted,fontSize=13.sp,lineHeight=18.sp)
        }
        LaunchedEffect(Unit){withFrameNanos{};refs.getOrNull(options.indexOfFirst{it.value==selected}.coerceAtLeast(0))?.requestFocus()}
    }
}

@Composable
internal fun MoreFiltersPopover(membership:Int,initial:String,onDismiss:()->Unit,onApply:(Int,String)->Unit){
    var draftMembership by remember{mutableIntStateOf(membership)}
    var draftInitial by remember{mutableStateOf(initial)}
    val memberRefs=remember{List(3){FocusRequester()}}
    val letterRefs=remember{initialOptions.map{FocusRequester()}}
    val actions=remember{List(3){FocusRequester()}}
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(Modifier.width(530.dp).heightIn(max=450.dp).background(Panel,RoundedCornerShape(16.dp)).padding(20.dp).testTag("more-filters"),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("更多筛选",color=White,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold)
            Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("影片范围",color=Muted,fontSize=13.sp,lineHeight=18.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    membershipOptions.forEachIndexed { i,opt->OptionChip(opt.label,draftMembership.toString()==opt.value,Modifier.weight(1f).focusRequester(memberRefs[i]).focusProperties{
                        up=FocusRequester.Cancel;down=letterRefs[i];left=if(i>0)memberRefs[i-1]else FocusRequester.Cancel;right=if(i<2)memberRefs[i+1]else FocusRequester.Cancel
                    }){draftMembership=opt.value.toInt()} }
                }
                Text("首字母",color=Muted,fontSize=13.sp,lineHeight=18.sp)
                initialOptions.chunked(7).forEachIndexed{row,values->Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    values.forEachIndexed{col,opt->val i=row*7+col
                        OptionChip(opt.label,draftInitial==opt.value,Modifier.weight(1f).focusRequester(letterRefs[i]).testTag("draft-letter:${opt.value}").focusProperties{
                            up=if(row>0)letterRefs[i-7]else memberRefs[minOf(col,2)]
                            down=if((row+1)*7<letterRefs.size)letterRefs[minOf(i+7,letterRefs.lastIndex)]else actions[minOf(col/3,2)]
                            left=if(col>0)letterRefs[i-1]else FocusRequester.Cancel
                            right=if(col<values.lastIndex)letterRefs[i+1]else FocusRequester.Cancel
                        },compact=true){draftInitial=opt.value}
                    };repeat(7-values.size){Spacer(Modifier.weight(1f))}
                }}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf("重置本面板","取消","应用").forEachIndexed { i,label->TvAction(label,modifier=Modifier.weight(1f).focusRequester(actions[i]).testTag("more-action:$i").focusProperties{
                    up=letterRefs[21+minOf(i*3,5)];down=FocusRequester.Cancel;left=if(i>0)actions[i-1]else FocusRequester.Cancel;right=if(i<2)actions[i+1]else FocusRequester.Cancel
                }){when(i){0->{draftMembership=3;draftInitial="0"};1->onDismiss();else->onApply(draftMembership,draftInitial)}} }
            }
        }
        LaunchedEffect(Unit){withFrameNanos{};memberRefs[membershipOptions.indexOfFirst{it.value==membership.toString()}.coerceAtLeast(0)].requestFocus()}
    }
}

@Composable
internal fun OptionChip(label:String,selected:Boolean,modifier:Modifier=Modifier,compact:Boolean=false,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState()
    Row(modifier.heightIn(min=36.dp).background(if(focused)Green else if(selected)Green.copy(alpha=.12f)else Color.Transparent,RoundedCornerShape(8.dp))
        .semantics{this.selected=selected}.clickable(interactionSource=source,indication=null,onClick=onClick).padding(horizontal=if(compact)4.dp else 8.dp,vertical=7.dp),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
        Text(label,color=if(focused)Bg else if(selected)Green else White,fontSize=14.sp,lineHeight=19.sp,modifier=Modifier.weight(1f),maxLines=if(compact)1 else 2)
        if(selected)Icon(Icons.Rounded.Check,null,Modifier.size(if(compact)12.dp else 16.dp),tint=if(focused)Bg else Green)
    }
}

@Composable
internal fun FilterTrigger(label:String,modifier:Modifier=Modifier,expanded:Boolean=false,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val hasFocus by source.collectIsFocusedAsState();val focused=hasFocus&&!expanded
    Row(modifier.heightIn(min=40.dp).background(if(focused)Green else Panel,RoundedCornerShape(12.dp))
        .border(1.dp,if(focused||expanded)Green else TvDesign.border,RoundedCornerShape(12.dp))
        .semantics{contentDescription=label}.clickable(interactionSource=source,indication=null,onClick=onClick).padding(horizontal=12.dp,vertical=9.dp),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        Text(label,color=if(focused)Bg else White,fontSize=14.sp,lineHeight=19.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
        Icon(Icons.Rounded.ExpandMore,null,Modifier.size(18.dp),tint=if(focused)Bg else White)
    }
}

@Composable
internal fun CatalogTitleButton(label:String,modifier:Modifier=Modifier,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState()
    Row(modifier.heightIn(min=44.dp).background(if(focused)Green else Color.Transparent,RoundedCornerShape(10.dp))
        .clickable(interactionSource=source,indication=null,onClick=onClick).padding(horizontal=4.dp),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Text(label,color=if(focused)Bg else White,fontSize=28.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold)
        Icon(Icons.Rounded.ExpandMore,null,Modifier.size(24.dp),tint=if(focused)Bg else White)
    }
}
