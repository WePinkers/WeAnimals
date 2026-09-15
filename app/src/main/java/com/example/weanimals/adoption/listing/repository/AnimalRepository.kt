package com.example.weanimals.adoption.listing.repository

import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.domain.SpeciesFilter

interface AnimalRepository {
    suspend fun getAvailableAnimalById(animalId: String): Result<Animal>
    suspend fun getAvailableAnimals(
        filter: SpeciesFilter? = null,
        lastDocument: AnimalPageCursor? = null,
        limit: Int = 10
    ): Result<AnimalPage>
}
