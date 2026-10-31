package com.example.weanimals.user.adoption.compatibility.repository

import com.example.weanimals.user.adoption.listing.domain.Animal

interface AnimalRecommendationRepository {
    suspend fun getAvailableAnimals(): Result<List<Animal>>
}
