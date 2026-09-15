package com.example.weanimals.adoption.confirmation.repository

import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.confirmation.domain.DemoApplicationUnavailableException
import com.example.weanimals.adoption.listing.repository.DemoAnimalRepository

class DemoAdoptionApplicationRepository(
    private val delegate: AdoptionApplicationRepository
) : AdoptionApplicationRepository {
    override suspend fun submit(candidate: AdoptionCandidate): Result<Unit> =
        if (candidate.animal.id == DemoAnimalRepository.DEMO_ANIMAL_ID) {
            Result.failure(DemoApplicationUnavailableException())
        } else {
            delegate.submit(candidate)
        }
}
