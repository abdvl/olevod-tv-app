package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(vm:AppViewModel) {
    val remembered=remember{vm.credentials.read()}
    var username by remember{mutableStateOf(remembered?.username.orEmpty())}
    var password by remember{mutableStateOf(remembered?.password.orEmpty())}
    var editCredentials by remember{mutableStateOf(remembered==null)}
    var captcha by remember{mutableStateOf("")}
    var captchaId by remember{mutableStateOf("")}
    var picture by remember{mutableStateOf("")}
    var error by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var refresh by remember{mutableIntStateOf(0)}
    val keypadFocus=remember{FocusRequester()}
    val usernameFocus=remember{FocusRequester()}
    val scope=rememberCoroutineScope()
    val loggedIn=vm.sessionVersion.let{vm.sessions.token!=null}
    fun rememberInput(){vm.rememberCredentials(username,password)}
    fun forget(){vm.forgetCredentials();username="";password="";editCredentials=true}
    LaunchedEffect(refresh,loggedIn){if(!loggedIn)try{val c=vm.api.captcha();captchaId=c.first;picture=c.second;captcha=""}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}}
    LaunchedEffect(loggedIn,editCredentials){if(!loggedIn){if(editCredentials)usernameFocus.requestFocus()else keypadFocus.requestFocus()}}
    Row(Modifier.fillMaxSize().padding(60.dp,20.dp).onPreviewKeyEvent{event->
        if(!loggedIn&&!editCredentials&&event.type==KeyEventType.KeyDown){
            val key=event.nativeKeyEvent.keyCode
            when(key){
                in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9->{if(captcha.length<8)captcha+=(key-android.view.KeyEvent.KEYCODE_0).toString();true}
                android.view.KeyEvent.KEYCODE_DEL->{captcha=captcha.dropLast(1);true}
                else->false
            }
        }else false
    },horizontalArrangement=Arrangement.spacedBy(50.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(18.dp)){
            Text(if(loggedIn)"欢迎回来"else"登录欧乐账号",color=White,fontSize=30.sp)
            Text("收藏与会员内容，随时接着看",color=Green,fontSize=19.sp)
            Text("账号密码加密保存在这台电视。下次只需输入新验证码。",color=Muted,fontSize=15.sp)
            if(loggedIn){
                Text(vm.sessions.name.ifBlank{"已登录"},color=White,fontSize=20.sp)
                TvAction("退出登录"){vm.logout();val saved=vm.credentials.read();username=saved?.username.orEmpty();password=saved?.password.orEmpty();editCredentials=saved==null}
                TvAction("清除记住的账号密码"){forget()}
            }
        }
        if(!loggedIn)Column(Modifier.width(360.dp).verticalScroll(rememberScrollState()).background(Panel).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(editCredentials){
                InputBox(username,{username=it},"邮箱 / 用户名",Modifier.focusRequester(usernameFocus),onEditingFinished={rememberInput()})
                InputBox(password,{password=it},"密码",password=true,onEditingFinished={rememberInput();if(username.isNotBlank()&&password.isNotBlank())editCredentials=false})
            }else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                TvAction("更换账号"){editCredentials=true}
                TvAction("清除记住的信息"){forget()}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                val model=remember(picture){if(picture.startsWith("data:"))runCatching{android.util.Base64.decode(picture.substringAfter(','),android.util.Base64.DEFAULT)}.getOrNull()else picture}
                AsyncImage(model,"登录验证码",Modifier.size(150.dp,55.dp))
                TvAction("换一张"){if(!busy)refresh++}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Text(captcha.ifBlank{"输入验证码"},color=if(captcha.isBlank())Muted else White,fontSize=21.sp,modifier=Modifier.weight(1f).padding(8.dp))
                TvAction("退格"){captcha=captcha.dropLast(1)}
            }
            listOf(listOf("1","2","3","4","5"),listOf("6","7","8","9","0")).forEach{row->
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){row.forEach{digit->
                    TvAction(digit,modifier=Modifier.weight(1f).then(if(digit=="1")Modifier.focusRequester(keypadFocus)else Modifier)){if(captcha.length<8)captcha+=digit}
                }}
            }
            if(error!=null)Text(error!!,color=Gold,fontSize=12.sp)
            TvAction(if(busy)"登录中…"else"登录",selected=true){if(!busy){
                if(username.isBlank()||password.isBlank()||captcha.isBlank()){error="请输入账号、密码和验证码"}
                else scope.launch{busy=true;error=null;try{vm.login(username,password,captcha,captchaId);captcha=""}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e);refresh++}finally{busy=false}}
            }}
        }
    }
}
