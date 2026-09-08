package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageIntegrationTest {
    @Test fun historyExceedsWebLimitAndSurvivesReopenWithAccountIsolation()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        var account="instrumentation-test-a"
        var db=HistoryStore(context){account}
        db.remove(null)
        repeat(25){i->db.save(Movie(990000L+i,"本机测试 $i",""),3,45000,120000)}
        assertEquals(25,db.records.value.size)
        db.close();db=HistoryStore(context){account};db.load()
        assertEquals(25,db.records.value.size)
        assertEquals(45000L,db.records.value.first().positionMs)
        account="instrumentation-test-b";db.load();assertTrue(db.records.value.isEmpty())
        db.save(Movie(990100,"隔离测试",""),1,10000,20000)
        assertEquals(1,db.records.value.size);db.remove(null)
        account="instrumentation-test-a";db.load();assertEquals(25,db.records.value.size);db.remove(null);db.close()
    }
}
