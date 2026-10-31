package com.example.weanimals.user.reporting.chat.presentation

data class LocalChatMessage(
    val text: String,
    val time: String,
    val type: LocalMessageType
)

enum class LocalMessageType {
    INCOMING,
    OUTGOING,
    SYSTEM
}
