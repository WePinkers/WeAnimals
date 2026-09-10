package com.example.weanimals.adoption.detail.interactor

import com.example.weanimals.adoption.detail.domain.Shelter
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import kotlinx.coroutines.CancellationException

class GetShelterDistanceInteractor(
    private val getUserLocation: GetUserLocationInteractor,
    private val calculateDistance: CalculateDistanceInteractor
) {
    suspend operator fun invoke(shelter: Shelter?): Double? {
        val shelterCoordinates = shelter?.coordinates ?: return null
        return try {
            val userCoordinates = getUserLocation() ?: return null
            calculateDistance(userCoordinates, shelterCoordinates)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}
