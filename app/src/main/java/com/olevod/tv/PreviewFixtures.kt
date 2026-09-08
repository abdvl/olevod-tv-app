package com.olevod.tv

import com.olevod.tv.data.Category
import com.olevod.tv.data.WatchRecord

/** Explicit preview/test data. Never written to local history or used in connected mode. */
internal fun previewWatchRecords(movies:List<Movie>)=movies.take(5).mapIndexed { i,m ->
    WatchRecord(m,if(i==1||i==3)i+6 else 0,(i+1)*384000L,5400000L,1000L-i)
}
internal fun previewHome(movies:List<Movie>,heroes:List<Hero>)=HomeState(heroes,
    listOf(1,2,3,6,14).map { id->
        HomeSection(Category(id,categoryLabel(id),emptyList(),emptyList(),emptyList()),
            movies.filter{it.category==id}.ifEmpty{movies}.take(10),loading=false)
    },loading=false)
