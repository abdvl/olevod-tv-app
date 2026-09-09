package com.olevod.tv

/** VIP is a library across years; its UI label must match the API filter. */
internal data class MiniRankingScope(val yearFilter: String, val label: String)

internal fun miniRankingScope(categoryId: Int, currentYear: String): MiniRankingScope =
    if (categoryId == 6) MiniRankingScope("0", "全部年份")
    else MiniRankingScope(currentYear, currentYear)
