package com.example.weanimals.user.adoption.detail.repository

import com.example.weanimals.user.adoption.detail.domain.AnimalDetails
import com.example.weanimals.user.adoption.detail.domain.AnimalShareDocument

interface AnimalShareRepository {
    suspend fun createPdf(
        details: AnimalDetails,
        distanceKm: Double?
    ): Result<AnimalShareDocument>
}
