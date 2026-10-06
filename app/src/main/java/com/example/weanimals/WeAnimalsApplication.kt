package com.example.weanimals

import android.app.Application
import com.example.weanimals.core.di.AppContainer
import com.example.weanimals.core.theme.ThemeManager

class WeAnimalsApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        ThemeManager.initTheme(this)
    }
}
