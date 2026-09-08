package com.olevod.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(vm:AppViewModel) {
    var username by remember{mutableStateOf("")}
    var password by remember{mutableStateOf("")}
    var captcha by remember{mutableStateOf("")}
    var captchaId by remember{mutableStateOf("")}
    var picture by remember{mutableStateOf("")}
    var error by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var refresh by remember{mutableIntStateOf(0)}
    val scope=rememberCoroutineScope()
    val loggedIn=vm.sessionVersion.let{vm.sessions.token!=null}
    LaunchedEffect(refresh,loggedIn){if(!loggedIn)try{val c=vm.api.captcha();captchaId=c.first;picture=c.second;captcha=""}catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}}
    Row(Modifier.fillMaxSize().padding(60.dp,25.dp),horizontalArrangement=Arrangement.spacedBy(50.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(18.dp)){
            Text(if(loggedIn)"欢迎回来"else"登录欧乐账号",color=White,fontSize=30.sp)
            Text("收藏与会员内容，随时接着看",color=Green,fontSize=19.sp)
            Text("使用网站已有账号登录。会员权限与播放质量以网站返回结果为准。",color=Muted,fontSize=15.sp)
            if(loggedIn){Text(vm.sessions.name.ifBlank{"已登录"},color=White,fontSize=20.sp);TvAction("退出登录"){vm.logout();password="";username=""}}
        }
        if(!loggedIn)Column(Modifier.width(360.dp).verticalScroll(rememberScrollState()).background(Panel).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            InputBox(username,{username=it},"邮箱 / 用户名")
            InputBox(password,{password=it},"密码",password=true)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                val model=remember(picture){if(picture.startsWith("data:"))runCatching{android.util.Base64.decode(picture.substringAfter(','),android.util.Base64.DEFAULT)}.getOrNull()else picture}
                AsyncImage(model,"登录验证码",Modifier.size(150.dp,55.dp))
                TvAction("换一张"){if(!busy)refresh++}
            }
            InputBox(captcha,{captcha=it},"图片验证码")
            if(error!=null)Text(error!!,color=Gold,fontSize=12.sp)
            TvAction(if(busy)"登录中…"else"登录",selected=true){if(!busy){
                if(username.isBlank()||password.isBlank()||captcha.isBlank()){error="请输入账号、密码和验证码"}
                else scope.launch{busy=true;error=null;try{vm.login(username,password,captcha,captchaId);password="";captcha=""}catch(e:Exception){if(e is CancellationException)throw e;error=if(e is com.olevod.tv.data.ApiException&&e.code==7)"登录未成功，请检查账号、密码和验证码"else safeError(e);refresh++}finally{busy=false}}
            }}
        }
    }
}
