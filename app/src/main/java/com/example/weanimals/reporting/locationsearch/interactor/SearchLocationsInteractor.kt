package com.example.weanimals.reporting.locationsearch.interactor

import com.example.weanimals.reporting.locationsearch.repository.LocationRepository

class SearchLocationsInteractor(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(query: String) = repository.searchLocations(query)
}
