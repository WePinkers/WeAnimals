package com.example.weanimals.adoption.listing.repository

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalEnergyLevel
import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.domain.AnimalStatus
import com.example.weanimals.adoption.listing.domain.Species
import com.example.weanimals.adoption.listing.domain.SpeciesFilter

/** Adds one local card to debuggable builds without changing production data. */
class DemoAnimalRepository(
    private val delegate: AnimalRepository
) : AnimalRepository {

    override suspend fun getAvailableAnimals(
        filter: SpeciesFilter?,
        lastDocument: AnimalPageCursor?,
        limit: Int
    ): Result<AnimalPage> {
        if (lastDocument != null) return delegate.getAvailableAnimals(filter, lastDocument, limit)

        val demoAnimal = demoAnimalFor(filter)
        return delegate.getAvailableAnimals(filter, null, limit).fold(
            onSuccess = { page ->
                val animals = listOfNotNull(demoAnimal)
                    .plus(page.animals)
                    .distinctBy(Animal::id)
                Result.success(page.copy(animals = animals))
            },
            onFailure = {
                // The debug card remains available while Firebase is being configured.
                Result.success(AnimalPage(listOfNotNull(demoAnimal)))
            }
        )
    }

    private fun demoAnimalFor(filter: SpeciesFilter?): Animal? = when (filter) {
        null, SpeciesFilter.ALL, SpeciesFilter.DOGS, SpeciesFilter.NEAREST -> Animal(
            id = DEMO_ANIMAL_ID,
            ngoId = "demo-shelter",
            shelterName = "Abrigo Esperança",
            shelterAddress = "Vila Marlene",
            name = "Canela",
            species = Species.DOG,
            breed = "SRD",
            ageText = "2 anos",
            ageYears = 2,
            size = "Porte médio",
            photoUrl = null,
            characteristics = listOf("Vacinada", "Castrada", "Dócil com crianças"),
            energyLevel = AnimalEnergyLevel.MODERATE,
            independent = true,
            goodWithChildren = true,
            goodWithOtherAnimals = true,
            status = AnimalStatus.AVAILABLE,
            createdAtMillis = System.currentTimeMillis() - THREE_MONTHS_MILLIS,
            latitude = DEMO_SHELTER_LATITUDE,
            longitude = DEMO_SHELTER_LONGITUDE
        )
        SpeciesFilter.CATS -> null
    }

    companion object {
        const val DEMO_ANIMAL_ID = "demo-canela"
        const val DEMO_SHELTER_LATITUDE = -23.5505
        const val DEMO_SHELTER_LONGITUDE = -46.6333
        private const val THREE_MONTHS_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
