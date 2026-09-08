package com.olevod.tv

import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class V2AccountStateTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    @Test fun rememberedStateWaitsForSuccessfulDiskWrite(){
        val entered=CountDownLatch(1);val release=CountDownLatch(1)
        val vault=object:CredentialVault{
            var value:RememberedCredentials?=null
            override fun read()=value
            override fun save(username:String,password:String){entered.countDown();check(release.await(5,TimeUnit.SECONDS));value=RememberedCredentials(username,password)}
            override fun clear(){value=null}
        }
        V2FixtureViewModel("credential-commit",vault).use{fixture->
            try{
                instrumentation.runOnMainSync{fixture.vm.rememberCredentials("fixture-user","fixture-only")}
                assertTrue(entered.await(5,TimeUnit.SECONDS))
                instrumentation.runOnMainSync{assertNull(fixture.vm.rememberedCredentials)}
                release.countDown()
                runBlocking{withTimeout(5000){while(withContext(Dispatchers.Main){fixture.vm.rememberedCredentials==null})delay(10)}}
                instrumentation.runOnMainSync{assertEquals("fixture-user",fixture.vm.rememberedCredentials?.username)}
            }finally{release.countDown()}
        }
    }
    @Test fun failedPersistenceAllowsLoginAttemptAndNeverClaimsRemembered()=runBlocking{
        val vault=object:CredentialVault{
            override fun read():RememberedCredentials?=null
            override fun save(username:String,password:String){throw IOException("injected storage failure")}
            override fun clear(){throw IOException("injected clear failure")}
        }
        V2FixtureViewModel("credential-failure",vault).use{fixture->
            withContext(Dispatchers.Main){fixture.vm.saveLoginCredentials("fixture-user","fixture-only")}
            withContext(Dispatchers.Main){assertNull(fixture.vm.rememberedCredentials);assertEquals("本次可继续登录，但账号未能记住",fixture.vm.credentialPersistenceError)}
        }
    }
    @Test fun favoriteIntentIsExactAccountAndConsumedOnlyOnce(){
        V2FixtureViewModel("favorite-intent").use{fixture->instrumentation.runOnMainSync{
            val vm=fixture.vm
            vm.pendingFavorite=PendingFavorite(9,true,vm.sessions.accountKey)
            assertNull(vm.consumeFavoriteIntent(10))
            assertEquals(true,vm.consumeFavoriteIntent(9)?.desired)
            assertNull(vm.consumeFavoriteIntent(9))
            vm.pendingFavorite=PendingFavorite(9,false,"another-account")
            assertNull(vm.consumeFavoriteIntent(9));assertNull(vm.pendingFavorite)
        }}
    }
    @Test fun explicitScopeDeletionCannotDeleteAnotherAccount()=runBlocking{
        V2FixtureViewModel("scope-deletion").use{fixture->
            val db=fixture.vm.history
            val movie=Movie(9,"scope fixture","")
            db.save(movie,2,20000,90000,"account-a")
            db.save(movie,7,70000,90000,"account-b")
            db.save(movie,1,10000,90000,"guest")
            db.remove(9,"account-a")
            assertEquals(1,db.records.value.size)
            assertEquals(10000L,db.records.value.single().positionMs)
            val cursor=db.readableDatabase.rawQuery("SELECT scope, position FROM watches ORDER BY scope",null)
            cursor.use{assertEquals(2,it.count);it.moveToFirst();assertEquals("account-b",it.getString(0));assertEquals(70000,it.getLong(1))}
        }
    }
}
