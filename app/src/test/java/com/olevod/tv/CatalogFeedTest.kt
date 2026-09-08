package com.olevod.tv

import com.olevod.tv.data.Filter
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CatalogFeedTest {
    @Test fun appendKeepsExistingItemsRetriesSamePageAndStopsAtEnd()=runBlocking {
        val server=MockWebServer();server.start()
        fun page(ids:List<Int>)=JSONObject().put("code",0).put("data",JSONObject().put("total",21)
            .put("list",JSONArray(ids.map{JSONObject().put("id",it).put("name","Film $it")}))).toString()
        try {
            server.enqueue(MockResponse().setBody(page((1..20).toList())))
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setBody(page(listOf(20,21))))
            val feed=CatalogFeed(OlevodApi(base=server.url("/").toString()),Filter(),this)
            val first=feed.loadNext()!!
            assertNull(feed.loadNext())
            first.join();assertEquals(20,feed.state.items.size)
            feed.loadNext()!!.join()
            assertEquals(20,feed.state.items.size);assertNotNull(feed.state.error)
            assertEquals(2,feed.state.nextPage)
            feed.loadNext()!!.join()
            assertEquals((1L..21L).toList(),feed.state.items.map{it.id})
            assertTrue(feed.state.endReached);assertNull(feed.loadNext())
            val requests=List(3){server.takeRequest().requestUrl!!.pathSegments}
            assertEquals("1",requests[0][requests[0].size-2])
            assertEquals("2",requests[1][requests[1].size-2])
            assertEquals("2",requests[2][requests[2].size-2])
        }finally{server.shutdown()}
    }
}
