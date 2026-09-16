package com.example.weanimals.adoption.tracking.interactor

import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.adoption.tracking.repository.AdoptionTrackingRepository

class ObserveAdoptionApplicationInteractor(private val repository: AdoptionTrackingRepository) {
    operator fun invoke(summary: AdoptionSentSummary) = repository.observe(summary)
}
