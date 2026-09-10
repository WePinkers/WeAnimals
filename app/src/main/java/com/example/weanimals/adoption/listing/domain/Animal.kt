package com.example.weanimals.adoption.listing.domain

data class Animal(
    val id: String,
    val ngoId: String,
    val shelterName: String,
    val shelterAddress: String,
    val name: String,
    val species: Species,
    val breed: String,
    val ageText: String,
    val ageYears: Int?,
    val size: String,
    val photoUrl: String?,
    val characteristics: List<String>,
    val energyLevel: AnimalEnergyLevel?,
    val independent: Boolean?,
    val goodWithChildren: Boolean?,
    val goodWithOtherAnimals: Boolean?,
    val status: AnimalStatus,
    val createdAtMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val distanceKm: Double? = null
)

enum class Species(val value: String) {
    DOG("cao"), CAT("gato"), OTHER("outro")
}

enum class AnimalStatus(val value: String) {
    AVAILABLE("disponivel"), RESERVED("reservado"), ADOPTED("adotado")
}

enum class AnimalEnergyLevel(val value: String) {
    LOW("baixa"), MODERATE("moderada"), HIGH("alta")
}
