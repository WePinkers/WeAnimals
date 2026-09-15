package com.example.weanimals.adoption.listing.repository

import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAnimalRepositoryTest {

    @Test
    fun exposesDemoDogWhenFirebaseIsUnavailable() = runTest {
        val repository = DemoAnimalRepository(FailingRepository())

        val page = repository.getAvailableAnimals(SpeciesFilter.ALL).getOrThrow()

        assertEquals(listOf(DemoAnimalRepository.DEMO_ANIMAL_ID), page.animals.map { it.id })
    }

    @Test
    fun doesNotPutDemoDogInCatFilter() = runTest {
        val repository = DemoAnimalRepository(FailingRepository())

        val page = repository.getAvailableAnimals(SpeciesFilter.CATS).getOrThrow()

        assertTrue(page.animals.isEmpty())
    }

    private class FailingRepository : AnimalRepository {
        override suspend fun getAvailableAnimalById(animalId: String) =
            Result.failure<com.example.weanimals.adoption.listing.domain.Animal>(IllegalStateException("offline"))
        override suspend fun getAvailableAnimals(
            filter: SpeciesFilter?,
            lastDocument: AnimalPageCursor?,
            limit: Int
        ): Result<AnimalPage> = Result.failure(IllegalStateException("Firebase unavailable"))
    }
}
