package com.example.weanimals.core.notification

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotificationEvent(
    val notificationId: Int,
    val channelId: String,
    val title: String,
    val message: String,
    val targetIntent: Intent? = null
)

object NotificationDispatcher {

    var isAppInForeground: Boolean = false

    private val _inAppNotifications = MutableSharedFlow<InAppNotificationEvent>(
        extraBufferCapacity = 64
    )
    val inAppNotifications: SharedFlow<InAppNotificationEvent> = _inAppNotifications.asSharedFlow()

    fun dispatchNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        targetIntent: Intent? = null
    ) {
        val event = InAppNotificationEvent(
            notificationId = notificationId,
            channelId = channelId,
            title = title,
            message = message,
            targetIntent = targetIntent
        )

        if (isAppInForeground) {
            _inAppNotifications.tryEmit(event)
            SystemNotificationHelper.postNotification(
                context = context,
                channelId = channelId,
                notificationId = notificationId,
                title = title,
                message = message,
                targetIntent = targetIntent
            )
        } else {
            SystemNotificationHelper.postNotification(
                context = context,
                channelId = channelId,
                notificationId = notificationId,
                title = title,
                message = message,
                targetIntent = targetIntent
            )
        }
    }
}
