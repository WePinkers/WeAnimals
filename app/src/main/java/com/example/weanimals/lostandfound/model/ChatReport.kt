package com.example.weanimals.lostandfound.model

data class ChatReport(
    val reportId: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val announcementId: String,
    val reporterUserId: String,
    val reportedUserId: String,
    val reason: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var status: String = "PENDING_REVIEW"
)
