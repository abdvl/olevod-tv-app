package com.olevod.tv.data

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.olevod.tv.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class WatchRecord(val movie:Movie,val episode:Int,val positionMs:Long,val durationMs:Long,val updatedAt:Long)
object MovieJson {
    fun encode(m:Movie)=JSONObject().put("id",m.id).put("title",m.title).put("image",m.image).put("note",m.note).put("score",m.score).put("category",m.category).put("year",m.year).put("area",m.area).put("vip",m.vip).toString()
    fun decode(value:String):Movie {val o=JSONObject(value);return Movie(o.getLong("id"),o.getString("title"),o.optString("image"),o.optString("note"),o.optString("score"),o.optInt("category"),o.optString("year"),o.optString("area"),o.optBoolean("vip"))}
}
class HistoryStore(context:Context,private val scope:()->String):SQLiteOpenHelper(context,"history.db",null,2) {
    private val _records=MutableStateFlow<List<WatchRecord>>(emptyList())
    val records=_records.asStateFlow()
    override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE watches (scope TEXT NOT NULL, id INTEGER NOT NULL, movie TEXT NOT NULL, episode INTEGER NOT NULL, position INTEGER NOT NULL, duration INTEGER NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(scope,id))")}
    override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int){if(old<2){db.execSQL("ALTER TABLE watches RENAME TO watches_old");onCreate(db);db.execSQL("INSERT INTO watches SELECT 'guest', id, movie, episode, position, duration, updated FROM watches_old");db.execSQL("DROP TABLE watches_old")}}
    private fun read():List<WatchRecord> {val result=mutableListOf<WatchRecord>();readableDatabase.query("watches",null,"scope=?",arrayOf(scope()),null,null,"updated DESC").use{c->while(c.moveToNext()){runCatching{WatchRecord(MovieJson.decode(c.getString(c.getColumnIndexOrThrow("movie"))),c.getInt(c.getColumnIndexOrThrow("episode")),c.getLong(c.getColumnIndexOrThrow("position")),c.getLong(c.getColumnIndexOrThrow("duration")),c.getLong(c.getColumnIndexOrThrow("updated")))}.getOrNull()?.let{result+=it}}};return result}
    suspend fun load()=withContext(Dispatchers.IO){synchronized(this@HistoryStore){_records.value=read()}}
    suspend fun save(movie:Movie,episode:Int,position:Long,duration:Long)=withContext(Dispatchers.IO){
        if(position<1000)return@withContext
        synchronized(this@HistoryStore){writableDatabase.insertWithOnConflict("watches",null,ContentValues().apply{put("scope",scope());put("id",movie.id);put("movie",MovieJson.encode(movie));put("episode",episode);put("position",position);put("duration",duration);put("updated",System.currentTimeMillis())},SQLiteDatabase.CONFLICT_REPLACE);_records.value=read()}
    }
    suspend fun remove(id:Long?)=withContext(Dispatchers.IO){synchronized(this@HistoryStore){writableDatabase.delete("watches",if(id==null)"scope=?" else "scope=? AND id=?",if(id==null)arrayOf(scope()) else arrayOf(scope(),id.toString()));_records.value=read()}}
}
