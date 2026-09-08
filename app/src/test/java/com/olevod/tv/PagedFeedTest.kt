package com.olevod.tv

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PagedFeedTest {
    @Test fun duplicatePageStopsAutomaticProgressAndRetriesTheSamePage()=runBlocking {
        val requests=mutableListOf<Int>()
        var duplicate=true
        val feed=PagedFeed(this,{it:Long->it}) {page->
            requests+=page
            when(page){1->FeedPage(listOf(1L,2L),10,true);else->if(duplicate)FeedPage(listOf(2L,1L),10,true)else FeedPage(listOf(2L,3L),3,false)}
        }
        feed.loadNext()!!.join();feed.loadNext()!!.join()
        assertEquals(listOf(1L,2L),feed.state.items)
        assertEquals(2,feed.state.nextPage);assertNotNull(feed.state.error)
        duplicate=false;feed.loadNext()!!.join()
        assertEquals(listOf(1,2,2),requests)
        assertEquals(listOf(1L,2L,3L),feed.state.items);assertTrue(feed.state.endReached)
    }
    @Test fun resetRejectsNonCooperativeOldResponseAndKeepsNewGeneration()=runBlocking {
        val oldResponse=CompletableDeferred<Unit>();val started=CompletableDeferred<Unit>()
        var old=true
        val feed=PagedFeed(this,{it:Long->it}) {
            if(old){started.complete(Unit);withContext(NonCancellable){oldResponse.await()};FeedPage(listOf(1L),1,false)}
            else FeedPage(listOf(2L),1,false)
        }
        val obsolete=feed.loadNext()!!;started.await();feed.reset();old=false
        feed.loadNext()!!.join();oldResponse.complete(Unit);obsolete.join()
        assertEquals(listOf(2L),feed.state.items);assertNull(feed.state.error);assertFalse(feed.state.loading)
    }
    @Test fun canceledRequestCanRetryWithoutAdvancingOrLosingEarlierPage()=runBlocking {
        val started=CompletableDeferred<Unit>()
        val feed=PagedFeed(this,{it:Long->it}) {page->
            if(page==1)FeedPage(listOf(1L),-1,true)else{started.complete(Unit);awaitCancellation()}
        }
        feed.loadNext()!!.join();val pending=feed.loadNext()!!;started.await();feed.cancel();pending.join()
        assertEquals(listOf(1L),feed.state.items);assertEquals(2,feed.state.nextPage)
        assertEquals(-1,feed.state.total);assertFalse(feed.state.loading);assertNull(feed.state.error)
    }
}
