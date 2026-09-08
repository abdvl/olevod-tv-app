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
class CatalogIntegrationTest {
    @Test fun liveCategoriesSortsPaginationAndChineseSearch()=runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveCatalog")=="true")
        val api=OlevodApi();api.config()
        val categories=api.categories()
        assertTrue(categories.map{it.id}.containsAll(listOf(1,2,3,4,6,14)))
        for(category in categories){
            val page=api.browse(Filter(category=category.id),size=10)
            assertEquals(minOf(10,page.total),page.items.size)
            assertTrue(page.items.all{it.id>0 && it.title.isNotBlank() && it.image.startsWith("https://")})
        }
        for(sort in listOf("update","desc","hot","score")){
            val first=api.browse(Filter(category=1,sort=sort))
            val second=api.browse(Filter(category=1,sort=sort),page=2)
            assertEquals(20,first.items.size);assertEquals(20,second.items.size)
            assertTrue(first.hasMore)
            assertNotEquals(first.items.map{it.id},second.items.map{it.id})
        }
        val filtered=api.browse(Filter(category=1,area="美国",year="2025",membership=2,sort="score"))
        assertTrue(filtered.total>0)
        assertTrue(filtered.items.all{it.year=="2025" && it.area.contains("美国")})
        val search=api.search("早春晴朗")
        assertTrue(search.items.any{it.title.contains("早春晴朗")})
    }
}
