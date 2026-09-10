package com.example.weanimals.adoption.detail.repository

import com.example.weanimals.adoption.detail.domain.AnimalDetails

interface AnimalDetailsRepository {
    suspend fun getAnimalDetails(animalId: String): Result<AnimalDetails>
}
