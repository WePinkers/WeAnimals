package com.example.weanimals.user.lostandfound.model

data class AnnouncementReport(
    val reportId: String = java.util.UUID.randomUUID().toString(),
    val reportType: String = "ANNOUNCEMENT",
    val announcementId: String,
    val announcementCategory: String,
    val reporterUserId: String,
    val reportedUserId: String,
    val reason: String,
    val description: String? = null,
    val evidenceUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var status: String = "PENDING_REVIEW"
)
