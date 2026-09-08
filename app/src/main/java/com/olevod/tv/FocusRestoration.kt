package com.olevod.tv

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*

/** One saved identity per route; duplicates in separate sections never share an anchor. */
internal class ContentFocusMemory(val anchor:MutableState<String?>) {
    val targets=mutableMapOf<String,FocusRequester>()
    var restorePending=anchor.value!=null
    fun remember(identity:String) { anchor.value=identity;restorePending=false }
    fun enter():Boolean = targets[anchor.value]?.requestFocus() ?: false
}
internal val LocalContentFocusMemory=compositionLocalOf<ContentFocusMemory?>{null}
internal val LocalFocusSection=compositionLocalOf{"content"}

/** Lazy layouts may need several frames to attach a target after scrolling or a page append. */
internal suspend fun FocusRequester.requestWhenAttached(stillWanted:()->Boolean={true}):Boolean {
    repeat(30){
        if(!stillWanted())return false
        val focused=try{requestFocus()}catch(_:IllegalStateException){false}
        if(focused)return true
        withFrameNanos{}
    }
    return false
}

@Composable
internal fun ContentFocusScope(content:@Composable ()->Unit) {
    val anchor=rememberSaveable{mutableStateOf<String?>(null)}
    val memory=remember{ContentFocusMemory(anchor)}
    val page=LocalPageFocus.current
    DisposableEffect(page,memory){
        page?.restoreBody={memory.enter()}
        onDispose{page?.restoreBody=null}
    }
    CompositionLocalProvider(LocalContentFocusMemory provides memory,content=content)
}

/** Call only on actual focus targets, after route/query/section identity has been established. */
@Composable
internal fun Modifier.restoreContentFocus(entity:String, requester:FocusRequester?=null):Modifier {
    val section=LocalFocusSection.current
    val identity="$section:$entity"
    val memory=LocalContentFocusMemory.current
    val target=requester?:remember(identity){FocusRequester()}
    DisposableEffect(memory,identity,target){
        memory?.targets?.set(identity,target)
        onDispose{if(memory?.targets?.get(identity)===target)memory.targets.remove(identity)}
    }
    LaunchedEffect(memory,identity){
        if(memory?.restorePending==true && memory.anchor.value==identity){
            withFrameNanos{}
            if(memory.restorePending && memory.anchor.value==identity){
                if(target.requestFocus())memory.restorePending=false
            }
        }
    }
    return this.focusRequester(target).onFocusChanged{if(it.isFocused)memory?.remember(identity)}
}
