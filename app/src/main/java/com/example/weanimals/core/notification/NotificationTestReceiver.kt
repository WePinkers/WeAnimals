package com.example.weanimals.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.weanimals.user.adoption.listing.presentation.AdoptionActivity
import com.example.weanimals.user.reporting.chat.presentation.CaseChatActivity
import com.example.weanimals.user.reporting.tracking.presentation.TrackingActivity

class NotificationTestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_TITLE

        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_MESSAGE

        val type = intent.getStringExtra(EXTRA_TYPE)?.lowercase() ?: TYPE_REPORT
        val reportId = intent.getStringExtra(EXTRA_REPORT_ID) ?: DEFAULT_REPORT_ID

        Log.d(
            TAG,
            "Broadcast recebido: título=$title, mensagem=$message, tipo=$type, reportId=$reportId"
        )

        // Ensure notification channels are initialized
        NotificationChannelManager.initChannels(context)

        val channelId = when (type) {
            TYPE_CHAT -> NotificationChannelManager.CHANNEL_CHAT
            TYPE_ADOPTION -> NotificationChannelManager.CHANNEL_ADOPTION
            else -> NotificationChannelManager.CHANNEL_REPORTS
        }

        val targetIntent = when (type) {
            TYPE_CHAT -> CaseChatActivity.newIntent(context, reportId, 4499)
            TYPE_ADOPTION -> Intent(context, AdoptionActivity::class.java)
            else -> TrackingActivity.newIntent(context, reportId)
        }

        val notificationId = (System.currentTimeMillis() % 10000).toInt()

        NotificationDispatcher.dispatchNotification(
            context = context,
            channelId = channelId,
            notificationId = notificationId,
            title = title,
            message = message,
            targetIntent = targetIntent
        )
    }

    companion object {
        private const val TAG = "DEBUG_NOTIFICACOES"
        const val ACTION_TEST_NOTIFICATION = "com.example.weanimals.TEST_NOTIFICATION"

        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_TYPE = "type"
        const val EXTRA_REPORT_ID = "reportId"

        const val TYPE_REPORT = "report"
        const val TYPE_CHAT = "chat"
        const val TYPE_ADOPTION = "adoption"

        private const val DEFAULT_TITLE = "Protocolo #4499 atualizado"
        private const val DEFAULT_MESSAGE = "A equipe de resgate está a caminho do local."
        private const val DEFAULT_REPORT_ID = "report_4487"
    }
}
