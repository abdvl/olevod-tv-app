package com.olevod.tv.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** AES-GCM vault. The default store holds sessions; credentials use a separate named vault. */
class SessionStore(context:Context,storeName:String="session") {
    private val prefs=context.getSharedPreferences(storeName,Context.MODE_PRIVATE)
    private val alias="olevod-$storeName-v1"
    private fun key():SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (ks.getKey(alias,null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())}.generateKey()
    }
    @Volatile private var cached:JSONObject?=read()
    val token:String? get()=cached?.optString("token")?.takeIf{it.isNotBlank()}
    val accountKey:String get()=cached?.optString("account")?.takeIf{it.isNotBlank()}?:"guest"
    val name:String get()=cached?.optString("name")?:""
    private fun read():JSONObject?=try{
        val raw=prefs.getString("value",null)
        if(raw==null)null else {val parts=raw.split('.');val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));JSONObject(String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8))}
    }catch(e:Exception){prefs.edit().clear().apply();null}
    fun save(token:String,name:String,account:String) {
        val value=JSONObject().put("token",token).put("name",name).put("account",java.security.MessageDigest.getInstance("SHA-256").digest(account.trim().lowercase(java.util.Locale.ROOT).toByteArray()).joinToString(""){"%02x".format(it)})
        val c=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.ENCRYPT_MODE,key())}
        val encrypted=c.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("value",Base64.encodeToString(c.iv,Base64.NO_WRAP)+"."+Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit()){ "无法保存登录会话" };cached=value
    }
    fun clear(){cached=null;prefs.edit().clear().apply()}
}
