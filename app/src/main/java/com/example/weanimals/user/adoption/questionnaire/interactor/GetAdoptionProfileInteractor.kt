package com.example.weanimals.user.adoption.questionnaire.interactor

import com.example.weanimals.user.adoption.questionnaire.repository.AdoptionProfileRepository

class GetAdoptionProfileInteractor(
    private val repository: AdoptionProfileRepository
) {
    suspend operator fun invoke() = repository.getProfile()
}
