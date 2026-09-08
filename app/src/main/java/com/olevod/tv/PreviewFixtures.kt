package com.olevod.tv

import androidx.compose.runtime.*
import com.olevod.tv.data.*

/** Public catalog data plus clearly synthetic state, using the same production composables. */
@Composable
internal fun SearchPreviewFixture(vm:AppViewModel,movies:List<Movie>,open:(Movie)->Unit){
    val scope=rememberCoroutineScope()
    val fixture=remember{val cache=mutableMapOf<String,CatalogFeed>();SearchFixture(movies.map{it.title}.take(10),
        {q->movies.filter{it.title.contains(q,true)}.map{it.title}.ifEmpty{movies.map{it.title}.take(8)}},
        {q->cache.getOrPut(q){CatalogFeed(scope){page->val found=movies.filter{it.title.contains(q,true)};CatalogPage(found.drop((page-1)*20).take(20),found.size,page,20)}}})}
    ConnectedSearch(vm,open,fixture)
}

@Composable
internal fun HistoryPreviewFixture(vm:AppViewModel,movies:List<Movie>,open:(Movie)->Unit,browse:()->Unit,login:()->Unit){
    var records by remember{mutableStateOf(previewWatchRecords(movies).take(8))}
    HistoryScreen(vm,open,browse,fixtureRecords=records,fixtureDelete={id->records=if(id==null)emptyList()else records.filter{it.movie.id!=id}},login=login)
}

@Composable
internal fun FavoritesPreviewFixture(vm:AppViewModel,movies:List<Movie>,open:(Movie)->Unit,browse:()->Unit){
    val scope=rememberCoroutineScope()
    val feed=remember{CatalogFeed(scope){page->CatalogPage(movies.drop((page-1)*20).take(20),movies.size,page,20)}}
    FavoritesScreen(vm,open,{},browse,fixtureFeed=feed)
}

@Composable
internal fun AccountPreviewFixture(vm:AppViewModel,first:Boolean=false){
    val fixture=remember(first){AccountFixture(if(first)null else RememberedCredentials("movie_fan","preview-only"),
        challenge={OlevodApi().captcha()},login={_,_,_,_->throw ApiException(-1,"布局预览不提交账号，请使用正式入口登录")})}
    AccountScreen(vm,fixture=fixture)
}

/** Explicit preview/test data. Never written to local history or used in connected mode. */
internal fun previewWatchRecords(movies:List<Movie>)=movies.take(5).mapIndexed { i,m ->
    WatchRecord(m,if(i==1||i==3)i+6 else 0,(i+1)*384000L,5400000L,System.currentTimeMillis()-i*86400000L)
}
internal fun previewHome(movies:List<Movie>,heroes:List<Hero>)=HomeState(heroes,
    listOf(1,2,3,4,6,14).map { id->
        HomeSection(Category(id,categoryLabel(id),emptyList(),emptyList(),emptyList()),
            movies.filter{it.category==id}.ifEmpty{movies}.take(10),loading=false)
    },loading=false)
