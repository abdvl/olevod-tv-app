package com.olevod.tv

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.olevod.tv.data.RememberedCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class AccountFixture(val remembered:RememberedCredentials?=null,val challenge:suspend()->Pair<String,String>,val login:suspend(String,String,String,String)->Unit)
@Composable
internal fun AccountScreen(vm:AppViewModel,onLoggedIn:()->Unit={},fixture:AccountFixture?=null) {
    val remembered=remember{if(fixture!=null)fixture.remembered else vm.rememberedCredentials}
    var username by remember{mutableStateOf(remembered?.username.orEmpty())}
    var password by remember{mutableStateOf(remembered?.password.orEmpty())}
    var editing by remember{mutableStateOf(remembered==null)}
    var captcha by remember{mutableStateOf("")}
    var challenge by remember{mutableStateOf<Pair<String,String>?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var refreshing by remember{mutableStateOf(false)}
    var refresh by remember{mutableIntStateOf(0)}
    var clearConfirmation by remember{mutableStateOf(false)}
    var enlarged by remember{mutableStateOf(false)}
    var captchaEdit by remember{mutableIntStateOf(0)}
    var focusRequest by remember{mutableIntStateOf(0)}
    var lastDigit by remember{mutableIntStateOf(0)}
    val userFocus=remember{FocusRequester()};val passwordFocus=remember{FocusRequester()};val changeFocus=remember{FocusRequester()};val forgetFocus=remember{FocusRequester()}
    val imageFocus=remember{FocusRequester()};val refreshFocus=remember{FocusRequester()};val inputFocus=remember{FocusRequester()};val loginFocus=remember{FocusRequester()}
    val digits=remember{List(12){FocusRequester()}}
    val scope=rememberCoroutineScope()
    val loggedIn=fixture==null&&vm.sessionVersion.let{vm.sessions.token!=null}
    val page=LocalPageFocus.current
    val canSubmit=username.isNotBlank()&&password.isNotBlank()&&captcha.isNotBlank()&&challenge!=null&&!busy&&!refreshing
    val leftEntry=if(editing)userFocus else changeFocus
    fun focusDefault(){when{loggedIn->changeFocus.requestFocus();editing->userFocus.requestFocus();else->digits[0].requestFocus()}}
    fun rememberInput(){if(fixture==null)vm.rememberCredentials(username,password)}
    fun digit(value:String){if(!busy){captcha=when(value){"退格"->removeLastCodePoint(captcha);"清空"->"";else->if(captcha.codePointCount(0,captcha.length)<8)captcha+value else captcha}}}
    fun refreshChallenge(){if(!busy){error=null;challenge=null;captcha="";refreshing=true;refresh++}}
    LaunchedEffect(refresh,loggedIn){
        if(!loggedIn){challenge=null;captcha="";refreshing=true
            try{val pair=fixture?.challenge?.invoke()?:vm.api.captcha();if(pair.first.isBlank()||pair.second.isBlank())error="验证码无法加载，请换一张"else{challenge=pair}}
            catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e)}finally{refreshing=false}
        }
    }
    DisposableEffect(page,loggedIn,editing){page?.enter={focusDefault()};onDispose{page?.enter=null}}
    LaunchedEffect(loggedIn,editing,focusRequest){withFrameNanos{};focusDefault()}
    Row(Modifier.fillMaxSize().padding(60.dp,12.dp,60.dp,20.dp).testTag("account-page").onPreviewKeyEvent{event->
        if(!loggedIn&&!editing&&event.type==KeyEventType.KeyDown){val key=event.nativeKeyEvent.keyCode
            when(key){in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9->{digit((key-android.view.KeyEvent.KEYCODE_0).toString());true};android.view.KeyEvent.KEYCODE_DEL->{digit("退格");true};else->false}
        }else false
    },horizontalArrangement=Arrangement.spacedBy(36.dp)){
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text(if(loggedIn)"欢迎回来"else"登录欧乐账号",color=White,fontSize=28.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold)
            Text(if(!editing&&!loggedIn)"这次只需输入验证码"else"收藏与会员内容，随时接着看",color=Green,fontSize=16.sp,lineHeight=22.sp)
            if(loggedIn){
                Text(vm.sessions.name.ifBlank{"已登录"},color=White,fontSize=20.sp,lineHeight=26.sp)
                TvAction("退出登录",modifier=Modifier.focusRequester(changeFocus).restoreContentFocus("logout")){vm.logout();val saved=vm.rememberedCredentials;username=saved?.username.orEmpty();password=saved?.password.orEmpty();editing=saved==null}
                TvAction("清除记住的账号密码",modifier=Modifier.focusRequester(forgetFocus).restoreContentFocus("forget")){clearConfirmation=true}
            }else if(editing){
                InputBox(username,{if(!busy)username=it},"邮箱 / 用户名",Modifier.focusRequester(userFocus).testTag("login-username").focusProperties{up=page?.header?:FocusRequester.Default;down=passwordFocus;right=imageFocus},onEditingFinished={passwordFocus.requestFocus()})
                InputBox(password,{if(!busy)password=it},"密码",Modifier.focusRequester(passwordFocus).testTag("login-password").focusProperties{up=userFocus;right=digits[0]},password=true,onEditingFinished={
                    rememberInput();if(username.isNotBlank()&&password.isNotBlank()){editing=false;focusRequest++}
                })
            }else{
                Text(username,color=White,fontSize=22.sp,lineHeight=28.sp)
                Text("••••••••",color=Muted,fontSize=18.sp,lineHeight=24.sp)
                if(fixture!=null||vm.rememberedCredentials?.let{it.username==username&&it.password==password}==true)Text("已自动填写账号与密码",color=Green,fontSize=14.sp,lineHeight=20.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    TvAction("更换账号",modifier=Modifier.focusRequester(changeFocus).restoreContentFocus("change-account").focusProperties{up=page?.header?:FocusRequester.Default;right=forgetFocus;down=digits[0]}){if(!busy)editing=true}
                    TvAction("清除已保存信息",modifier=Modifier.focusRequester(forgetFocus).restoreContentFocus("forget").focusProperties{left=changeFocus;right=imageFocus;down=digits[0]}){if(!busy)clearConfirmation=true}
                }
            }
            Text("账号密码加密保存在这台电视，下次只需输入新验证码。",color=Muted,fontSize=14.sp,lineHeight=22.sp)
            vm.credentialPersistenceError?.let{Text(it,color=TvDesign.warning,fontSize=14.sp,lineHeight=22.sp)}
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(TvDesign.border))
        if(loggedIn)Column(Modifier.width(360.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text("账号已登录",color=White,fontSize=22.sp,lineHeight=28.sp)
            Text("可以同步影视收藏，查看网站历史并播放账号有权限的内容。",color=Muted,fontSize=16.sp,lineHeight=24.sp)
        }else Column(Modifier.width(360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("输入验证码",color=White,fontSize=22.sp,lineHeight=28.sp)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                val imageSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}
                val imageFocused by imageSource.collectIsFocusedAsState()
                val picture=challenge?.second.orEmpty()
                val model=remember(picture){if(picture.startsWith("data:"))runCatching{android.util.Base64.decode(picture.substringAfter(','),android.util.Base64.DEFAULT)}.getOrNull()else picture}
                Box(Modifier.size(178.dp,55.dp).background(androidx.compose.ui.graphics.Color(0xFFF3F4EB),RoundedCornerShape(8.dp)).focusRequester(imageFocus)
                    .focusProperties{left=leftEntry;right=refreshFocus;up=page?.header?:FocusRequester.Default;down=inputFocus}
                    .border(2.dp,if(imageFocused)Green else TvDesign.border,RoundedCornerShape(8.dp)).clickable(interactionSource=imageSource,indication=null){if(!busy){if(challenge!=null)enlarged=true else refreshChallenge()}},contentAlignment=Alignment.Center){
                    if(refreshing)Text("正在加载…",color=Muted,fontSize=13.sp)else AsyncImage(model,"登录验证码，确认放大",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
                }
                TvAction("换一张",Icons.Rounded.Refresh,modifier=Modifier.focusRequester(refreshFocus).testTag("captcha-refresh").focusProperties{left=imageFocus;up=page?.header?:FocusRequester.Default;down=inputFocus}){refreshChallenge()}
            }
            InputBox(captcha,{if(!busy)captcha=it.takeCodePoints(8)},"输入验证码",Modifier.focusRequester(inputFocus).testTag("captcha-input").focusProperties{up=imageFocus;down=digits[0];left=leftEntry},editRequest=captchaEdit,onEditingFinished={digits[lastDigit].requestFocus()},digitSlots=true)
            val labels=listOf("1","2","3","4","5","6","7","8","9","退格","0","清空")
            labels.chunked(3).forEachIndexed{row,values->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                values.forEachIndexed{column,label->val index=row*3+column
                    KeyButton(label,Modifier.weight(1f).focusRequester(digits[index]).testTag("captcha-key:$label").onFocusChanged{if(it.isFocused)lastDigit=index}.focusProperties{
                        up=if(row==0)inputFocus else digits[index-3]
                        down=if(row<3)digits[index+3]else if(canSubmit)loginFocus else FocusRequester.Cancel
                        left=if(column>0)digits[index-1]else leftEntry
                        right=if(column<2)digits[index+1]else FocusRequester.Cancel
                    }){digit(label)}
                }
            }}
            TvAction(if(busy)"登录中…"else"登录",Icons.Rounded.Login,selected=canSubmit,enabled=canSubmit||busy,modifier=Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(50)).focusRequester(loginFocus).testTag("login-submit")
                .focusProperties{canFocus=canSubmit||busy;up=digits[lastDigit];down=FocusRequester.Cancel;left=leftEntry}
                .semantics{if(!canSubmit)disabled()}){
                if(canSubmit){busy=true;error=null;val submittedChallenge=challenge!!;val submittedUser=username;val submittedPassword=password;val submittedCaptcha=captcha
                    scope.launch{try{
                        if(fixture!=null)fixture.login(submittedUser,submittedPassword,submittedCaptcha,submittedChallenge.first)else vm.login(submittedUser,submittedPassword,submittedCaptcha,submittedChallenge.first)
                        captcha="";onLoggedIn()
                    }catch(e:Exception){if(e is CancellationException)throw e;error=safeError(e);challenge=null;captcha="";refresh++;focusRequest++}finally{busy=false}}
                }
            }
            error?.let{Text(it,color=TvDesign.error,fontSize=13.sp,lineHeight=18.sp)}
            Text("确认输入数字 · 填完后选择登录",color=Muted,fontSize=13.sp,lineHeight=18.sp)
            TvAction("使用系统键盘",Icons.Rounded.Keyboard,modifier=Modifier.focusProperties{up=digits[9];left=leftEntry}){if(!busy)captchaEdit++}
        }
    }
    if(clearConfirmation)TvConfirmDialog("清除记住的账号密码？","下次需要重新输入账号密码。当前登录与观看历史不会被删除。","清除",onCancel={clearConfirmation=false;scope.launch{withFrameNanos{};forgetFocus.requestFocus()}},onConfirm={
        clearConfirmation=false;if(fixture==null)vm.forgetCredentials();username="";password="";editing=true;focusRequest++
    })
    if(enlarged)androidx.compose.ui.window.Dialog(onDismissRequest={enlarged=false;scope.launch{withFrameNanos{};imageFocus.requestFocus()}}){
        val close=remember{FocusRequester()}
        Column(Modifier.background(Panel,RoundedCornerShape(14.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            val picture=challenge?.second.orEmpty();val model=remember(picture){if(picture.startsWith("data:"))runCatching{android.util.Base64.decode(picture.substringAfter(','),android.util.Base64.DEFAULT)}.getOrNull()else picture}
            AsyncImage(model,"放大的登录验证码",Modifier.fillMaxWidth().height(110.dp).background(androidx.compose.ui.graphics.Color(0xFFF3F4EB)),contentScale=ContentScale.Fit)
            TvAction("返回输入",modifier=Modifier.focusRequester(close)){enlarged=false;scope.launch{withFrameNanos{};imageFocus.requestFocus()}}
        }
        LaunchedEffect(Unit){withFrameNanos{};close.requestFocus()}
    }
}
internal fun String.takeCodePoints(count:Int):String=substring(0,offsetByCodePoints(0,minOf(count,codePointCount(0,length))))
