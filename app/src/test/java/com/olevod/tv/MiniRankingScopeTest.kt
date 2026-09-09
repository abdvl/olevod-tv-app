package com.olevod.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class MiniRankingScopeTest {
    @Test fun vipRequestsAndLabelsAllYears() {
        assertEquals(MiniRankingScope("0", "全部年份"), miniRankingScope(6, "2026"))
    }

    @Test fun ordinaryCategoriesKeepTheRequestedCurrentYear() {
        listOf(1, 2, 3, 4, 14).forEach { category ->
            assertEquals(MiniRankingScope("2026", "2026"), miniRankingScope(category, "2026"))
        }
    }
}
