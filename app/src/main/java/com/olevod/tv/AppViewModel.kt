package com.olevod.tv

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.olevod.tv.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel as CoroutineChannel
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
    var pendingResume:WatchRecord?=null
    var pendingChannel by mutableStateOf<Channel?>(null)
    val credentials=CredentialsStore(application)
    fun rememberCredentials(username:String,password:String){if(username.isNotBlank()&&password.isNotBlank())viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO){credentials.save(username,password)}}
    fun forgetCredentials(){credentials.clear()}
    val sessions=SessionStore(application)
    val history=HistoryStore(application){sessions.accountKey}
    init{viewModelScope.launch{history.load()}}
    private data class PendingWatch(val watch:WatchRecord,val account:String,val token:String?,val force:Boolean)
    private val watchQueue=CoroutineChannel<PendingWatch>(CoroutineChannel.UNLIMITED)
    private val cloudQueue=CoroutineChannel<PendingWatch>(CoroutineChannel.UNLIMITED)
    private val lastSync=mutableMapOf<String,Pair<Long,Long>>()
    var historySyncError by mutableStateOf<String?>(null)
        private set
    init {
        viewModelScope.launch {
            for(pending in watchQueue) {
                val watch=pending.watch
                history.save(watch.movie,watch.episode,watch.positionMs,watch.durationMs,pending.account)
                cloudQueue.send(pending)
            }
        }
        viewModelScope.launch {
            for(pending in cloudQueue) {
                val watch=pending.watch
                val capturedToken=pending.token
                if(capturedToken==null || watch.positionMs<1000 || watch.durationMs<=0 ||
                    pending.account!=sessions.accountKey || capturedToken!=sessions.token)continue
                val key="${pending.account}:${watch.movie.id}:${watch.episode}"
                val previous=lastSync[key]
                if(previous!=null && (previous.second==watch.positionMs || (!pending.force && watch.updatedAt-previous.first<30000)))continue
                try {
                    OlevodApi(token={capturedToken}).syncWatch(watch)
                    lastSync[key]=watch.updatedAt to watch.positionMs
                    if(pending.account==sessions.accountKey)historySyncError=null
                }catch(e:Exception){
                    if(e is CancellationException)throw e
                    if(pending.account==sessions.accountKey)historySyncError="本机进度已保存；网站同步失败，下次观看时重试"
                }
            }
        }
    }
    fun record(movie:Movie,episode:Int,position:Long,duration:Long,account:String=sessions.accountKey,forceSync:Boolean=false){
        val capturedToken=sessions.token.takeIf{account==sessions.accountKey}
        watchQueue.trySend(PendingWatch(WatchRecord(movie,episode,position,duration,System.currentTimeMillis()),account,capturedToken,forceSync))
    }
    var sessionVersion by mutableIntStateOf(0)
        private set
    val api=OlevodApi(token={sessions.token},onUnauthorized={logout()})
    suspend fun login(username:String,password:String,captcha:String,captchaId:String){kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){credentials.save(username,password)};val result=api.login(username,password,captcha,captchaId);kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){sessions.save(result.token,result.name,result.accountId)};history.load();sessionVersion++;loadHome()}
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
