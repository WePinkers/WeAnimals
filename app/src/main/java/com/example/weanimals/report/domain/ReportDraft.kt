package com.example.weanimals.report.domain

data class ReportDraft(
    val animalType: String,
    val urgency: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val photoUri: String?
)
