package com.example.weanimals.user.reporting.locationsearch.repository

import com.example.weanimals.user.reporting.locationsearch.domain.LocationDetails

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<LocationDetails>
    suspend fun searchLocations(query: String): Result<List<LocationDetails>>
}
