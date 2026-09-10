package com.example.weanimals.adoption.questionnaire.interactor

import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.adoption.questionnaire.repository.AdoptionProfileRepository

class SaveAdoptionProfileInteractor(
    private val repository: AdoptionProfileRepository
) {
    suspend operator fun invoke(profile: AdoptionProfile) = repository.saveProfile(profile)
}
