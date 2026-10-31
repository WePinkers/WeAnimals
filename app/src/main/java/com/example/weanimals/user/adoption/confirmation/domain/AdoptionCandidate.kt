package com.example.weanimals.user.adoption.confirmation.domain

import com.example.weanimals.user.adoption.listing.domain.Animal
import com.example.weanimals.user.adoption.questionnaire.domain.AdoptionProfile

data class AdoptionCandidate(
    val animal: Animal,
    val profile: AdoptionProfile,
    val matchPercentage: Int
)

class AlreadyAppliedException : IllegalStateException("Application already exists.")
