package com.example.weanimals.user.ombudsman.domain

enum class ManifestationType {
    RECLAMACAO,
    DENUNCIA,
    SOLICITACAO;

    val displayName: String
        get() = when (this) {
            RECLAMACAO -> "Reclamação"
            DENUNCIA -> "Denúncia"
            SOLICITACAO -> "Solicitação"
        }
}

enum class ManifestationStatus {
    EM_ANALISE,
    AGUARDANDO_INFORMACOES,
    EM_PROVIDENCIA,
    CONCLUIDA;

    val displayName: String
        get() = when (this) {
            EM_ANALISE -> "Em análise"
            AGUARDANDO_INFORMACOES -> "Aguardando informações"
            EM_PROVIDENCIA -> "Em providência"
            CONCLUIDA -> "Concluída"
        }
}

data class TimelineEvent(
    val title: String,
    val subtitle: String,
    val isCompleted: Boolean
)

data class OmbudsmanProtocol(
    val protocolNumber: String,
    val type: ManifestationType,
    val subject: String,
    val description: String,
    val isAnonymous: Boolean,
    val dateFormatted: String,
    val status: ManifestationStatus,
    val timelineEvents: List<TimelineEvent> = emptyList(),
    val attachmentsCount: Int = 0
)
