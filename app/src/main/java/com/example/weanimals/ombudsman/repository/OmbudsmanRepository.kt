package com.example.weanimals.ombudsman.repository

import com.example.weanimals.ombudsman.domain.ManifestationStatus
import com.example.weanimals.ombudsman.domain.ManifestationType
import com.example.weanimals.ombudsman.domain.OmbudsmanProtocol
import com.example.weanimals.ombudsman.domain.TimelineEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OmbudsmanRepository {

    private val protocols = mutableListOf(
        OmbudsmanProtocol(
            protocolNumber = "WA-2026-000123",
            type = ManifestationType.DENUNCIA,
            subject = "Suposto golpe / cobrança indevida",
            description = "Relato sobre cobrança indevida em campanha com indícios de fraude.",
            isAnonymous = false,
            dateFormatted = "22/10/2026",
            status = ManifestationStatus.EM_ANALISE,
            timelineEvents = listOf(
                TimelineEvent("Manifestação recebida", "22/10/2026 · 14:32", true),
                TimelineEvent("Em análise pela equipe", "22/10/2026 · 15:10", true),
                TimelineEvent("Aguardando resposta", "—", false)
            ),
            attachmentsCount = 1
        ),
        OmbudsmanProtocol(
            protocolNumber = "WA-2026-000098",
            type = ManifestationType.RECLAMACAO,
            subject = "Problema técnico no app",
            description = "Ocorreu uma falha ao tentar atualizar os dados do perfil.",
            isAnonymous = false,
            dateFormatted = "18/10/2026",
            status = ManifestationStatus.CONCLUIDA,
            timelineEvents = listOf(
                TimelineEvent("Manifestação recebida", "18/10/2026 · 10:15", true),
                TimelineEvent("Análise concluída", "19/10/2026 · 09:30", true)
            ),
            attachmentsCount = 0
        )
    )

    fun getProtocols(): List<OmbudsmanProtocol> = protocols.toList()

    fun getProtocolByNumber(number: String): OmbudsmanProtocol? {
        return protocols.firstOrNull { it.protocolNumber == number }
    }

    fun addProtocol(
        type: ManifestationType,
        subject: String,
        description: String,
        isAnonymous: Boolean,
        attachmentsCount: Int = 0
    ): OmbudsmanProtocol {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        val formattedDate = dateFormat.format(now)
        val formattedTime = timeFormat.format(now)

        val nextNumber = String.format(Locale.US, "%06d", protocols.size + 124)
        val protocolNumber = "WA-2026-$nextNumber"

        val newProtocol = OmbudsmanProtocol(
            protocolNumber = protocolNumber,
            type = type,
            subject = subject,
            description = description,
            isAnonymous = isAnonymous,
            dateFormatted = formattedDate,
            status = ManifestationStatus.EM_ANALISE,
            timelineEvents = listOf(
                TimelineEvent("Manifestação recebida", "$formattedDate · $formattedTime", true),
                TimelineEvent("Em análise pela equipe", "$formattedDate · $formattedTime", true),
                TimelineEvent("Aguardando resposta", "—", false)
            ),
            attachmentsCount = attachmentsCount
        )

        protocols.add(0, newProtocol)
        return newProtocol
    }
}
