package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NativeLoginFlowTest {
    @Test fun captchaAndLoginUseSameNativeClient()=runBlocking {
        org.junit.Assume.assumeTrue("Live account tests are opt-in",InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val api=OlevodApi()
        val input=File(context.filesDir,".native-login-input.json");input.delete()
        val confirmed=File(context.filesDir,"login-confirmed.txt");confirmed.delete()
        val challenge=api.captcha()
        File(context.filesDir,"captcha-native.png").writeBytes(android.util.Base64.decode(challenge.second.substringAfter(','),android.util.Base64.DEFAULT))
        val deadline=System.currentTimeMillis()+600000
        while(!input.exists()&&System.currentTimeMillis()<deadline)delay(300)
        assertTrue("Waiting for explicit user CAPTCHA input",input.exists())
        val payload=try{JSONObject(input.readText())}finally{input.delete()}
        val result=api.login(payload.getString("username"),payload.getString("password"),payload.getString("captcha"),challenge.first)
        SessionStore(context).save(result.token,result.name,result.accountId)
        assertNotNull(SessionStore(context).token)
        confirmed.writeText("Native CAPTCHA + API login succeeded; encrypted session committed")
    }
}
