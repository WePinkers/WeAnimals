package com.example.weanimals.user.adoption.tracking.interactor

import com.example.weanimals.user.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.user.adoption.tracking.repository.AdoptionTrackingRepository

class ObserveAdoptionApplicationInteractor(private val repository: AdoptionTrackingRepository) {
    operator fun invoke(summary: AdoptionSentSummary) = repository.observe(summary)
}
