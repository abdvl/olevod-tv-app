package com.olevod.tv.data

import android.content.Context

class RememberedCredentials(val username:String,val password:String) {
    override fun toString()="RememberedCredentials([redacted])"
}

/** Separate encrypted vault; logout only clears SessionStore's session vault. */
class CredentialsStore(context:Context,storeName:String="remembered-login") {
    private val vault=SessionStore(context,storeName)
    fun read():RememberedCredentials?=vault.token?.let{RememberedCredentials(vault.name,it)}
    fun save(username:String,password:String){
        require(username.isNotBlank() && password.isNotBlank())
        vault.save(password,username,"remembered-login")
    }
    fun clear()=vault.clear()
}
