package com.olevod.tv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.olevod.tv.data.Filter
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** -1 means an unknown total; it is never rendered as zero results. */
data class PagedFeedState<T>(val items:List<T> = emptyList(),val total:Int=-1,val nextPage:Int=1,
    val loading:Boolean=false,val endReached:Boolean=false,val error:String?=null)
data class FeedPage<T>(val items:List<T>,val total:Int,val hasMore:Boolean)
typealias CatalogFeedState = PagedFeedState<Movie>

/** One cumulative feed per query/account. Cancellation and generation both reject late results. */
open class PagedFeed<T>(private val scope:CoroutineScope,private val identity:(T)->Long,
                       private val loadPage:suspend(Int)->FeedPage<T>) {
    var state by mutableStateOf(PagedFeedState<T>())
        private set
    private var generation=0L
    private var job:Job?=null

    fun loadNext():Job? {
        if(state.loading || state.endReached)return null
        val requestedPage=state.nextPage
        val requestGeneration=generation
        state=state.copy(loading=true,error=null)
        job=scope.launch {
            try {
                val page=loadPage(requestedPage)
                currentCoroutineContext().ensureActive()
                if(requestGeneration!=generation)return@launch
                val known=state.items.map(identity).toHashSet()
                val additions=page.items.distinctBy(identity).filter{identity(it) !in known}
                if(page.items.isNotEmpty() && additions.isEmpty() && page.hasMore) {
                    state=state.copy(loading=false,error="网站返回了重复内容，请重试加载")
                } else {
                    state=state.copy(items=state.items+additions,total=page.total,nextPage=requestedPage+1,
                        loading=false,endReached=page.items.isEmpty() || !page.hasMore,error=null)
                }
            } catch(e:Exception) {
                if(requestGeneration==generation)state=state.copy(loading=false,error=if(e is CancellationException)null else safeError(e))
                if(e is CancellationException)throw e
            }
        }
        return job
    }

    fun cancel() {
        generation++
        job?.cancel();job=null
        state=state.copy(loading=false)
    }
    fun reset() { cancel();state=PagedFeedState() }
}

class CatalogFeed(scope:CoroutineScope,loadPage:suspend(Int)->com.olevod.tv.data.CatalogPage):
    PagedFeed<Movie>(scope,{it.id},{page->loadPage(page).let{FeedPage(it.items,it.total,it.hasMore)}}) {
    constructor(api:OlevodApi,filter:Filter,scope:CoroutineScope):this(scope,{page->api.browse(filter,page=page)})
}
