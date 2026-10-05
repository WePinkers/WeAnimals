package com.example.weanimals.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelManager {
    const val CHANNEL_REPORTS = "channel_reports"
    const val CHANNEL_CHAT = "channel_chat"
    const val CHANNEL_ADOPTION = "channel_adoption"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val reportsChannel = NotificationChannel(
                CHANNEL_REPORTS,
                "Atualizações de Ocorrências",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de triagem, deslocamento e resgate de denúncias"
                enableVibration(true)
            }

            val chatChannel = NotificationChannel(
                CHANNEL_CHAT,
                "Mensagens",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Respostas e alertas do chat de apoio e abrigos"
                enableVibration(true)
            }

            val adoptionChannel = NotificationChannel(
                CHANNEL_ADOPTION,
                "Processo de Adoção",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resultados de candidatura e acompanhamento pós-adoção"
            }

            notificationManager.createNotificationChannels(
                listOf(reportsChannel, chatChannel, adoptionChannel)
            )
        }
    }
}
