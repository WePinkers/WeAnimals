package com.example.weanimals.reporting.locationsearch.interactor

import com.example.weanimals.reporting.locationsearch.repository.LocationRepository

class GetCurrentLocationInteractor(
    private val repository: LocationRepository
) {
    suspend operator fun invoke() = repository.getCurrentLocation()
}
