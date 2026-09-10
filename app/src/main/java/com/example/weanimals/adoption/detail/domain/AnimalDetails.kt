package com.example.weanimals.adoption.detail.domain

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

data class Shelter(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: Coordinates?
)
