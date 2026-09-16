package com.example.weanimals.adoption.checkin.interactor

import com.example.weanimals.adoption.checkin.repository.AdoptionFollowUpRepository

class GetAdoptionFollowUpInteractor(private val repository: AdoptionFollowUpRepository) {
    suspend operator fun invoke(animalId: String) = repository.getFollowUp(animalId)
}
