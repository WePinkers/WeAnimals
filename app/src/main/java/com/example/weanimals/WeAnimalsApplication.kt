package com.example.weanimals

import android.app.Application
import com.example.weanimals.core.di.AppContainer

class WeAnimalsApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
