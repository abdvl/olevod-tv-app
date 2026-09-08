package com.olevod.tv

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.olevod.tv.data.Category
import com.olevod.tv.data.Filter

/** Public-image layout fixture used only by the debug preview entry. */
@Composable
internal fun CatalogPreview(movies:List<Movie>,open:(Movie)->Unit){
    var id by rememberSaveable{mutableIntStateOf(1)}
    var filter by remember{mutableStateOf(Filter())}
    val categories=remember{listOf(1,2,3,4,6,14).map{Category(it,categoryLabel(it),listOf("大陆","香港","台湾","日本","韩国","欧美","泰国","其它"),(2026 downTo 1980).map(Int::toString),listOf(1 to "剧情",2 to "动作",3 to "喜剧",4 to "爱情",5 to "科幻",6 to "悬疑"))}}
    CatalogPageContent(categories.first{it.id==id},categories,filter,CatalogFeedState(movies,total=movies.size,nextPage=2,endReached=true),open,
        changeFilter={filter=it},chooseCategory={id=it;filter=Filter(category=it)},loadMore={})
}
