package com.example.weanimals.adoption.detail.repository

import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.domain.EnergyLevel
import com.example.weanimals.adoption.detail.domain.Shelter
import com.example.weanimals.core.location.domain.Coordinates

class DemoAnimalDetailsRepository(
    private val delegate: AnimalDetailsRepository,
    private val demoAnimalId: String,
    private val demoShelterCoordinates: Coordinates
) : AnimalDetailsRepository {

    override suspend fun getAnimalDetails(animalId: String): Result<AnimalDetails> =
        if (animalId == demoAnimalId) Result.success(demoDetails())
        else delegate.getAnimalDetails(animalId)

    private fun demoDetails() = AnimalDetails(
        id = demoAnimalId,
        name = "Canela",
        photoUrls = emptyList(),
        breed = "SRD",
        ageText = "2 anos",
        size = "Porte médio",
        vaccinated = true,
        neutered = true,
        goodWithChildren = true,
        goodWithOtherAnimals = true,
        energyLevel = EnergyLevel.MODERATE,
        description = "Resgatada em janeiro, dócil com crianças e outros cães. Castrada, vacinada e vermifugada. Ainda está se acostumando com espaços fechados, prefere um lar com quintal.",
        adoptionRequirements = listOf(
            "Maior de 18 anos, com documento de identidade",
            "Visita prévia ao abrigo ou ao local de convivência",
            "Assinatura do termo de responsabilidade"
        ),
        createdAtMillis = System.currentTimeMillis() - THREE_MONTHS_MILLIS,
        shelter = Shelter(
            id = "demo-shelter",
            name = "Abrigo Esperança",
            address = "Vila Marlene",
            coordinates = demoShelterCoordinates
        )
    )

    private companion object {
        const val THREE_MONTHS_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
