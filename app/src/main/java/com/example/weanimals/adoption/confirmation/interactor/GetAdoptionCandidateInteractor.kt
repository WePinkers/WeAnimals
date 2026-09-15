package com.example.weanimals.adoption.confirmation.interactor

import com.example.weanimals.adoption.compatibility.interactor.GetAnimalRecommendationsInteractor
import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.listing.repository.AnimalRepository
import com.example.weanimals.adoption.questionnaire.interactor.GetAdoptionProfileInteractor
import kotlinx.coroutines.CancellationException

class GetAdoptionCandidateInteractor(
    private val getProfile: GetAdoptionProfileInteractor,
    private val animalRepository: AnimalRepository,
    private val getRecommendations: GetAnimalRecommendationsInteractor
) {
    suspend operator fun invoke(animalId: String): Result<AdoptionCandidate> = try {
        val profile = requireNotNull(getProfile().getOrThrow()) { "Adoption profile is missing." }
        val animal = animalRepository.getAvailableAnimalById(animalId).getOrThrow()
        Result.success(AdoptionCandidate(
            animal = animal,
            profile = profile,
            matchPercentage = getRecommendations.matchPercentage(profile, animal)
        ))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
