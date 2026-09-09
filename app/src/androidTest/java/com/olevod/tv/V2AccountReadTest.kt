package com.olevod.tv

import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.json.JSONObject

/** Read-only, explicitly authorized saved account contract checks. No channel or mutation calls. */
class V2AccountReadTest {
    @Test fun savedAccountCanReadVipFavoritesAndCloudHistory()=runBlocking{
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val session=SessionStore(InstrumentationRegistry.getInstrumentation().targetContext)
        assertNotNull("A current authorized login is required",session.token)
        val api=OlevodApi(token={session.token})
        // Validate with the authenticated user endpoint: token presence alone does not imply validity.
        val user=api.request(listOf("pub","user","info"),JSONObject()).getJSONObject("data")
        assertTrue("Accepted user info must identify an account",user.has("userId")&&!user.isNull("userId"))
        android.util.Log.i("V2AccountRead","userInfo accepted; memberGroup=${user.optInt("groupId",-1)}")
        val raw=api.request(listOf("v1","pub","vod","detail","80632","true")).getJSONObject("data")
        val rows=raw.optJSONArray("urls")
        var baseHttps=0;var baseEmpty=0;var premiumHttps=0;var premiumCount=0
        for(i in 0 until (rows?.length()?:0)){
            val row=rows!!.getJSONObject(i)
            if(row.optString("url").isBlank())baseEmpty++
            if(row.optString("url").startsWith("https://"))baseHttps++
            val variants=row.optJSONArray("vip_urls")
            premiumCount+=variants?.length()?:0
            for(j in 0 until (variants?.length()?:0))if(variants!!.optJSONObject(j)?.optString("url")?.startsWith("https://")==true)premiumHttps++
        }
        android.util.Log.i("V2AccountRead","VIP source projection: baseHttps=$baseHttps baseEmpty=$baseEmpty premiumCount=$premiumCount premiumHttps=$premiumHttps restricted=${raw.optBoolean("vip")}")
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

    @Test fun savedAccountPersonalListsCanBeReadIndependentlyOfVip():Unit=runBlocking{
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val session=SessionStore(InstrumentationRegistry.getInstrumentation().targetContext)
        assertNotNull("A current authorized login is required",session.token)
        val api=OlevodApi(token={session.token})
        val favorites=api.favorites(1)
        assertTrue(favorites.items.all{it.id>0&&it.title.isNotBlank()})
        val history=api.cloudHistory(1)
        assertTrue(history.items.all{it.movie.id>0&&it.positionMs>=0&&it.updatedAt==0L})
    }
}
