package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistorySyncIntegrationTest {
    @Test fun syncActualLocalWatchAndReadBack()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val sessions=SessionStore(context);assertNotNull(sessions.token)
        val store=HistoryStore(context){sessions.accountKey};store.load()
        val watch=store.records.value.firstOrNull{it.movie.id==80632L && it.positionMs>=1000 && it.durationMs>0}
        assertNotNull("Requires an actual native VIP viewing record",watch)
        val api=OlevodApi(token={sessions.token});api.syncWatch(watch!!)
        var cloud:WatchRecord?=null
        for(attempt in 0 until 8){
            cloud=api.cloudHistory(1).items.firstOrNull{it.movie.id==watch.movie.id}
            if(cloud!=null && kotlin.math.abs(watch.positionMs-cloud.positionMs)<1500)break
            kotlinx.coroutines.delay(2000)
        }
        assertNotNull("New watch must appear in account history",cloud)
        assertEquals(watch.episode,cloud!!.episode)
        assertTrue(kotlin.math.abs(watch.positionMs-cloud.positionMs)<1500)
        store.close()
    }
}
