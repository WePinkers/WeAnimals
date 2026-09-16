package com.example.weanimals.adoption.confirmation.domain

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile

data class AdoptionCandidate(
    val animal: Animal,
    val profile: AdoptionProfile,
    val matchPercentage: Int
)

class AlreadyAppliedException : IllegalStateException("Application already exists.")
