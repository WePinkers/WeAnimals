package com.example.weanimals.core.location.repository

import com.example.weanimals.core.location.domain.Coordinates

interface UserLocationRepository {
    suspend fun getUserLocation(): Coordinates?
}
