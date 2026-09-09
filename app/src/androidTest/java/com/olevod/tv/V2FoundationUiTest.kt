@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.olevod.tv

import android.view.KeyEvent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class V2FoundationUiTest {
    @get:Rule val compose=createComposeRule()
    private fun key(code:Int) = InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)

    @Test fun singleHeaderKeepsEveryItemVisibleAndOnlyConfirmNavigates() {
        var selected="home"
        compose.setContent {
            val refs=remember { navigationItems.associate { it.key to FocusRequester() } }
            MaterialTheme { UnifiedHeader("home",refs,{}) {selected=it} }
            LaunchedEffect(Unit) { withFrameNanos{};refs.getValue("home").requestFocus() }
        }
        compose.onNodeWithTag("nav:home").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithTag("nav:search").assertIsFocused()
        assertEquals("home",selected)
        navigationItems.drop(1).forEach { item ->
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            compose.onNodeWithTag("nav:${item.key}").assertIsFocused().assertIsDisplayed()
            compose.onNodeWithTag("nav:short").assertIsDisplayed()
            assertEquals("home",selected)
        }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle {assertEquals("account",selected)}
        val headers=compose.onAllNodesWithTag("unified-header").fetchSemanticsNodes()
        assertEquals(1,headers.size)
    }

    @Test fun wholePosterAndMetadataScrollInsideViewportWithoutFocusScale() {
        val movies=(1L..12L).map {Movie(it,"完整海报与双行长标题示例 $it","",note="更新至24集",score="8.0",year="2026",area="大陆")}
        var density=1f
        compose.setContent {
            val entry=remember{FocusRequester()}
            density=LocalDensity.current.density
            MaterialTheme { CompositionLocalProvider(LocalBringIntoViewSpec provides EdgeBringIntoViewSpec) {
                LazyColumn(Modifier.fillMaxSize().background(Bg),contentPadding=PaddingValues(36.dp,8.dp,36.dp,64.dp)) {
                    items(movies.chunked(3)){ row -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        row.forEach { m->
                            PosterTile(m,Modifier.weight(1f).then(if(m.id==1L)Modifier.focusRequester(entry)else Modifier)){}
                            if(m.id==1L)LaunchedEffect(Unit){entry.requestFocus()}
                        }
                    } }
                }
            } }
        }
        compose.onNodeWithTag("poster:1").assertIsFocused()
        val before=compose.onNodeWithTag("poster:1").fetchSemanticsNode().boundsInRoot.size
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        val after=compose.onNodeWithTag("poster:1").fetchSemanticsNode().boundsInRoot.size
        assertEquals(before,after)
        repeat(3){key(KeyEvent.KEYCODE_DPAD_DOWN)}
        compose.waitForIdle()
        val card=compose.onNodeWithTag("poster:11")
        card.assertIsFocused().assertIsDisplayed()
        val bounds=card.getUnclippedBoundsInRoot()
        val viewport=compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue("Entire focused card including title must be above bottom",bounds.bottom<=viewport.bottom)
        assertTrue("Entire focused poster must be below top",bounds.top>=viewport.top)
        assertTrue("Minimum bottom room",viewport.bottom-bounds.bottom>=2.dp)
    }
    @Test fun scoreBadgeIsOverArtworkTopRightAndLongTitleKeepsFullWidth() {
        val longTitle="这是一个用于验证评分不再挤占文字空间的很长影片标题"
        val scored=Movie(801L,longTitle,"",score="9.3",year="2026")
        val unscored=Movie(802L,"没有评分的影片","",year="2026")
        compose.setContent { MaterialTheme { Row(Modifier.padding(24.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            PosterTile(scored,Modifier.width(176.dp)){}
            PosterTile(unscored,Modifier.width(176.dp)){}
        } } }
        val artwork=compose.onNodeWithTag("artwork:801",useUnmergedTree=true).getUnclippedBoundsInRoot()
        val badge=compose.onNodeWithTag("poster-score:801",useUnmergedTree=true)
        badge.assertIsDisplayed().assertContentDescriptionEquals("评分 9.3")
        val scoreBounds=badge.getUnclippedBoundsInRoot()
        assertTrue("Score is within the artwork, not the title row",scoreBounds.left>=artwork.left&&scoreBounds.right<=artwork.right&&scoreBounds.top>=artwork.top&&scoreBounds.bottom<=artwork.bottom)
        assertTrue("Score is at the right edge",artwork.right-scoreBounds.right<=12.dp)
        assertTrue("Score is at the top edge",scoreBounds.top-artwork.top<=12.dp)
        assertTrue("Score is in the right half",scoreBounds.left>artwork.left+(artwork.right-artwork.left)/2)
        val title=compose.onNodeWithText(longTitle,useUnmergedTree=true).getUnclippedBoundsInRoot()
        assertTrue("Long title starts below artwork",title.top>=artwork.bottom)
        assertEquals("Score no longer subtracts width from the title",(artwork.right-artwork.left).value,(title.right-title.left).value,1f)
        compose.onNodeWithTag("poster-score:802",useUnmergedTree=true).assertDoesNotExist()
        compose.onAllNodesWithTag("poster-score:801",useUnmergedTree=true).assertCountEquals(1)
        // The badge exposes a single spoken description via clearAndSetSemantics.
        // A leftover title-row score would still expose its raw Text separately.
        compose.onAllNodesWithText("9.3",useUnmergedTree=true).assertCountEquals(0)
    }

    @Test fun fitPreservesAllFourCornersForPortraitAndVeryTallImages() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val dimensions=listOf(100 to 150, 120 to 160, 30 to 300)
        val colors=listOf(android.graphics.Color.RED,android.graphics.Color.GREEN,android.graphics.Color.BLUE,android.graphics.Color.YELLOW)
        val files=dimensions.mapIndexed { index,(w,h)->
            val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap);val paint=Paint()
            val boxes=listOf(floatArrayOf(0f,0f,w/2f,h/2f),floatArrayOf(w/2f,0f,w.toFloat(),h/2f),
                floatArrayOf(0f,h/2f,w/2f,h.toFloat()),floatArrayOf(w/2f,h/2f,w.toFloat(),h.toFloat()))
            boxes.forEachIndexed { quadrant,b->paint.color=colors[quadrant];canvas.drawRect(b[0],b[1],b[2],b[3],paint) }
            File(context.cacheDir,"fit-fixture-$index.png").also { file->file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle() }
        }
        try {
            compose.setContent { MaterialTheme { Row {
                files.forEachIndexed { index,file->PosterArtwork(Movie(index.toLong(),"比例样本",file.toURI().toString()),Modifier.width(120.dp).height(180.dp)) }
            } } }
            files.forEachIndexed { index,_ ->
                val node=compose.onNodeWithTag("artwork:$index",useUnmergedTree=true)
                compose.waitUntil(10_000) {
                    val pixels=node.captureToImage().toPixelMap()
                    val (w,h)=dimensions[index]
                    val scale=minOf(pixels.width.toFloat()/w,pixels.height.toFloat()/h)
                    val x=((pixels.width-w*scale)/2+w*scale*.75f).toInt()
                    val y=((pixels.height-h*scale)/2+h*scale*.25f).toInt()
                    val sample=pixels[x,y]
                    sample.green>.9f && sample.red<.1f && sample.alpha>.9f
                }
                val pixels=node.captureToImage().toPixelMap()
                val (w,h)=dimensions[index]
                val scale=minOf(pixels.width.toFloat()/w,pixels.height.toFloat()/h)
                val left=(pixels.width-w*scale)/2;val top=(pixels.height-h*scale)/2
                listOf(.1f to .1f,.9f to .1f,.1f to .9f,.9f to .9f).forEachIndexed { quadrant,(x,y)->
                    val actual=pixels[(left+w*scale*x).toInt(),(top+h*scale*y).toInt()]
                    val expected=Color(colors[quadrant])
                    assertEquals("Image $index quadrant $quadrant red",expected.red,actual.red,.08f)
                    assertEquals("Image $index quadrant $quadrant green",expected.green,actual.green,.08f)
                    assertEquals("Image $index quadrant $quadrant blue",expected.blue,actual.blue,.08f)
                }
            }
        } finally {files.forEach {it.delete()}}
    }

}
