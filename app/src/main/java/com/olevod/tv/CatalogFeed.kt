package com.olevod.tv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.olevod.tv.data.Filter
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class CatalogFeedState(val items:List<Movie> = emptyList(),val total:Int=0,val nextPage:Int=1,val loading:Boolean=false,val endReached:Boolean=false,val error:String?=null)

class CatalogFeed(private val scope:CoroutineScope,private val loadPage:suspend(Int)->com.olevod.tv.data.CatalogPage) {
    constructor(api:OlevodApi,filter:Filter,scope:CoroutineScope):this(scope,{page->api.browse(filter,page=page)})
    var state by mutableStateOf(CatalogFeedState())
        private set
    fun loadNext():Job? {
        if(state.loading || state.endReached)return null
        state=state.copy(loading=true,error=null)
        return scope.launch {
            try {
                val page=loadPage(state.nextPage)
                val merged=(state.items+page.items).distinctBy{it.id}
                state=state.copy(items=merged,total=page.total,nextPage=state.nextPage+1,loading=false,
                    endReached=page.items.isEmpty() || !page.hasMore)
            }catch(e:Exception){
                state=state.copy(loading=false,error=if(e is CancellationException)null else safeError(e))
                if(e is CancellationException)throw e
            }
        }
    }
}
