package com.example.weanimals.user.lostandfound.model

data class LostAndFoundChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val requestId: String,
    val senderUserId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
