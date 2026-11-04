package com.example.weanimals.user.adoption.detail.domain

import com.example.weanimals.core.location.domain.Coordinates

data class AnimalDetails(
    val id: String,
    val name: String,
    val photoUrls: List<String>,
    val breed: String,
    val ageText: String,
    val size: String,
    val vaccinated: Boolean,
    val neutered: Boolean,
    val goodWithChildren: Boolean?,
    val goodWithOtherAnimals: Boolean?,
    val energyLevel: EnergyLevel?,
    val description: String,
    val adoptionRequirements: List<String>,
    val createdAtMillis: Long,
    val shelter: Shelter?
)

enum class EnergyLevel { LOW, MODERATE, HIGH }

/**
 * Requisitos gerais da plataforma para qualquer adoção.
 * Eles não dependem do cadastro individual feito pelo abrigo.
 */
object AdoptionRequirements {
    val fixed: List<String> = listOf(
        "Maior de 18 anos, com documento de identidade",
        "Visita prévia ao abrigo ou ao local de convivência",
        "Assinatura do termo de responsabilidade"
    )
}

data class Shelter(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: Coordinates?
)
