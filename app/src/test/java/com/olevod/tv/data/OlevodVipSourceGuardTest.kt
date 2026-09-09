package com.olevod.tv.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** Synthetic server responses only: never reads a saved account or requests the public service. */
class OlevodVipSourceGuardTest {
    @Test fun ordinaryAndRestrictedHttpsDoNotProbeTheAccount(): Unit = runBlocking {
        for (restricted in listOf(false, true)) withServer { server ->
            server.enqueue(detailResponse(restricted, listOf("https://media.example.test/movie.m3u8")))
            val result = api(server).detail(80632)
            assertEquals("https://media.example.test/movie.m3u8", result.episodes.single().uri)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun mixedSourcesKeepTheirOrderAndDoNotProbeTheAccount(): Unit = runBlocking {
        withServer { server ->
            val sources = listOf("", "https://media.example.test/second.m3u8")
            server.enqueue(detailResponse(true, sources))
            assertEquals(sources, api(server).detail(80632).episodes.map { it.uri })
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun noEpisodesIsNotMistakenForRestrictedEmptySources(): Unit = runBlocking {
        withServer { server ->
            server.enqueue(detailResponse(true, emptyList()))
            assertTrue(api(server).detail(80632).episodes.isEmpty())
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun unrestrictedBlankAndUnsupportedNonblankSourcesDoNotProbeTheAccount(): Unit = runBlocking {
        for ((restricted, source) in listOf(false to "", true to "http://media.example.test/source")) {
            withServer { server ->
                server.enqueue(detailResponse(restricted, listOf(source)))
                assertEquals(source, api(server).detail(80632).episodes.single().uri)
                assertEquals(1, server.requestCount)
            }
        }
    }

    @Test fun guestRestrictedByMovieOrEpisodeGetsLoginWithoutAnAccountRequest(): Unit = runBlocking {
        for (movieRestricted in listOf(true, false)) withServer { server ->
            server.enqueue(detailResponse(movieRestricted, listOf("", "   "), episodeVip = !movieRestricted))
            var logoutCalls = 0
            val error = expectApi { api(server, token = { null }, unauthorized = { logoutCalls++ }).detail(80632) }
            assertEquals(12, error.code)
            assertTrue(error.userMessage.contains("登录"))
            assertEquals(0, logoutCalls)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun expiredAccountResponsesClearOnlyThatSessionAndDoNotExposeServerEcho(): Unit = runBlocking {
        for (code in listOf(13, 14, 16)) withServer { server ->
            val token = AtomicReference<String?>(OLD_TOKEN)
            var logoutCalls = 0
            server.enqueue(detailResponse(true, listOf("")))
            server.enqueue(businessError(code))
            val error = expectApi {
                api(server, token::get) { logoutCalls++; token.set(null) }.detail(80632)
            }
            assertEquals(code, error.code)
            assertEquals(1, logoutCalls)
            assertNull(token.get())
            assertSafe(error)
            assertAccountRequest(server)
        }
    }

    @Test fun loginRequiredAccountResponseDoesNotClearAnExistingSession(): Unit = runBlocking {
        withServer { server ->
            var logoutCalls = 0
            server.enqueue(detailResponse(true, listOf("")))
            server.enqueue(businessError(12))
            val error = expectApi { api(server, unauthorized = { logoutCalls++ }).detail(80632) }
            assertEquals(12, error.code)
            assertEquals(0, logoutCalls)
            assertSafe(error)
            assertAccountRequest(server)
        }
    }

    @Test fun acceptedNonmemberGetsEntitlementErrorWithoutLogout(): Unit = runBlocking {
        withServer { server ->
            var logoutCalls = 0
            server.enqueue(detailResponse(true, listOf("")))
            server.enqueue(userResponse(JSONObject().put("userId", 42).put("groupId", 1)))
            val error = expectApi { api(server, unauthorized = { logoutCalls++ }).detail(80632) }
            assertFalse(error.code in setOf(12, 13, 14, 16))
            assertTrue(error.userMessage.contains("会员") || error.userMessage.contains("VIP") || error.userMessage.contains("权益"))
            assertFalse(error.userMessage.contains("失效"))
            assertEquals(0, logoutCalls)
            assertSafe(error)
            assertAccountRequest(server)
        }
    }

    @Test fun acceptedMemberWithEmptyBaseDoesNotSilentlyChoosePremiumVariant(): Unit = runBlocking {
        withServer { server ->
            var logoutCalls = 0
            server.enqueue(detailResponse(true, listOf(""), premium = true))
            server.enqueue(userResponse(JSONObject().put("userId", 42).put("groupId", 3)))
            val error = expectApi { api(server, unauthorized = { logoutCalls++ }).detail(80632) }
            assertFalse(error.code in setOf(12, 13, 14, 16))
            assertTrue(error.userMessage.contains("片源") || error.userMessage.contains("播放源") || error.userMessage.contains("播放地址"))
            assertFalse(error.userMessage.contains("失效"))
            assertEquals(0, logoutCalls)
            assertSafe(error)
            assertAccountRequest(server)
        }
    }

    @Test fun missingAndInvalidAccountFieldsRemainUnknownWithoutLogout(): Unit = runBlocking {
        val responses = listOf(
            JSONObject(),
            JSONObject().put("userId", 42),
            JSONObject().put("groupId", 3),
            JSONObject().put("userId", JSONObject.NULL).put("groupId", 3),
            JSONObject().put("userId", 42).put("groupId", JSONObject.NULL),
            JSONObject().put("userId", 42).put("groupId", "unrecognized"),
            JSONObject().put("userId", "").put("groupId", 3),
            JSONObject().put("userId", 0).put("groupId", 3),
            JSONObject().put("userId", -1).put("groupId", 3),
            JSONObject().put("userId", 42).put("groupId", 0),
        )
        for (user in responses) withServer { server ->
            var logoutCalls = 0
            server.enqueue(detailResponse(true, listOf("")))
            server.enqueue(userResponse(user))
            val error = expectApi { api(server, unauthorized = { logoutCalls++ }).detail(80632) }
            assertEquals("Malformed user info must remain unconfirmed", -2, error.code)
            assertTrue("Missing fields must not be diagnosed as lack of membership", error.userMessage.contains("确认"))
            assertEquals(0, logoutCalls)
            assertSafe(error)
            assertAccountRequest(server)
        }
    }

    @Test fun accountHttpFailureIsNotMisreportedAsMembershipOrSessionExpiry(): Unit = runBlocking {
        withServer { server ->
            var logoutCalls = 0
            server.enqueue(detailResponse(true, listOf("")))
            server.enqueue(MockResponse().setResponseCode(503).setBody(ECHO))
            val error = expectApi { api(server, unauthorized = { logoutCalls++ }).detail(80632) }
            assertEquals(503, error.code)
            assertEquals(0, logoutCalls)
            assertSafe(error)
            assertAccountRequest(server)
        }
    }

    @Test fun accountChangeWhileDetailIsPendingCancelsBeforeProbingNewAccount(): Unit = runBlocking {
        withServer { server ->
            val token = AtomicReference<String?>(OLD_TOKEN)
            var logoutCalls = 0
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    token.set(NEW_TOKEN)
                    return detailResponse(true, listOf(""))
                }
            }
            expectCancellation { api(server, token::get) { logoutCalls++ }.detail(80632) }
            assertEquals(NEW_TOKEN, token.get())
            assertEquals(0, logoutCalls)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun accountChangeWhileUserInfoIsPendingCancelsItsOtherwiseValidResponse(): Unit = runBlocking {
        accountSwitchDuringUserInfo(userResponse(JSONObject().put("userId", 42).put("groupId", 3)))
    }

    @Test fun expiredOldUserInfoCannotLogOutOrReplaceNewAccount(): Unit = runBlocking {
        accountSwitchDuringUserInfo(businessError(13))
    }

    @Test fun signOutWhileUserInfoFailsCancelsTheOldServiceError(): Unit = runBlocking {
        accountSwitchDuringUserInfo(MockResponse().setResponseCode(503).setBody(ECHO), replacement = null)
    }

    private suspend fun accountSwitchDuringUserInfo(response: MockResponse, replacement: String? = NEW_TOKEN) {
        withServer { server ->
            val token = AtomicReference<String?>(OLD_TOKEN)
            var logoutCalls = 0
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return if (request.requestUrl?.encodedPath == "/pub/user/info") {
                        token.set(replacement)
                        response
                    } else detailResponse(true, listOf(""))
                }
            }
            expectCancellation { api(server, token::get) { logoutCalls++; token.set(null) }.detail(80632) }
            assertEquals(replacement, token.get())
            assertEquals(0, logoutCalls)
            assertAccountRequest(server)
        }
    }

    private fun api(server: MockWebServer, token: () -> String? = { OLD_TOKEN }, unauthorized: () -> Unit = {}) =
        OlevodApi(token = token, base = server.url("/").toString(), onUnauthorized = unauthorized)

    private fun detailResponse(restricted: Boolean, sources: List<String>, episodeVip: Boolean = false, premium: Boolean = false): MockResponse {
        val rows = JSONArray()
        sources.forEachIndexed { index, source ->
            val row = JSONObject().put("index", index + 1).put("title", "第${index + 1}集").put("url", source).put("vip", episodeVip)
            if (premium) row.put("vip_urls", JSONArray().put(JSONObject().put("url", "https://media.example.test/$ECHO").put("vip", true)))
            rows.put(row)
        }
        val movie = JSONObject().put("id", 80632).put("name", "Synthetic restricted fixture").put("vip", restricted).put("urls", rows)
        return MockResponse().setBody(JSONObject().put("code", 0).put("data", movie).toString())
    }

    private fun userResponse(user: JSONObject): MockResponse =
        MockResponse().setBody(JSONObject().put("code", 0).put("data", user).put("msg", ECHO).toString())

    private fun businessError(code: Int): MockResponse =
        MockResponse().setBody(JSONObject().put("code", code).put("msg", ECHO).toString())

    private fun assertAccountRequest(server: MockWebServer) {
        assertEquals(2, server.requestCount)
        assertEquals("GET", server.takeRequest().method)
        val request = server.takeRequest()
        assertEquals("/pub/user/info", request.requestUrl!!.encodedPath)
        assertEquals("POST", request.method)
        assertEquals(0, JSONObject(request.body.readUtf8()).length())
        assertEquals("old", request.requestUrl!!.queryParameter("_he"))
    }

    private fun assertSafe(error: ApiException) {
        val message = error.userMessage
        for (privateValue in listOf(ECHO, OLD_TOKEN, NEW_TOKEN, "https://", "media.example.test")) {
            assertFalse("Error must not reveal transport values", message.contains(privateValue))
        }
    }

    private suspend fun expectApi(block: suspend () -> Unit): ApiException {
        try { block() } catch (error: ApiException) { return error }
        throw AssertionError("Expected an explicit API error")
    }

    private suspend fun expectCancellation(block: suspend () -> Unit) {
        try { block() } catch (_: CancellationException) { return }
        throw AssertionError("Stale account result must be cancelled")
    }

    private suspend fun withServer(block: suspend (MockWebServer) -> Unit) {
        val server = MockWebServer()
        server.start()
        try { block(server) } finally { server.shutdown() }
    }

    companion object {
        private const val OLD_TOKEN = "old.payload.signature"
        private const val NEW_TOKEN = "new.payload.signature"
        private const val ECHO = "synthetic-private-echo"
    }
}
