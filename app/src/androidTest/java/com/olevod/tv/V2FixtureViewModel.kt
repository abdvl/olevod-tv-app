package com.olevod.tv

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.Closeable
import java.io.File

/** A fresh, namespaced AppViewModel for fixture tests; never opens the user's stored account/history. */
internal class V2FixtureViewModel(label: String) : Closeable {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefix = "v2-$label-${System.nanoTime()}"
    private val models = ViewModelStore()
    lateinit var vm: AppViewModel
        private set

    init {
        val app = NamespacedApplication(context, prefix)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            vm = ViewModelProvider(models, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(app) as T
            })[AppViewModel::class.java]
        }
    }

    override fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { models.clear() }
        vm.history.close()
        context.deleteDatabase("$prefix-history.db")
        listOf("search", "session", "remembered-login").forEach { context.deleteSharedPreferences("$prefix-$it") }
    }

    private class NamespacedApplication(base: Context, private val prefix: String) : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences("$prefix-$name", mode)
        override fun getDatabasePath(name: String): File = super.getDatabasePath("$prefix-$name")
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            super.openOrCreateDatabase("$prefix-$name", mode, factory)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            super.openOrCreateDatabase("$prefix-$name", mode, factory, errorHandler)
    }
}
