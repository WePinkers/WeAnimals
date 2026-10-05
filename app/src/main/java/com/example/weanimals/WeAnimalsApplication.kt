package com.example.weanimals

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.weanimals.core.di.AppContainer
import com.example.weanimals.core.notification.NotificationChannelManager
import com.example.weanimals.core.notification.NotificationDispatcher

class WeAnimalsApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    private var activeActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        NotificationChannelManager.initChannels(this)
        registerActivityLifecycleTracker()
    }

    private fun registerActivityLifecycleTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                activeActivityCount++
                NotificationDispatcher.isAppInForeground = activeActivityCount > 0
            }

            override fun onActivityStopped(activity: Activity) {
                activeActivityCount = (activeActivityCount - 1).coerceAtLeast(0)
                NotificationDispatcher.isAppInForeground = activeActivityCount > 0
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
