package com.example.weanimals.user.adoption.compatibility.domain

import com.example.weanimals.user.adoption.listing.domain.Animal

data class AnimalRecommendation(
    val animal: Animal,
    val matchPercentage: Int,
    val matchLevel: MatchLevel,
    val distanceKm: Double?
)
