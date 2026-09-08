package com.olevod.tv

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.olevod.tv.data.Category
import com.olevod.tv.data.Filter

/** Public-image layout fixture used only by the debug preview entry. */
@Composable
internal fun CatalogPreview(movies:List<Movie>,open:(Movie)->Unit,initialCategory:Int=1,onCategory:(Int)->Unit={}){
    var id by rememberSaveable{mutableIntStateOf(initialCategory)}
    val saver=remember{androidx.compose.runtime.saveable.listSaver<Filter,Any>(save={listOf(it.category,it.area,it.year,it.type,it.initial,it.membership,it.sort)},restore={Filter(it[0] as Int,it[1] as String,it[2] as String,it[3] as Int,it[4] as String,it[5] as Int,it[6] as String)})}
    var filter by rememberSaveable(stateSaver=saver){mutableStateOf(Filter(category=initialCategory))}
    val categories=remember{listOf(1,2,3,4,6,14).map{Category(it,categoryLabel(it),listOf("大陆","香港","台湾","日本","韩国","欧美","泰国","其它"),(2026 downTo 1980).map(Int::toString),listOf(1 to "剧情",2 to "动作",3 to "喜剧",4 to "爱情",5 to "科幻",6 to "悬疑"))}}
    CatalogPageContent(categories.first{it.id==id},categories,filter,CatalogFeedState(movies,total=movies.size,nextPage=2,endReached=true),open,
        changeFilter={filter=it},chooseCategory={id=it;filter=Filter(category=it);onCategory(it)},loadMore={})
}
