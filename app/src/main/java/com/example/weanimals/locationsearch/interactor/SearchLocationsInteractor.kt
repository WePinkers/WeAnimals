package com.example.weanimals.locationsearch.interactor

import com.example.weanimals.locationsearch.repository.LocationRepository

class SearchLocationsInteractor(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(query: String) = repository.searchLocations(query)
}
