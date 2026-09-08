package com.olevod.tv.data

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/** Synthetic responses preserve public website field contracts without account or signed URL fixtures. */
class OlevodAdapterContractTest {
    @Test fun favoriteRecordUsesVodIdentityAndChannelUsesDetailIdentity() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"list":[{"id":900,"vodId":123,"name":"测试影片"}],"total":1}}"""))
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"list":[{"channelId":58,"stream_id":"CCTV13HD","title":"新闻","detail":{"id":58,"name":"新闻"}}]}}"""))
            val api=OlevodApi(base=server.url("/").toString())
            assertEquals(123L,api.favorites(1).items.single().id)
            assertEquals("CCTV13HD",api.favoriteChannels().single().streamId)
        } finally {server.shutdown()}
    }
    @Test fun loginUsesStableWebsiteUserIdInsteadOfLoginAlias() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"token":"test.session.only","user":{"userId":42,"userName":"alias","userNickName":"昵称"}}}"""))
            val session=OlevodApi(base=server.url("/").toString()).login("different-alias","test-password","test-captcha","captcha-id")
            assertEquals("42",session.accountId);assertEquals("昵称",session.name)
        } finally {server.shutdown()}
    }
    @Test fun onlyExpiredSessionCodesClearExistingSession() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            var clears=0
            val api=OlevodApi(base=server.url("/").toString(),onUnauthorized={clears++})
            for(code in listOf(12,13)){server.enqueue(MockResponse().setBody("""{"code":$code}"""));try{api.categories()}catch(expected:ApiException){}}
            assertEquals(1,clears)
        } finally {server.shutdown()}
    }

    @Test fun searchSelectsVodGroupAndUsesItsPaginationTotal() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"total":99,"data":[{"type":"tv","list":[{"id":58,"title":"频道"}],"total":1},{"type":"vod","list":[{"id":123,"name":"流浪地球","pic":"poster.jpg","score":7.9},{"id":456,"name":"流浪地球 VIP","vip":true}],"total":3,"page":1,"pageSize":2},{"type":"live","list":null,"total":0},{"type":"overseas","list":null,"total":0}]}}"""))
            val result = OlevodApi(base = server.url("/").toString()).search("流浪地球", size = 2)
            assertEquals(listOf(123L, 456L), result.items.map { it.id })
            assertEquals(listOf("流浪地球", "流浪地球 VIP"), result.items.map { it.title })
            assertEquals(3, result.total)
            assertTrue(result.hasMore)
            assertTrue(result.items.last().vip)
        } finally { server.shutdown() }
    }

    @Test fun missingVodSearchGroupDoesNotBecomePhantomMovie() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"total":1,"data":[{"type":"tv","list":[{"id":58,"title":"频道"}],"total":1}]}}"""))
            val result = OlevodApi(base = server.url("/").toString()).search("频道")
            assertTrue(result.items.isEmpty())
            assertEquals(0, result.total)
            assertFalse(result.hasMore)
        } finally { server.shutdown() }
    }

    @Test fun liveDistributionGetsFreshTimeSignatureWithoutAccountToken() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            repeat(2) { server.enqueue(MockResponse().setBody("""{"code":0,"data":{"detail":{"hls":"https://media.example.test/live.m3u8?quality=hd"},"programs":[]}}""")); server.enqueue(MockResponse().setBody("""{"code":0,"data":{"groupId":1}}""")) }
            var clock = 1700000000L
            val api = OlevodApi(token = { "private.header.signature" }, base = server.url("/").toString(), now = { clock })
            val channel = Channel(58, "CCTV13HD", "News", "", "")
            val first = api.liveDetail(channel, "2026-09-08").uri.toHttpUrl()
            assertEquals(RequestSignature.at(clock), first.queryParameter("token"))
            assertEquals("hd", first.queryParameter("quality"))
            assertNull(first.queryParameter("_he"))
            assertNull(first.queryParameter("_pl"))
            assertNull(first.queryParameter("_si"))
            clock++
            val second = api.liveDetail(channel, "2026-09-08").uri.toHttpUrl()
            assertEquals(RequestSignature.at(clock), second.queryParameter("token"))
            assertNotEquals(first.queryParameter("token"), second.queryParameter("token"))
        } finally { server.shutdown() }
    }

    @Test fun liveGroupsUseObservedTitleField() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":[{"type":"tv","data":{"groups":[{"id":1,"title":"央视","type":"tv"},{"id":2,"title":"地方","type":"tv"}]}}]}"""))
            assertEquals(listOf(1 to "央视", 2 to "地方"), OlevodApi(base = server.url("/").toString()).liveGroups())
        } finally { server.shutdown() }
    }

    @Test fun replayPostsProgrammeIdentityAndTokenOnlyToApi() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"hls":"https://media.example.test/replay.m3u8"}}"""))
            val api = OlevodApi(token = { "header.payload.signature" }, base = server.url("/").toString())
            val channel = Channel(58, "CCTV13HD", "新闻", "", "")
            val programme = Programme(123, "测试节目", "00:00", "2026-09-08T00:00:00+08:00", "2026-09-08T01:00:00+08:00", true, 1)
            assertEquals("https://media.example.test/replay.m3u8", api.replay(channel, programme))
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/pub/tv/vod/url", request.requestUrl!!.encodedPath)
            val body = JSONObject(request.body.readUtf8())
            assertEquals(123L, body.getLong("programmeId"))
            assertEquals("CCTV13HD", body.getString("stream_id"))
            assertEquals("header", request.requestUrl!!.queryParameter("_he"))
            assertEquals("payload", request.requestUrl!!.queryParameter("_pl"))
            assertEquals("signature", request.requestUrl!!.queryParameter("_si"))
            assertNull(request.requestUrl!!.queryParameter("_vv"))
        } finally { server.shutdown() }
    }

    @Test fun liveProgrammeRetainsTimezoneAndReplayCapability() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"detail":{"hls":"https://media.example.test/live.m3u8","favorite":true,"liveHasVod":false},"programs":[{"id":123,"title":"午夜新闻","showTime":"00:00","start":"2026-09-08T00:00:00+08:00","end":"2026-09-08T01:00:00+08:00","liveType":1,"hasVod":true}]}}"""))
            val result = OlevodApi(base = server.url("/").toString()).liveDetail(Channel(58,"CCTV13HD","新闻","",""),"2026-09-08")
            assertTrue(result.favorite)
            assertTrue(result.programmes.single().hasReplay)
            assertEquals("2026-09-08T00:00:00+08:00", result.programmes.single().start)
            assertEquals(listOf("v1","pub","live","info","tv","58","CCTV13HD","2026-09-08"), server.takeRequest().requestUrl!!.pathSegments)
        } finally { server.shutdown() }
    }

    @Test fun gzipWithoutContentEncodingStillParsesBusinessEnvelope() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val bytes = ByteArrayOutputStream()
            GZIPOutputStream(bytes).use { it.write("""{"code":0,"data":[]}""".toByteArray()) }
            server.enqueue(MockResponse().setBody(Buffer().write(bytes.toByteArray())))
            assertTrue(OlevodApi(base = server.url("/").toString()).categories().isEmpty())
        } finally { server.shutdown() }
    }

    @Test fun favoriteCancelUsesIdsArrayRatherThanSaveShape() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{}}"""))
            OlevodApi(base = server.url("/").toString()).favorite(123, false)
            val request = server.takeRequest()
            assertEquals("/pub/vod/favorite/cancel", request.requestUrl!!.encodedPath)
            val body = JSONObject(request.body.readUtf8())
            assertEquals(123L, body.getJSONArray("ids").getLong(0))
            assertFalse(body.has("id"))
        } finally { server.shutdown() }
    }
    @Test fun cloudHistoryUsesSecondsAndSyncDoesNotSendPercentage() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"list":[{"id":91,"vodId":123,"name":"Film","episode":2,"watchDuration":40.5,"watchPercent":6939}],"total":21}}"""))
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{}}"""))
            val api=OlevodApi(base=server.url("/").toString())
            val page=api.cloudHistory(2);val record=page.items.single()
            assertEquals(21,page.total);assertEquals(123L,record.movie.id)
            assertEquals(40500L,record.positionMs);assertEquals(6939000L,record.durationMs)
            assertEquals(2,JSONObject(server.takeRequest().body.readUtf8()).getInt("page"))
            api.syncWatch(record.copy(updatedAt=1788825600000))
            val request=server.takeRequest()
            assertEquals("/pub/user/watches/sync",request.requestUrl!!.encodedPath)
            val body=org.json.JSONArray(request.body.readUtf8()).getJSONObject(0)
            assertEquals(40L,body.getLong("duration"))
            assertEquals(6939.0,body.getDouble("percent"),0.0)
            assertEquals(1788825600L,body.getLong("saveTime"))
        }finally{server.shutdown()}
    }

}
