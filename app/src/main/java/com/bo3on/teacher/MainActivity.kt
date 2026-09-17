package com.bo3on.teacher

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import com.bo3on.teacher.ui.navigation.TeacherApp
import com.bo3on.teacher.ui.components.Bo3onTheme
import com.bo3on.teacher.ui.viewmodel.TeacherViewModel
import java.util.Locale

class MainActivity : FragmentActivity() {
    private lateinit var vm: TeacherViewModel
    private var stoppedAt = 0L
    override fun attachBaseContext(base: Context) {
        val locale = Locale("ar", "DZ")
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale); config.setLayoutDirection(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm = ViewModelProvider(this)[TeacherViewModel::class.java]
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = settings.theme == "dark" || settings.theme == "system" && isSystemInDarkTheme()
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            DisposableEffect(settings.pinHash.isNotEmpty()) {
                if (settings.pinHash.isNotEmpty()) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                onDispose { }
            }
            Bo3onTheme(settings.theme) { TeacherApp(vm, this) }
        }
    }
    override fun onStop() { stoppedAt = System.currentTimeMillis(); super.onStop() }
    override fun onStart() { super.onStart(); if (::vm.isInitialized && stoppedAt != 0L && System.currentTimeMillis() - stoppedAt > 30_000) vm.lock() }
}
