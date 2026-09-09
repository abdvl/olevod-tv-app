package com.olevod.tv

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.olevod.tv.data.Filter
import com.olevod.tv.data.OlevodApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit public-catalog opt-in; no saved session, playback URLs, or account mutations. */
class V02VipMiniCatalogTest {
    @Test fun vipAllYearsRankingsHaveTwelveDistinctMovies(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveCatalog") == "true")
        val api = OlevodApi()
        val scope = miniRankingScope(6, "2026")
        assertEquals("0", scope.yearFilter)
        assertEquals("全部年份", scope.label)
        listOf("hot", "score").forEach { sort ->
            val page = api.browse(Filter(category=6, year=scope.yearFilter, sort=sort), size=20)
            val unique = page.items.map { it.id }.toSet()
            assertTrue("VIP $sort needs at least twelve distinct films", unique.size >= 12)
            assertEquals("Ranking rows must not repeat film IDs", page.items.size, unique.size)
            assertTrue("VIP catalog results must retain their identity", page.items.all { it.id > 0 && it.category == 6 })
            Log.i("V02VipMiniCatalog", "sort=$sort year=${scope.yearFilter} total=${page.total} rows=${page.items.size} distinct=${unique.size}")
        }
    }
}
