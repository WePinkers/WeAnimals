package com.example.weanimals.core.location.interactor

import com.example.weanimals.core.location.repository.UserLocationRepository

class GetUserLocationInteractor(private val repository: UserLocationRepository) {
    suspend operator fun invoke() = repository.getUserLocation()
}
