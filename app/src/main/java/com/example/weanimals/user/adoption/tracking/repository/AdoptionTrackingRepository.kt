package com.example.weanimals.user.adoption.tracking.repository

import com.example.weanimals.user.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.user.adoption.tracking.domain.AdoptionApplication
import kotlinx.coroutines.flow.Flow

interface AdoptionTrackingRepository {
    fun observe(summary: AdoptionSentSummary): Flow<Result<AdoptionApplication>>
    suspend fun withdraw(animalId: String): Result<Unit>
}
