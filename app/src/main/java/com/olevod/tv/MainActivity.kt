package com.olevod.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OlevodApp(if (BuildConfig.DEBUG) intent.getStringExtra("screen") ?: "home" else "home", BuildConfig.DEBUG && intent.getBooleanExtra("preview",false), initialMovieId=if(BuildConfig.DEBUG&&intent.hasExtra("movieId"))intent.getLongExtra("movieId",0)else null) }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
