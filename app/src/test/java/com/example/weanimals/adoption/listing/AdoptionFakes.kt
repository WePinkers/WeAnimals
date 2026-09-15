package com.example.weanimals.adoption.listing

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.domain.AnimalStatus
import com.example.weanimals.adoption.listing.domain.Species
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import com.example.weanimals.adoption.listing.repository.AnimalRepository
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.repository.UserLocationRepository

fun animal(id: String, latitude: Double? = 0.0, longitude: Double? = 0.0) = Animal(
    id = id,
    ngoId = "ong-1",
    shelterName = "Abrigo",
    shelterAddress = "Endereço",
    name = id,
    species = Species.DOG,
    breed = "SRD",
    ageText = "2 anos",
    ageYears = 2,
    size = "pequeno",
    photoUrl = null,
    characteristics = listOf("vacinado", "castrado"),
    energyLevel = null,
    independent = null,
    goodWithChildren = null,
    goodWithOtherAnimals = null,
    status = AnimalStatus.AVAILABLE,
    createdAtMillis = 1000,
    latitude = latitude,
    longitude = longitude
)

data class TestCursor(val page: Int) : AnimalPageCursor

data class AnimalRequest(val filter: SpeciesFilter?, val cursor: AnimalPageCursor?, val limit: Int)

class FakeAnimalRepository : AnimalRepository {
    val requests = mutableListOf<AnimalRequest>()
    var answer: suspend (AnimalRequest) -> Result<AnimalPage> = { Result.success(AnimalPage(emptyList())) }
    var byIdAnswer: suspend (String) -> Result<Animal> = {
        Result.failure(UnsupportedOperationException("No by-id answer configured."))
    }

    override suspend fun getAvailableAnimalById(animalId: String): Result<Animal> =
        byIdAnswer(animalId)

    override suspend fun getAvailableAnimals(
        filter: SpeciesFilter?,
        lastDocument: AnimalPageCursor?,
        limit: Int
    ): Result<AnimalPage> {
        val request = AnimalRequest(filter, lastDocument, limit)
        requests.add(request)
        return answer(request)
    }
}

class FakeUserLocationRepository(var coordinates: Coordinates? = null) : UserLocationRepository {
    override suspend fun getUserLocation() = coordinates
}
