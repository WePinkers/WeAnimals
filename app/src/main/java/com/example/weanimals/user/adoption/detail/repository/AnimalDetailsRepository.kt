package com.example.weanimals.user.adoption.detail.repository

import com.example.weanimals.user.adoption.detail.domain.AnimalDetails

interface AnimalDetailsRepository {
    suspend fun getAnimalDetails(animalId: String): Result<AnimalDetails>
}
