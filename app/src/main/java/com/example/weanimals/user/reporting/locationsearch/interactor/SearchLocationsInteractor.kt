package com.example.weanimals.user.reporting.locationsearch.interactor

import com.example.weanimals.user.reporting.locationsearch.repository.LocationRepository

class SearchLocationsInteractor(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(query: String) = repository.searchLocations(query)
}
