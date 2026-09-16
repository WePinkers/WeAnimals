package com.example.weanimals.adoption.tracking.interactor

import com.example.weanimals.adoption.tracking.repository.AdoptionTrackingRepository

class WithdrawAdoptionApplicationInteractor(private val repository: AdoptionTrackingRepository) {
    suspend operator fun invoke(animalId: String) = repository.withdraw(animalId)
}
