package com.example.weanimals.locationsearch.interactor

import com.example.weanimals.locationsearch.repository.LocationRepository

class GetCurrentLocationInteractor(
    private val repository: LocationRepository
) {
    suspend operator fun invoke() = repository.getCurrentLocation()
}
