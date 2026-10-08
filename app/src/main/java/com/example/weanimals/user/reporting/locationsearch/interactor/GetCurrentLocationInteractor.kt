package com.example.weanimals.user.reporting.locationsearch.interactor

import com.example.weanimals.user.reporting.locationsearch.repository.LocationRepository

class GetCurrentLocationInteractor(
    private val repository: LocationRepository
) {
    suspend operator fun invoke() = repository.getCurrentLocation()
}
