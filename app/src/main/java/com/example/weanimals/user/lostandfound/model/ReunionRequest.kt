package com.example.weanimals.user.lostandfound.model

data class ReunionRequest(
    val reunionId: String = java.util.UUID.randomUUID().toString(),
    val announcementId: String,
    val requestId: String,
    val initiatedByUserId: String,
    val confirmingUserId: String,
    var status: String = "PENDING_CONFIRMATION", // PENDING_CONFIRMATION, CONFIRMED, NOT_CONFIRMED
    val createdAt: Long = System.currentTimeMillis(),
    var respondedAt: Long? = null,
    var confirmedAt: Long? = null
)
