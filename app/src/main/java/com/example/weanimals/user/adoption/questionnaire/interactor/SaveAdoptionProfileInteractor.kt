package com.example.weanimals.user.adoption.questionnaire.interactor

import com.example.weanimals.user.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.user.adoption.questionnaire.repository.AdoptionProfileRepository

class SaveAdoptionProfileInteractor(
    private val repository: AdoptionProfileRepository
) {
    suspend operator fun invoke(profile: AdoptionProfile) = repository.saveProfile(profile)
}
