package com.example.weanimals.adoption.sent.domain

data class AdoptionSentSummary(
    val animalId: String,
    val animalName: String,
    val shelterName: String,
    val matchPercentage: Int,
    val breed: String = "",
    val ageText: String = "",
    val photoUrl: String? = null
)

enum class AdoptionApplicationStatus(val storageValue: String) {
    PENDING("pending"),
    REVIEWING("reviewing"),
    APPROVED("approved"),
    REJECTED("rejected"),
    VISIT_SCHEDULED("visit_scheduled"),
    ADOPTED("adopted"),
    CANCELLED("cancelled")
}
