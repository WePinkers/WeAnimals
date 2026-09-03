package com.example.weanimals.reporting.chat.domain

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderRole: String = SENDER_USER,
    val senderName: String? = null,
    val text: String = "",
    val createdAtMillis: Long = 0L,
    val isMine: Boolean = false
) {
    companion object {
        const val SENDER_USER = "user"
        const val SENDER_TEAM = "team"
    }
}
