@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.olevod.tv

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.olevod.tv.data.RememberedCredentials
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Fake credentials and a local one-pixel image; no real login, captcha or credential store writes. */
class V2AccountUiTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var isolated:V2FixtureViewModel
    private var challenges=0
    private var loggedIn=0
    private val submissions=mutableListOf<Submission>()
    private val pending=mutableListOf<CompletableDeferred<Unit>>()
    private val remembered=RememberedCredentials("fixture-user","fixture-password")
    private data class Submission(val username:String,val password:String,val captcha:String,val challenge:String)
    private val image="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/h9sAAAAASUVORK5CYII="

    @Before fun setUp(){isolated=V2FixtureViewModel("account")}
    @After fun tearDown(){pending.forEach{it.complete(Unit)};if(::isolated.isInitialized)isolated.close()}

    @Test fun firstVisitFocusesUsernameAndEmptyFormCannotSubmit(){
        showAccount(remembered=null)
        focused("login-username")
        compose.onNodeWithTag("login-submit").assertIsNotEnabled()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        focused("login-password")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        focused("captcha-key:1")
        typeHardware("1234")
        compose.onNodeWithTag("login-submit").assertIsNotEnabled()
        compose.runOnIdle{assertTrue(submissions.isEmpty());assertEquals(0,loggedIn)}
    }

    @Test fun rememberedAccountStartsAtOneAndKeypadKeepsFiveDigitsUntilExplicitSubmit(){
        showAccount()
        focused("captcha-key:1")
        listOf(KeyEvent.KEYCODE_DPAD_RIGHT to "2",KeyEvent.KEYCODE_DPAD_RIGHT to "3",
            KeyEvent.KEYCODE_DPAD_DOWN to "6",KeyEvent.KEYCODE_DPAD_LEFT to "5",KeyEvent.KEYCODE_DPAD_LEFT to "4",
            KeyEvent.KEYCODE_DPAD_DOWN to "7",KeyEvent.KEYCODE_DPAD_RIGHT to "8",KeyEvent.KEYCODE_DPAD_RIGHT to "9",
            KeyEvent.KEYCODE_DPAD_DOWN to "清空",KeyEvent.KEYCODE_DPAD_LEFT to "0",KeyEvent.KEYCODE_DPAD_LEFT to "退格",
            KeyEvent.KEYCODE_DPAD_UP to "7",KeyEvent.KEYCODE_DPAD_UP to "4",KeyEvent.KEYCODE_DPAD_UP to "1").forEach{(key,label)->press(key);focused("captcha-key:$label")}
        assertEquals("",captchaText())
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        press(KeyEvent.KEYCODE_DPAD_RIGHT);press(KeyEvent.KEYCODE_DPAD_CENTER)
        press(KeyEvent.KEYCODE_DPAD_RIGHT);press(KeyEvent.KEYCODE_DPAD_CENTER)
        repeat(2){press(KeyEvent.KEYCODE_DPAD_LEFT)}
        press(KeyEvent.KEYCODE_DPAD_DOWN);focused("captcha-key:4");press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals("1234",captchaText())
        compose.runOnIdle{assertTrue("Fourth digit must not submit",submissions.isEmpty())}
        press(KeyEvent.KEYCODE_DPAD_RIGHT);focused("captcha-key:5");press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals("12345",captchaText())
        compose.runOnIdle{assertTrue(submissions.isEmpty())}
        press(KeyEvent.KEYCODE_DPAD_DOWN);focused("captcha-key:8")
        press(KeyEvent.KEYCODE_DPAD_DOWN);focused("captcha-key:0")
        press(KeyEvent.KEYCODE_DPAD_DOWN);focused("login-submit")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000){loggedIn==1}
        compose.runOnIdle{assertEquals(listOf(Submission("fixture-user","fixture-password","12345","challenge-1")),submissions)}
    }

    @Test fun busySubmissionIgnoresRepeatedConfirmAndCaptchaRefresh(){
        val release=gate()
        showAccount(login={release.await()})
        focused("captcha-key:1")
        typeHardware("1234")
        moveOneToSubmit()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000){submissions.size==1}
        repeat(3){press(KeyEvent.KEYCODE_DPAD_CENTER)}
        repeat(4){press(KeyEvent.KEYCODE_DPAD_UP)}
        focused("captcha-key:1")
        moveOneToRefresh()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle{assertEquals(1,submissions.size);assertEquals(1,challenges);assertEquals(0,loggedIn)}
        assertEquals("1234",captchaText())
        compose.runOnIdle{release.complete(Unit)}
        compose.waitUntil(5_000){loggedIn==1}
        compose.runOnIdle{assertEquals(1,submissions.size);assertEquals(1,challenges)}
    }

    @Test fun failedLoginClearsDigitsRefreshesPairAndReturnsToOne(){
        showAccount(login={if(submissions.size==1)throw IOException("fixture failure")})
        focused("captcha-key:1")
        typeHardware("1234")
        moveOneToSubmit()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000){challenges==2}
        focused("captcha-key:1")
        assertEquals("",captchaText())
        compose.runOnIdle{assertEquals(0,loggedIn);assertEquals("challenge-1",submissions.single().challenge)}
        typeHardware("5678")
        moveOneToSubmit()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000){loggedIn==1}
        compose.runOnIdle{
            assertEquals(listOf("challenge-1","challenge-2"),submissions.map{it.challenge})
            assertEquals(listOf("1234","5678"),submissions.map{it.captcha})
        }
    }

    @Test fun refreshingClearsDigitsButCompletionDoesNotStealNewFocus(){
        val release=gate()
        showAccount(challenge={index->if(index==2)release.await()})
        focused("captcha-key:1")
        typeHardware("7")
        moveOneToRefresh()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5_000){challenges==2}
        focused("captcha-refresh")
        assertEquals("",captchaText())
        press(KeyEvent.KEYCODE_DPAD_DOWN);focused("captcha-input")
        press(KeyEvent.KEYCODE_DPAD_DOWN);focused("captcha-key:1")
        press(KeyEvent.KEYCODE_DPAD_RIGHT);focused("captcha-key:2")
        compose.runOnIdle{release.complete(Unit)}
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        focused("captcha-key:2")
        compose.runOnIdle{assertTrue(submissions.isEmpty())}
    }

    @Test fun forgettingRememberedCredentialsCanCancelBackToItsTrigger(){
        showAccount()
        focused("captcha-key:1")
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithText("更换账号").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("清除已保存信息").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("confirm-cancel")
        press(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("清除已保存信息").assertIsFocused()
        compose.onNodeWithText("fixture-user").assertExists()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("confirm-cancel")
        press(KeyEvent.KEYCODE_DPAD_RIGHT);focused("confirm-accept")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        focused("login-username")
        compose.onNodeWithText("fixture-user").assertDoesNotExist()
        compose.runOnIdle{assertTrue(submissions.isEmpty());assertTrue(isolated.vm.history.records.value.isEmpty())}
    }

    private fun showAccount(remembered:RememberedCredentials?=this.remembered,challenge:suspend(Int)->Unit={},login:suspend()->Unit={}){
        val fixture=AccountFixture(remembered=remembered,challenge={val index=++challenges;challenge(index);"challenge-$index" to image},
            login={username,password,captcha,id->submissions+=Submission(username,password,captcha,id);login()})
        compose.setContent{MaterialTheme{CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec){
            Column(Modifier.fillMaxSize().background(Bg)){
                Spacer(Modifier.height(TvDesign.headerHeight))
                Box(Modifier.weight(1f)){ContentFocusScope{AccountScreen(isolated.vm,onLoggedIn={loggedIn++},fixture=fixture)}}
            }
        }}}
    }
    private fun moveOneToSubmit(){
        listOf("4","7","退格").forEach{press(KeyEvent.KEYCODE_DPAD_DOWN);focused("captcha-key:$it")}
        press(KeyEvent.KEYCODE_DPAD_DOWN);focused("login-submit")
    }
    private fun moveOneToRefresh(){
        press(KeyEvent.KEYCODE_DPAD_UP);focused("captcha-input")
        press(KeyEvent.KEYCODE_DPAD_UP)
        press(KeyEvent.KEYCODE_DPAD_RIGHT);focused("captcha-refresh")
    }
    private fun captchaText():String=compose.onNodeWithTag("captcha-input").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(""){it.text}
    private fun typeHardware(value:String){value.forEach{press(KeyEvent.KEYCODE_0+(it-'0'))}}
    private fun gate()=CompletableDeferred<Unit>().also{pending+=it}
    private fun press(code:Int){InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code);compose.mainClock.advanceTimeBy(200);compose.waitForIdle()}
    private fun focused(tag:String){compose.waitUntil(5_000){runCatching{compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Focused]}.getOrDefault(false)};compose.onNodeWithTag(tag).assertIsFocused()}
}
