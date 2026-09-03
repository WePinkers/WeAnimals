package com.example.weanimals.reporting.report.domain

data class Report(
    val id: String = "",
    val protocolNumber: Int = 0,
    val userId: String = "",
    val animalType: String = ANIMAL_DOG,
    val urgency: String = URGENCY_HIGH,
    val description: String = "",
    val photoUrl: String? = null,
    val photoFileName: String? = null,
    val photoData: ByteArray? = null,
    val hasPhoto: Boolean = false,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = ReportStatus.NEW,
    val triageClassification: String? = null,
    val triageAnswers: Map<String, Boolean> = emptyMap(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val isViewed: Boolean = false
) {
    companion object {
        const val ANIMAL_DOG = "dog"
        const val ANIMAL_CAT = "cat"
        const val ANIMAL_OTHER = "other"

        const val URGENCY_LOW = "low"
        const val URGENCY_MEDIUM = "medium"
        const val URGENCY_HIGH = "high"
    }
}

data class NewReport(
    val animalType: String,
    val urgency: String,
    val description: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUri: String? = null,
    val triageClassification: String? = null,
    val triageAnswers: Map<String, Boolean> = emptyMap()
)

object ReportStatus {
    const val NEW = "new"
    const val UNDER_REVIEW = "under_review"
    const val IN_PROGRESS = "in_progress"
    const val RESCUED = "rescued"
    const val CLOSED = "closed"
}

object ReportProtocol {
    const val FIRST_NUMBER = 4472
}
