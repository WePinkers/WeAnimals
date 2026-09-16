package com.example.weanimals.adoption.tracking.domain

import com.example.weanimals.adoption.sent.domain.AdoptionApplicationStatus
import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary

data class AdoptionApplication(
    val summary: AdoptionSentSummary,
    val status: AdoptionApplicationStatus,
    val submittedAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
