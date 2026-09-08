package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AuthenticatedContentTest {
    @Test fun inspectAuthorizedContentContracts()=runBlocking {
        org.junit.Assume.assumeTrue("Live account tests are opt-in",InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val sessions=SessionStore(context)
        assertNotNull("Requires completed login test",sessions.token)
        val api=OlevodApi(token={sessions.token})
        val report=JSONObject()
        fun shape(value:Any?):String=when(value){is JSONObject->value.keys().asSequence().toList().sorted().joinToString(",");is org.json.JSONArray->"array:${value.length()}";else->value?.javaClass?.simpleName?:"null"}
        suspend fun inspect(name:String,block:suspend()->Unit){try{block();report.put(name,"ok")}catch(e:Exception){report.put(name,if(e is ApiException)"code:${e.code}"else e.javaClass.simpleName)}}
        inspect("profile"){val d=api.request(listOf("pub","user","info"),JSONObject()).get("data");report.put("profile_shape",shape(d));if(d is JSONObject)report.put("membership_group",d.optInt("groupId",-1))}
        inspect("vip_detail"){val d=api.detail(80632);report.put("vip_episodes",d.episodes.size);assertTrue(d.episodes.isNotEmpty())}
        inspect("favorites"){val p=api.favorites(1);report.put("favorites_total",p.total)}
        inspect("channel_favorites"){report.put("favorite_channel_count",api.favoriteChannels().size);val rows=api.request(listOf("pub","user","favorites"),JSONObject().put("page",0).put("pageSize",999).put("favoriteType",3)).getJSONObject("data").optJSONArray("list");report.put("channel_row_shape",shape(rows?.optJSONObject(0)));val ids=org.json.JSONArray();if(rows!=null)for(i in 0 until rows.length()){val r=rows.getJSONObject(i);ids.put(JSONObject().put("channelId",r.opt("channelId")).put("detailId",r.optJSONObject("detail")?.opt("id")).put("recordId",r.opt("ID")).put("favoriteType",r.opt("favoriteType")).put("platform",r.opt("platform")))};report.put("channel_identity_map",ids)}
        inspect("cloud_history"){val d=api.request(listOf("pub","vod","history","list"),JSONObject().put("page",1).put("pageSize",20)).get("data");report.put("cloud_shape",shape(d));if(d is JSONObject){report.put("cloud_total",d.optInt("total",-1));report.put("cloud_item_shape",shape(d.optJSONArray("list")?.optJSONObject(0)));report.put("cloud_vod_ids",org.json.JSONArray(d.optJSONArray("list").let{a->if(a==null)emptyList<Long>()else (0 until a.length()).map{a.getJSONObject(it).optLong("vodId")}}));val first=d.optJSONArray("list")?.optJSONObject(0);report.put("cloud_duration",first?.optDouble("watchDuration"));report.put("cloud_percent",first?.optDouble("watchPercent"))}}
        inspect("live_detail"){val c=api.channels().first.first();val d=api.liveDetail(c,LocalDate.now(ZoneId.of("Asia/Shanghai")).toString());report.put("live_programmes",d.programmes.size)}
        File(context.filesDir,"validation-report.json").writeText(report.toString())
    }
}
