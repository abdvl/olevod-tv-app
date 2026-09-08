package com.olevod.tv.data

import android.content.Context

class RememberedCredentials(val username:String,val password:String) {
    override fun toString()="RememberedCredentials([redacted])"
}

/** Separate encrypted vault; logout only clears SessionStore's session vault. */
interface CredentialVault {
    fun read():RememberedCredentials?
    fun save(username:String,password:String)
    fun clear()
}
class CredentialsStore(context:Context,storeName:String="remembered-login"):CredentialVault {
    private val vault=SessionStore(context,storeName)
    override fun read():RememberedCredentials?=vault.token?.let{RememberedCredentials(vault.name,it)}
    override fun save(username:String,password:String){
        require(username.isNotBlank() && password.isNotBlank())
        vault.save(password,username,"remembered-login")
    }
    override fun clear()=vault.clear(synchronous=true)
}
