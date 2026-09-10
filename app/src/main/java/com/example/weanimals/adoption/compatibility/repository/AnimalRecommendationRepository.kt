package com.example.weanimals.adoption.compatibility.repository

import com.example.weanimals.adoption.listing.domain.Animal

interface AnimalRecommendationRepository {
    suspend fun getAvailableAnimals(): Result<List<Animal>>
}
