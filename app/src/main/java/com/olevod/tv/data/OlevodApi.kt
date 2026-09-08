package com.olevod.tv.data

import com.olevod.tv.Movie
import com.olevod.tv.Hero
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class Category(val id:Int,val name:String,val areas:List<String>,val years:List<String>,val types:List<Pair<Int,String>>)
data class Filter(val category:Int=1,val area:String="0",val year:String="0",val type:Int=0,val initial:String="0",val membership:Int=3,val sort:String="update")
data class CatalogPage(val items:List<Movie>,val total:Int,val page:Int,val pageSize:Int) { val hasMore get()=if(total>=0)page*pageSize<total else items.size>=pageSize }
data class Episode(val index:Int,val title:String,val uri:String,val vip:Boolean){override fun toString()="Episode(index=$index, title=$title, uri=[redacted], vip=$vip)"}
data class Detail(val movie:Movie,val description:String,val actor:String,val director:String,val episodes:List<Episode>,val favorite:Boolean,val resumeEpisode:Int,val resumeSeconds:Long)
data class Channel(val id:Long,val streamId:String,val title:String,val image:String,val programme:String)
data class Programme(val id:Long,val title:String,val time:String,val start:String,val end:String,val hasReplay:Boolean,val state:Int)
class LiveDetail(val channel:Channel,val uri:String,val programmes:List<Programme>,val favorite:Boolean)
data class CloudHistoryPage(val items:List<WatchRecord>,val total:Int)
class LoginSession(val token:String,val name:String,val accountId:String)
class ApiException(val code:Int,val userMessage:String):IOException(userMessage)

object RequestSignature {
    fun at(seconds:Long):String {
        require(seconds>=0)
        val value=seconds.toString()
        val columns=Array(4){StringBuilder()}
        value.forEach { c -> val bits=c.code.toString(2);repeat(4){columns[it].append(bits[2+it])} }
        val pieces=columns.map{it.toString().toLong(2).toString(16).padStart(3,'0')}
        val hash=MessageDigest.getInstance("MD5").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}
        return hash.take(3)+pieces[0]+hash.substring(6,11)+pieces[1]+hash.substring(14,19)+pieces[2]+hash.substring(22,27)+pieces[3]+hash.substring(30)
    }
}

class OlevodApi(
    private val token:()->String? = {null},
    private val client:OkHttpClient=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS).followRedirects(false).build(),
    private val base:String="https://api.olelive.com",
    private val now:()->Long={System.currentTimeMillis()/1000},
    private val onUnauthorized:()->Unit={},
) {
    @Volatile var imageBase="https://static.olelive.com"
        private set
    private fun url(parts:List<String>,get:Boolean,requestToken:String?):HttpUrl {
        val b=base.toHttpUrl().newBuilder();parts.forEach{b.addPathSegment(it)}
        if(get)b.addQueryParameter("_vv",RequestSignature.at(now()))
        requestToken?.split('.')?.takeIf{it.size==3}?.let { t ->listOf("_he","_pl","_si").forEachIndexed{i,k->b.addQueryParameter(k,t[i])} }
        return b.build()
    }
    suspend fun request(parts:List<String>,post:Any?=null):JSONObject {
        val requestToken=token()
        val request=Request.Builder().url(url(parts,post==null,requestToken)).header("Referer","https://www.olevod.com/").header("User-Agent","Mozilla/5.0 OlevodTV/0.1")
        if(post!=null)request.post(post.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        val call=client.newCall(request.build())
        val bytes=suspendCancellableCoroutine<ByteArray> { continuation ->
            continuation.invokeOnCancellation{call.cancel()}
            call.enqueue(object:Callback {
                override fun onFailure(call:Call,e:IOException){if(continuation.isActive)continuation.resumeWithException(IOException("网络请求失败，请检查连接"))}
                override fun onResponse(call:Call,response:Response){response.use {
                    try {
                        if(!it.isSuccessful)throw ApiException(it.code,if(it.code==429)"请求过于频繁，请稍后重试" else "服务暂时不可用（HTTP ${it.code}）")
                        val body=it.body?.bytes()?:throw IOException("服务器返回空内容")
                        if(continuation.isActive)continuation.resume(body)
                    } catch(e:Exception){if(continuation.isActive)continuation.resumeWithException(if(e is ApiException)e else IOException("无法读取服务器响应"))}
                }}
            })
        }
        val raw=if(bytes.size>2&&bytes[0]==0x1f.toByte()&&bytes[1]==0x8b.toByte())GZIPInputStream(bytes.inputStream()).readBytes() else bytes
        val json=try{JSONObject(raw.toString(Charsets.UTF_8))}catch(e:Exception){throw ApiException(-1,"网站返回了无法识别的数据")}
        val code=json.optInt("code",-1)
        if(code in setOf(13,14,16) && requestToken!=null && token()==requestToken)onUnauthorized()
        if(code!=0)throw ApiException(code,when(code){12->"请先登录账号";13,14,16->"登录已失效，请重新登录";else->"请求未完成（网站代码 $code）"})
        return json
    }
    private suspend fun get(vararg p:String)=request(p.toList()).get("data")
    suspend fun config(){val o=get("v1","pub","index","data") as JSONObject; val candidate=o.optString("image_base");if(candidate.startsWith("https://"))imageBase=candidate.trimEnd('/')}
    fun image(path:String):String=when{path.startsWith("https://")->path;path.startsWith("http://")->"https://"+path.removePrefix("http://");path.startsWith("//")->"https:$path";path.isBlank()->"";else->imageBase+"/"+path.trimStart('/')}
    fun movie(o:JSONObject)=Movie(o.optLong("id",o.optLong("vodId")),o.optString("name",o.optString("title","未知影片")),image(o.optString("picThumb").ifBlank{o.optString("pic")}),o.optString("remarks"),o.optDouble("score",0.0).takeIf{it>0}?.let{"%.1f".format(java.util.Locale.ROOT,it)}?:"",o.optInt("typeId1",1),o.optString("year"),o.optString("area"),o.optBoolean("vip"))
    suspend fun categories():List<Category> = (get("v1","pub","vod","list","type") as JSONArray).objects().map { o->Category(o.getInt("typeId"),o.getString("typeName"),o.optJSONArray("area").strings(),o.optJSONArray("year").strings(),o.optJSONArray("children").objects().map{it.getInt("typeId") to it.getString("typeName")}) }
    suspend fun banners():List<Hero> = (get("v1","pub","index","banners") as JSONArray).objects().filter{it.optInt("bannerType")==4}.map{Hero(it.getLong("id"),it.getString("title"),image(it.optString("img")),it.optString("desc"))}
    suspend fun browse(filter:Filter,page:Int=1,size:Int=20):CatalogPage {
        require(page>0&&size in 1..48)
        require(filter.sort in setOf("update","desc","hot","score"))
        require(filter.membership in 1..3)
        val o=get("v1","pub","vod","list","true",filter.membership.toString(),filter.initial,filter.area,filter.category.toString(),filter.type.toString(),filter.year,filter.sort,page.toString(),size.toString()) as JSONObject
        return CatalogPage(o.optJSONArray("list").objects().map(::movie),o.optInt("total",-1),page,size)
    }
    suspend fun detail(id:Long):Detail {
        val o=get("v1","pub","vod","detail",id.toString(),"true") as JSONObject
        return Detail(movie(o),o.optString("content").replace(Regex("<[^>]*>"),""),o.optString("actor"),o.optString("director"),o.optJSONArray("urls").objects().map{Episode(it.getInt("index"),it.optString("title"),it.optString("url"),it.optBoolean("vip"))},o.optBoolean("favorite"),o.optInt("recordEpisode"),o.optLong("recordWatchDuration"))
    }
    suspend fun search(query:String,category:Int=0,page:Int=1,size:Int=20):CatalogPage {
        val o=get("v1","pub","index","search",query,"vod",category.toString(),page.toString(),size.toString()) as JSONObject
        val group=o.optJSONArray("data").objects().firstOrNull{it.optString("type")=="vod"}
        return CatalogPage(group?.optJSONArray("list").objects().map(::movie),group?.optInt("total",-1)?:0,page,size)
    }
    suspend fun hotWords():List<String> = (get("v1","pub","index","search","hot","keywords") as JSONArray).objects().filter{it.optString("type")=="vod"}.flatMap{it.optJSONArray("words").strings()}
    suspend fun suggestions(query:String):List<String> {val d=get("v1","pub","index","search","keywords",query);return when(d){is JSONArray->d.objects().filter{it.optString("type")=="vod"}.flatMap{it.optJSONArray("words").strings()}.distinct();else->emptyList()}}
    suspend fun captcha():Pair<String,String>{val d=request(listOf("pub","captcha"),JSONObject()).getJSONObject("data");return d.getString("captchaId") to d.getString("picPath")}
    suspend fun login(username:String,password:String,captcha:String,captchaId:String):LoginSession{
        val d=request(listOf("pub","user","login"),JSONObject().put("username",username).put("password",password).put("captcha",captcha).put("captcha_id",captchaId)).getJSONObject("data")
        val user=d.getJSONObject("user")
        return LoginSession(d.getString("token"),user.optString("userNickName").ifBlank{user.optString("userName","已登录")},user.get("userId").toString())
    }
    suspend fun favorite(id:Long,save:Boolean){request(if(save)listOf("pub","vod","favorite")else listOf("pub","vod","favorite","cancel"),if(save)JSONObject().put("id",id)else JSONObject().put("ids",JSONArray().put(id)))}
    suspend fun favorites(page:Int):CatalogPage {val o=request(listOf("pub","vod","favorite","list"),JSONObject().put("page",page).put("pageSize",20)).getJSONObject("data");return CatalogPage(o.optJSONArray("list").objects().map{movie(it).copy(id=it.getLong("vodId"))},o.optInt("total"),page,20)}
    suspend fun cloudHistory(page:Int):CloudHistoryPage {
        val o=request(listOf("pub","vod","history","list"),JSONObject().put("page",page).put("pageSize",20)).getJSONObject("data")
        return CloudHistoryPage(o.optJSONArray("list").objects().map{WatchRecord(movie(it).copy(id=it.getLong("vodId")),it.optInt("episode"),(it.optDouble("watchDuration",0.0)*1000).toLong(),(it.optDouble("watchPercent",0.0)*1000).toLong(),0)},o.optInt("total"))
    }
    suspend fun syncWatch(record:WatchRecord) {
        require(record.movie.id>0 && record.positionMs>=1000 && record.durationMs>0)
        request(listOf("pub","user","watches","sync"),JSONArray().put(JSONObject()
            .put("id",record.movie.id).put("title",record.movie.title).put("episode",record.episode)
            .put("duration",record.positionMs/1000).put("percent",record.durationMs/1000)
            .put("saveTime",record.updatedAt/1000).put("type","vod")))
    }
    private suspend fun favoriteChannelRows():List<JSONObject> = request(listOf("pub","user","favorites"),JSONObject().put("page",0).put("pageSize",999).put("favoriteType",3)).getJSONObject("data").optJSONArray("list").objects()
    suspend fun favoriteChannel(id:Long,save:Boolean){
        val count=favoriteChannelRows().count{it.optLong("channelId")==id}
        if((save&&count>0)||(!save&&count==0))return
        request(if(save)listOf("pub","user","favorite","save")else listOf("pub","user","favorite","cancel","channel"),JSONObject().put("channelId",id).put("favoriteType",3))
        repeat(6){attempt->
            if(attempt>0)kotlinx.coroutines.delay(1000)
            if(favoriteChannelRows().any{it.optLong("channelId")==id}==save)return
        }
        throw ApiException(-2,"网站尚未确认收藏变更，请稍后刷新")
    }
    suspend fun favoriteChannels():List<Channel> = favoriteChannelRows().map{row->val d=row.getJSONObject("detail");Channel(d.getLong("id"),row.getString("stream_id"),row.optString("title").ifBlank{d.optString("name")},image(row.optString("currentImg")),d.optString("currentTitle"))}.distinctBy{it.id}
    suspend fun liveGroups():List<Pair<Int,String>> { val d=get("v1","pub","live","conditions") as JSONArray;return d.objects().firstOrNull{it.optString("type")=="tv"}?.optJSONObject("data")?.optJSONArray("groups").objects().map{it.optInt("id") to it.optString("title")} }
    fun channel(o:JSONObject)=Channel(o.optLong("id"),o.optString("streamId"),o.optString("title"),image(o.optString("currentImg",o.optString("icon"))),o.optString("currentTitle"))
    suspend fun channels(group:Int=0,order:Int=3,page:Int=1):Pair<List<Channel>,Int>{val o=get("v1","pub","live","list","tv","0","0",order.toString(),group.toString(),page.toString(),"36") as JSONObject;return o.optJSONArray("list").objects().map(::channel) to o.optInt("total")}
    suspend fun liveDetail(channel:Channel,date:String):LiveDetail {val o=get("v1","pub","live","info","tv",channel.id.toString(),channel.streamId,date) as JSONObject;val d=o.getJSONObject("detail");val member=token()!=null && request(listOf("pub","user","info"),JSONObject()).getJSONObject("data").optInt("groupId")==3;val source=if(member)d.optJSONArray("urls").objects().maxByOrNull{it.optInt("type")}?.optString("hls")?.takeIf{it.isNotBlank()}?:d.optString("hls")else d.optString("hls");val signed=if(source.startsWith("https://"))source.toHttpUrl().newBuilder().addQueryParameter("token",RequestSignature.at(now())).build().toString()else source;return LiveDetail(channel,signed,o.optJSONArray("programs").objects().map{Programme(it.getLong("id"),it.optString("title"),it.optString("showTime"),it.optString("start"),it.optString("end"),it.optBoolean("hasVod"),it.optInt("liveType"))},d.optBoolean("favorite"))}
    suspend fun replay(channel:Channel,programme:Programme):String=request(listOf("pub","tv","vod","url"),JSONObject().put("programmeId",programme.id).put("stream_id",channel.streamId)).getJSONObject("data").getString("hls")
}
internal fun JSONArray?.objects():List<JSONObject> = if(this==null)emptyList()else (0 until length()).mapNotNull{optJSONObject(it)}
internal fun JSONArray?.strings():List<String> = if(this==null)emptyList()else (0 until length()).map{optString(it)}.filter{it.isNotBlank()}
