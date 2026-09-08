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
    val sessions=SessionStore(application)
    val history=HistoryStore(application){sessions.accountKey}
    init{viewModelScope.launch{history.load()}}
    fun record(movie:Movie,episode:Int,position:Long,duration:Long){viewModelScope.launch{history.save(movie,episode,position,duration)}}
    var sessionVersion by mutableIntStateOf(0)
        private set
    val api=OlevodApi(token={sessions.token})
    suspend fun login(username:String,password:String,captcha:String,captchaId:String){val result=api.login(username,password,captcha,captchaId);sessions.save(result.first,result.second,username);history.load();sessionVersion++;loadHome()}
    fun logout(){sessions.clear();viewModelScope.launch{history.load()};sessionVersion++;loadHome()}
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
