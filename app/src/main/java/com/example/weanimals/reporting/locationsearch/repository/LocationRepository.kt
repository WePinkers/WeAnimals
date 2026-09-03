package com.example.weanimals.reporting.locationsearch.repository

import com.example.weanimals.reporting.locationsearch.domain.LocationDetails

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<LocationDetails>
    suspend fun searchLocations(query: String): Result<List<LocationDetails>>
}
