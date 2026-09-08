package com.olevod.tv.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class OlevodApiTest {
    @Test fun signatureMatchesWebsiteReferenceVectors() {
        assertEquals("249000ecf8310060b801007a28e300c1",RequestSignature.at(1700000000))
        assertEquals("a3c0e0eb4e210ca3366114456593081d",RequestSignature.at(1788825600))
    }

    @Test fun queryUsesVerifiedFilterOrderAndEscapesInput()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"list":[],"total":0}}"""))
            val api=OlevodApi(base=server.url("/").toString(),now={1700000000})
            val result=api.browse(Filter(category=1,area="美国",year="2025",type=104,initial="L",membership=2,sort="score"),2,20)
            val r=server.takeRequest()
            assertEquals(listOf("v1","pub","vod","list","true","2","L","美国","1","104","2025","score","2","20"),r.requestUrl!!.pathSegments)
            assertFalse(result.hasMore)
            assertNull(r.requestUrl!!.queryParameter("_he"))
        }finally{server.shutdown()}
    }
    @Test fun searchWithSlashRemainsOnePathSegment()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{"data":[],"total":0}}"""))
            OlevodApi(base=server.url("/").toString()).search("A/B ? 电影")
            assertEquals("A/B ? 电影",server.takeRequest().requestUrl!!.pathSegments[4])
        }finally{server.shutdown()}
    }
    @Test fun businessErrorDoesNotExposeServerEcho()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"code":13,"msg":"private-token-echo"}"""))
            try { OlevodApi(base=server.url("/").toString()).categories();fail("Expected expired session") }
            catch(e:ApiException){assertEquals(13,e.code);assertFalse(e.message!!.contains("private-token"))}
        }finally{server.shutdown()}
    }
    @Test fun authenticatedApiNeverFollowsRedirectToOtherOrigin()=runBlocking {
        val origin=MockWebServer();val other=MockWebServer();origin.start();other.start()
        try {
            origin.enqueue(MockResponse().setResponseCode(302).setHeader("Location",other.url("/collect")))
            try {OlevodApi(token={"header.payload.signature"},base=origin.url("/").toString()).categories();fail("Expected redirect rejection")}
            catch(e:ApiException){assertEquals(302,e.code)}
            assertEquals(0,other.requestCount)
        }finally{origin.shutdown();other.shutdown()}
    }
}
