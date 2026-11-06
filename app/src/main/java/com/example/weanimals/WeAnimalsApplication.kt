package com.example.weanimals

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.weanimals.core.di.AppContainer
import com.example.weanimals.core.notification.NotificationChannelManager
import com.example.weanimals.core.notification.NotificationDispatcher
import com.example.weanimals.core.theme.ThemeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class WeAnimalsApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    private var activeActivityCount = 0
    private var communityNotificationRegistration: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        ThemeManager.initTheme(this)
        NotificationChannelManager.initChannels(this)
        registerActivityLifecycleTracker()
        observeCommunityNotifications()
    }

    private fun observeCommunityNotifications() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            communityNotificationRegistration?.remove()
            communityNotificationRegistration = null

            val user = auth.currentUser
            if (user == null || user.isAnonymous) return@addAuthStateListener

            communityNotificationRegistration = FirebaseFirestore.getInstance()
                .collection("community_notifications")
                .whereEqualTo("recipientId", user.uid)
                .whereEqualTo("read", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    snapshot?.documentChanges
                        ?.filter { change -> change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED }
                        ?.forEach { change ->
                            dispatchCommunityNotification(change.document.id, change.document.data)
                        }
                }
        }
    }

    private fun dispatchCommunityNotification(
        documentId: String,
        data: Map<String, Any>,
    ) {
        val actorName = data["actorName"] as? String ?: "Alguém da Comunidade"
        val message = data["message"] as? String ?: "$actorName mencionou você na Comunidade."
        NotificationDispatcher.dispatchNotification(
            context = this,
            channelId = NotificationChannelManager.CHANNEL_CHAT,
            notificationId = documentId.hashCode() and Int.MAX_VALUE,
            title = "Nova menção na Comunidade",
            message = message,
        )

        FirebaseFirestore.getInstance()
            .collection("community_notifications")
            .document(documentId)
            .update("read", true)
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
