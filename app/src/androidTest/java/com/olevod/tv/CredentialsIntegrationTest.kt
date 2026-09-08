package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CredentialsIntegrationTest {
    @Test fun encryptedCredentialsSurviveReopenAndSessionLogout() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val credentials=CredentialsStore(context,"credentials-test")
        try{
            credentials.save("synthetic-account","synthetic-secret")
            val restored=CredentialsStore(context,"credentials-test").read()!!
            assertEquals("synthetic-account",restored.username)
            assertEquals("synthetic-secret",restored.password)
            val disk=File(context.applicationInfo.dataDir,"shared_prefs/credentials-test.xml").readText()
            assertFalse(disk.contains("synthetic-account"));assertFalse(disk.contains("synthetic-secret"))
            val session=SessionStore(context,"session-credentials-test")
            session.save("synthetic.test.token","test","42");session.clear()
            assertNotNull(CredentialsStore(context,"credentials-test").read())
        }finally{credentials.clear()}
        assertNull(CredentialsStore(context,"credentials-test").read())
    }

    @Test fun rememberUserProvidedCredentialsOnAuthorizedDevice() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("rememberLogin")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val input=File(context.filesDir,".native-login-input.json")
        assertTrue("Requires private authorized input",input.exists())
        val data=try{JSONObject(input.readText())}finally{input.delete()}
        CredentialsStore(context).save(data.getString("username"),data.getString("password"))
        val restored=CredentialsStore(context).read()!!
        assertEquals(data.getString("username"),restored.username)
        assertEquals(data.getString("password"),restored.password)
    }
}
