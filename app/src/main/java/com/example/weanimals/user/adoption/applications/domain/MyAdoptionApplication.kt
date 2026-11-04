package com.example.weanimals.user.adoption.applications.domain

import com.example.weanimals.user.adoption.sent.domain.AdoptionApplicationStatus
import com.example.weanimals.user.adoption.sent.domain.AdoptionSentSummary

data class MyAdoptionApplication(
    val summary: AdoptionSentSummary,
    val status: AdoptionApplicationStatus,
    val submittedAtMillis: Long,
)
