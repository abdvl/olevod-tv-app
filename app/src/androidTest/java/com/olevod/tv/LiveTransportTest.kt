package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import androidx.media3.datasource.*
import android.net.Uri

@RunWith(AndroidJUnit4::class)
class LiveTransportTest {
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun inspectNativeManifestTransport()=runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val session=SessionStore(context);val api=OlevodApi(token={session.token})
        val report=JSONObject()
        for(id in listOf(58L,1L)) {
            val c=api.channels().first.find{it.id==id}?:api.channels().first.first()
            val d=api.liveDetail(c,LocalDate.now(ZoneId.of("Asia/Shanghai")).toString())
            var uri=d.uri
            for(stage in 0..1){
                val key="${c.id}_$stage"
                val source=DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 OlevodTV/0.1").setDefaultRequestProperties(mapOf("Referer" to "https://www.olevod.com/")).createDataSource()
                try {
                    val stream=DataSourceInputStream(source,DataSpec.Builder().setUri(uri).build())
                    stream.open();val text=stream.bufferedReader().readText();val base=source.uri.toString();stream.close()
                    report.put(key,JSONObject().put("status",200).put("host",Uri.parse(base).host).put("variant",text.contains("#EXT-X-STREAM-INF")))
                    if(!text.contains("#EXT-X-STREAM-INF"))break
                    val child=text.lineSequence().first{it.isNotBlank()&&!it.startsWith('#')};uri=java.net.URI(base).resolve(child).toString()
                } catch(e:Exception){report.put(key,JSONObject().put("status",(e as? HttpDataSource.InvalidResponseCodeException)?.responseCode?:-1).put("host",Uri.parse(uri).host).put("error",e.javaClass.simpleName));break}finally{source.close()}
            }
        }
        File(context.filesDir,"media-diagnostics.json").writeText(report.toString())
    }
}
