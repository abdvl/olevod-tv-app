package com.olevod.tv.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AppUpdatesTest {
    private fun release(tag: String = "v0.3.0") = JSONObject().put("tag_name", tag).put("draft", false)
        .put("prerelease", false).put("body", "修复播放")
        .put("assets", JSONArray().put(JSONObject().put("name", "olevod-tv.apk").put("state", "uploaded")
            .put("size", 1024).put("browser_download_url", "https://github.com/abdvl/olevod-tv-app/releases/download/$tag/olevod-tv.apk")
            .put("digest", "sha256:" + "a".repeat(64))))
    @Test fun comparesNumericVersionsAndNormalizesTags() {
        assertEquals(ReleaseVersion.parse("v0.2"), ReleaseVersion.parse("0.2.0"))
        assertEquals(ReleaseVersion.parse("1.2.3"), ReleaseVersion.parse("v1.2.3+build.5"))
        assertTrue(ReleaseVersion.parse("0.10.0")!! > ReleaseVersion.parse("0.9.9")!!)
        assertTrue(ReleaseVersion.parse("1.0.0")!! > ReleaseVersion.parse("0.99.99")!!)
        listOf("v1.0.0-rc.1", "latest", "1", "01.2.3", "1.2.3.4", "999999999999999999999.0").forEach { assertNull(ReleaseVersion.parse(it)) }
    }
    @Test fun skipsSameAndOlderVersions() {
        assertNull(GitHubUpdateRepository.parseRelease(release("v0.2"), "0.2.0"))
        assertNull(GitHubUpdateRepository.parseRelease(release("v0.1"), "0.2.0"))
        assertEquals("v0.3.0", GitHubUpdateRepository.parseRelease(release(), "0.2.0")!!.tag)
    }
    @Test fun rejectsPreviewMissingAmbiguousOrForeignAssets() {
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.put("draft", true) }, { it.put("prerelease", true) },
            { it.put("assets", JSONArray()) },
            { it.getJSONArray("assets").put(it.getJSONArray("assets").getJSONObject(0)) },
            { it.getJSONArray("assets").getJSONObject(0).put("browser_download_url", "https://example.com/app.apk") },
            { it.getJSONArray("assets").getJSONObject(0).put("size", 0) },
            { it.getJSONArray("assets").getJSONObject(0).put("digest", "sha256:broken") }
        )
        mutations.forEach { mutation ->
            val json = release().also(mutation)
            assertThrows(IllegalStateException::class.java) { GitHubUpdateRepository.parseRelease(json, "0.2.0") }
        }
    }
    @Test fun supportsOlderReleaseWithoutDigest() {
        val json = release()
        json.getJSONArray("assets").getJSONObject(0).put("digest", JSONObject.NULL)
        assertNull(GitHubUpdateRepository.parseRelease(json, "0.2.0")!!.sha256)
    }
    @Test fun handlesHttpSuccessRateLimitAndNoReleases() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val repository = GitHubUpdateRepository(latestUrl = server.url("/latest").toString())
            server.enqueue(MockResponse().setBody(release().toString()))
            assertEquals("v0.3.0", repository.latest("0.2.0")!!.tag)
            assertEquals("application/vnd.github+json", server.takeRequest().getHeader("Accept"))
            for (code in listOf(404, 403, 429, 500)) {
                server.enqueue(MockResponse().setResponseCode(code))
                try { repository.latest("0.2.0"); fail("Expected HTTP $code to fail") }
                catch (_: IllegalStateException) { }
            }
        }
    }
}
