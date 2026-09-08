package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Optional live test: input is injected through run-as stdin, never an APK asset. */
@RunWith(AndroidJUnit4::class)
class LoginIntegrationTest {
    @Test fun loginAndPersistEncryptedSession()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.filesDir,".login-test.json")
        assumeTrue("Live login is opt-in",InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        assertTrue("Requires user supplied live CAPTCHA",file.exists())
        val input=try{JSONObject(file.readText())}finally{file.delete()}
        val api=OlevodApi()
        val username=input.getString("username")
        val result=api.login(username,input.getString("password"),input.getString("captcha"),input.getString("captcha_id"))
        val session=SessionStore(context)
        session.save(result.token,result.name,result.accountId)
        assertNotNull(SessionStore(context).token)
        File(context.filesDir,"login-confirmed.txt").writeText("API login succeeded and encrypted session saved")
        val prefs=context.getSharedPreferences("session",0).getString("value","")!!
        assertFalse("Session must be encrypted",prefs.contains(result.token))
    }
}
