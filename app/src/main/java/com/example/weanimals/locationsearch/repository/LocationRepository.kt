package com.example.weanimals.locationsearch.repository

import com.example.weanimals.locationsearch.domain.LocationDetails

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<LocationDetails>
    suspend fun searchLocations(query: String): Result<List<LocationDetails>>
}
