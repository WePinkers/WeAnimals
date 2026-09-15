package com.example.weanimals.adoption.confirmation.interactor

import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.confirmation.repository.AdoptionApplicationRepository

class SubmitAdoptionApplicationInteractor(
    private val repository: AdoptionApplicationRepository
) {
    suspend operator fun invoke(candidate: AdoptionCandidate): Result<Unit> {
        require(candidate.animal.ngoId.isNotBlank()) { "Shelter is missing." }
        require(candidate.matchPercentage in 0..100)
        return repository.submit(candidate)
    }
}
