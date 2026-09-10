package com.example.weanimals.adoption.compatibility.domain

import com.example.weanimals.adoption.listing.domain.Animal

data class AnimalRecommendation(
    val animal: Animal,
    val matchPercentage: Int,
    val distanceKm: Double?
)
