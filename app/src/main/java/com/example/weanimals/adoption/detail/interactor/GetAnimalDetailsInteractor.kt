package com.example.weanimals.adoption.detail.interactor

import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.repository.AnimalDetailsRepository
import kotlinx.coroutines.CancellationException

class GetAnimalDetailsInteractor(
    private val repository: AnimalDetailsRepository
) {
    suspend operator fun invoke(animalId: String): Result<AnimalDetails> = try {
        require(animalId.isNotBlank())
        repository.getAnimalDetails(animalId)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
