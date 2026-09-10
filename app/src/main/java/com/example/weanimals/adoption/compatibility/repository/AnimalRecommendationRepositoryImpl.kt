package com.example.weanimals.adoption.compatibility.repository

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.repository.AnimalRepository
import kotlinx.coroutines.CancellationException

class AnimalRecommendationRepositoryImpl(
    private val animalRepository: AnimalRepository
) : AnimalRecommendationRepository {

    override suspend fun getAvailableAnimals(): Result<List<Animal>> = try {
        val animals = mutableListOf<Animal>()
        var cursor: AnimalPageCursor? = null
        do {
            val page = animalRepository.getAvailableAnimals(
                filter = null,
                lastDocument = cursor,
                limit = PAGE_SIZE
            ).getOrThrow()
            animals += page.animals
            cursor = page.nextPage
        } while (cursor != null && animals.size < MAX_ANIMALS)
        Result.success(animals.distinctBy(Animal::id).take(MAX_ANIMALS))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object {
        const val PAGE_SIZE = 25
        const val MAX_ANIMALS = 200
    }
}
