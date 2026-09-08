package com.olevod.tv

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.olevod.tv.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeSection(val category:Category,val movies:List<Movie> = emptyList(),val loading:Boolean=true,val error:String?=null)
data class HomeState(val heroes:List<Hero> = emptyList(),val sections:List<HomeSection> = emptyList(),val loading:Boolean=true,val error:String?=null)

class AppViewModel(application:Application):AndroidViewModel(application) {
    private val searchPrefs=application.getSharedPreferences("search",0)
    var searchHistory by mutableStateOf(runCatching{org.json.JSONArray(searchPrefs.getString("words","[]")).let{a->(0 until a.length()).map{a.getString(it)}}}.getOrDefault(emptyList()))
        private set
    fun saveQuery(query:String){if(query.isBlank())return;searchHistory=(listOf(query.trim())+searchHistory).distinct().take(20);searchPrefs.edit().putString("words",org.json.JSONArray(searchHistory).toString()).apply()}
    fun clearSearchHistory(){searchHistory=emptyList();searchPrefs.edit().remove("words").apply()}
    var pendingChannel by mutableStateOf<Channel?>(null)
    val sessions=SessionStore(application)
    val history=HistoryStore(application){sessions.accountKey}
    init{viewModelScope.launch{history.load()}}
    fun record(movie:Movie,episode:Int,position:Long,duration:Long,account:String=sessions.accountKey){viewModelScope.launch{history.save(movie,episode,position,duration,account)}}
    var sessionVersion by mutableIntStateOf(0)
        private set
    val api=OlevodApi(token={sessions.token},onUnauthorized={logout()})
    suspend fun login(username:String,password:String,captcha:String,captchaId:String){val result=api.login(username,password,captcha,captchaId);sessions.save(result.token,result.name,result.accountId);history.load();sessionVersion++;loadHome()}
    fun logout(){sessions.clear();history.clearView();viewModelScope.launch{history.load()};sessionVersion++;loadHome()}
    private val _home=MutableStateFlow(HomeState())
    val home=_home.asStateFlow()
    private var homeJob:Job?=null
    fun loadHome() {
        if(homeJob?.isActive==true)return
        homeJob=viewModelScope.launch {
            _home.update{it.copy(loading=true,error=null)}
            try {
                api.config()
                val categories=api.categories()
                _home.update{it.copy(sections=categories.map{c->HomeSection(c)})}
                try{val heroes=api.banners();_home.update{it.copy(heroes=heroes)}}catch(e:Exception){if(e is CancellationException)throw e}
                _home.update{it.copy(loading=false)}
                categories.forEach { c ->loadSection(c) }
            }catch(e:Exception){if(e is CancellationException)throw e;_home.update{it.copy(loading=false,error=safeError(e))}}
        }
    }
    private suspend fun loadSection(c:Category) {
        try {
            val list=api.browse(Filter(category=c.id),size=10).items
            _home.update{it.copy(sections=it.sections.map{s->if(s.category.id==c.id)s.copy(movies=list,loading=false,error=null) else s})}
        }catch(e:Exception){if(e is CancellationException)throw e;_home.update{it.copy(sections=it.sections.map{s->if(s.category.id==c.id)s.copy(loading=false,error=safeError(e)) else s})}}
    }
    fun retrySection(c:Category){viewModelScope.launch{loadSection(c)}}
}
internal fun safeError(e:Exception):String=when(e){is ApiException->e.userMessage;is java.io.IOException->"网络连接失败，请重试";else->"数据暂时无法加载，请重试"}
