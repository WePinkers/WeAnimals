package com.example.weanimals.adoption.detail.interactor

import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.repository.AnimalShareRepository

class CreateAnimalSharePdfInteractor(
    private val repository: AnimalShareRepository
) {
    suspend operator fun invoke(details: AnimalDetails, distanceKm: Double?) =
        repository.createPdf(details, distanceKm)
}
