package com.example.weanimals.core.notification

import android.content.Context
import android.util.Log
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.report.domain.ReportStatus
import com.example.weanimals.reporting.tracking.presentation.TrackingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class ReportStatusNotificationEnum(
    val statusKey: String,
    val titleFormat: String,
    val defaultMessage: String
) {
    RECEIVED(
        ReportStatus.NEW,
        "Protocolo #%1\$d: Denúncia recebida",
        "Sua denúncia entrou na fila de prioridade e aguarda triagem da equipe."
    ),
    TEAM_ASSIGNED(
        ReportStatus.UNDER_REVIEW,
        "Protocolo #%1\$d: Equipe acionada",
        "Uma equipe de resgate voluntária assumiu o caso."
    ),
    ON_THE_WAY(
        ReportStatus.IN_PROGRESS,
        "Protocolo #%1\$d: Equipe a caminho",
        "Os socorristas estão se deslocando até o endereço indicado."
    ),
    RESCUED(
        ReportStatus.RESCUED,
        "Protocolo #%1\$d: Resgate concluído",
        "O animal foi localizado e resgatado com segurança."
    ),
    SHELTERED(
        ReportStatus.CLOSED,
        "Protocolo #%1\$d: Encaminhado a abrigo",
        "O animal está em segurança e foi acolhido por um abrigo parceiro."
    );

    companion object {
        fun fromKey(key: String): ReportStatusNotificationEnum {
            return entries.firstOrNull { it.statusKey == key } ?: RECEIVED
        }
    }
}

object ReportStatusNotifier {

    private const val TAG = "DEBUG_NOTIFICACOES"
    private var simulationJob: Job? = null

    fun notifyReportCreated(
        context: Context,
        reportId: String,
        protocolNumber: Int,
        animalTypeAndLocation: String
    ) {
        val title = "Denúncia recebida"
        val message = "Protocolo #$protocolNumber: Seu reporte foi registrado com sucesso e está na fila de triagem."
        val notificationId = protocolNumber.coerceAtLeast(1)

        val targetIntent = TrackingActivity.newIntent(context, reportId).apply {
            putExtra("extra_protocol", "#$protocolNumber")
            putExtra("extra_title", animalTypeAndLocation)
            putExtra("extra_urgency", Report.URGENCY_MEDIUM)
        }

        Log.d(TAG, "Notificação Única de Criação Enviada: Protocolo=#$protocolNumber")

        NotificationDispatcher.dispatchNotification(
            context = context,
            channelId = NotificationChannelManager.CHANNEL_REPORTS,
            notificationId = notificationId,
            title = title,
            message = message,
            targetIntent = targetIntent
        )
    }

    fun notifyStatusChange(
        context: Context,
        reportId: String,
        protocolNumber: Int,
        animalTypeAndLocation: String,
        statusEnum: ReportStatusNotificationEnum
    ) {
        val title = String.format(Locale.getDefault(), statusEnum.titleFormat, protocolNumber)
        val message = statusEnum.defaultMessage
        val notificationId = protocolNumber.coerceAtLeast(1)

        val targetIntent = TrackingActivity.newIntent(context, reportId).apply {
            putExtra("extra_protocol", "#$protocolNumber")
            putExtra("extra_title", animalTypeAndLocation)
            putExtra("extra_urgency", Report.URGENCY_MEDIUM)
        }

        Log.d(
            TAG,
            "Atualização de Status: Título='$title', Mensagem='$message', Foreground=${NotificationDispatcher.isAppInForeground}"
        )

        NotificationDispatcher.dispatchNotification(
            context = context,
            channelId = NotificationChannelManager.CHANNEL_REPORTS,
            notificationId = notificationId,
            title = title,
            message = message,
            targetIntent = targetIntent
        )
    }

    fun simulateCaseProgress(
        context: Context,
        scope: CoroutineScope,
        reportId: String = "report_4487",
        protocolNumber: Int = 4499,
        animalTypeAndLocation: String = "Cão — Vila Maria Alta",
        onStatusUpdated: ((String) -> Unit)? = null,
        intervalMillis: Long = 15_000L
    ) {
        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Main) {
            val sequence = listOf(
                ReportStatusNotificationEnum.RECEIVED,
                ReportStatusNotificationEnum.TEAM_ASSIGNED,
                ReportStatusNotificationEnum.ON_THE_WAY,
                ReportStatusNotificationEnum.RESCUED,
                ReportStatusNotificationEnum.SHELTERED
            )

            sequence.forEach { step ->
                onStatusUpdated?.invoke(step.statusKey)
                notifyStatusChange(
                    context = context,
                    reportId = reportId,
                    protocolNumber = protocolNumber,
                    animalTypeAndLocation = animalTypeAndLocation,
                    statusEnum = step
                )
                delay(intervalMillis)
            }
        }
    }

    fun simulateStatusProgression(
        context: Context,
        scope: CoroutineScope,
        reportId: String = "report_4487",
        protocolNumber: Int = 4499,
        animalTypeAndLocation: String = "Cão — Vila Maria Alta",
        intervalMillis: Long = 15_000L
    ) {
        simulateCaseProgress(
            context = context,
            scope = scope,
            reportId = reportId,
            protocolNumber = protocolNumber,
            animalTypeAndLocation = animalTypeAndLocation,
            intervalMillis = intervalMillis
        )
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }
}
