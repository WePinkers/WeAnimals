package com.example.weanimals.adoption.questionnaire.interactor

import com.example.weanimals.adoption.questionnaire.repository.AdoptionProfileRepository

class GetAdoptionProfileInteractor(
    private val repository: AdoptionProfileRepository
) {
    suspend operator fun invoke() = repository.getProfile()
}
