package com.example.weanimals.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.weanimals.R

object SystemNotificationHelper {

    private const val TAG = "DEBUG_NOTIFICACOES"

    fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        targetIntent: Intent? = null
    ) {
        // Ensure notification channels exist on Android O+
        NotificationChannelManager.initChannels(context)

        // Check POST_NOTIFICATIONS permission on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Permissão POST_NOTIFICATIONS não foi concedida pelo usuário!")
                return
            }
        }

        val pendingIntent = targetIntent?.let { intent ->
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.clay600))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        pendingIntent?.let { builder.setContentIntent(it) }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d(
                TAG,
                "Notificação emitida com sucesso! ID=$notificationId, Canal=$channelId, Título='$title'"
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Erro ao emitir notificação ID=$notificationId: ${exception.message}", exception)
        }
    }
}
