package com.olevod.tv

import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Read-only, explicitly authorized saved account contract checks. No channel or mutation calls. */
class V2AccountReadTest {
    @Test fun savedAccountCanReadVipFavoritesAndCloudHistory()=runBlocking{
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val session=SessionStore(InstrumentationRegistry.getInstrumentation().targetContext)
        assertNotNull("A current authorized login is required",session.token)
        val api=OlevodApi(token={session.token})
        val vip=api.detail(80632)
        assertTrue(vip.episodes.isNotEmpty())
        val schemes=vip.episodes.map{when{
            it.uri.startsWith("https://")->"https"
            it.uri.startsWith("http://")->"http"
            it.uri.isBlank()->"empty"
            else->"other"
        }}.groupingBy{it}.eachCount()
        assertTrue("VIP source schemes (URLs omitted): $schemes",vip.episodes.all{it.uri.startsWith("https://")})
        val favorites=api.favorites(1)
        assertTrue(favorites.items.all{it.id>0&&it.title.isNotBlank()})
        val history=api.cloudHistory(1)
        assertTrue(history.items.all{it.movie.id>0&&it.positionMs>=0&&it.updatedAt==0L})
    }
}
