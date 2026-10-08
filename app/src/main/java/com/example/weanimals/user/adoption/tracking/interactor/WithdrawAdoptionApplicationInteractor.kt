package com.example.weanimals.user.adoption.tracking.interactor

import com.example.weanimals.user.adoption.tracking.repository.AdoptionTrackingRepository

class WithdrawAdoptionApplicationInteractor(private val repository: AdoptionTrackingRepository) {
    suspend operator fun invoke(animalId: String) = repository.withdraw(animalId)
}
