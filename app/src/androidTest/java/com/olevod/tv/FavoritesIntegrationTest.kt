package com.olevod.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesIntegrationTest {
    @Test fun movieAndChannelFavoriteRoundTripsRestoreOriginalState()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogin")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val session=SessionStore(context);assertNotNull(session.token)
        val api=OlevodApi(token={session.token})
        val movieId=80632L
        val initialMovie=api.detail(movieId).favorite
        try{api.favorite(movieId,true);assertTrue(api.favorites(1).items.any{it.id==movieId});api.favorite(movieId,false);assertFalse(api.favorites(1).items.any{it.id==movieId})}
        finally{api.favorite(movieId,initialMovie)}
        val channelId=58L
        val initialChannel=api.favoriteChannels().any{it.id==channelId}
        suspend fun waitFor(saved:Boolean){
            for(attempt in 0 until 30){
                if(api.favoriteChannels().any{it.id==channelId}==saved)return
                kotlinx.coroutines.delay(2000)
            }
            fail("Channel favorite did not converge after 60 seconds")
        }
        try{
            try{api.favoriteChannel(channelId,false)}catch(e:ApiException){if(e.code != -2)throw e}
            waitFor(false)
            try{api.favoriteChannel(channelId,true)}catch(e:ApiException){if(e.code != -2)throw e}
            waitFor(true)
            api.favoriteChannel(channelId,true)
            assertEquals(1,api.favoriteChannels().count{it.id==channelId})
        }finally{
            try{api.favoriteChannel(channelId,initialChannel)}catch(e:ApiException){if(e.code != -2)throw e}
            waitFor(initialChannel)
        }
    }
}
