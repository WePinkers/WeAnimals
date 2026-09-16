package com.example.weanimals.map.overview.interactor

import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import com.example.weanimals.map.overview.domain.PublicOccurrence
import com.example.weanimals.map.overview.repository.PublicOccurrenceRepository
import kotlinx.coroutines.CancellationException

data class NearbyOccurrences(val userLocation: Coordinates?, val occurrences: List<PublicOccurrence>)

class GetNearbyOccurrencesInteractor(
    private val repository: PublicOccurrenceRepository,
    private val getUserLocation: GetUserLocationInteractor,
    private val calculateDistance: CalculateDistanceInteractor
) {
    suspend operator fun invoke(): Result<NearbyOccurrences> = try {
        val location = getUserLocation()
        if (location == null) Result.success(NearbyOccurrences(null, emptyList()))
        else {
            val nearby = repository.getOccurrences().getOrThrow()
                .map { it.copy(distanceKm = calculateDistance(location, it.coordinates)) }
                .filter { (it.distanceKm ?: Double.MAX_VALUE) <= RADIUS_KM }
                .sortedBy { it.distanceKm }
            Result.success(NearbyOccurrences(location, nearby))
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object { const val RADIUS_KM = 2.0 }
}
